package com.example.mentalhealthdiary;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.MoodEntryAdapter;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.example.mentalhealthdiary.service.TipsWorkManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.mentalhealthdiary.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        selectedDate = new Date();
        updateDateButtonText();

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
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());

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
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                selectedDate = calendar.getTime();
                updateDateButtonText();
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        datePickerButton.setText(dateFormat.format(selectedDate));
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

        MoodEntry entry = new MoodEntry(selectedDate, moodScore, content);
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
        saveButton.setText("保存");
        currentEditingId = 0;
        selectedDate = new Date();
        updateDateButtonText();
    }

    private void saveMoodEntry() {
        if (currentEditingId > 0) {
            updateEntry();
            return;
        }

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

        MoodEntry entry = new MoodEntry(selectedDate, moodScore, content);
        
        // 在后台线程中保存数据
        executorService.execute(() -> {
            database.moodEntryDao().insert(entry);
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                diaryContent.setText("");
                moodRadioGroup.clearCheck();
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
        executorService.shutdown();
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
            .setIcon(R.drawable.ic_breathing)
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
}