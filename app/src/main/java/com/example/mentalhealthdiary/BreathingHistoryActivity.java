package com.example.mentalhealthdiary;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.database.BreathingSession;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BreathingHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView totalTimeText;
    private TextView sessionCountText;
    private boolean isSelectionMode = false;
    private Set<Integer> selectedItems = new HashSet<>();
    private MenuItem deleteMenuItem;
    private MenuItem selectAllMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_history);

        // 设置返回按钮
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("呼吸记录");

        recyclerView = findViewById(R.id.historyRecyclerView);
        totalTimeText = findViewById(R.id.totalTimeText);
        sessionCountText = findViewById(R.id.sessionCountText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        loadBreathingSessions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_breathing_history, menu);
        deleteMenuItem = menu.findItem(R.id.action_delete);
        selectAllMenuItem = menu.findItem(R.id.action_select_all);
        updateMenuItems();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select:
                toggleSelectionMode();
                // 显示操作提示
                Snackbar.make(findViewById(android.R.id.content), 
                    isSelectionMode ? "进入选择模式" : "退出选择模式", 
                    Snackbar.LENGTH_SHORT).show();
                return true;
            
            case R.id.action_select_all:
                selectAllItems();
                // 显示操作提示
                Snackbar.make(findViewById(android.R.id.content), 
                    "已全选 " + recyclerView.getAdapter().getItemCount() + " 项", 
                    Snackbar.LENGTH_SHORT).show();
                return true;
            
            case R.id.action_clear_all:
                showClearAllDialog();
                return true;
            
            case R.id.action_delete:
                deleteSelectedItems();
                return true;
            
            case android.R.id.home:
                if (isSelectionMode) {
                    exitSelectionMode();
                    // 显示操作提示
                    Snackbar.make(findViewById(android.R.id.content), 
                        "已退出选择模式", 
                        Snackbar.LENGTH_SHORT).show();
                    return true;
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSelectionMode() {
        isSelectionMode = !isSelectionMode;
        selectedItems.clear();
        updateMenuItems();
        updateActionBar();
        recyclerView.getAdapter().notifyDataSetChanged();
        
        // 显示提示信息
        if (isSelectionMode) {
            Snackbar.make(findViewById(android.R.id.content), 
                "点击项目进行选择", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        updateMenuItems();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void updateMenuItems() {
        if (deleteMenuItem != null) {
            deleteMenuItem.setVisible(isSelectionMode && !selectedItems.isEmpty());
        }
        if (selectAllMenuItem != null) {
            selectAllMenuItem.setVisible(isSelectionMode);
        }
    }

    private void selectAllItems() {
        selectedItems.clear();
        for (int i = 0; i < recyclerView.getAdapter().getItemCount(); i++) {
            selectedItems.add(i);
        }
        updateMenuItems();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void deleteSelectedItems() {
        new AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除选中的 " + selectedItems.size() + " 条记录吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                List<BreathingSession> toDelete = new ArrayList<>();
                for (int position : selectedItems) {
                    toDelete.add(((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.get(position));
                }
                batchDelete(toDelete);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void batchDelete(List<BreathingSession> toDelete) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            for (BreathingSession session : toDelete) {
                db.breathingSessionDao().delete(session);
            }

            int totalDuration = db.breathingSessionDao().getTotalDuration();
            List<BreathingSession> remainingSessions = db.breathingSessionDao().getAllSessions();

            runOnUiThread(() -> {
                ((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions = remainingSessions;
                recyclerView.getAdapter().notifyDataSetChanged();
                updateStatistics(remainingSessions.size(), totalDuration);
                exitSelectionMode();

                Snackbar.make(findViewById(android.R.id.content), 
                    "已删除 " + toDelete.size() + " 条记录", 
                    Snackbar.LENGTH_LONG).show();
            });
        }).start();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
            .setTitle("清空记录")
            .setMessage("确定要清空所有呼吸记录吗？此操作不可撤销。")
            .setPositiveButton("清空", (dialog, which) -> {
                clearAllRecords();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void clearAllRecords() {
        new Thread(() -> {
            AppDatabase.getInstance(this)
                .breathingSessionDao()
                .deleteAll();

            runOnUiThread(() -> {
                ((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.clear();
                recyclerView.getAdapter().notifyDataSetChanged();
                updateStatistics(0, 0);

                Snackbar.make(findViewById(android.R.id.content), 
                    "已清空所有记录", 
                    Snackbar.LENGTH_LONG).show();
            });
        }).start();
    }

    private void loadBreathingSessions() {
        new Thread(() -> {
            List<BreathingSession> sessions = AppDatabase.getInstance(this)
                .breathingSessionDao()
                .getAllSessions();

            int totalDuration = AppDatabase.getInstance(this)
                .breathingSessionDao()
                .getTotalDuration();

            runOnUiThread(() -> {
                recyclerView.setAdapter(new BreathingHistoryAdapter(sessions));
                updateStatistics(sessions.size(), totalDuration);
            });
        }).start();
    }

    private void updateStatistics(int sessionCount, int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        
        String timeText;
        if (hours > 0) {
            timeText = String.format("%d小时%d分", hours, minutes);
        } else {
            timeText = String.format("%d分钟", minutes);
        }
        totalTimeText.setText(timeText);
        
        sessionCountText.setText(String.format("%d", sessionCount));
    }


    private void updateActionBar() {
        if (isSelectionMode) {
            getSupportActionBar().setTitle("已选择 " + selectedItems.size() + " 项");
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        } else {
            getSupportActionBar().setTitle("呼吸记录");
            getSupportActionBar().setHomeAsUpIndicator(null);
        }
    }

    private void showDeleteDialog(BreathingSession session, int position) {
        new AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条呼吸记录吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                new Thread(() -> {
                    AppDatabase.getInstance(this)
                        .breathingSessionDao()
                        .delete(session);

                    int totalDuration = AppDatabase.getInstance(this)
                        .breathingSessionDao()
                        .getTotalDuration();

                    runOnUiThread(() -> {
                        ((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.remove(position);
                        recyclerView.getAdapter().notifyItemRemoved(position);
                        recyclerView.getAdapter().notifyItemRangeChanged(position, recyclerView.getAdapter().getItemCount());
                        updateStatistics(recyclerView.getAdapter().getItemCount(), totalDuration);

                        Snackbar.make(findViewById(android.R.id.content), 
                            "已删除记录", Snackbar.LENGTH_LONG)
                            .setAction("撤销", v -> {
                                undoDelete(session, position);
                            })
                            .show();
                    });
                }).start();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void undoDelete(BreathingSession session, int position) {
        new Thread(() -> {
            AppDatabase.getInstance(this)
                .breathingSessionDao()
                .insert(session);

            int totalDuration = AppDatabase.getInstance(this)
                .breathingSessionDao()
                .getTotalDuration();

            runOnUiThread(() -> {
                ((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.add(position, session);
                recyclerView.getAdapter().notifyItemInserted(position);
                recyclerView.getAdapter().notifyItemRangeChanged(position, recyclerView.getAdapter().getItemCount());
                updateStatistics(recyclerView.getAdapter().getItemCount(), totalDuration);
            });
        }).start();
    }

    class BreathingHistoryAdapter extends RecyclerView.Adapter<BreathingHistoryAdapter.ViewHolder> {
        private List<BreathingSession> sessions;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

        BreathingHistoryAdapter(List<BreathingSession> sessions) {
            this.sessions = sessions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_breathing_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BreathingSession session = sessions.get(position);
            
            holder.dateText.setText(dateFormat.format(new Date(session.timestamp)));
            holder.durationText.setText(String.format("练习时长: %d分%d秒", 
                session.duration / 60, session.duration % 60));
            
            // 根据时长设置成就等级和对应图标
            if (session.duration >= 900) { // 15分钟以上
                holder.achievementText.setText("禅定大师");
                holder.achievementIcon.setImageResource(R.drawable.ic_achievement_master);
            } else if (session.duration >= 300) { // 5分钟以上
                holder.achievementText.setText("进阶者");
                holder.achievementIcon.setImageResource(R.drawable.ic_achievement_intermediate);
            } else {
                holder.achievementText.setText("初心者");
                holder.achievementIcon.setImageResource(R.drawable.ic_achievement_beginner);
            }

            // 设置长按删除
            holder.itemView.setOnLongClickListener(v -> {
                if (!isSelectionMode) {
                    showDeleteDialog(session, position);
                }
                return true;
            });

            // 设置选择模式的点击效果
            if (isSelectionMode) {
                boolean isSelected = selectedItems.contains(position);
                holder.itemView.setBackgroundResource(
                    isSelected ? R.drawable.selected_item_background : 
                               android.R.color.transparent);
                
                // 添加选择标记
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(isSelected);
                
                holder.itemView.setOnClickListener(v -> {
                    if (selectedItems.contains(position)) {
                        selectedItems.remove(position);
                    } else {
                        selectedItems.add(position);
                    }
                    notifyItemChanged(position);
                    updateMenuItems();
                    updateActionBar();
                });
            } else {
                holder.itemView.setBackgroundResource(android.R.color.transparent);
                holder.checkBox.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;
            TextView durationText;
            TextView achievementText;
            CheckBox checkBox;
            ImageView achievementIcon;

            ViewHolder(View view) {
                super(view);
                dateText = view.findViewById(R.id.dateText);
                durationText = view.findViewById(R.id.durationText);
                achievementText = view.findViewById(R.id.achievementText);
                checkBox = view.findViewById(R.id.checkBox);
                achievementIcon = view.findViewById(R.id.achievementIcon);
            }
        }
    }
} 