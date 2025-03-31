package com.example.mentalhealthdiary.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.MoodEntry;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoodEntryAdapter extends RecyclerView.Adapter<MoodEntryAdapter.ViewHolder> {
    private List<MoodEntry> entries = new ArrayList<>();
    private List<MoodEntry> originalEntries = new ArrayList<>();
    private OnEntryClickListener listener;
    private OnEntryDeleteListener deleteListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault());

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
        
        // 处理日记内容中的图片
        String content = entry.getDiaryContent();
        if (content != null && content.contains("[[IMG:")) {
            // 创建可变文本
            SpannableStringBuilder builder = new SpannableStringBuilder(content);
            
            // 查找所有图片标记
            Pattern pattern = Pattern.compile("\\[\\[IMG:(.*?)\\]\\]");
            Matcher matcher = pattern.matcher(content);
            
            // 记录偏移量
            int offset = 0;
            
            while (matcher.find()) {
                int start = matcher.start() - offset;
                int end = matcher.end() - offset;
                String fileName = matcher.group(1);
                
                // 加载图片
                File imageFile = new File(new File(holder.itemView.getContext().getFilesDir(), "diary_images"), fileName);
                if (imageFile.exists()) {
                    try {
                        // 加载并缩放图片
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        int width = Math.min(bitmap.getWidth(), 200);
                        int height = (int)(width * ((float)bitmap.getHeight() / bitmap.getWidth()));
                        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        
                        // 创建图片Span
                        ImageSpan imageSpan = new ImageSpan(holder.itemView.getContext(), bitmap);
                        
                        // 替换文本为图片
                        builder.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        // 更新偏移量
                        offset += (end - start - 1);
                    } catch (Exception e) {
                        Log.e("MoodEntryAdapter", "Error loading image: " + fileName, e);
                    }
                }
            }
            
            // 设置处理后的文本
            holder.contentText.setText(builder);
        } else {
            // 没有图片，直接设置文本
            holder.contentText.setText(content);
        }
        
        holder.moodEmoji.setText(getMoodEmoji(entry.getMoodScore()));
        
        // 设置天气信息
        String weather = entry.getWeather();
        if (weather != null && !weather.isEmpty()) {
            switch (weather) {
                case "晴":
                    holder.weatherText.setText("☀️ 晴天");
                    break;
                case "多云":
                    holder.weatherText.setText("☁️ 多云");
                    break;
                case "雨":
                    holder.weatherText.setText("🌧️ 雨天");
                    break;
                default:
                    holder.weatherText.setText("");
                    break;
            }
            holder.weatherText.setVisibility(View.VISIBLE);
        } else {
            holder.weatherText.setVisibility(View.GONE);
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
        private final TextView weatherText;

        public ViewHolder(View view) {
            super(view);
            dateText = view.findViewById(R.id.dateText);
            contentText = view.findViewById(R.id.contentText);
            moodEmoji = view.findViewById(R.id.moodEmoji);
            editButton = view.findViewById(R.id.editButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            weatherText = view.findViewById(R.id.weatherText);
        }
    }
} 