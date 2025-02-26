package com.example.mentalhealthdiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.MoodEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.ViewHolder> {
    private List<MoodEntry> entries = new ArrayList<>();
    private List<MoodEntry> originalEntries = new ArrayList<>();
    private OnEntryClickListener listener;
    private OnEntryDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnEntryClickListener {
        void onEntryClick(MoodEntry entry);
    }

    public interface OnEntryDeleteListener {
        void onEntryDelete(MoodEntry entry);
    }

    public void setOnEntryClickListener(OnEntryClickListener listener) {
        this.listener = listener;
    }

    public void setOnEntryDeleteListener(OnEntryDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoodEntry entry = entries.get(position);
        holder.dateText.setText(dateFormat.format(entry.getDate()));
        holder.contentText.setText(entry.getDiaryContent());
        holder.moodEmoji.setText(getMoodEmoji(entry.getMoodScore()));
        
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntryClick(entry);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onEntryDelete(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setEntries(List<MoodEntry> entries) {
        this.originalEntries = new ArrayList<>(entries);
        this.entries = new ArrayList<>(entries);
        notifyDataSetChanged();
    }

    public void resetFilter() {
        entries = new ArrayList<>(originalEntries);
        notifyDataSetChanged();
    }

    public void resetTimeFilter() {
        resetFilter();  // 重用现有的重置方法
    }

    public void filterByDate(Date startDate) {
        List<MoodEntry> filteredList = new ArrayList<>();
        for (MoodEntry entry : originalEntries) {
            if (entry.getDate().after(startDate)) {
                filteredList.add(entry);
            }
        }
        entries = filteredList;
        notifyDataSetChanged();
    }

    public void sortByDateDesc() {
        entries.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        notifyDataSetChanged();
    }

    public void sortByDateAsc() {
        entries.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        notifyDataSetChanged();
    }

    public void sortByMoodDesc() {
        entries.sort((a, b) -> b.getMoodScore() - a.getMoodScore());
        notifyDataSetChanged();
    }

    public void sortByMoodAsc() {
        entries.sort((a, b) -> a.getMoodScore() - b.getMoodScore());
        notifyDataSetChanged();
    }

    private String getMoodEmoji(int score) {
        switch (score) {
            case 1: return "😢";
            case 2: return "😕";
            case 3: return "😐";
            case 4: return "😊";
            case 5: return "😄";
            default: return "";
        }
    }

    public void filterByMood(int moodScore) {
        List<MoodEntry> filteredList = new ArrayList<>();
        for (MoodEntry entry : originalEntries) {
            if (entry.getMoodScore() == moodScore) {
                filteredList.add(entry);
            }
        }
        entries = filteredList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView contentText;
        private final TextView moodEmoji;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        public ViewHolder(View view) {
            super(view);
            dateText = view.findViewById(R.id.dateText);
            contentText = view.findViewById(R.id.contentText);
            moodEmoji = view.findViewById(R.id.moodEmoji);
            editButton = view.findViewById(R.id.editButton);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
} 