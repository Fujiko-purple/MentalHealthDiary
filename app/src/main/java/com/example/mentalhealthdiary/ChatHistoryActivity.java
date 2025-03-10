package com.example.mentalhealthdiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.annotation.NonNull;
import android.view.View;

import com.example.mentalhealthdiary.adapter.ChatHistoryAdapter;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.mentalhealthdiary.utils.PreferenceManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatHistoryActivity extends AppCompatActivity implements ChatHistoryAdapter.OnHistoryClickListener {
    private RecyclerView recyclerView;
    private ChatHistoryAdapter adapter;
    private AppDatabase database;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("聊天记录");
        }
        
        database = AppDatabase.getInstance(this);
        
        // 初始化RecyclerView
        recyclerView = findViewById(R.id.chatHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatHistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        // 设置新对话按钮
        FloatingActionButton newChatFab = findViewById(R.id.newChatFab);
        newChatFab.setOnClickListener(v -> {
            // 清除最后一次对话的ID
            PreferenceManager.saveLastChatId(this, -1);
            
            // 启动新对话
            Intent intent = new Intent(this, AIChatActivity.class);
            startActivity(intent);
            finish();
        });

        // 观察聊天历史记录
        database.chatHistoryDao().getAllHistories().observe(this, histories -> {
            if (histories != null && !histories.isEmpty()) {
                adapter.setHistories(histories);
            } else {
                // 显示空状态
                findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            }
        });

        // 添加滑动删除功能
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatHistory history = adapter.getHistories().get(position);
                onHistoryDelete(history);
            }
        }).attachToRecyclerView(recyclerView);

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 清理空对话
        executorService.execute(() -> {
            List<ChatHistory> emptyChats = database.chatHistoryDao().getEmptyChats();
            if (!emptyChats.isEmpty()) {
                database.chatHistoryDao().deleteAll(emptyChats);
            }
        });
    }

    @Override
    public void onHistoryClick(ChatHistory history) {
        // 保存最后一次对话ID
        PreferenceManager.saveLastChatId(this, history.getId());
        
        Intent intent = new Intent(this, AIChatActivity.class);
        intent.putExtra("chat_history_id", history.getId());
        startActivity(intent);
        finish();
    }

    @Override
    public void onHistoryEdit(ChatHistory history) {
        // 显示重命名对话框
        EditText input = new EditText(this);
        input.setText(history.getTitle());
        input.setSelection(input.getText().length());
        
        new AlertDialog.Builder(this)
            .setTitle("重命名对话")
            .setView(input)
            .setPositiveButton("确定", (dialog, which) -> {
                String newTitle = input.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    new Thread(() -> {
                        history.setTitle(newTitle);
                        database.chatHistoryDao().update(history);
                    }).start();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public void onHistoryDelete(ChatHistory history) {
        new AlertDialog.Builder(this)
            .setTitle("删除对话")
            .setMessage("确定要删除这个对话吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                new Thread(() -> {
                    // 如果删除的是最后一次对话，更新为最新的一条记录
                    if (PreferenceManager.getLastChatId(this) == history.getId()) {
                        // 获取除了要删除的记录之外的最新记录
                        List<ChatHistory> histories = database.chatHistoryDao().getAllHistoriesSync();
                        long newLastId = -1;
                        
                        // 找到除了要删除的记录外最新的一条
                        for (ChatHistory h : histories) {
                            if (h.getId() != history.getId()) {
                                if (newLastId == -1 || h.getTimestamp().after(
                                    database.chatHistoryDao().getHistoryById(newLastId).getTimestamp())) {
                                    newLastId = h.getId();
                                }
                            }
                        }
                        
                        // 保存新的最后一次对话ID
                        final long finalNewLastId = newLastId;
                        PreferenceManager.saveLastChatId(this, finalNewLastId);
                        
                        // 删除记录
                        database.chatHistoryDao().delete(history);
                        
                        // 在UI线程中返回到聊天界面
                        runOnUiThread(() -> {
                            Intent intent = new Intent(this, AIChatActivity.class);
                            if (finalNewLastId != -1) {
                                intent.putExtra("chat_history_id", finalNewLastId);
                            }
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        // 如果删除的不是最后一次对话，直接删除
                        database.chatHistoryDao().delete(history);
                    }
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 返回到 AIChatActivity，并加载最后一次对话
            Intent intent = new Intent(this, AIChatActivity.class);
            long lastChatId = PreferenceManager.getLastChatId(this);
            if (lastChatId != -1) {
                intent.putExtra("chat_history_id", lastChatId);
            }
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 重写返回键行为，与工具栏返回按钮行为一致
        Intent intent = new Intent(this, AIChatActivity.class);
        long lastChatId = PreferenceManager.getLastChatId(this);
        if (lastChatId != -1) {
            intent.putExtra("chat_history_id", lastChatId);
        }
        startActivity(intent);
        finish();
    }
} 