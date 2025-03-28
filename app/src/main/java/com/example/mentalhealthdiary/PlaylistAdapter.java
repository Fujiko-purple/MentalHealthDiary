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

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private List<String> songs;
    private List<String> selectedSongs = new ArrayList<>();
    private Context context;
    private boolean isSelectionMode = false;
    private String selectedFreeBreathingMusic;

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
        this.selectedFreeBreathingMusic = prefs.getString("last_selected_song", null);
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

    public void setCurrentPlayingSong(String song) {
        this.selectedFreeBreathingMusic = song;
        notifyDataSetChanged();  // 确保刷新列表显示
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String song = songs.get(position);
        
        // 显示歌曲名称，如果是当前选中的歌曲则加上标记和特殊样式
        String displayText = song;
        if (song.equals(selectedFreeBreathingMusic)) {
            displayText = "▶ " + song;  // 添加播放标记
            holder.textView.setTextColor(context.getResources().getColor(R.color.free_breathing_text));
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.selected_song_background));
        } else {
            holder.textView.setTextColor(Color.BLACK);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        holder.textView.setText(displayText);
        
        // 显示/隐藏复选框
        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedSongs.contains(song));

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
        TextView textView;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.songName);
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
} 