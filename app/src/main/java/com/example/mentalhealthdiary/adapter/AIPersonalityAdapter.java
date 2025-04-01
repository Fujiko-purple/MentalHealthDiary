package com.example.mentalhealthdiary.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.google.android.material.chip.Chip;

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
        
        // 修改头像加载逻辑
        try {
            // 先尝试从 drawable 资源加载
            int resourceId = holder.itemView.getContext().getResources()
                    .getIdentifier(personality.getAvatar(), "drawable", 
                            holder.itemView.getContext().getPackageName());
            
            if (resourceId != 0) {
                // 使用 Glide 加载并处理图片
                Glide.with(holder.itemView.getContext())
                    .load(resourceId)
                    .circleCrop()  // 将图片裁剪成圆形
                    .into(holder.avatarImage);
            } else {
                // 如果找不到资源，使用默认头像
                holder.avatarImage.setImageResource(R.drawable.ic_ai_assistant);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 发生错误时使用默认头像
            holder.avatarImage.setImageResource(R.drawable.ic_ai_assistant);
        }
        
        // 设置选中状态
        boolean isSelected = personality.getId().equals(selectedId);

        // 设置选中状态的视觉效果
        holder.selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.avatarGlow.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // 设置标签（可以根据性格类型设置不同标签）
        String tag = "";
        switch (personality.getId()) {
            case "ganyu_cbt":
                tag = "温柔疗愈";
                break;
            case "natsume_narrative_pro":
                tag = "故事疗法";
                break;
            case "cat_girl":
                tag = "可爱治愈";
                break;
            case "kafka_rebt":
                tag = "危险魅力";
                break;
            case "tiga_divine":
                tag = "光之疗愈";
                break;
            case "yangjian_tactician":
                tag = "战术思维";
                break;
            case "dt_music":
                tag = "音乐解析";
                break;
            case "patrick_naive":
                tag = "反向逻辑";
                break;
            default:
                tag = "专业咨询";
                break;
        }
        holder.personalityTag.setText(tag);
        
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
        ImageView selectedIndicator;
        View avatarGlow;
        Chip personalityTag;

        ViewHolder(View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            nameText = itemView.findViewById(R.id.nameText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            selectedIndicator = itemView.findViewById(R.id.selectedIndicator);
            avatarGlow = itemView.findViewById(R.id.avatarGlow);
            personalityTag = itemView.findViewById(R.id.personalityTag);
        }
    }
} 