package com.example.mentalhealthdiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.database.BreathingSession;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BreathingHistoryAdapter extends RecyclerView.Adapter<BreathingHistoryAdapter.ViewHolder> {
    private List<BreathingSession> sessions;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView achievementIcon;
        public TextView dateText, durationText, achievementText;
        public CheckBox checkBox;

        public ViewHolder(View view) {
            super(view);
            achievementIcon = view.findViewById(R.id.achievementIcon);
            dateText = view.findViewById(R.id.dateText);
            durationText = view.findViewById(R.id.durationText);
            achievementText = view.findViewById(R.id.achievementText);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_breathing_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BreathingSession session = sessions.get(position);
        
        // 设置日期和时长
        holder.dateText.setText(dateFormat.format(session.timestamp));
        holder.durationText.setText(formatDuration(session.duration));
        
        // 根据时长设置成就等级和图标
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
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    public void setSessions(List<BreathingSession> sessions) {
        this.sessions = sessions;
        notifyDataSetChanged();
    }

    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d分%d秒", minutes, remainingSeconds);
    }
} 