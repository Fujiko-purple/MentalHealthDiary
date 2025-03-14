package com.example.mentalhealthdiary.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.AIPersonality;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Date;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private List<ChatHistory> histories = new ArrayList<>();
    private OnHistoryClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
    private Set<Long> selectedItems = new HashSet<>();
    private int selectedPosition = -1;

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
        
        holder.checkBox.setOnClickListener(v -> {
            toggleSelection(history.getId());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                selectedPosition = holder.getAdapterPosition();
                listener.onHistoryClick(history);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, history);
            return true;
        });

        holder.itemView.setSelected(position == selectedPosition);
    }

    private void showPopupMenu(View view, ChatHistory history) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.menu_chat_history_item);
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                if (listener != null) {
                    listener.onHistoryEdit(history);
                }
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                if (listener != null) {
                    listener.onHistoryDelete(history);
                }
                return true;
            }
            return false;
        });
        
        popup.show();
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
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedItems.clear();
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedItems() {
        return selectedItems;
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
            checkBox.setChecked(selectedItems.contains(history.getId()));
            
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