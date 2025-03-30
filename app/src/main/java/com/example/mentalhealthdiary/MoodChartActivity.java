package com.example.mentalhealthdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
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
                moodDistributionChart.setNoDataText("暂无数据");
                averageMoodText.setText("暂无数据");
                mostFrequentMoodText.setText("暂无数据");
                recordDaysText.setText("0 天");
                return;
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
        leftAxis.setAxisMaximum(5.5f);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setLabelCount(6, true);
        
        // 禁用右轴
        moodTrendChart.getAxisRight().setEnabled(false);
        
        // 设置X轴
        XAxis xAxis = moodTrendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextSize(10f);
        
        if (currentView == WEEK_VIEW) {
            // 周视图特定设置
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
            xAxis.setLabelRotationAngle(0);
        }
        
        // 设置动画
        moodTrendChart.animateX(500);
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
            set.setValueTextSize(10f);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.LINEAR); // 线性连接
            set.setDrawCircleHole(true);
            
            // 高亮处理
            set.setHighlightEnabled(true);
            set.setHighLightColor(Color.rgb(255, 159, 64));
            
            // 为空值绘制断点
            set.setDrawCircles(true);
            set.setDrawValues(true);
            
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
} 