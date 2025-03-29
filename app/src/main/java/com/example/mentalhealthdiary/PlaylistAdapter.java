package com.example.mentalhealthdiary;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.view.GestureDetector;
import android.view.MotionEvent;
import androidx.annotation.NonNull;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> 
        implements View.OnTouchListener {
    private List<String> songs;
    private List<String> selectedSongs = new ArrayList<>();
    private Context context;
    private boolean isSelectionMode = false;
    private String currentPlayingSong;
    private GestureDetector gestureDetector;

    public interface OnItemClickListener {
        void onItemClick(String song);
        void onItemLongClick(String song);
    }

    private OnItemClickListener listener;

    public PlaylistAdapter(Context context, List<String> songs, OnItemClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
        
        // 从 SharedPreferences 获取上次选择的歌曲
        SharedPreferences prefs = context.getSharedPreferences("custom_playlist", Context.MODE_PRIVATE);
        this.currentPlayingSong = prefs.getString("last_selected_song", null);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                // 处理长按事件
            }
        });
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        selectedSongs.clear();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public List<String> getSelectedSongs() {
        return new ArrayList<>(selectedSongs);
    }

    public void toggleSelection(String song) {
        if (selectedSongs.contains(song)) {
            selectedSongs.remove(song);
        } else {
            selectedSongs.add(song);
        }
        notifyDataSetChanged();
    }

    public boolean isSelected(String song) {
        return selectedSongs.contains(song);
    }

    public void setCurrentPlayingSong(String song) {
        this.currentPlayingSong = song;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String song = songs.get(position);
        
        // 只使用 currentPlayingSong 来控制高亮
        if (song.equals(currentPlayingSong)) {
            holder.songName.setText("▶ " + song);  // 添加播放标记
            holder.songName.setTextColor(context.getResources().getColor(R.color.free_breathing_text));
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.selected_song_background));
        } else {
            holder.songName.setText(song);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.songName.setTextColor(context.getResources().getColor(R.color.song_text_color));
        }
        
        // 显示/隐藏复选框
        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected(song));

        // 设置触摸监听器
        holder.itemView.setOnTouchListener(this);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(song);
            } else {
                listener.onItemClick(song);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(song);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView songName;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.songName);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    public void updateSongs(List<String> newSongs) {
        this.songs = new ArrayList<>(newSongs);
        notifyDataSetChanged();
    }

    public List<String> getSongs() {
        return songs;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
} 