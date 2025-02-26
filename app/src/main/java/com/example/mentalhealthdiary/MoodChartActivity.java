package com.example.mentalhealthdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.ChartTouchListener;
import android.view.MotionEvent;
import android.widget.Toast;
import java.util.Map;
import java.util.HashMap;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;
import java.util.List;
import android.util.Log;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.AdapterView;
import com.github.mikephil.charting.components.Legend;
import android.graphics.Paint;
import com.google.android.material.snackbar.Snackbar;
import android.util.TypedValue;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.Collections;
import com.google.android.material.chip.ChipGroup;

public class MoodChartActivity extends AppCompatActivity {
    private LineChart moodChart;
    private TextView averageMoodText;
    private TextView mostFrequentMoodText;
    private TextView recordDaysText;
    private ChipGroup timeRangeChips;
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

        moodChart = findViewById(R.id.moodChart);
        averageMoodText = findViewById(R.id.averageText);
        mostFrequentMoodText = findViewById(R.id.mostFrequentMoodText);
        recordDaysText = findViewById(R.id.recordDaysText);
        timeRangeChips = findViewById(R.id.timeRangeChips);
        database = AppDatabase.getInstance(this);
        moodDistributionChart = findViewById(R.id.moodDistributionChart);

        setupChart();
        loadMoodData();

