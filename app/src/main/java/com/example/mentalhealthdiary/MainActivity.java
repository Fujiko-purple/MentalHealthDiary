package com.example.mentalhealthdiary;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private Button saveButton;
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

        // 设置RecyclerView
        RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        aiButton.setOnClickListener(v -> {
            showAIAssistantDialog();
        });

        // 确保使用正确的ID初始化weatherRadioGroup
        weatherRadioGroup = findViewById(R.id.weatherRadioGroup);
        Log.d("WeatherDebug", "天气RadioGroup ID: " + R.id.weatherRadioGroup);
        Log.d("WeatherDebug", "天气RadioGroup 是否为null: " + (weatherRadioGroup == null));
        
        weatherRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.weather_sunny) {
                selectedWeather = "晴";
                Log.d("WeatherDebug", "选择了晴天");
            } else if (checkedId == R.id.weather_cloudy) {
                selectedWeather = "多云";
                Log.d("WeatherDebug", "选择了多云");
            } else if (checkedId == R.id.weather_rainy) {
                selectedWeather = "雨";
                Log.d("WeatherDebug", "选择了下雨");
            }
        });
        
        // 设置图片插入按钮
        findViewById(R.id.btnInsertImage).setOnClickListener(v -> pickImage());
        
        // 设置位置插入按钮
        findViewById(R.id.btnInsertLocation).setOnClickListener(v -> {
            getCurrentLocation();
        });
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
        
        // 设置天气 - 添加调试日志
        String weather = entry.getWeather();
        Log.d("WeatherDebug", "读取到的天气: " + weather);
        
        if (weather != null) {
            selectedWeather = weather;
            
            // 打印每个RadioButton的ID
            Log.d("WeatherDebug", "晴天ID: " + R.id.weather_sunny);
            Log.d("WeatherDebug", "多云ID: " + R.id.weather_cloudy);
            Log.d("WeatherDebug", "下雨ID: " + R.id.weather_rainy);
            
            // 检查当前选中的RadioButton
            Log.d("WeatherDebug", "当前选中的ID: " + weatherRadioGroup.getCheckedRadioButtonId());
            
            // 清除之前的选择
            weatherRadioGroup.clearCheck();
            
            // 根据天气值选择相应的RadioButton
            if (weather.equals("晴")) {
                weatherRadioGroup.check(R.id.weather_sunny);
            } else if (weather.equals("多云")) {
                weatherRadioGroup.check(R.id.weather_cloudy);
            } else if (weather.equals("雨")) {
                weatherRadioGroup.check(R.id.weather_rainy);
            }
            
            // 再次检查选中状态
            Log.d("WeatherDebug", "设置后的ID: " + weatherRadioGroup.getCheckedRadioButtonId());
        } else {
            // 如果没有天气数据，清除选择
            weatherRadioGroup.clearCheck();
        }
        
        // 设置日记内容
        loadDiaryContent(entry.getDiaryContent());
        
        // 修改保存按钮文本
        saveButton.setText("更新");
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
        saveButton.setText("保存");
        currentEditingId = 0;
        selectedDate = null;  // 重置选择的日期
        updateDateButtonText();
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
        
        MoodEntry entry = new MoodEntry(selectedDate, moodScore, content, selectedWeather);
        
        if (currentEditingId > 0) {
            entry.setId(currentEditingId);
        }
        
        executorService.execute(() -> {
            if (currentEditingId > 0) {
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
                
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
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
        String[] options = {"时间从新到旧", "时间从旧到新", "心情从好到坏", "心情从坏到好"};
        new AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: adapter.sortByDateDesc(); break;
                    case 1: adapter.sortByDateAsc(); break;
                    case 2: adapter.sortByMoodDesc(); break;
                    case 3: adapter.sortByMoodAsc(); break;
                }
            })
            .show();
    }

    private void showFilterDialog() {
        String[] options = {"全部时间", "最近一周", "最近一月", "最近三月"};
        new AlertDialog.Builder(this)
            .setTitle("时间筛选")
            .setItems(options, (dialog, which) -> {
                Calendar cal = Calendar.getInstance();
                switch (which) {
                    case 0: adapter.resetTimeFilter(); break;
                    case 1: 
                        cal.add(Calendar.DAY_OF_YEAR, -7);
                        adapter.filterByDate(cal.getTime());
                        break;
                    case 2:
                        cal.add(Calendar.MONTH, -1);
                        adapter.filterByDate(cal.getTime());
                        break;
                    case 3:
                        cal.add(Calendar.MONTH, -3);
                        adapter.filterByDate(cal.getTime());
                        break;
                }
            })
            .show();
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
}