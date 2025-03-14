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
import android.widget.Button;
import android.widget.Toast;

import com.example.mentalhealthdiary.adapter.ChatHistoryAdapter;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.mentalhealthdiary.utils.PreferenceManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class ChatHistoryActivity extends AppCompatActivity implements ChatHistoryAdapter.OnHistoryClickListener {
    private RecyclerView recyclerView;
    private ChatHistoryAdapter adapter;
    private AppDatabase database;
    private ExecutorService executorService;
    private Button selectAllButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        // 设置 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("「足迹」");
        }
        
        database = AppDatabase.getInstance(this);
        
        // 初始化RecyclerView
        recyclerView = findViewById(R.id.chatHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatHistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        // 初始化按钮
        selectAllButton = findViewById(R.id.selectAllButton);
        deleteButton = findViewById(R.id.deleteButton);

        // 设置全选按钮点击事件
        selectAllButton.setOnClickListener(v -> {
            if (selectAllButton.getText().equals("全选")) {
                adapter.selectAll();
                selectAllButton.setText("取消全选");
            } else {
                adapter.clearSelection();
                selectAllButton.setText("全选");
            }
        });

        // 设置删除按钮点击事件
        deleteButton.setOnClickListener(v -> {
            Set<Long> selectedIds = adapter.getSelectedItems();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "请先选择要删除的记录", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定要删除选中的 " + selectedIds.size() + " 条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    executorService.execute(() -> {
                        // 获取当前对话ID
                        long currentChatId = PreferenceManager.getLastChatId(this);
                        
                        // 执行批量删除
                        database.chatHistoryDao().deleteByIds(new ArrayList<>(selectedIds));

                        // 获取最新的历史记录列表
                        List<ChatHistory> remainingHistories = database.chatHistoryDao().getAllHistoriesSync();
                        
                        runOnUiThread(() -> {
                            // 更新UI
                            if (remainingHistories.isEmpty()) {
                                PreferenceManager.saveLastChatId(this, -1);
                                findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                                findViewById(R.id.chatHistoryRecyclerView).setVisibility(View.GONE);
                            } else {
                                if (selectedIds.contains(currentChatId)) {
                                    ChatHistory latest = remainingHistories.get(0);
                                    PreferenceManager.saveLastChatId(this, latest.getId());
                                }
                                findViewById(R.id.emptyView).setVisibility(View.GONE);
                                findViewById(R.id.chatHistoryRecyclerView).setVisibility(View.VISIBLE);
                            }
                            
                            adapter.setHistories(remainingHistories);
                            adapter.clearSelection();
                            selectAllButton.setText("全选");
                            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("取消", null)
                .show();
        });

        // 观察聊天历史记录
        database.chatHistoryDao().getAllHistories().observe(this, histories -> {
            if (histories != null) {
                adapter.setHistories(histories);
                
                // 更新空状态视图
                boolean isEmpty = histories.isEmpty();
                findViewById(R.id.emptyView).setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                findViewById(R.id.chatHistoryRecyclerView).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        // 修改滑动删除的实现
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatHistory history = adapter.getHistories().get(position);
                
                // 显示删除确认对话框，并在用户取消时恢复item
                new AlertDialog.Builder(ChatHistoryActivity.this)
                    .setTitle("删除对话")
                    .setMessage("确定要删除这个对话吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        new Thread(() -> {
                            database.chatHistoryDao().delete(history);
                            
                            if (PreferenceManager.getLastChatId(ChatHistoryActivity.this) == history.getId()) {
                                List<ChatHistory> histories = database.chatHistoryDao().getAllHistoriesSync();
                                
                                runOnUiThread(() -> {
                                    if (histories.isEmpty()) {
                                        PreferenceManager.saveLastChatId(ChatHistoryActivity.this, -1);
                                    } else {
                                        ChatHistory latest = histories.get(0);
                                        PreferenceManager.saveLastChatId(ChatHistoryActivity.this, latest.getId());
                                    }
                                    Toast.makeText(ChatHistoryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(ChatHistoryActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }).start();
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 用户取消删除，恢复item的显示
                        adapter.notifyItemChanged(position);
                    })
                    .setOnCancelListener(dialog -> {
                        // 用户点击对话框外部，也要恢复item的显示
                        adapter.notifyItemChanged(position);
                    })
                    .show();
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
        
        // 直接跳转，不使用延迟
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

    @Override
    public void onHistoryDelete(ChatHistory history) {
        // 实现接口要求的方法，但实际的删除逻辑已经移到了滑动删除中
        new Thread(() -> {
            database.chatHistoryDao().delete(history);
            
            if (PreferenceManager.getLastChatId(this) == history.getId()) {
                List<ChatHistory> histories = database.chatHistoryDao().getAllHistoriesSync();
                
                runOnUiThread(() -> {
                    if (histories.isEmpty()) {
                        PreferenceManager.saveLastChatId(this, -1);
                    } else {
                        ChatHistory latest = histories.get(0);
                        PreferenceManager.saveLastChatId(this, latest.getId());
                    }
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
} 