package com.example.mentalhealthdiary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private List<String> songs;
    private List<String> selectedSongs = new ArrayList<>();
    private Context context;
    private boolean isSelectionMode = false;

    public interface OnItemClickListener {
        void onItemClick(String song);
        void onItemLongClick(String song);
    }

    private OnItemClickListener listener;

    public PlaylistAdapter(Context context, List<String> songs, OnItemClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String song = songs.get(position);
        holder.textView.setText(song);
        
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
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }
} 