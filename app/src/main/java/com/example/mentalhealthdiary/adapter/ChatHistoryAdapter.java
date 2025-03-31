package com.example.mentalhealthdiary.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.ChatHistoryActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private List<ChatHistory> histories = new ArrayList<>();
    private OnHistoryClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
    private Set<Long> selectedItems = new HashSet<>();
    private int selectedPosition = -1;
    private boolean isSelectionMode = false;
    private long currentChatId = -1;

    public interface OnHistoryClickListener {
        void onHistoryClick(ChatHistory history);
        void onHistoryEdit(ChatHistory history);
        void onHistoryDelete(ChatHistory history);
    }

    public ChatHistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatHistory history = histories.get(position);
        holder.bind(history);
        
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedItems.contains(history.getId())) {
                    selectedItems.remove(history.getId());
                } else {
                    selectedItems.add(history.getId());
                }
                notifyItemChanged(position);
            } else {
                if (listener != null) {
                    selectedPosition = holder.getAdapterPosition();
                    listener.onHistoryClick(history);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, history);
            return true;
        });

        if (history.getId() == currentChatId && !isSelectionMode) {
            holder.itemView.setBackgroundResource(R.drawable.current_chat_background);
        } else if (position == selectedPosition && !isSelectionMode) {
            holder.itemView.setBackgroundResource(R.drawable.selected_item_background);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
        
        holder.itemView.setHapticFeedbackEnabled(true);
    }

    private void showPopupMenu(View view, ChatHistory history) {
        // 创建自定义弹出窗口
        View popupView = LayoutInflater.from(view.getContext()).inflate(R.layout.popup_menu_custom, null);
        
        // 创建PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        
        // 设置点击事件
        View editItem = popupView.findViewById(R.id.action_edit_layout);
        View deleteItem = popupView.findViewById(R.id.action_delete_layout);
        
        editItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryEdit(history);
            }
            popupWindow.dismiss();
        });
        
        deleteItem.setOnClickListener(v -> {
            popupWindow.dismiss();
            // 使用 ChatHistoryActivity 的删除确认对话框
            if (view.getContext() instanceof ChatHistoryActivity) {
                ((ChatHistoryActivity) view.getContext()).showDeleteConfirmationDialog(history);
            }
        });
        
        // 设置背景和动画
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setAnimationStyle(R.style.PopupAnimation);
        
        // 测量视图
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        
        // 计算显示位置 - 修改为显示在右下方
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        
        // 计算水平位置 - 显示在右侧
        int x = location[0] + view.getWidth() - popupView.getMeasuredWidth();
        
        // 计算垂直位置 - 显示在项目下方
        int y = location[1] + view.getHeight();
        
        // 显示弹出窗口
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, x, y);
    }

    @Override
    public int getItemCount() {
        return histories.size();
    }

    public void setHistories(List<ChatHistory> histories) {
        this.histories = histories != null ? histories : new ArrayList<>();
        selectedItems.clear();
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public List<ChatHistory> getHistories() {
        return histories;
    }

    public void toggleSelection(long id) {
        if (selectedItems.contains(id)) {
            selectedItems.remove(id);
        } else {
            selectedItems.add(id);
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedItems.clear();
        for (ChatHistory history : histories) {
            selectedItems.add(history.getId());
        }
        setSelectionMode(true);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedItems.clear();
        setSelectionMode(false);
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setCurrentChatId(long id) {
        this.currentChatId = id;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImage;
        private TextView titleText;
        private TextView timeText;
        private CheckBox checkBox;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            titleText = itemView.findViewById(R.id.titleText);
            timeText = itemView.findViewById(R.id.timeText);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
        
        public void bind(ChatHistory history) {
            titleText.setText(history.getTitle());
            timeText.setText(formatDate(history.getTimestamp()));
            
            if (isSelectionMode) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selectedItems.contains(history.getId()));
                
                checkBox.setOnClickListener(v -> {
                    if (checkBox.isChecked()) {
                        selectedItems.add(history.getId());
                    } else {
                        selectedItems.remove(history.getId());
                    }
                });
            } else {
                checkBox.setVisibility(View.GONE);
                checkBox.setOnClickListener(null);
            }
            
            String personalityId = history.getPersonalityId();
            Log.d("ChatHistoryAdapter", String.format(
                "绑定历史记录: ID=%d, 标题=%s, 性格ID=%s",
                history.getId(),
                history.getTitle(),
                personalityId
            ));
            
            if (personalityId != null) {
                String avatarResourceName = getAvatarResourceName(personalityId);
                Log.d("ChatHistoryAdapter", "头像资源名称: " + avatarResourceName);
                
                try {
                    int resourceId = itemView.getContext().getResources()
                            .getIdentifier(avatarResourceName, "drawable", 
                                    itemView.getContext().getPackageName());
                    Log.d("ChatHistoryAdapter", "资源ID: " + resourceId);
                    
                    if (resourceId != 0) {
                        Glide.with(itemView.getContext())
                            .load(resourceId)
                            .circleCrop()
                            .into(avatarImage);
                    } else {
                        Log.e("ChatHistoryAdapter", "找不到资源: " + avatarResourceName);
                        avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                    }
                } catch (Exception e) {
                    Log.e("ChatHistoryAdapter", "加载头像出错", e);
                    e.printStackTrace();
                    avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                }
            } else {
                Log.w("ChatHistoryAdapter", "没有找到性格ID");
                avatarImage.setImageResource(R.drawable.ic_ai_assistant);
            }
        }
    }
    
    // 根据personalityId获取对应的头像资源名称
    private String getAvatarResourceName(String personalityId) {
        if (personalityId == null) return "ic_ai_assistant";
        
        Log.d("ChatHistoryAdapter", "获取头像，性格ID: " + personalityId);
        
        switch (personalityId) {
            case "ganyu_cbt":
                return "ic_ganyu_counselor";
            case "natsume_narrative_pro":
                return "ic_natsume";
            case "cat_girl":
                return "ic_cat_girl";
            case "kafka_rebt":
                return "ic_kafka";
            case "tiga_divine":
                return "ic_tiga_divine";
            case "yangjian_tactician":
                return "ic_yangjian";
            case "dt_music":
                return "ic_davidtao";
            case "patrick_naive":
                return "ic_patrick_wisdom";
            case "default":
                return "ic_counselor";
            default:
                Log.w("ChatHistoryAdapter", "未知的性格ID: " + personalityId);
                return "ic_ai_assistant";
        }
    }

    private String formatDate(Date date) {
        return dateFormat.format(date);
    }
} 