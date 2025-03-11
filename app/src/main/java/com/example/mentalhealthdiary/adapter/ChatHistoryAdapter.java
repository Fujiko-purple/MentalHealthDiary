package com.example.mentalhealthdiary.adapter;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.ChatHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private List<ChatHistory> histories = new ArrayList<>();
    private OnHistoryClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
    private Set<Long> selectedItems = new HashSet<>();

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
                listener.onHistoryClick(history);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, history);
            return true;
        });
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
        this.histories = histories;
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
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedItems() {
        return selectedItems;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView titleText;
        private TextView timeText;

        public ViewHolder(View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkBox);
            titleText = view.findViewById(R.id.titleText);
            timeText = view.findViewById(R.id.timeText);

            view.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ChatHistory history = histories.get(position);
                        toggleSelection(history.getId());
                    }
                }
            });

            view.setOnLongClickListener(v -> {
                showPopupMenu(v, histories.get(getAdapterPosition()));
                return true;
            });
        }

        public void bind(ChatHistory chatHistory) {
            titleText.setText(chatHistory.getTitle());
            timeText.setText(dateFormat.format(chatHistory.getTimestamp()));
            checkBox.setChecked(selectedItems.contains(chatHistory.getId()));
        }
    }
} 