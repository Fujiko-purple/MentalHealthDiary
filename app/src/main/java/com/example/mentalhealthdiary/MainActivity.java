package com.example.mentalhealthdiary;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import android.content.Context;
import android.os.Looper;
import android.app.TimePickerDialog;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        // 初始化天气选择
        weatherRadioGroup = findViewById(R.id.weatherRadioGroup);
        weatherRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.weather_sunny) {
                selectedWeather = "晴";
            } else if (checkedId == R.id.weather_cloudy) {
                selectedWeather = "多云";
            } else if (checkedId == R.id.weather_rainy) {
                selectedWeather = "雨";
            }
        });
        
        // 设置图片插入按钮
        findViewById(R.id.btnInsertImage).setOnClickListener(v -> {
            openImagePicker();
        });
        
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
        // 设置当前日期为记录的日期
        selectedDate = entry.getDate();
        updateDateButtonText();

        // 设置心情评分
        int radioButtonId = -1;
        switch (entry.getMoodScore()) {
            case 1: radioButtonId = R.id.mood_1; break;
            case 2: radioButtonId = R.id.mood_2; break;
            case 3: radioButtonId = R.id.mood_3; break;
            case 4: radioButtonId = R.id.mood_4; break;
            case 5: radioButtonId = R.id.mood_5; break;
        }
        moodRadioGroup.check(radioButtonId);

        // 设置日记内容
        diaryContent.setText(entry.getDiaryContent());

        // 修改保存按钮文本
        saveButton.setText("更新");

        // 保存正在编辑的记录ID
        currentEditingId = entry.getId();
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
            
            currentEditingId = 0;
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                diaryContent.setText("");
                moodRadioGroup.clearCheck();
                weatherRadioGroup.clearCheck();
                selectedWeather = null;
                selectedDate = null;
                updateDateButtonText();
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
    private void openImagePicker() {
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
                // 在光标位置插入图片引用
                insertTextAtCursor("📷 [图片]");
                
                // 您可能想要保存图片或处理图片URI
                // 这里只是简单地插入了一个标记
            }
        }
    }
}