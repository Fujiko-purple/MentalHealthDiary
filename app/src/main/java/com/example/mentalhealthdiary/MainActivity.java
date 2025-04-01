package com.example.mentalhealthdiary;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.MoodEntryAdapter;
import com.example.mentalhealthdiary.config.RemoteConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.example.mentalhealthdiary.service.TipsWorkManager;
import com.example.mentalhealthdiary.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int REQUEST_IMAGE_PICK = 101;
    private RadioGroup moodRadioGroup;
    private EditText diaryContent;
    private MaterialButton saveButton;
    private Button datePickerButton;
    private Date selectedDate;
    private AppDatabase database;
    private ExecutorService executorService;
    private MoodEntryAdapter adapter;
    private long currentEditingId = 0;
    private ChipGroup moodFilterChips;
    private MaterialButton filterButton;
    private MaterialButton sortButton;
    private MaterialButton aiButton;
    private String[] writingPrompts = {
        "写下此刻的心情，记录生活的点滴...",
        "今天有什么令你印象深刻的事情吗？",
        "分享一个让你感到愉快的小事...",
        "此刻的感受是什么？为何会有这样的感受？",
        "有什么困扰着你？写下来或许会舒缓些...",
        "记录下这一刻，让时光定格于此..."
    };
    private RadioGroup weatherRadioGroup;
    private String selectedWeather = null;
    private LocationListener locationListener;
    private View weatherIndicator;
    private TimeFilter currentTimeFilter = TimeFilter.ALL;
    private enum SortOrder {
        DATE_DESC,    // 时间从新到旧
        DATE_ASC,     // 时间从旧到新
        MOOD_DESC,    // 心情从好到坏
        MOOD_ASC      // 心情从坏到好
    }
    private SortOrder currentSortOrder = SortOrder.DATE_DESC; // 默认排序方式
    private MaterialButton viewModeButton;
    private boolean isGridMode = false;
    private RecyclerView.LayoutManager listLayoutManager;
    private RecyclerView.LayoutManager gridLayoutManager;
    private MaterialButton cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化远程配置
        RemoteConfig.init(getApplicationContext());
        
        // 添加日志
        Log.d("Config", "API Key: " + RemoteConfig.getApiKey());
        Log.d("Config", "API Base: " + RemoteConfig.getApiBaseUrl());
        Log.d("Config", "Model Name: " + RemoteConfig.getModelName());
        
        // 请求通知权限（Android 13及以上需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1
                );
            }
        }

        // 初始化每日心理小贴士
        TipsWorkManager.scheduleDailyTips(this);

        // 初始化数据库
        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // 初始化视图
        moodRadioGroup = findViewById(R.id.moodRadioGroup);
        diaryContent = findViewById(R.id.diaryContent);
        saveButton = findViewById(R.id.saveButton);
        datePickerButton = findViewById(R.id.datePickerButton);
        TextView charCountText = findViewById(R.id.charCountText);
        TextView moodDescriptionText = findViewById(R.id.moodDescriptionText);
        updateDateButtonText();

        // 添加文本变化监听器以更新字数统计
        diaryContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                int maxLength = 500; // 设置最大字数
                charCountText.setText(length + "/" + maxLength);
                
                // 超出字数限制时改变颜色提醒
                if (length > maxLength) {
                    charCountText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    charCountText.setTextColor(Color.parseColor("#99000000"));
                }
            }
        });

        // 监听心情选择变化
        moodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String description;
            switch (checkedId) {
                case R.id.mood_1:
                    description = "心情低落...需要关爱";
                    break;
                case R.id.mood_2:
                    description = "有点不开心，但会好起来的";
                    break;
                case R.id.mood_3:
                    description = "平静且安稳的心情";
                    break;
                case R.id.mood_4:
                    description = "感到快乐与满足";
                    break;
                case R.id.mood_5:
                    description = "心情大好，充满活力!";
                    break;
                default:
                    description = "选择一个心情...";
                    break;
            }
            
            // 文本淡入效果
            moodDescriptionText.setAlpha(0f);
            moodDescriptionText.setText(description);
            moodDescriptionText.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
            
            // 为所有按钮设置动画
            for (int i = 0; i < group.getChildCount(); i++) {
                View button = group.getChildAt(i);
                
                if (button.getId() == checkedId) {
                    // 被选中的按钮有弹跳效果
                    button.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator())
                        .withEndAction(() -> {
                            button.animate()
                                .scaleX(1.1f)
                                .scaleY(1.1f)
                                .setDuration(100)
                                .start();
                        })
                        .start();
                    button.setAlpha(1.0f);
                } else {
                    // 未选中的按钮缩小并变淡
                    button.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .alpha(0.7f)
                        .setDuration(200)
                        .start();
                }
            }
        });

        // 设置RecyclerView
        RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);
        listLayoutManager = new LinearLayoutManager(this);
        gridLayoutManager = new GridLayoutManager(this, 2); // 每行两个卡片
        
        // 默认使用列表布局
        historyRecyclerView.setLayoutManager(listLayoutManager);
        
        // 设置适配器
        adapter = new MoodEntryAdapter();
        adapter.setOnEntryClickListener(this::showEditDialog);
        adapter.setOnEntryDeleteListener(this::showDeleteDialog);
        historyRecyclerView.setAdapter(adapter);

        // 观察数据变化
        database.moodEntryDao().getAllEntries().observe(this, entries -> {
            adapter.setEntries(entries);
        });

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> saveMoodEntry());

        // 设置日期选择按钮点击事件
        datePickerButton.setOnClickListener(v -> {
            showDatePickerDialog();
            
            // 随机更换写作提示
            int randomIndex = new Random().nextInt(writingPrompts.length);
            diaryContent.setHint(writingPrompts[randomIndex]);
        });

        // 初始化筛选和排序功能
        moodFilterChips = findViewById(R.id.moodFilterChips);
        filterButton = findViewById(R.id.filterButton);
        sortButton = findViewById(R.id.sortButton);

        // 设置心情筛选
        moodFilterChips.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                adapter.resetFilter();
            } else if (checkedId == R.id.chipHappy) {
                adapter.filterByMood(5);
            } else if (checkedId == R.id.chipGood) {
                adapter.filterByMood(4);
            } else if (checkedId == R.id.chipNeutral) {
                adapter.filterByMood(3);
            } else if (checkedId == R.id.chipSad) {
                adapter.filterByMood(2);
            } else if (checkedId == R.id.chipVeryBad) {
                adapter.filterByMood(1);
            }
        });

        // 设置排序按钮
        sortButton.setOnClickListener(v -> showSortDialog());

        // 设置筛选按钮
        filterButton.setOnClickListener(v -> showFilterDialog());

        // 添加 AI 助手按钮
        aiButton = findViewById(R.id.aiButton);
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.ai_pulse);
        aiButton.startAnimation(pulseAnimation);
        
        aiButton.setOnClickListener(v -> {
            // 点击时停止动画并显示对话框
            aiButton.clearAnimation();
            showAIAssistantDialog();
            
            // 对话框关闭后恢复动画
            new Handler().postDelayed(() -> 
                aiButton.startAnimation(pulseAnimation), 500);
        });

        // 确保使用正确的ID初始化weatherRadioGroup
        weatherRadioGroup = findViewById(R.id.weatherRadioGroup);
        Log.d("WeatherDebug", "天气RadioGroup ID: " + R.id.weatherRadioGroup);
        Log.d("WeatherDebug", "天气RadioGroup 是否为null: " + (weatherRadioGroup == null));
        
        // 初始化天气指示器
        weatherIndicator = findViewById(R.id.weather_indicator);
        
        // 设置天气选择监听器
        weatherRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.weather_sunny) {
                selectedWeather = "晴";
            } else if (checkedId == R.id.weather_cloudy) {
                selectedWeather = "多云";
            } else if (checkedId == R.id.weather_rainy) {
                selectedWeather = "雨";
            }
            
            // 动画移动指示器
            animateWeatherIndicator(checkedId);
        });
        
        // 设置图片插入按钮
        findViewById(R.id.btnInsertImage).setOnClickListener(v -> pickImage());
        
        // 设置位置插入按钮
        findViewById(R.id.btnInsertLocation).setOnClickListener(v -> {
            getCurrentLocation();
        });

        // 初始化布局切换按钮
        viewModeButton = findViewById(R.id.viewModeButton);
        viewModeButton.setOnClickListener(v -> toggleViewMode(historyRecyclerView));

        // 初始化取消按钮
        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> cancelEditing());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        }
        
        // 先选择日期
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                // 设置选择的日期
                calendar.set(year, month, dayOfMonth);
                
                // 然后显示时间选择器 - 使用 TimePickerDialog 而不是 DatePickerDialog
                new TimePickerDialog(
                    this,
                    (timeView, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        selectedDate = calendar.getTime();
                        updateDateButtonText();
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true  // 是否使用24小时制
                ).show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINESE);
        datePickerButton.setText(dateFormat.format(selectedDate != null ? selectedDate : new Date()));
    }

    private void showEditDialog(MoodEntry entry) {
        currentEditingId = entry.getId();
        
        // 设置日期
        selectedDate = entry.getDate();
        updateDateButtonText();
        
        // 设置心情 - 使用正确的RadioButton ID
        int moodScore = entry.getMoodScore();
        switch (moodScore) {
            case 1: moodRadioGroup.check(R.id.mood_1); break;
            case 2: moodRadioGroup.check(R.id.mood_2); break;
            case 3: moodRadioGroup.check(R.id.mood_3); break;
            case 4: moodRadioGroup.check(R.id.mood_4); break;
            case 5: moodRadioGroup.check(R.id.mood_5); break;
        }
        
        // 设置天气
        String weather = entry.getWeather();
        if (weather != null) {
            selectedWeather = weather;
            
            int checkedId;
            if (weather.equals("晴")) {
                checkedId = R.id.weather_sunny;
            } else if (weather.equals("多云")) {
                checkedId = R.id.weather_cloudy;
            } else if (weather.equals("雨")) {
                checkedId = R.id.weather_rainy;
            } else {
                checkedId = -1;
            }

            if (checkedId != -1) {
                weatherRadioGroup.check(checkedId);
                // 添加延迟以确保UI已更新
                weatherRadioGroup.post(() -> animateWeatherIndicator(checkedId));
            } else {
                weatherRadioGroup.clearCheck();
                weatherIndicator.setVisibility(View.INVISIBLE);
            }
        } else {
            weatherRadioGroup.clearCheck();
            weatherIndicator.setVisibility(View.INVISIBLE);
        }
        
        // 设置日记内容
        loadDiaryContent(entry.getDiaryContent());
        
        // 修改保存按钮文本和图标
        saveButton.setText("更新");
        saveButton.setIcon(getDrawable(R.drawable.ic_update));
        
        // 显示取消按钮
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void updateEntry() {
        // 获取选中的心情等级（1-5）
        int moodScore = getMoodScore();
        if (moodScore == 0) {
            Toast.makeText(this, "请选择心情", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = diaryContent.getText().toString();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入日记内容", Toast.LENGTH_SHORT).show();
            return;
        }

        MoodEntry entry = new MoodEntry(selectedDate, moodScore, content, selectedWeather);
        entry.setId(currentEditingId);

        // 在后台线程中更新数据
        executorService.execute(() -> {
            database.moodEntryDao().update(entry);
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                clearInputs();
            });
        });
    }

    private void clearInputs() {
        diaryContent.setText("");
        moodRadioGroup.clearCheck();
        weatherRadioGroup.clearCheck();
        saveButton.setText("记录");
        saveButton.setIcon(getDrawable(R.drawable.ic_save));
        currentEditingId = 0;
        selectedDate = null;  // 重置选择的日期
        updateDateButtonText();
        
        // 隐藏取消按钮
        cancelButton.setVisibility(View.GONE);
    }

    private void saveMoodEntry() {
        String content = diaryContent.getText().toString().trim();
        int moodScore = getMoodScore();
        
        if (moodScore == 0) {
            Toast.makeText(this, "请选择一个心情", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保日期已选择
        if (selectedDate == null) {
            selectedDate = new Date(); // 使用当前日期
        }
        
        // 添加日志检查天气
        Log.d("WeatherDebug", "保存时的天气: " + selectedWeather);
        
        // 创建要保存的记录对象
        MoodEntry entry = new MoodEntry(selectedDate, moodScore, content, selectedWeather);
        
        if (currentEditingId > 0) {
            entry.setId(currentEditingId);
            
            // 显示确认对话框
            showCustomConfirmDialog(
                "确认更新",
                "确定要更新这条记录吗？",
                "确定",
                "取消",
                false, // 这是普通确认对话框，不是警告
                () -> saveMoodEntryToDatabase(entry)
            );
        } else {
            // 如果是新建操作，直接保存
            saveMoodEntryToDatabase(entry);
        }
    }

    private void saveMoodEntryToDatabase(MoodEntry entry) {
        executorService.execute(() -> {
            if (entry.getId() > 0) {
                database.moodEntryDao().update(entry);
            } else {
                database.moodEntryDao().insert(entry);
            }
            
            // 重置状态
            runOnUiThread(() -> {
                diaryContent.setText("");
                moodRadioGroup.clearCheck();
                weatherRadioGroup.clearCheck();  // 确保清除天气选择
                selectedWeather = null;  // 重置天气变量
                selectedDate = null;
                updateDateButtonText();
                currentEditingId = 0;
                saveButton.setText("记录");
                saveButton.setIcon(getDrawable(R.drawable.ic_save));
                
                // 隐藏取消按钮
                cancelButton.setVisibility(View.GONE);
                
                // 清除选中状态
                clearCardSelection();
                
                Toast.makeText(MainActivity.this, 
                        entry.getId() > 0 ? "更新成功" : "保存成功", 
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private int getMoodScore() {
        int radioButtonId = moodRadioGroup.getCheckedRadioButtonId();
        switch (radioButtonId) {
            case R.id.mood_1: return 1;
            case R.id.mood_2: return 2;
            case R.id.mood_3: return 3;
            case R.id.mood_4: return 4;
            case R.id.mood_5: return 5;
            default: return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_chart:
                startActivity(new Intent(this, MoodChartActivity.class));
                return true;
            case R.id.action_breathing:
                startActivity(new Intent(this, BreathingActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.add(Menu.NONE, R.id.action_chart, Menu.NONE, "心情图表")
            .setIcon(R.drawable.ic_chart)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, R.id.action_breathing, Menu.NONE, "正念呼吸")
            .setIcon(R.drawable.ic_achievement_beginner)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    private void showDeleteDialog(MoodEntry entry) {
        new AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除", (dialog, which) -> deleteEntry(entry))
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteEntry(MoodEntry entry) {
        executorService.execute(() -> {
            database.moodEntryDao().delete(entry);
            runOnUiThread(() -> 
                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void showSortDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_sort_options);

        // 设置对话框宽度为屏幕宽度的75%
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        dialog.getWindow().setAttributes(lp);
        
        View view = dialog.findViewById(R.id.sortOptionsGroup);
        RadioGroup sortOptionsGroup = view.findViewById(R.id.sortOptionsGroup);
        
        // 根据当前排序方式设置选中项
        switch (currentSortOrder) {
            case DATE_DESC:
                sortOptionsGroup.check(R.id.sort_date_desc);
                break;
            case DATE_ASC:
                sortOptionsGroup.check(R.id.sort_date_asc);
                break;
            case MOOD_DESC:
                sortOptionsGroup.check(R.id.sort_mood_desc);
                break;
            case MOOD_ASC:
                sortOptionsGroup.check(R.id.sort_mood_asc);
                break;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setCancelable(true)
                .create();
                
        // 设置对话框窗口属性
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 设置选项点击监听
        sortOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_date_desc) {
                adapter.sortByDateDesc();
                currentSortOrder = SortOrder.DATE_DESC;
                Toast.makeText(MainActivity.this, "📝 按时间从新到旧排序", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.sort_date_asc) {
                adapter.sortByDateAsc();
                currentSortOrder = SortOrder.DATE_ASC;
                Toast.makeText(MainActivity.this, "🕰️ 按时间从旧到新排序", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.sort_mood_desc) {
                adapter.sortByMoodDesc();
                currentSortOrder = SortOrder.MOOD_DESC;
                Toast.makeText(MainActivity.this, "😊 按心情从好到坏排序", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.sort_mood_asc) {
                adapter.sortByMoodAsc();
                currentSortOrder = SortOrder.MOOD_ASC;
                Toast.makeText(MainActivity.this, "🌈 按心情从坏到好排序", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void showFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_time_filter);

        // 设置对话框宽度为屏幕宽度的75%
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        dialog.getWindow().setAttributes(lp);
        
        View view = dialog.findViewById(R.id.timeFilterGroup);
        RadioGroup timeFilterGroup = view.findViewById(R.id.timeFilterGroup);
        
        // 设置当前选中项
        switch(currentTimeFilter) {
            case WEEK:
                timeFilterGroup.check(R.id.filter_week);
                break;
            case TWO_WEEKS:
                timeFilterGroup.check(R.id.filter_two_weeks);
                break;
            case MONTH:
                timeFilterGroup.check(R.id.filter_month);
                break;
            case ALL:
                timeFilterGroup.check(R.id.filter_all);
                break;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setCancelable(true)
                .create();
                
        // 设置对话框窗口属性
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 设置选项点击监听
        timeFilterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_week) {
                currentTimeFilter = TimeFilter.WEEK;
            } else if (checkedId == R.id.filter_two_weeks) {
                currentTimeFilter = TimeFilter.TWO_WEEKS;
            } else if (checkedId == R.id.filter_month) {
                currentTimeFilter = TimeFilter.MONTH;
            } else if (checkedId == R.id.filter_all) {
                currentTimeFilter = TimeFilter.ALL;
            }
            loadMoodEntries();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void showAIAssistantDialog() {
        Intent intent = new Intent(this, AIChatActivity.class);
        // 获取最后一次对话的ID
        long lastChatId = PreferenceManager.getLastChatId(this);
        if (lastChatId != -1) {
            // 如果存在最后一次对话，则加载该对话
            intent.putExtra("chat_history_id", lastChatId);
        }
        startActivity(intent);
    }

    // 图片选择方法
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // 获取位置信息 - 修改实现以避免ANR
    private void getCurrentLocation() {
        // 检查位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    REQUEST_LOCATION_PERMISSION);
            return;
        }
        
        // 显示加载对话框
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在获取位置...");
        progressDialog.show();
        
        // 使用后台线程处理位置获取
        executorService.execute(() -> {
            try {
                // 获取系统位置服务
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                
                // 检查GPS或网络定位是否可用
                boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                
                if (!isGPSEnabled && !isNetworkEnabled) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "请开启位置服务", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 获取最后已知位置
                String provider = isNetworkEnabled ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
                final Location[] locationResult = new Location[1];
                
                // 创建一个倒计时锁，用于等待位置更新
                CountDownLatch latch = new CountDownLatch(1);
                
                // 在主线程上设置位置监听器
                runOnUiThread(() -> {
                    try {
                        locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                // 获取到位置后保存并释放锁
                                locationResult[0] = location;
                                latch.countDown();
                            }
                            
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}
                            
                            @Override
                            public void onProviderEnabled(String provider) {}
                            
                            @Override
                            public void onProviderDisabled(String provider) {}
                        };
                        
                        // 请求位置更新
                        locationManager.requestLocationUpdates(provider, 0, 0, locationListener, Looper.getMainLooper());
                        
                        // 尝试获取最后已知位置
                        Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                        if (lastKnownLocation != null) {
                            locationResult[0] = lastKnownLocation;
                            latch.countDown();
                        }
                    } catch (SecurityException e) {
                        latch.countDown(); // 确保在权限错误时也能解锁
                    }
                });
                
                // 等待位置信息，最多10秒
                boolean locationObtained = latch.await(10, TimeUnit.SECONDS);
                
                // 无论成功或超时，都移除位置监听器
                runOnUiThread(() -> {
                    if (locationListener != null) {
                        locationManager.removeUpdates(locationListener);
                        locationListener = null;
                    }
                });
                
                if (locationObtained && locationResult[0] != null) {
                    // 位置获取成功，在后台处理地理编码
                    processLocationInBackground(locationResult[0], progressDialog);
                } else {
                    // 位置获取超时
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "位置获取超时或失败", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                // 确保任何异常都会在UI线程上处理
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "获取位置时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Location error", e);
                });
            }
        });
    }

    // 在后台处理地理编码 - 修改为使用中文区域
    private void processLocationInBackground(Location location, ProgressDialog progressDialog) {
        executorService.execute(() -> {
            try {
                // 使用中文区域设置进行地理编码
                Geocoder geocoder = new Geocoder(this, Locale.CHINESE);
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(), 1);
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        
                        // 尝试获取多种中文地址组件
                        String country = address.getCountryName() != null ? address.getCountryName() : "";
                        String adminArea = address.getAdminArea() != null ? address.getAdminArea() : "";  // 省/州
                        String locality = address.getLocality() != null ? address.getLocality() : "";  // 市
                        String subLocality = address.getSubLocality() != null ? address.getSubLocality() : "";  // 区/县
                        String featureName = address.getFeatureName() != null ? address.getFeatureName() : "";  // 街道号码等
                        String thoroughfare = address.getThoroughfare() != null ? address.getThoroughfare() : "";  // 街道
                        
                        // 构建更详细的中文地址
                        StringBuilder locationTextBuilder = new StringBuilder();
                        
                        // 添加城市和区县信息
                        if (!locality.isEmpty()) {
                            locationTextBuilder.append(locality);
                            if (!subLocality.isEmpty()) {
                                locationTextBuilder.append(subLocality);
                            }
                        } else if (!adminArea.isEmpty()) {
                            locationTextBuilder.append(adminArea);
                        }
                        
                        // 添加街道信息
                        if (!thoroughfare.isEmpty()) {
                            if (locationTextBuilder.length() > 0) {
                                locationTextBuilder.append("，");
                            }
                            locationTextBuilder.append(thoroughfare);
                            if (!featureName.isEmpty() && !featureName.equals(thoroughfare)) {
                                locationTextBuilder.append(featureName);
                            }
                        }
                        
                        String locationText = locationTextBuilder.length() > 0 ? 
                                locationTextBuilder.toString() : "当前位置";
                        
                        insertTextAtCursor("📍 " + locationText);
                        
                        // 输出完整地址信息到日志，帮助调试
                        Log.d("Location", "完整地址: " + address.getAddressLine(0));
                    } else {
                        Toast.makeText(MainActivity.this, "无法解析位置信息", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "地理编码失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Geocoding error", e);
                });
            }
        });
    }

    // 在光标位置插入文本
    private void insertTextAtCursor(String text) {
        int start = Math.max(diaryContent.getSelectionStart(), 0);
        int end = Math.max(diaryContent.getSelectionEnd(), 0);
        diaryContent.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，重新尝试获取位置
                getCurrentLocation();
            } else {
                Toast.makeText(this, "需要位置权限才能使用该功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                // 保存图片到应用私有存储
                saveImageAndInsertReference(selectedImage);
            }
        }
    }

    // 保存图片并插入引用
    private void saveImageAndInsertReference(Uri imageUri) {
        executorService.execute(() -> {
            try {
                // 生成唯一文件名
                String fileName = "diary_img_" + System.currentTimeMillis() + ".jpg";
                
                // 创建应用私有目录中的文件
                File imagesDir = new File(getFilesDir(), "diary_images");
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs();
                }
                
                File outputFile = new File(imagesDir, fileName);
                
                // 复制图片内容
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                inputStream.close();
                outputStream.close();
                
                // 验证图片是否成功保存
                boolean fileExists = outputFile.exists();
                long fileSize = outputFile.length();
                
                Log.d("ImageSaving", "图片保存状态: 存在=" + fileExists + ", 大小=" + fileSize + "字节");
                
                // 在UI线程更新文本
                runOnUiThread(() -> {
                    // 插入特殊标记，包含图片路径
                    String imageTag = "[[IMG:" + fileName + "]]";
                    insertTextAtCursor(imageTag);
                    
                    // 立即尝试显示图片
                    refreshDiaryContent();
                    
                    // 提示用户
                    Toast.makeText(this, "图片已插入", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "图片处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Image processing error", e);
                });
            }
        });
    }

    // 添加刷新内容方法
    private void refreshDiaryContent() {
        String content = diaryContent.getText().toString();
        loadDiaryContent(content);
    }

    // 在加载日记内容时处理图片标记
    private void loadDiaryContent(String content) {
        if (content == null) return;
        
        // 创建可变文本
        SpannableStringBuilder builder = new SpannableStringBuilder(content);
        
        // 查找所有图片标记
        Pattern pattern = Pattern.compile("\\[\\[IMG:(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);
        
        // 记录偏移量（因为替换后文本长度会变化）
        int offset = 0;
        
        while (matcher.find()) {
            int start = matcher.start() - offset;
            int end = matcher.end() - offset;
            String fileName = matcher.group(1);
            
            // 加载图片
            File imageFile = new File(new File(getFilesDir(), "diary_images"), fileName);
            if (imageFile.exists()) {
                try {
                    // 加载并缩放图片
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        int maxWidth = diaryContent.getWidth() - 50; // 留出边距
                        if (maxWidth <= 0) maxWidth = 300; // 默认宽度
                        
                        int width = Math.min(bitmap.getWidth(), maxWidth);
                        int height = (int)(width * ((float)bitmap.getHeight() / bitmap.getWidth()));
                        
                        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        
                        // 创建图片Span
                        ImageSpan imageSpan = new ImageSpan(this, bitmap);
                        
                        // 替换文本为图片
                        builder.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        // 更新偏移量 - 这里是关键修复
                        offset += (end - start - 1);
                        
                        Log.d("ImageLoading", "成功加载图片: " + fileName);
                    } else {
                        Log.e("ImageLoading", "无法解码图片: " + fileName);
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "加载图片错误: " + fileName, e);
                }
            } else {
                Log.e("ImageLoading", "图片文件不存在: " + imageFile.getAbsolutePath());
            }
        }
        
        // 设置处理后的文本
        diaryContent.setText(builder);
    }

    // 添加指示器动画方法
    private void animateWeatherIndicator(int checkedId) {
        if (checkedId == -1) {
            // 如果没有选中，隐藏指示器
            weatherIndicator.setVisibility(View.INVISIBLE);
            return;
        }
        
        // 获取选中的RadioButton
        RadioButton selected = findViewById(checkedId);
        if (selected == null) return;
        
        // 确保指示器可见
        weatherIndicator.setVisibility(View.VISIBLE);
        
        // 计算指示器应该移动到的位置
        float targetX = selected.getX() + (selected.getWidth() - weatherIndicator.getWidth()) / 2;
        
        // 创建并启动动画
        weatherIndicator.animate()
            .x(targetX)
            .setDuration(300)
            .setInterpolator(new FastOutSlowInInterpolator())
            .start();
    }

    /**
     * 根据当前的时间筛选设置加载心情记录
     */
    private void loadMoodEntries() {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime(); // 当前时间作为结束日期
        Date startDate = null;

        // 根据筛选器设置起始日期
        switch (currentTimeFilter) {
            case WEEK:
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                startDate = calendar.getTime();
                break;
            case TWO_WEEKS:
                calendar.add(Calendar.DAY_OF_YEAR, -14);
                startDate = calendar.getTime();
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, -1);
                startDate = calendar.getTime();
                break;
            case ALL:
                // 不设置startDate，表示获取所有记录
                break;
        }

        final Date finalStartDate = startDate;
        
        // 在UI上显示当前筛选状态
        String filterDescription;
        if (currentTimeFilter == TimeFilter.ALL) {
            filterDescription = "📚 正在展示你的所有心情故事";
        } else if (currentTimeFilter == TimeFilter.WEEK) {
            filterDescription = "🕰️ 回到一周前的记忆";
        } else if (currentTimeFilter == TimeFilter.TWO_WEEKS) {
            filterDescription = "📅 展示两周内的情感轨迹";
        } else {
            filterDescription = "🌙 呈现一个月的心路历程";
        }
        Toast.makeText(MainActivity.this, filterDescription, Toast.LENGTH_SHORT).show();

        // 从数据库加载记录
        executorService.execute(() -> {
            List<MoodEntry> filteredEntries;
            if (finalStartDate != null) {
                // 加载指定时间范围内的记录
                filteredEntries = database.moodEntryDao().getEntriesBetweenDates(finalStartDate, endDate);
            } else {
                // 加载所有记录
                filteredEntries = database.moodEntryDao().getAllEntriesAsList();
            }

            // 更新UI
            runOnUiThread(() -> {
                adapter.setEntries(filteredEntries);
            });
        });
    }

    // 在保存图片的方法中添加压缩逻辑
    private String saveSelectedImage(Uri imageUri) {
        try {
            String fileName = "diary_img_" + System.currentTimeMillis() + ".jpg";
            File outputDir = new File(getFilesDir(), "diary_images");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, fileName);
            
            // 从Uri获取原始图片
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // 计算合适的压缩尺寸 - 目标宽度约为屏幕宽度的一半
            int targetWidth = getResources().getDisplayMetrics().widthPixels / 2;
            int targetHeight = (int) (targetWidth * ((float) originalBitmap.getHeight() / originalBitmap.getWidth()));
            
            // 创建压缩后的图片
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
            
            // 保存压缩后的图片
            FileOutputStream fos = new FileOutputStream(outputFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos); // 85%质量的JPEG
            fos.close();
            
            // 回收Bitmap
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
            }
            if (resizedBitmap != null && !resizedBitmap.isRecycled()) {
                resizedBitmap.recycle();
            }
            
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 添加切换布局模式的方法
    private void toggleViewMode(RecyclerView recyclerView) {
        isGridMode = !isGridMode;
        
        // 更新RecyclerView的布局管理器
        recyclerView.setLayoutManager(isGridMode ? gridLayoutManager : listLayoutManager);
        
        // 更新适配器显示模式
        adapter.setGridMode(isGridMode);
        
        // 更新按钮外观
        viewModeButton.setIcon(getDrawable(isGridMode ? R.drawable.ic_list_view : R.drawable.ic_grid_view));
        viewModeButton.setText(isGridMode ? "列表" : "相册");
    }

    // 在需要清除选中状态的地方调用此方法
    private void clearCardSelection() {
        if (adapter != null) {
            adapter.clearSelection();
        }
    }

    // 修改cancelEditing方法
    private void cancelEditing() {
        showCustomConfirmDialog(
            "确认取消",
            "确定要取消编辑吗？所有更改将不会保存。",
            "确定",
            "继续编辑",
            true, // 这是警告对话框
            () -> {
                // 清除所有表单内容
                diaryContent.setText("");
                moodRadioGroup.clearCheck();
                weatherRadioGroup.clearCheck();
                
                // 重置天气选择
                selectedWeather = null;
                weatherIndicator.setVisibility(View.INVISIBLE);
                
                // 重置日期
                selectedDate = null;
                updateDateButtonText();
                
                // 重置编辑状态
                currentEditingId = 0;
                saveButton.setText("记录");
                saveButton.setIcon(getDrawable(R.drawable.ic_save));
                
                // 隐藏取消按钮
                cancelButton.setVisibility(View.GONE);
                
                // 清除卡片选中状态
                clearCardSelection();
                
                Toast.makeText(MainActivity.this, "已取消编辑", Toast.LENGTH_SHORT).show();
            }
        );
    }

    /**
     * 显示自定义确认对话框
     * @param title 标题
     * @param message 内容
     * @param positiveText 确认按钮文本
     * @param negativeText 取消按钮文本
     * @param isWarning 是否为警告对话框（影响图标和颜色）
     * @param positiveAction 确认按钮点击事件
     */
    private void showCustomConfirmDialog(String title, String message, 
                                       String positiveText, String negativeText,
                                       boolean isWarning,
                                       Runnable positiveAction) {
        // 创建对话框
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirmation);
        
        // 设置对话框窗口属性
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(R.style.CustomDialogAnimation);
        }
        
        // 设置控件
        ImageView iconView = dialog.findViewById(R.id.dialogIcon);
        TextView titleView = dialog.findViewById(R.id.dialogTitle);
        TextView messageView = dialog.findViewById(R.id.dialogMessage);
        MaterialButton negativeButton = dialog.findViewById(R.id.negativeButton);
        MaterialButton positiveButton = dialog.findViewById(R.id.positiveButton);
        
        // 根据对话框类型设置样式
        if (isWarning) {
            iconView.setImageResource(R.drawable.ic_warning);
            iconView.setColorFilter(Color.parseColor("#FF9800")); // 警告图标为橙色
            positiveButton.setBackgroundColor(Color.parseColor("#FF5722")); // 警告按钮为红色
        } else {
            iconView.setImageResource(R.drawable.ic_info); // 需要创建info图标
            iconView.setColorFilter(Color.parseColor("#2196F3")); // 信息图标为蓝色
            positiveButton.setBackgroundColor(Color.parseColor("#4CAF50")); // 确认按钮为绿色
        }
        
        // 设置文本
        titleView.setText(title);
        messageView.setText(message);
        positiveButton.setText(positiveText);
        negativeButton.setText(negativeText);
        
        // 设置按钮事件
        negativeButton.setOnClickListener(v -> dialog.dismiss());
        positiveButton.setOnClickListener(v -> {
            if (positiveAction != null) {
                positiveAction.run();
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }

    // 添加波纹效果方法
    private void createRippleEffect(View view) {
        View parent = (View) view.getParent();
        
        // 创建圆形波纹效果
        int centerX = view.getLeft() + view.getWidth() / 2;
        int centerY = view.getTop() + view.getHeight() / 2;
        
        // 创建圆形动画drawable
        ShapeDrawable rippleDrawable = new ShapeDrawable(new OvalShape());
        rippleDrawable.getPaint().setColor(Color.parseColor("#33009688")); // 使用主题色的透明版本
        
        // 创建一个覆盖层来显示波纹
        ImageView rippleView = new ImageView(this);
        rippleView.setLayoutParams(new FrameLayout.LayoutParams(parent.getWidth(), parent.getHeight()));
        rippleView.setImageDrawable(rippleDrawable);
        rippleView.setX(0);
        rippleView.setY(0);
        
        // 将波纹视图添加到RadioGroup所在的容器中
        if (parent.getParent() instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) parent.getParent();
            container.addView(rippleView);
            
            // 设置波纹初始大小和位置
            rippleView.setScaleX(0);
            rippleView.setScaleY(0);
            rippleView.setPivotX(centerX);
            rippleView.setPivotY(centerY);
            
            // 播放波纹动画
            rippleView.animate()
                .scaleX(2.0f)
                .scaleY(2.0f)
                .alpha(0)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    // 动画结束后移除波纹视图
                    container.removeView(rippleView);
                })
                .start();
        }
    }
}