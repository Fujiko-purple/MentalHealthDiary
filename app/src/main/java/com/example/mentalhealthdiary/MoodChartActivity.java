package com.example.mentalhealthdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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
     * 生成心情洞察
     */
    private void generateMoodInsights(List<MoodEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        
        // 按照日期对心情记录进行排序
        entries.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        
        // 找出星期几的心情规律
        Map<Integer, List<Float>> dayOfWeekMoods = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        
        for (MoodEntry entry : entries) {
            calendar.setTime(entry.getDate());
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            
            if (!dayOfWeekMoods.containsKey(dayOfWeek)) {
                dayOfWeekMoods.put(dayOfWeek, new ArrayList<>());
            }
            
            dayOfWeekMoods.get(dayOfWeek).add((float)entry.getMoodScore());
        }
        
        // 计算每个星期几的平均心情
        Map<Integer, Float> avgDayOfWeekMood = new HashMap<>();
        int bestDay = -1;
        float bestDayMood = 0;
        int worstDay = -1;
        float worstDayMood = 6;
        
        for (Map.Entry<Integer, List<Float>> entry : dayOfWeekMoods.entrySet()) {
            int day = entry.getKey();
            List<Float> moods = entry.getValue();
            
            if (moods.size() < 2) {
                continue; // 需要至少2个数据点才有效
            }
            
            float avg = 0;
            for (Float mood : moods) {
                avg += mood;
            }
            avg /= moods.size();
            
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
        
        // 生成星期几心情洞察
        generateDayOfWeekInsight(bestDay, worstDay);
        
        // 分析心情趋势 (上升/下降/稳定)
        if (entries.size() >= 7) {
            generateTrendInsight(entries);
        }

        // 优化心情洞察的建议功能
        generateSuggestion(entries, worstDay);
    }

    /**
     * 生成每周心情洞察
     */
    private void generateDayOfWeekInsight(int bestDay, int worstDay) {
        if (bestDay == -1 || worstDay == -1) {
            moodTrendInsight.setText("记录更多数据，我们将为您提供每周心情规律分析。");
            return;
        }
        
        String bestDayName = getDayOfWeekInChinese(bestDay);
        String worstDayName = getDayOfWeekInChinese(worstDay);
        
        StringBuilder insight = new StringBuilder();
        insight.append("您的心情在");
        
        if (bestDay == Calendar.SATURDAY || bestDay == Calendar.SUNDAY) {
            insight.append("周末");
        } else {
            insight.append(bestDayName);
        }
        
        insight.append("通常更积极");
        
        if (worstDay != -1) {
            insight.append("，").append(worstDayName).append("是一周中心情较低落的时候");
        }
        
        insight.append("。");
        moodTrendInsight.setText(insight.toString());
    }

    /**
     * 生成心情趋势洞察
     */
    private void generateTrendInsight(List<MoodEntry> entries) {
        // 获取最近30天和之前30天的数据
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date thirtyDaysAgo = calendar.getTime();
        
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date sixtyDaysAgo = calendar.getTime();
        
        List<MoodEntry> recentEntries = new ArrayList<>();
        List<MoodEntry> previousEntries = new ArrayList<>();
        
        for (MoodEntry entry : entries) {
            if (entry.getDate().after(thirtyDaysAgo)) {
                recentEntries.add(entry);
            } else if (entry.getDate().after(sixtyDaysAgo)) {
                previousEntries.add(entry);
            }
        }
        
        if (recentEntries.isEmpty()) {
            moodPatternInsight.setText("继续记录您的心情，我们将为您分析心情变化趋势。");
            return;
        }
        
        // 计算平均心情
        float recentAvg = calculateAverageMood(recentEntries);
        
        // 如果有上一阶段数据，比较变化
        if (!previousEntries.isEmpty()) {
            float previousAvg = calculateAverageMood(previousEntries);
            float change = recentAvg - previousAvg;
            
            if (Math.abs(change) < 0.2) {
                moodPatternInsight.setText("近期您的心情整体保持稳定。");
            } else if (change > 0) {
                moodPatternInsight.setText(String.format(
                    "近30天心情整体呈上升趋势，比前30天平均提高了%.1f分。",
                    change));
            } else {
                moodPatternInsight.setText(String.format(
                    "近30天心情有所波动，比前30天平均下降了%.1f分。您可以多关注自己的情绪状态。",
                    Math.abs(change)));
            }
        } else {
            // 只有最近的数据
            boolean increasing = isIncreasingTrend(recentEntries);
            
            if (increasing) {
                moodPatternInsight.setText("最近您的心情总体呈上升趋势，继续保持良好状态！");
            } else {
                moodPatternInsight.setText("最近您的心情有所波动，适当放松有助于缓解压力。");
            }
        }
    }

    /**
     * 计算平均心情值
     */
    private float calculateAverageMood(List<MoodEntry> entries) {
        if (entries.isEmpty()) return 0;
        
        float sum = 0;
        for (MoodEntry entry : entries) {
            sum += entry.getMoodScore();
        }
        
        return sum / entries.size();
    }

    /**
     * 判断是否是上升趋势
     */
    private boolean isIncreasingTrend(List<MoodEntry> entries) {
        if (entries.size() < 5) return true; // 数据太少，默认为上升
        
        // 简单线性回归
        int n = entries.size();
        float sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            float x = i;
            float y = entries.get(i).getMoodScore();
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        float slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope > 0;
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

    private void setupInsightsVisibility(List<MoodEntry> entries) {
        View insightsCard = findViewById(R.id.insightsCard);
        
        // 如果数据少于7条，隐藏洞察卡片
        if (entries == null || entries.size() < 7) {
            insightsCard.setVisibility(View.GONE);
        } else {
            insightsCard.setVisibility(View.VISIBLE);
        }
    }

    // 优化心情洞察的建议功能
    private void generateSuggestion(List<MoodEntry> entries, int worstDay) {
        View suggestionContainer = findViewById(R.id.suggestionContainer);
        TextView suggestionText = findViewById(R.id.suggestionText);
        
        // 如果数据不足，不显示建议
        if (entries.size() < 14) {
            suggestionContainer.setVisibility(View.GONE);
            return;
        }
        
        String suggestion = "";
        
        // 根据心情数据生成个性化建议
        if (worstDay != -1) {
            String worstDayName = getDayOfWeekInChinese(worstDay);
            suggestion = String.format("建议：根据您的心情记录，尝试在%s安排一些愉快的活动，可能有助于提升情绪。", worstDayName);
        } else {
            // 分析心情波动
            boolean highVariability = analyzeMoodVariability(entries);
            if (highVariability) {
                suggestion = "建议：您的心情波动较大，可以尝试冥想或深呼吸等放松技巧，有助于稳定情绪。";
            } else {
                suggestion = "建议：保持良好的睡眠和运动习惯，有助于维持积极的心情状态。";
            }
        }
        
        suggestionText.setText(suggestion);
        suggestionContainer.setVisibility(View.VISIBLE);
    }

    // 分析心情波动程度
    private boolean analyzeMoodVariability(List<MoodEntry> entries) {
        if (entries.size() < 5) return false;
        
        // 计算标准差
        float mean = 0;
        for (MoodEntry entry : entries) {
            mean += entry.getMoodScore();
        }
        mean /= entries.size();
        
        float variance = 0;
        for (MoodEntry entry : entries) {
            variance += Math.pow(entry.getMoodScore() - mean, 2);
        }
        variance /= entries.size();
        
        float stdDev = (float) Math.sqrt(variance);
        
        // 如果标准差大于1.2，认为波动较大
        return stdDev > 1.2;
    }
} 