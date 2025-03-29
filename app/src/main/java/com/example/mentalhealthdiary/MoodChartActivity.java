package com.example.mentalhealthdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    private void applyTheme() {
        try {
            // 获取当前主题颜色
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            
            // 确保图表数据存在
            if (moodDistributionChart != null && moodDistributionChart.getData() != null && 
                moodDistributionChart.getData().getDataSetByIndex(0) != null) {
                
                PieDataSet dataSet = (PieDataSet) moodDistributionChart.getData().getDataSetByIndex(0);
                dataSet.setColors(
                    Color.rgb(255, 107, 107),  // 暗红色
                    Color.rgb(255, 159, 64),   // 暗橙色
                    Color.rgb(255, 205, 86),   // 暗黄色
                    Color.rgb(75, 192, 192),   // 青绿色
                    Color.rgb(54, 162, 235)    // 深蓝色
                );
                
                moodDistributionChart.invalidate();
            }
        } catch (Exception e) {
            Log.e("MoodChart", "Error applying theme", e);
        }
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
} 