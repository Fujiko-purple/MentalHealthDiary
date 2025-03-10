package com.example.mentalhealthdiary.adapter;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_LOADING = 2;
    private List<ChatMessage> messages;
    private AIPersonality currentPersonality;
    private Handler loadingAnimationHandler = new Handler();
    private int loadingDots = 0;
    private TextView currentLoadingView;

    public ChatAdapter(List<ChatMessage> messages, AIPersonality personality) {
        this.messages = messages;
        this.currentPersonality = personality;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isLoading() ? TYPE_LOADING : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_LOADING) {
            LoadingViewHolder loadingHolder = (LoadingViewHolder) holder;
            startLoadingAnimation(loadingHolder.loadingDots);
        } else {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;
            ChatMessage message = messages.get(position);

            messageHolder.messageText.setText(message.getMessage());
            
            if (message.isUser()) {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_sent);
                messageHolder.messageContainer.setGravity(Gravity.END);
                messageHolder.avatarImage.setVisibility(View.GONE);
            } else {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
                messageHolder.messageContainer.setGravity(Gravity.START);
                messageHolder.avatarImage.setVisibility(View.VISIBLE);
                
                AIPersonality messagePersonality = message.getPersonalityId() != null ?
                        AIPersonalityConfig.getPersonalityById(message.getPersonalityId()) :
                        currentPersonality;
                
                try {
                    String avatarName = messagePersonality.getAvatar();
                    Log.d("ChatAdapter", "Loading avatar for personality: " + 
                          messagePersonality.getName() + ", Avatar: " + avatarName);
                    
                    int resourceId = holder.itemView.getContext().getResources()
                            .getIdentifier(avatarName, "drawable", 
                                    holder.itemView.getContext().getPackageName());
                    
                    if (resourceId != 0) {
                        Glide.with(holder.itemView.getContext())
                            .load(resourceId)
                            .circleCrop()
                            .into(messageHolder.avatarImage);
                    } else {
                        messageHolder.avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                    }
                } catch (Exception e) {
                    Log.e("ChatAdapter", "Error loading avatar", e);
                    messageHolder.avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                }
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == TYPE_LOADING) {
            stopLoadingAnimation();
        }
    }

    private void startLoadingAnimation(TextView dotsView) {
        currentLoadingView = dotsView;
        loadingDots = 0;
        updateLoadingDots();
    }

    private void stopLoadingAnimation() {
        loadingAnimationHandler.removeCallbacksAndMessages(null);
        currentLoadingView = null;
    }

    private void updateLoadingDots() {
        if (currentLoadingView != null) {
            String dots = "";
            for (int i = 0; i < loadingDots; i++) {
                dots += ".";
            }
            currentLoadingView.setText(dots);
            loadingDots = (loadingDots + 1) % 4;
            loadingAnimationHandler.postDelayed(this::updateLoadingDots, 500);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        TextView loadingDots;

        LoadingViewHolder(View view) {
            super(view);
            loadingDots = view.findViewById(R.id.loadingDots);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView avatarImage;
        LinearLayout messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
    }

    public void setCurrentPersonality(AIPersonality personality) {
        Log.d("ChatAdapter", "Setting personality: " + 
              (personality != null ? personality.getName() + ", ID: " + personality.getId() : "null"));
        this.currentPersonality = personality;
    }
} 