package com.example.mentalhealthdiary;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
        // 使用自定义对话框替代AlertDialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_breathing_record);
        
        // 设置对话框宽度为屏幕宽度的85%
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 修改对话框文本内容
        TextView titleText = dialog.findViewById(R.id.dialogTitleText);
        TextView messageText = dialog.findViewById(R.id.dialogMessageText);
        
        titleText.setText("删除记录");
        messageText.setText("确定要删除选中的 " + selectedItems.size() + " 条记录吗？");
        
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button deleteButton = dialog.findViewById(R.id.deleteButton);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        deleteButton.setOnClickListener(v -> {
            List<BreathingSession> toDelete = new ArrayList<>();
            for (int position : selectedItems) {
                toDelete.add(((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.get(position));
            }
            batchDelete(toDelete);
            dialog.dismiss();
        });
        
        dialog.show();
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
        // 使用自定义对话框替代AlertDialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_breathing_record);
        
        // 设置对话框宽度为屏幕宽度的85%
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 修改对话框文本内容
        TextView titleText = dialog.findViewById(R.id.dialogTitleText);
        TextView messageText = dialog.findViewById(R.id.dialogMessageText);
        
        titleText.setText("清空记录");
        messageText.setText("确定要清空所有呼吸记录吗？此操作不可撤销。");
        
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button deleteButton = dialog.findViewById(R.id.deleteButton);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        deleteButton.setOnClickListener(v -> {
            clearAllRecords();
            dialog.dismiss();
        });
        
        dialog.show();
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

    private void showDeleteConfirmationDialog(int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_breathing_record);
        
        // 设置对话框宽度为屏幕宽度的85%
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button deleteButton = dialog.findViewById(R.id.deleteButton);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        deleteButton.setOnClickListener(v -> {
            // 执行删除操作
            deleteBreathingRecord(position);
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void deleteBreathingRecord(int position) {
        BreathingSession session = ((BreathingHistoryAdapter) recyclerView.getAdapter()).sessions.get(position);
        
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
                holder.achievementText.setBackgroundResource(R.drawable.achievement_master_background);
            } else if (session.duration >= 300) { // 5分钟以上
                holder.achievementText.setText("进阶者");
                holder.achievementIcon.setImageResource(R.drawable.ic_achievement_intermediate);
                holder.achievementText.setBackgroundResource(R.drawable.achievement_intermediate_background);
            } else {
                holder.achievementText.setText("初心者");
                holder.achievementIcon.setImageResource(R.drawable.ic_achievement_beginner);
                holder.achievementText.setBackgroundResource(R.drawable.achievement_beginner_background);
            }

            // 设置长按删除
            holder.itemView.setOnLongClickListener(v -> {
                if (!isSelectionMode) {
                    showDeleteConfirmationDialog(position);
                }
                return true;
            });

            // 设置选择模式的点击效果
            if (isSelectionMode) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(selectedItems.contains(position));
                
                // 使用背景资源来表示选中状态，而不是CardView的stroke
                CardView cardView = (CardView) holder.itemView;
                if (selectedItems.contains(position)) {
                    cardView.setCardBackgroundColor(getResources().getColor(R.color.selected_item_background));
                } else {
                    cardView.setCardBackgroundColor(getResources().getColor(R.color.item_background));
                }
                
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
                holder.checkBox.setVisibility(View.GONE);
                ((CardView) holder.itemView).setCardBackgroundColor(getResources().getColor(R.color.item_background));
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