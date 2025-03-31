package com.example.mentalhealthdiary.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

    // 心情常量定义 - 匹配MoodEntry中的现有分数
    private static final int MOOD_AWFUL = 1;
    private static final int MOOD_SAD = 2;
    private static final int MOOD_NEUTRAL = 3;
    private static final int MOOD_GOOD = 4;
    private static final int MOOD_HAPPY = 5;

    // 跟踪记录的展开状态
    private final List<Integer> expandedPositions = new ArrayList<>();

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
        
        // 设置日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());
        holder.dateText.setText(sdf.format(entry.getDate()));
        
        // 设置心情表情
        holder.moodEmoji.setText(getMoodEmoji(entry.getMoodScore()));
        
        // 设置心情颜色指示
        holder.moodColorIndicator.setBackgroundColor(getMoodColor(entry.getMoodScore()));
        
        // 设置心情气泡背景色
        GradientDrawable bubbleBackground = (GradientDrawable) holder.moodBubbleBackground.getBackground();
        bubbleBackground.setColor(getMoodBubbleColor(entry.getMoodScore()));
        
        // 设置内容
        holder.contentText.setText(entry.getDiaryContent());
        
        // 设置天气
        if (entry.getWeather() != null && !entry.getWeather().isEmpty()) {
            holder.weatherEmoji.setText(getWeatherEmoji(entry.getWeather()));
            holder.weatherText.setText(entry.getWeather());
            holder.weatherContainer.setVisibility(View.VISIBLE);
        } else {
            holder.weatherContainer.setVisibility(View.GONE);
        }
        
        // 检查内容是否需要展开按钮
        String content = entry.getDiaryContent();
        if (content != null && content.length() > 100) {
            holder.expandButton.setVisibility(View.VISIBLE);
            
            // 根据展开状态设置内容和按钮
            boolean isExpanded = expandedPositions.contains(position);
            if (isExpanded) {
                // 展开状态 - 显示全部内容
                holder.contentText.setMaxLines(Integer.MAX_VALUE);
                holder.contentText.setEllipsize(null);
                holder.expandButton.setRotation(180); // 翻转箭头指向上方
            } else {
                // 折叠状态 - 限制显示行数
                holder.contentText.setMaxLines(3);
                holder.contentText.setEllipsize(TextUtils.TruncateAt.END);
                holder.expandButton.setRotation(0); // 箭头指向下方
            }
            
            // 设置展开按钮点击事件
            holder.expandButton.setOnClickListener(v -> {
                // 切换展开状态
                if (isExpanded) {
                    expandedPositions.remove(Integer.valueOf(position));
                } else {
                    expandedPositions.add(position);
                }
                // 刷新当前条目
                notifyItemChanged(position);
            });
        } else {
            holder.expandButton.setVisibility(View.GONE);
            // 短内容始终完整显示
            holder.contentText.setMaxLines(Integer.MAX_VALUE);
            holder.contentText.setEllipsize(null);
        }
        
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
        expandedPositions.clear(); // 重置所有展开状态
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

    /**
     * 根据心情分数获取对应的颜色
     */
    private int getMoodColor(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return Color.parseColor("#FF8A65"); // 橙色
            case MOOD_GOOD:
                return Color.parseColor("#4FC3F7"); // 蓝色
            case MOOD_NEUTRAL:
                return Color.parseColor("#81C784"); // 绿色
            case MOOD_SAD:
                return Color.parseColor("#9575CD"); // 紫色
            case MOOD_AWFUL:
                return Color.parseColor("#E57373"); // 红色
            default:
                return Color.parseColor("#BDBDBD"); // 灰色（默认）
        }
    }
    
    /**
     * 根据心情分数获取对应的气泡背景色（更浅的色调）
     */
    private int getMoodBubbleColor(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return Color.parseColor("#FFF3E0"); // 淡橙色
            case MOOD_GOOD:
                return Color.parseColor("#E1F5FE"); // 淡蓝色
            case MOOD_NEUTRAL:
                return Color.parseColor("#E8F5E9"); // 淡绿色
            case MOOD_SAD:
                return Color.parseColor("#EDE7F6"); // 淡紫色
            case MOOD_AWFUL:
                return Color.parseColor("#FFEBEE"); // 淡红色
            default:
                return Color.parseColor("#F5F5F5"); // 淡灰色（默认）
        }
    }
    
    /**
     * 根据心情分数获取对应的表情
     */
    private String getMoodEmoji(int mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return "😄";
            case MOOD_GOOD:
                return "😊";
            case MOOD_NEUTRAL:
                return "😐";
            case MOOD_SAD:
                return "😔";
            case MOOD_AWFUL:
                return "😩";
            default:
                return "😶";
        }
    }
    
    /**
     * 根据天气类型获取对应的表情
     */
    private String getWeatherEmoji(String weather) {
        if (weather.contains("晴")) return "☀️";
        if (weather.contains("多云")) return "⛅";
        if (weather.contains("阴")) return "☁️";
        if (weather.contains("雨")) return "🌧️";
        if (weather.contains("雪")) return "❄️";
        if (weather.contains("雾")) return "🌫️";
        return "🌤️"; // 默认
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView contentText;
        private final TextView moodEmoji;
        private final ImageButton editButton;
        private final ImageButton deleteButton;
        private final TextView weatherText;
        private final View moodColorIndicator;
        private final View moodBubbleBackground;
        private final TextView weatherEmoji;
        private final LinearLayout weatherContainer;
        private final ImageButton expandButton;

        public ViewHolder(View view) {
            super(view);
            dateText = view.findViewById(R.id.dateText);
            contentText = view.findViewById(R.id.contentText);
            moodEmoji = view.findViewById(R.id.moodEmoji);
            editButton = view.findViewById(R.id.editButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            weatherText = view.findViewById(R.id.weatherText);
            moodColorIndicator = view.findViewById(R.id.moodColorIndicator);
            moodBubbleBackground = view.findViewById(R.id.moodBubbleBackground);
            weatherEmoji = view.findViewById(R.id.weatherEmoji);
            weatherContainer = view.findViewById(R.id.weatherContainer);
            expandButton = view.findViewById(R.id.expandButton);
        }
    }
} 