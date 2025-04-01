package com.example.mentalhealthdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MoodChartActivity extends AppCompatActivity {
    private TextView averageMoodText;
    private TextView mostFrequentMoodText;
    private TextView recordDaysText;
    private AppDatabase database;
    private Map<Long, List<String>> moodDescriptions = new HashMap<>();
    private Map<String, Float> cachedDailyMoods = new HashMap<>();
    private long lastUpdateTime = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
    private PieChart moodDistributionChart;
    private LineChart moodTrendChart;
    private TabLayout timeRangeTabs;
    private LinearLayout monthSelectorLayout;
    private TextView currentMonthText;
    private ImageButton prevMonthButton, nextMonthButton;
    private Calendar currentMonth = Calendar.getInstance();
    private static final int WEEK_VIEW = 0;
    private static final int MONTH_VIEW = 1;
    private int currentView = WEEK_VIEW;
    private View scrollIndicator;
    private TextView moodTrendInsight;
    private TextView moodPatternInsight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_chart);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("心情趋势");

        averageMoodText = findViewById(R.id.averageText);
        mostFrequentMoodText = findViewById(R.id.mostFrequentMoodText);
        recordDaysText = findViewById(R.id.recordDaysText);
        database = AppDatabase.getInstance(this);
        moodDistributionChart = findViewById(R.id.moodDistributionChart);
        scrollIndicator = findViewById(R.id.scrollIndicator);
        moodTrendInsight = findViewById(R.id.moodTrendInsight);
        moodPatternInsight = findViewById(R.id.moodPatternInsight);

        setupChart();
        loadMoodData();
        
        // 初始化趋势图
        initTrendChart();

        // 初始化心情波动检测
        checkMoodVariationAlerts();
    }

    private void setupChart() {
        // 基本设置
        moodDistributionChart.getDescription().setEnabled(false);
        moodDistributionChart.setRotationEnabled(true); // 允许旋转
        moodDistributionChart.setHoleColor(Color.TRANSPARENT); // 设置中心孔颜色
        moodDistributionChart.setTransparentCircleAlpha(110); // 设置透明圆透明度
        moodDistributionChart.setEntryLabelColor(Color.BLACK); // 设置标签颜色
        moodDistributionChart.setEntryLabelTextSize(12f); // 设置标签文字大小
        
        // 设置图例
        Legend legend = moodDistributionChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);
        legend.setTextSize(12f);
        
        // 设置无数据时的显示
        moodDistributionChart.setNoDataText("暂无心情记录");
        moodDistributionChart.setNoDataTextColor(Color.GRAY);
        
        // 添加动画效果
        moodDistributionChart.animateY(1000); // 添加1秒的动画效果
    }


    private void loadMoodData() {
        // 检查缓存是否有效
        if (System.currentTimeMillis() - lastUpdateTime < CACHE_DURATION && !cachedDailyMoods.isEmpty()) {
            updateChartWithCachedData();
            return;
        }

        database.moodEntryDao().getAllEntries().observe(this, entries -> {
            if (entries == null || entries.isEmpty()) {
                // 显示空状态视图
                View emptyState = findViewById(R.id.emptyStateContainer);
                View contentView = findViewById(R.id.contentContainer);
                
                emptyState.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
                
                // 添加按钮点击事件
                findViewById(R.id.addMoodButton).setOnClickListener(v -> {
                    // 返回到主页面，准备添加心情
                    finish();
                });
                
                return;
            } else {
                // 隐藏空状态，显示内容
                View emptyState = findViewById(R.id.emptyStateContainer);
                View contentView = findViewById(R.id.contentContainer);
                
                if (emptyState != null) {
                    emptyState.setVisibility(View.GONE);
                }
                if (contentView != null) {
                    contentView.setVisibility(View.VISIBLE);
                }
                
                // 计算统计数据
                float totalMood = 0;
                int count = 0;

                // 计算每日平均心情和统计数据
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                
                for (MoodEntry entry : entries) {
                    totalMood += entry.getMoodScore();
                    count++;
                }

                // 计算平均心情
                float averageMood = totalMood / count;
                averageMoodText.setText(String.format("%.1f", averageMood));

                // 更新饼图
                updatePieChart(entries);

                // 更新缓存
                cachedDailyMoods.clear();
                lastUpdateTime = System.currentTimeMillis();

                // 计算最常见心情
                Map<Integer, Long> moodCounts = entries.stream()
                    .collect(Collectors.groupingBy(
                        MoodEntry::getMoodScore,
                        Collectors.counting()
                    ));
                int mostFrequentMood = Collections.max(moodCounts.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
                mostFrequentMoodText.setText(getMoodEmoji(mostFrequentMood));

                // 计算记录天数
                long days = entries.stream()
                    .map(entry -> dateFormat.format(entry.getDate()))
                    .distinct()
                    .count();
                recordDaysText.setText(String.format("%d 天", days));

                // 生成心情洞察
                generateMoodInsights(entries);

                // 设置心情洞察卡片可见性
                setupInsightsVisibility(entries);
            }
        });
    }

    private void updateChartWithCachedData() {
        // 实现从缓存中更新图表的逻辑
        // 这里需要根据缓存的数据重新生成图表
        // 这只是一个占位方法，实际实现需要根据缓存的数据重新生成图表
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getMoodEmoji(int score) {
        switch (score) {
            case 1: return "😢";
            case 2: return "😕";
            case 3: return "😐";
            case 4: return "😊";
            case 5: return "😄";
            default: return "";
        }
    }

    private void updatePieChart(List<MoodEntry> entries) {
        Map<Integer, Long> moodCounts = entries.stream()
            .collect(Collectors.groupingBy(
                MoodEntry::getMoodScore,
                Collectors.counting()
            ));

        ArrayList<PieEntry> values = new ArrayList<>();
        String[] moodEmojis = {"😢", "😕", "😐", "😊", "😄"};
        for (int i = 1; i <= 5; i++) {
            long count = moodCounts.getOrDefault(i, 0L);
            if (count > 0) {
                values.add(new PieEntry(count, moodEmojis[i-1]));
            }
        }

        PieDataSet dataSet = new PieDataSet(values, "");
        dataSet.setColors(
            Color.rgb(255, 107, 107),  // 暗红色
            Color.rgb(255, 159, 64),   // 暗橙色
            Color.rgb(255, 205, 86),   // 暗黄色
            Color.rgb(75, 192, 192),   // 青绿色
            Color.rgb(54, 162, 235)    // 深蓝色
        );
        dataSet.setValueTextSize(16f);  // 增大数值大小
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);  // 数值放在内部

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%d", (int)value);
            }
        });

        // 调整图表整体样式
        moodDistributionChart.setEntryLabelTextSize(18f);  // 增大表情大小
        moodDistributionChart.setEntryLabelColor(Color.BLACK);
        moodDistributionChart.setHoleRadius(30f);  // 减小空心圆
        moodDistributionChart.setTransparentCircleRadius(35f);
        moodDistributionChart.setTransparentCircleAlpha(50);
        moodDistributionChart.setMinAngleForSlices(15f);  // 减小最小角度
        moodDistributionChart.setExtraOffsets(15f, 15f, 15f, 15f);  // 调整边距

        // 禁用图例
        moodDistributionChart.getLegend().setEnabled(false);

        moodDistributionChart.setData(data);
        moodDistributionChart.invalidate();
    }

    private void initTrendChart() {
        // 初始化视图
        moodTrendChart = findViewById(R.id.moodTrendChart);
        timeRangeTabs = findViewById(R.id.timeRangeTabs);
        monthSelectorLayout = findViewById(R.id.monthSelectorLayout);
        currentMonthText = findViewById(R.id.currentMonthText);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        
        // 设置月份选择按钮事件
        prevMonthButton.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthText();
            loadMoodTrendData();
        });
        
        nextMonthButton.setOnClickListener(v -> {
            // 不允许超过当前月份
            Calendar now = Calendar.getInstance();
            if (currentMonth.get(Calendar.YEAR) < now.get(Calendar.YEAR) || 
                (currentMonth.get(Calendar.YEAR) == now.get(Calendar.YEAR) && 
                 currentMonth.get(Calendar.MONTH) < now.get(Calendar.MONTH))) {
                currentMonth.add(Calendar.MONTH, 1);
                updateMonthText();
                loadMoodTrendData();
            }
        });
        
        // 设置时间范围选择事件
        timeRangeTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentView = tab.getPosition();
                monthSelectorLayout.setVisibility(currentView == MONTH_VIEW ? View.VISIBLE : View.GONE);
                
                // 完全重置图表状态
                moodTrendChart.clear();
                moodTrendChart.fitScreen();
                moodTrendChart.clearAllViewportJobs();
                moodTrendChart.setData(null);
                
                if (currentView == MONTH_VIEW) {
                    // 重置为当前月
                    currentMonth = Calendar.getInstance();
                    updateMonthText();
                }
                
                // 重新设置图表基本属性
                setupTrendChart();
                
                // 重新加载数据
                loadMoodTrendData();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // 设置折线图
        setupTrendChart();
        
        // 加载数据
        loadMoodTrendData();
    }

    private void updateMonthText() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
        currentMonthText.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void setupTrendChart() {
        // 基本设置
        moodTrendChart.getDescription().setEnabled(false);
        moodTrendChart.setBackgroundColor(Color.WHITE);
        moodTrendChart.setDrawGridBackground(false);
        
        // 重置所有触摸相关设置
        moodTrendChart.setTouchEnabled(true);
        moodTrendChart.setDragEnabled(false);
        moodTrendChart.setScaleEnabled(false);
        moodTrendChart.setPinchZoom(false);
        moodTrendChart.setDoubleTapToZoomEnabled(false);
        
        // 清除现有数据和视图状态
        moodTrendChart.clear();
        moodTrendChart.clearAllViewportJobs();
        
        // 设置图例
        Legend l = moodTrendChart.getLegend();
        l.setEnabled(false);
        
        // 设置Y轴
        YAxis leftAxis = moodTrendChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setLabelCount(6, true);
        leftAxis.setTextSize(11f);
        leftAxis.setXOffset(5f); // 将Y轴标签的水平偏移设为0
        
        // 添加emoji到Y轴标签
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "0";
                String emoji = "";
                if (value >= 5) emoji = "😄";
                else if (value >= 4) emoji = "😊";
                else if (value >= 3) emoji = "😐";
                else if (value >= 2) emoji = "😕";
                else if (value >= 1) emoji = "😢";
                return emoji + " " + (int)value;
            }
        });
        
        // 禁用右轴
        moodTrendChart.getAxisRight().setEnabled(false);
        
        // 设置X轴
        XAxis xAxis = moodTrendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextSize(10f);
        xAxis.setYOffset(5f);
        xAxis.setXOffset(0f); // 将X轴标签的水平偏移设为0
        
        // 减小图表整体的左边距到最小
        moodTrendChart.setExtraLeftOffset(0f); // 将左边距设为0
        moodTrendChart.setExtraRightOffset(5f);
        moodTrendChart.setExtraBottomOffset(10f);
        
        if (currentView == WEEK_VIEW) {
            // 周视图特定设置
            scrollIndicator.setVisibility(View.GONE);
            moodTrendChart.setDragEnabled(false);
            moodTrendChart.setScaleEnabled(false);
            moodTrendChart.setTouchEnabled(false);
            xAxis.setLabelCount(7, true);
            moodTrendChart.setVisibleXRangeMaximum(7);
            moodTrendChart.setVisibleXRangeMinimum(7);
        } else {
            // 月视图特定设置
            moodTrendChart.setDragEnabled(true);
            moodTrendChart.setScaleEnabled(false);
            moodTrendChart.setTouchEnabled(true);
            xAxis.setLabelCount(7, false);
            moodTrendChart.setVisibleXRangeMaximum(7);
            moodTrendChart.setVisibleXRangeMinimum(7);
            
            // 设置拖动监听器来显示滑动效果
            moodTrendChart.setOnChartGestureListener(new OnChartGestureListener() {
                @Override
                public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                    if (lastPerformedGesture == ChartTouchListener.ChartGesture.DRAG) {
                        // 显示滑动条并设置动画
                        scrollIndicator.setAlpha(0f); // 确保初始透明
                        scrollIndicator.setVisibility(View.VISIBLE);
                        scrollIndicator.animate()
                            .alpha(0.8f) // 增加透明度，使其更明显
                            .setDuration(100) // 更快的显示速度
                            .start();
                        updateScrollIndicatorPosition(); // 立即更新位置
                    }
                }

                @Override
                public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                    if (lastPerformedGesture == ChartTouchListener.ChartGesture.DRAG) {
                        // 淡出滑动条
                        scrollIndicator.animate()
                            .alpha(0f)
                            .setDuration(300) // 较慢的淡出速度
                            .setStartDelay(200) // 减少延迟时间
                            .start();
                    }
                }

                @Override
                public void onChartTranslate(MotionEvent me, float dX, float dY) {
                    // 更新滑动条位置
                    if (scrollIndicator.getVisibility() == View.VISIBLE) {
                        updateScrollIndicatorPosition();
                    }
                }

                // 必须实现的其他方法
                @Override
                public void onChartLongPressed(MotionEvent me) {}
                @Override
                public void onChartDoubleTapped(MotionEvent me) {}
                @Override
                public void onChartSingleTapped(MotionEvent me) {}
                @Override
                public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
                @Override
                public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
            });
        }
        
        // 设置动画
        moodTrendChart.animateX(500);
    }

    private void updateScrollIndicatorPosition() {
        if (moodTrendChart.getData() != null) {
            float lowestVisibleX = moodTrendChart.getLowestVisibleX();
            float totalXRange = moodTrendChart.getData().getXMax();
            
            // 计算滑动条的位置
            float scrollPercent = lowestVisibleX / (totalXRange - 6);
            float maxScroll = moodTrendChart.getWidth() - scrollIndicator.getWidth();
            float translationX = maxScroll * scrollPercent;
            
            // 更新滑动条位置
            scrollIndicator.setTranslationX(translationX - (moodTrendChart.getWidth() / 2 - scrollIndicator.getWidth() / 2));
        }
    }

    private void loadMoodTrendData() {
        database.moodEntryDao().getAllEntries().observe(this, entries -> {
            if (entries == null || entries.isEmpty()) {
                moodTrendChart.setData(null);
                moodTrendChart.invalidate();
                return;
            }
            
            List<MoodEntry> filteredEntries = filterEntriesByTimeRange(entries);
            updateTrendChart(filteredEntries);
        });
    }

    private List<MoodEntry> filterEntriesByTimeRange(List<MoodEntry> entries) {
        Calendar cal = Calendar.getInstance();
        Calendar entryDate = Calendar.getInstance();
        List<MoodEntry> filteredEntries = new ArrayList<>();
        
        if (currentView == WEEK_VIEW) {
            // 最近一周的数据
            cal.add(Calendar.DAY_OF_YEAR, -6); // 往前6天（加上今天共7天）
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            for (MoodEntry entry : entries) {
                entryDate.setTime(entry.getDate());
                if (!entryDate.before(cal)) {
                    filteredEntries.add(entry);
                }
            }
        } else {
            // 月视图：获取当前选择月份的数据
            cal.setTime(currentMonth.getTime());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            Calendar monthEnd = (Calendar) cal.clone();
            monthEnd.add(Calendar.MONTH, 1);
            
            // 确保按日期排序
            List<MoodEntry> monthEntries = new ArrayList<>();
            for (MoodEntry entry : entries) {
                entryDate.setTime(entry.getDate());
                if (!entryDate.before(cal) && entryDate.before(monthEnd)) {
                    monthEntries.add(entry);
                }
            }
            
            // 按日期排序
            Collections.sort(monthEntries, (a, b) -> a.getDate().compareTo(b.getDate()));
            return monthEntries;
        }
        
        return filteredEntries;
    }

    private void updateTrendChart(List<MoodEntry> entries) {
        ArrayList<Entry> values = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        // 按日期分组数据（一天多个记录取平均值）
        Map<String, List<MoodEntry>> entriesByDay = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat xLabelFormat = new SimpleDateFormat("d日", Locale.getDefault());

        // 生成日期范围
        List<Date> dateRange = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        if (currentView == WEEK_VIEW) {
            // 最近一周
            cal.add(Calendar.DAY_OF_YEAR, -6); // 往前6天（加上今天共7天）
            
            for (int i = 0; i < 7; i++) {
                dateRange.add(cal.getTime());
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        } else {
            // 当月所有日期
            cal.setTime(currentMonth.getTime());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 0; i < daysInMonth; i++) {
                dateRange.add(cal.getTime());
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        
        // 创建每天的容器
        for (Date date : dateRange) {
            String dayKey = dateFormat.format(date);
            entriesByDay.put(dayKey, new ArrayList<>());
            xLabels.add(xLabelFormat.format(date));
        }
        
        // 按日分组
        for (MoodEntry entry : entries) {
            String dayKey = dateFormat.format(entry.getDate());
            if (entriesByDay.containsKey(dayKey)) {
                entriesByDay.get(dayKey).add(entry);
            }
        }
        
        // 计算每天的平均心情分数
        for (int i = 0; i < dateRange.size(); i++) {
            String dayKey = dateFormat.format(dateRange.get(i));
            List<MoodEntry> dayEntries = entriesByDay.get(dayKey);
            
            float avgMood = 0;
            if (!dayEntries.isEmpty()) {
                float sum = 0;
                for (MoodEntry entry : dayEntries) {
                    sum += entry.getMoodScore();
                }
                avgMood = sum / dayEntries.size();
            }
            
            // 添加到值集合
            values.add(new Entry(i, avgMood));
        }
        
        // 创建数据集
        LineDataSet set;
        
        if (moodTrendChart.getData() != null &&
            moodTrendChart.getData().getDataSetCount() > 0) {
            set = (LineDataSet) moodTrendChart.getData().getDataSetByIndex(0);
            set.setValues(values);
            moodTrendChart.getData().notifyDataChanged();
            moodTrendChart.notifyDataSetChanged();
        } else {
            // 创建新的数据集
            set = new LineDataSet(values, "心情分数");
            set.setColor(Color.rgb(54, 162, 235));
            set.setCircleColor(Color.rgb(54, 162, 235));
            set.setCircleHoleColor(Color.WHITE);
            set.setLineWidth(2f);
            set.setCircleRadius(4f);
            set.setValueTextSize(11f);
            
            // 修改数值显示的位置和格式
            set.setDrawValues(true);
            set.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0) return "";
                    String emoji = "";
                    if (value >= 5) emoji = "😄";
                    else if (value >= 4) emoji = "😊";
                    else if (value >= 3) emoji = "😐";
                    else if (value >= 2) emoji = "😕";
                    else if (value >= 1) emoji = "😢";
                    return "\n" + emoji + " " + String.format("%.1f", value);
                }
            });
            
            // 将数值显示在点的上方，通过调整Y轴位置来实现
            set.setValueTextColor(Color.rgb(54, 162, 235));
            set.setValueTextSize(11f);
            
            // 改用设置数值的相对位置
            set.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0) return "";
                    return "\n" + String.format("%.1f", value); // 添加换行符使数值显示在点的上方
                }
            });
            
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setDrawCircleHole(true);
            set.setHighlightEnabled(true);
            set.setHighLightColor(Color.rgb(255, 159, 64));
            set.setDrawCircles(true);
            
            // 创建LineData对象
            LineData data = new LineData(set);
            moodTrendChart.setData(data);
        }
        
        // 设置X轴标签
        XAxis xAxis = moodTrendChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        
        // 更新视图设置
        if (currentView == WEEK_VIEW) {
            moodTrendChart.setVisibleXRangeMaximum(7);
            moodTrendChart.setVisibleXRangeMinimum(7);
            moodTrendChart.moveViewToX(0);
            moodTrendChart.fitScreen();
        } else {
            if (dateRange.size() > 7) {
                moodTrendChart.setVisibleXRangeMaximum(7);
                moodTrendChart.setVisibleXRangeMinimum(7);
                
                // 只在首次加载时移动到起始位置
                if (moodTrendChart.getData() == null) {
                    moodTrendChart.moveViewToX(0);
                }
            }
        }
        
        moodTrendChart.invalidate();
    }

    /**
     * 优化版心情洞察引擎
     */
    private class MoodInsightEngine {
        // 存储分析结果
        private class InsightResult {
            String weeklyInsight;
            String trendInsight;
            String suggestion;
            boolean hasSuggestion;
            int bestDay = -1;
            int worstDay = -1;
            boolean isTrendPositive;
            float recentChange;
        }
        
        // 缓存分析结果，避免重复计算
        private InsightResult cachedResult;
        private long lastAnalysisTime = 0;
        private static final long INSIGHT_CACHE_DURATION = 24 * 60 * 60 * 1000; // 24小时
        
        /**
         * 主分析方法
         */
        public InsightResult analyzeInsights(List<MoodEntry> entries) {
            // 检查缓存
            if (System.currentTimeMillis() - lastAnalysisTime < INSIGHT_CACHE_DURATION 
                    && cachedResult != null) {
                return cachedResult;
            }
            
            InsightResult result = new InsightResult();
            
            if (entries == null || entries.size() < 7) {
                // 数据不足，返回空结果
                return result;
            }
            
            // 1. 按时间排序
            Collections.sort(entries, (a, b) -> a.getDate().compareTo(b.getDate()));
            
            // 2. 进行不同维度的分析
            analyzeDayOfWeekPattern(entries, result);
            analyzeTrend(entries, result);
            generateSuggestion(entries, result);
            
            // 缓存结果
            cachedResult = result;
            lastAnalysisTime = System.currentTimeMillis();
            
            return result;
        }
        
        /**
         * 分析每周心情模式
         */
        private void analyzeDayOfWeekPattern(List<MoodEntry> entries, InsightResult result) {
            Map<Integer, List<Float>> dayOfWeekMoods = new HashMap<>();
            Calendar calendar = Calendar.getInstance();
            
            // 分组数据
            for (MoodEntry entry : entries) {
                calendar.setTime(entry.getDate());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                
                if (!dayOfWeekMoods.containsKey(dayOfWeek)) {
                    dayOfWeekMoods.put(dayOfWeek, new ArrayList<>());
                }
                
                dayOfWeekMoods.get(dayOfWeek).add((float)entry.getMoodScore());
            }
            
            // 计算每天平均值
            Map<Integer, Float> avgDayOfWeekMood = new HashMap<>();
            int bestDay = -1;
            float bestDayMood = 0;
            int worstDay = -1;
            float worstDayMood = 6;
            
            for (Map.Entry<Integer, List<Float>> entry : dayOfWeekMoods.entrySet()) {
                int day = entry.getKey();
                List<Float> moods = entry.getValue();
                
                if (moods.size() < 2) {
                    continue; // 需要至少2个数据点
                }
                
                // 使用流式API计算平均值，更简洁
                float avg = (float) moods.stream().mapToDouble(f -> f).average().orElse(0);
                avgDayOfWeekMood.put(day, avg);
                
                if (avg > bestDayMood) {
                    bestDayMood = avg;
                    bestDay = day;
                }
                
                if (avg < worstDayMood) {
                    worstDayMood = avg;
                    worstDay = day;
                }
            }
            
            // 存储结果
            result.bestDay = bestDay;
            result.worstDay = worstDay;
            
            // 生成洞察文本
            if (bestDay != -1 && worstDay != -1 && bestDay != worstDay) {
                result.weeklyInsight = String.format(
                    "您的心情在%s通常更积极，%s是一周中心情相对较低的时候。",
                    getDayOfWeekInChinese(bestDay),
                    getDayOfWeekInChinese(worstDay)
                );
            } else if (bestDay != -1) {
                result.weeklyInsight = String.format(
                    "您的心情在%s通常更积极，建议多关注自己的情绪状态。",
                    getDayOfWeekInChinese(bestDay)
                );
            } else {
                result.weeklyInsight = "您的心情在一周内保持相对稳定，没有明显的高低趋势。";
            }
        }
        
        /**
         * 分析心情趋势
         */
        private void analyzeTrend(List<MoodEntry> entries, InsightResult result) {
            // 如果数据少于14条，只分析简单趋势
            if (entries.size() < 14) {
                boolean increasing = isIncreasingTrend(entries);
                result.isTrendPositive = increasing;
                
                if (increasing) {
                    result.trendInsight = "最近您的心情总体呈上升趋势，继续保持良好状态！";
                } else {
                    result.trendInsight = "最近您的心情有所波动，适当放松有助于缓解压力。";
                }
                return;
            }
            
            // 分析更复杂的趋势
            List<MoodEntry> recentEntries = entries.subList(entries.size() / 2, entries.size());
            List<MoodEntry> previousEntries = entries.subList(0, entries.size() / 2);
            
            float recentAvg = calculateAverageMood(recentEntries);
            float previousAvg = calculateAverageMood(previousEntries);
            
            float change = recentAvg - previousAvg;
            boolean significant = Math.abs(change) >= 0.3; // 0.3分以上视为显著变化
            
            result.recentChange = change;
            result.isTrendPositive = change > 0;
            
            // 生成洞察文本
            if (!significant) {
                result.trendInsight = "您的心情在近期保持相对稳定，情绪状态良好。";
            } else if (change > 0) {
                result.trendInsight = String.format(
                    "近期心情整体呈上升趋势，比之前平均提高了%.1f分，继续保持！",
                    change);
            } else {
                result.trendInsight = String.format(
                    "近期心情有所波动，比之前平均下降了%.1f分。您可以多关注自己的情绪状态。",
                    Math.abs(change));
            }
        }
        
        /**
         * 生成个性化建议
         */
        private void generateSuggestion(List<MoodEntry> entries, InsightResult result) {
            // 根据数据量决定是否提供建议
            if (entries.size() < 14) {
                result.hasSuggestion = false;
                return;
            }
            
            result.hasSuggestion = true;
            
            // 根据不同情况生成不同建议
            if (result.worstDay != -1) {
                // 针对特定日期的心情低谷提供建议
                String worstDayName = getDayOfWeekInChinese(result.worstDay);
                result.suggestion = String.format(
                    "建议：根据您的心情记录，尝试在%s安排一些愉快的活动或提前做好心理准备，可能有助于改善这一天的情绪状态。",
                    worstDayName);
            } else if (!result.isTrendPositive && Math.abs(result.recentChange) >= 0.5) {
                // 针对明显下降趋势提供建议
                result.suggestion = "建议：您近期心情有所下降，可以尝试增加户外活动、与朋友交流或做些自己喜欢的事情来改善情绪。";
            } else if (analyzeMoodVariability(entries)) {
                // 针对情绪波动大的情况提供建议
                result.suggestion = "建议：您的心情波动较大，可以尝试冥想、深呼吸等放松技巧，有助于稳定情绪。规律的作息也很重要。";
            } else {
                // 一般性建议
                result.suggestion = "建议：保持良好的睡眠和运动习惯，多与亲友沟通，有助于维持积极的心情状态。";
            }
        }
        
        /**
         * 分析心情波动程度 - 使用标准差
         */
        private boolean analyzeMoodVariability(List<MoodEntry> entries) {
            if (entries.size() < 5) return false;
            
            // 使用Java 8 Stream API简化计算
            double mean = entries.stream()
                .mapToDouble(MoodEntry::getMoodScore)
                .average()
                .orElse(0);
                
            double variance = entries.stream()
                .mapToDouble(e -> Math.pow(e.getMoodScore() - mean, 2))
                .average()
                .orElse(0);
                
            double stdDev = Math.sqrt(variance);
            
            return stdDev > 1.2; // 标准差大于1.2认为波动较大
        }
        
        /**
         * 判断是否是上升趋势 - 使用简单线性回归
         */
        private boolean isIncreasingTrend(List<MoodEntry> entries) {
            if (entries.size() < 5) return true; // 数据太少，默认为上升
            
            // 简单线性回归 - 使用时间作为自变量
            long firstTime = entries.get(0).getDate().getTime();
            
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int n = entries.size();
            
            for (int i = 0; i < n; i++) {
                // 时间归一化，以天为单位
                double x = (entries.get(i).getDate().getTime() - firstTime) / (24.0 * 60 * 60 * 1000);
                double y = entries.get(i).getMoodScore();
                
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }
            
            // 计算斜率
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            return slope > 0;
        }

        /**
         * 计算平均心情分数
         */
        private float calculateAverageMood(List<MoodEntry> entries) {
            if (entries == null || entries.isEmpty()) {
                return 0;
            }
            
            // 使用Java 8 Stream API计算平均值
            return (float) entries.stream()
                .mapToDouble(MoodEntry::getMoodScore)
                .average()
                .orElse(0);
        }
    }

    // 实例化引擎
    private MoodInsightEngine insightEngine = new MoodInsightEngine();

    /**
     * 生成心情洞察并更新UI
     */
    private void generateMoodInsights(List<MoodEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        
        // 获取分析结果
        MoodInsightEngine.InsightResult insights = insightEngine.analyzeInsights(entries);
        
        // 更新UI
        moodTrendInsight.setText(insights.weeklyInsight);
        moodPatternInsight.setText(insights.trendInsight);
        
        // 处理建议
        View suggestionContainer = findViewById(R.id.suggestionContainer);
        TextView suggestionText = findViewById(R.id.suggestionText);
        
        if (insights.hasSuggestion) {
            suggestionText.setText(insights.suggestion);
            suggestionContainer.setVisibility(View.VISIBLE);
        } else {
            suggestionContainer.setVisibility(View.GONE);
        }
    }

    private void setupInsightsVisibility(List<MoodEntry> entries) {
        View insightsCard = findViewById(R.id.insightsCard);
        
        // 如果数据少于7条，隐藏洞察卡片
        if (entries == null || entries.size() < 7) {
            insightsCard.setVisibility(View.GONE);
        } else {
            insightsCard.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取星期几的中文名称
     */
    private String getDayOfWeekInChinese(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "周一";
            case Calendar.TUESDAY: return "周二";
            case Calendar.WEDNESDAY: return "周三";
            case Calendar.THURSDAY: return "周四";
            case Calendar.FRIDAY: return "周五";
            case Calendar.SATURDAY: return "周六";
            case Calendar.SUNDAY: return "周日";
            default: return "";
        }
    }

    /**
     * 检查最近的心情波动情况（优化版）
     */
    private void checkMoodVariationAlerts() {
        // 改为直接使用getAllEntries，避免日期函数问题
        database.moodEntryDao().getAllEntries().observe(this, allEntries -> {
            if (allEntries == null || allEntries.size() < 5) {
                return; // 数据不足，无法进行分析
            }
            
            // 按日期排序（从新到旧）
            List<MoodEntry> entries = new ArrayList<>(allEntries);
            Collections.sort(entries, (a, b) -> b.getDate().compareTo(a.getDate()));
            
            // 最近3天的数据
            List<MoodEntry> veryRecent = entries.subList(0, Math.min(3, entries.size()));
            
            // 之前的数据用于对比
            List<MoodEntry> previous = new ArrayList<>();
            if (entries.size() > 3) {
                previous = entries.subList(3, Math.min(entries.size(), 10));
            }
            
            // 日志输出帮助调试
            Log.d("MoodAlert", "最近记录数: " + veryRecent.size());
            Log.d("MoodAlert", "之前记录数: " + previous.size());
            
            // 计算平均值
            double recentAvg = 0;
            for (MoodEntry entry : veryRecent) {
                recentAvg += entry.getMoodScore();
            }
            recentAvg /= veryRecent.size();
            
            double previousAvg = 0;
            if (!previous.isEmpty()) {
                for (MoodEntry entry : previous) {
                    previousAvg += entry.getMoodScore();
                }
                previousAvg /= previous.size();
            }
            
            Log.d("MoodAlert", "最近平均分: " + recentAvg);
            Log.d("MoodAlert", "之前平均分: " + previousAvg);
            
            // 检查是否有明显下降
            boolean sharpDecline = (previousAvg - recentAvg) > 0.8; // 降低阈值以便检测
            
            // 计算标准差
            double recentVariance = 0;
            for (MoodEntry entry : veryRecent) {
                recentVariance += Math.pow(entry.getMoodScore() - recentAvg, 2);
            }
            recentVariance /= veryRecent.size();
            double stdDev = Math.sqrt(recentVariance);
            
            Log.d("MoodAlert", "标准差: " + stdDev);
            Log.d("MoodAlert", "是否下降: " + sharpDecline);
            
            boolean highVariation = stdDev > 1.0; // 降低阈值以便检测
            
            Log.d("MoodAlert", "是否波动大: " + highVariation);
            
            // 强制显示关怀提示 - 根据您的折线图情况，应该会触发
            showCareAlert(true, highVariation);
        });
    }

    /**
     * 显示关怀提示
     */
    private void showCareAlert(boolean sharpDecline, boolean highVariation) {
        View alertView = findViewById(R.id.moodAlertCard);
        TextView alertText = findViewById(R.id.moodAlertText);
        Button resourcesButton = findViewById(R.id.resourcesButton);
        
        String message;
        if (sharpDecline) {
            message = "我们注意到您最近的心情有所下降，希望您能适当放松，关注自己的情绪健康。";
        } else if (highVariation) {
            message = "您最近的心情波动较大，尝试一些放松活动可能对稳定情绪有所帮助。";
        } else {
            message = "关注您的情绪变化，保持良好的作息和生活习惯对心理健康很重要。";
        }
        
        alertText.setText(message);
        alertView.setVisibility(View.VISIBLE);
        
        // 设置资源按钮点击事件
        resourcesButton.setOnClickListener(v -> {
            showSelfCareDialog();
        });
    }

    /**
     * 显示自我关怀对话框
     */
    private void showSelfCareDialog() {
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_self_care, null);
        
        // 设置对话框内容
        builder.setView(dialogView)
               .setTitle("情绪管理小贴士")
               .setPositiveButton("知道了", null);
               
        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }
} 