package com.example.mentalhealthdiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.AIPersonality;

import java.util.List;

public class AIPersonalityAdapter extends RecyclerView.Adapter<AIPersonalityAdapter.ViewHolder> {
    private List<AIPersonality> personalities;
    private String selectedId;
    private OnPersonalitySelectedListener listener;

    public interface OnPersonalitySelectedListener {
        void onPersonalitySelected(AIPersonality personality);
    }

    public AIPersonalityAdapter(List<AIPersonality> personalities, 
                              String selectedId,
                              OnPersonalitySelectedListener listener) {
        this.personalities = personalities;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_personality, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AIPersonality personality = personalities.get(position);
        
        holder.nameText.setText(personality.getName());
        holder.descriptionText.setText(personality.getDescription());
        
        // 设置头像
        int resourceId = holder.itemView.getContext().getResources()
                .getIdentifier(personality.getAvatar(), "drawable", 
                        holder.itemView.getContext().getPackageName());
        holder.avatarImage.setImageResource(resourceId);
        
        // 设置选中状态
        holder.selectRadio.setChecked(personality.getId().equals(selectedId));
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            selectedId = personality.getId();
            listener.onPersonalitySelected(personality);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return personalities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView nameText;
        TextView descriptionText;
        RadioButton selectRadio;

        ViewHolder(View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            nameText = itemView.findViewById(R.id.nameText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            selectRadio = itemView.findViewById(R.id.selectRadio);
        }
    }
} 