        // 设置时间范围选择监听
        timeRangeChips.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipWeek) {
                updateChartTimeRange(7);
            } else if (checkedId == R.id.chipMonth) {
                updateChartTimeRange(30);
            } else if (checkedId == R.id.chipThreeMonths) {
                updateChartTimeRange(90);
            } else if (checkedId == R.id.chipAll) {
                moodChart.fitScreen();  // 显示所有数据
            }
        });
    }

    private void updateChartTimeRange(int days) {
        if (!moodChart.isEmpty()) {
            float lastX = moodChart.getData().getXMax();
            float firstX = lastX - (days * 86400000f);
            
            // 设置最小和最大可见范围
            moodChart.setVisibleXRangeMaximum(days * 86400000f);
            moodChart.setVisibleXRangeMinimum(days * 86400000f / 2);  // 允许查看更详细的区间
            
            // 移动到最新数据
            moodChart.moveViewToX(lastX);
            
            // 强制重绘
            moodChart.invalidate();
        }
    }

    private void setupChart() {
        // 基本设置
        moodChart.getDescription().setEnabled(false);
        moodChart.setTouchEnabled(true);
        moodChart.setDragEnabled(true);
        moodChart.setScaleEnabled(true);
        moodChart.setPinchZoom(true);
        moodChart.setDoubleTapToZoomEnabled(true);
        
        // 设置背景和边距
        moodChart.setBackgroundColor(Color.WHITE);
        moodChart.setDrawGridBackground(false);
        moodChart.setViewPortOffsets(50f, 20f, 50f, 150f);
        
        // 设置边框
        moodChart.setDrawBorders(true);
        moodChart.setBorderColor(Color.parseColor("#E0E0E0"));
        moodChart.setBorderWidth(1f);
        
        // X轴设置
        XAxis xAxis = moodChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(30f);  // 减小旋转角度
        xAxis.setLabelCount(5, false);  // 减少标签数量，允许自动调整
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextSize(11f);
        xAxis.setGranularity(0f);  // 允许更密集的标签
        xAxis.setSpaceMin(0.5f);  // 在开始处添加一些空间
        xAxis.setSpaceMax(0.5f);  // 在结束处添加一些空间
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setGridLineWidth(0.5f);
        xAxis.setYOffset(30f);
        xAxis.setXOffset(15f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return dateFormat.format(new Date((long) value));
            }
        });

        // Y轴设置
        YAxis leftAxis = moodChart.getAxisLeft();
        leftAxis.setDrawLabels(true);
        leftAxis.setSpaceBottom(15f);
        leftAxis.setAxisMinimum(0.5f);
        leftAxis.setAxisMaximum(5.5f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawZeroLine(true);
        leftAxis.setZeroLineColor(Color.parseColor("#E0E0E0"));
        leftAxis.setZeroLineWidth(1f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            private final String[] moodLabels = {"", "😢", "😕", "😐", "😊", "😄"};
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                return (index >= 1 && index <= 5) ? moodLabels[index] : "";
            }
        });
        moodChart.getAxisRight().setEnabled(false);

        // 调整图例位置，确保不遮挡日期
        Legend legend = moodChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(true);
        legend.setYOffset(25f);
        legend.setXOffset(25f);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.LINE);

        // 在现有的设置中添加以下代码
        moodChart.setNoDataText("暂无心情记录");
        
        // 添加动画效果
        moodChart.animateX(1000); // 添加1秒的动画效果

        applyTheme();
    }

    private void applyTheme() {
        try {
            // 获取当前主题颜色
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            int primaryColor = typedValue.data;
            
            // 确保图表数据存在
            if (moodChart != null && moodChart.getData() != null && 
                moodChart.getData().getDataSetByIndex(0) != null) {
                
                LineDataSet dataSet = (LineDataSet) moodChart.getData().getDataSetByIndex(0);
                dataSet.setColor(primaryColor);
                dataSet.setCircleColor(primaryColor);
                dataSet.setFillColor(Color.argb(32, 
                    Color.red(primaryColor), 
                    Color.green(primaryColor), 
                    Color.blue(primaryColor)));
                
                moodChart.invalidate();
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
                moodChart.setNoDataText("暂无心情记录");
                averageMoodText.setText("暂无数据");
                mostFrequentMoodText.setText("暂无数据");
                recordDaysText.setText("0 天");
                moodDistributionChart.setNoDataText("暂无数据");
                return;
            }

            // 获取当前时间
            long currentTime = System.currentTimeMillis();
            long sevenDaysAgo = currentTime - (7 * 86400000L);
            long fourteenDaysAgo = currentTime - (14 * 86400000L);

            // 统计数据
            float currentWeekMood = 0;
            int currentWeekCount = 0;
            float lastWeekMood = 0;
            int lastWeekCount = 0;

            // 计算每日平均心情和统计数据
            TreeMap<String, Float> dailyMoods = new TreeMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            // 先按日期分组存储数据
            Map<String, List<String>> dailyDescriptions = new HashMap<>();
            Map<String, Long> dateToMillis = new HashMap<>();

            for (MoodEntry entry : entries) {
                String dateStr = dateFormat.format(entry.getDate());
                float currentScore = entry.getMoodScore();
                long entryTime = entry.getDate().getTime();
                
                // 统计最近两周的数据
                if (entryTime >= sevenDaysAgo) {
                    currentWeekMood += currentScore;
                    currentWeekCount++;
                } else if (entryTime >= fourteenDaysAgo) {
                    lastWeekMood += currentScore;
                    lastWeekCount++;
                }
                
                // 计算每日平均心情
                if (dailyMoods.containsKey(dateStr)) {
                    float existingScore = dailyMoods.get(dateStr);
                    dailyMoods.put(dateStr, (existingScore + currentScore) / 2);
                } else {
                    dailyMoods.put(dateStr, currentScore);
                }

                // 保存日记内容
                String content = entry.getDiaryContent();
                if (content != null && !content.trim().isEmpty()) {
                    if (!dailyDescriptions.containsKey(dateStr)) {
                        dailyDescriptions.put(dateStr, new ArrayList<>());
                    }
                    dailyDescriptions.get(dateStr).add(content);
                }

                // 保存日期对应的时间戳
                dateToMillis.put(dateStr, entry.getDate().getTime());
            }

            // 将按日期分组的描述转换为按时间戳存储
            for (Map.Entry<String, List<String>> entry : dailyDescriptions.entrySet()) {
                Long timeMillis = dateToMillis.get(entry.getKey());
                if (timeMillis != null) {
                    moodDescriptions.put(timeMillis, entry.getValue());
                }
            }

            // 计算周平均值和变化趋势
            float currentWeekAvg = currentWeekCount > 0 ? currentWeekMood / currentWeekCount : 0;
            float lastWeekAvg = lastWeekCount > 0 ? lastWeekMood / lastWeekCount : 0;
            
            // 计算变化百分比
            String trendText = "";
            if (lastWeekAvg > 0) {
                float change = ((currentWeekAvg - lastWeekAvg) / lastWeekAvg) * 100;
                String trendSymbol = change >= 0 ? "↑" : "↓";
                trendText = String.format(" %s %.1f%%", trendSymbol, Math.abs(change));
            }

            // 更新统计文本
            String statsText = String.format("本周平均: %.1f%s", currentWeekAvg, trendText);
            averageMoodText.setText(statsText);

            // 生成图表数据
            ArrayList<Entry> values = new ArrayList<>();
            for (String dateStr : dailyMoods.keySet()) {
                try {
                    Date date = dateFormat.parse(dateStr);
                    if (date != null) {
                        values.add(new Entry(date.getTime(), dailyMoods.get(dateStr)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 设置数据
            LineDataSet dataSet = new LineDataSet(values, "心情变化");
            
            // 线条样式优化
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);  // 使用更平滑的曲线
            dataSet.setCubicIntensity(0.1f);  // 减小曲线强度，使其更自然
            dataSet.setDrawFilled(true);
            dataSet.setFillAlpha(50);  // 设置填充透明度
            dataSet.setDrawValues(false);  // 不显示数值，减少视觉干扰
            
            // 数据点样式
            dataSet.setDrawCircles(true);
            dataSet.setCircleColor(Color.parseColor("#4A90E2"));
            dataSet.setCircleHoleColor(Color.WHITE);
            dataSet.setCircleRadius(4f);
            dataSet.setCircleHoleRadius(2f);
            
            // 启用点击高亮
            dataSet.setHighlightEnabled(true);
            dataSet.setDrawHighlightIndicators(true);
            
            dataSet.setHighlightLineWidth(1.5f);
            dataSet.setHighLightColor(Color.parseColor("#FF4081"));
            
            moodChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    long dateMillis = (long) e.getX();
                    String dateStr = dateFormat.format(new Date(dateMillis));
                    
                    // 调试输出
                    Log.d("MoodChart", "Selected date: " + dateStr);
                    Log.d("MoodChart", "Available dates: " + moodDescriptions.keySet().toString());
                    
                    List<String> descriptions = moodDescriptions.get(dateMillis);
                    
                    showMoodDetails(e, dateStr, descriptions);
                }

                @Override
                public void onNothingSelected() {
                    // 不需要处理
                }
            });
            
            LineData lineData = new LineData(dataSet);
            moodChart.setData(lineData);
            
            // 设置默认可见范围和缩放限制
            if (!values.isEmpty()) {
                float lastX = values.get(values.size() - 1).getX();
                
                // 默认显示最近7天
                updateChartTimeRange(7);
            }

            // 添加缩放完成的监听器
            moodChart.setOnChartGestureListener(new OnChartGestureListener() {
                @Override
                public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                    // 缩放或滑动结束后，确保Y轴范围正确
                    moodChart.getAxisLeft().setAxisMinimum(0.5f);
                    moodChart.getAxisLeft().setAxisMaximum(5.5f);
                    moodChart.invalidate();
                }
                
                // 需要实现接口的其他方法，但可以留空
                @Override public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
                @Override public void onChartLongPressed(MotionEvent me) {}
                @Override public void onChartDoubleTapped(MotionEvent me) {}
                @Override public void onChartSingleTapped(MotionEvent me) {}
                @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
                @Override public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
                @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {}
            });

            moodChart.invalidate();

            // 更新缓存
            cachedDailyMoods.clear();
            cachedDailyMoods.putAll(dailyMoods);
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

            // 更新心情趋势图
            updateLineChart(entries);
            
            // 更新心情分布饼图
            updatePieChart(entries);
        });
    }

    private void updateChartWithCachedData() {
        // 实现从缓存中更新图表的逻辑
        // 这里需要根据缓存的数据重新生成图表
        // 这只是一个占位方法，实际实现需要根据缓存的数据重新生成图表
    }

    private void showMoodDetails(Entry entry, String dateStr, List<String> descriptions) {
        View rootView = findViewById(android.R.id.content);
        String message = formatMoodDetails(entry, dateStr, descriptions);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                .setAction("关闭", v -> {})
                .show();
    }

    private String formatMoodDetails(Entry entry, String dateStr, List<String> descriptions) {
        StringBuilder message = new StringBuilder();
        message.append(dateStr).append("\n");
        message.append("心情评分: ").append(String.format("%.1f", entry.getY())).append("\n");
        
        if (descriptions != null && !descriptions.isEmpty()) {
            message.append("日记内容:\n");
            for (String desc : descriptions) {
                message.append("- ").append(desc).append("\n");
            }
        } else {
            message.append("无日记内容");
        }
        
        return message.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 添加更多统计信息
    private void updateStatistics(List<MoodEntry> entries) {
        float monthlyAvg = calculateMonthlyAverage(entries);
        float weeklyAvg = calculateWeeklyAverage(entries);
        float dailyAvg = calculateDailyAverage(entries);
        
        String statsText = String.format(
            "日平均: %.1f\n周平均: %.1f\n月平均: %.1f",
            dailyAvg, weeklyAvg, monthlyAvg
        );
        averageMoodText.setText(statsText);
    }

    // 添加心情预测趋势
    private String analyzeMoodTrend(List<Float> recentMoods) {
        // 简单的线性回归分析
        float trend = calculateTrendSlope(recentMoods);
        if (trend > 0.1) {
            return "心情呈上升趋势 ↑";
        } else if (trend < -0.1) {
            return "心情呈下降趋势 ↓";
        }
        return "心情较为稳定 →";
    }

    private void handleDataLoadError(Exception e) {
        Log.e("MoodChart", "数据加载错误", e);
        moodChart.setNoDataText("数据加载失败，请重试");
        
        // 显示重试按钮
        Snackbar.make(moodChart, "数据加载失败", Snackbar.LENGTH_INDEFINITE)
                .setAction("重试", v -> loadMoodData())
                .show();
    }

    private float calculateMonthlyAverage(List<MoodEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        float sum = 0;
        int count = 0;
        
        for (MoodEntry entry : entries) {
            if (entry.getDate().getTime() >= thirtyDaysAgo) {
                sum += entry.getMoodScore();
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }

    private float calculateWeeklyAverage(List<MoodEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        float sum = 0;
        int count = 0;
        
        for (MoodEntry entry : entries) {
            if (entry.getDate().getTime() >= sevenDaysAgo) {
                sum += entry.getMoodScore();
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }

    private float calculateDailyAverage(List<MoodEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        
        long oneDayAgo = System.currentTimeMillis() - (24L * 60 * 60 * 1000);
        float sum = 0;
        int count = 0;
        
        for (MoodEntry entry : entries) {
            if (entry.getDate().getTime() >= oneDayAgo) {
                sum += entry.getMoodScore();
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }

    private float calculateTrendSlope(List<Float> recentMoods) {
        if (recentMoods == null || recentMoods.size() < 2) return 0;
        
        float sumX = 0;
        float sumY = 0;
        float sumXY = 0;
        float sumXX = 0;
        int n = recentMoods.size();
        
        for (int i = 0; i < n; i++) {
            float x = i;
            float y = recentMoods.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        // 计算线性回归斜率
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
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

    private void setupPieChart() {
        moodDistributionChart.getDescription().setEnabled(false);
        moodDistributionChart.setDrawHoleEnabled(true);
        moodDistributionChart.setHoleColor(Color.WHITE);
        moodDistributionChart.setTransparentCircleRadius(30f);
        moodDistributionChart.setHoleRadius(30f);
        moodDistributionChart.setCenterText("心情分布");
        moodDistributionChart.setCenterTextSize(14f);
        moodDistributionChart.setRotationEnabled(false);
        moodDistributionChart.setHighlightPerTapEnabled(true);

        Legend l = moodDistributionChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setTextSize(12f);
        l.setDrawInside(false);
        l.setXOffset(10f);
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

    private void updateLineChart(List<MoodEntry> entries) {
        ArrayList<Entry> values = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // 按日期分组计算平均心情
        Map<String, List<Float>> dailyMoods = new HashMap<>();
        for (MoodEntry entry : entries) {
            String dateStr = dateFormat.format(entry.getDate());
            if (!dailyMoods.containsKey(dateStr)) {
                dailyMoods.put(dateStr, new ArrayList<>());
            }
            dailyMoods.get(dateStr).add((float) entry.getMoodScore());
        }
        
        // 计算每日平均值并创建数据点
        for (Map.Entry<String, List<Float>> entry : dailyMoods.entrySet()) {
            try {
                Date date = dateFormat.parse(entry.getKey());
                if (date != null) {
                    float avgMood = (float) entry.getValue().stream()
                        .mapToDouble(Float::doubleValue)
                        .average()
                        .orElse(0.0);
                    values.add(new Entry(date.getTime(), avgMood));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 按时间排序
        values.sort((a, b) -> Float.compare(a.getX(), b.getX()));
        
        // 创建数据集
        LineDataSet dataSet = new LineDataSet(values, "心情变化");
        
        // 设置样式
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#204A90E2"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#4A90E2"));
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(false);
        
        // 设置数据
        LineData lineData = new LineData(dataSet);
        moodChart.setData(lineData);
        
        // 刷新图表
        moodChart.invalidate();
    }
} 