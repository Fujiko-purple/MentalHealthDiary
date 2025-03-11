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
        ChatMessage message = messages.get(position);
        
        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).bind(message);
            if (loadingAnimationHandler == null) {
                loadingAnimationHandler = new Handler();
                loadingAnimationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateLoadingDots();
                        loadingAnimationHandler.postDelayed(this, 500);
                    }
                }, 500);
            }
        } else {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;

            Log.d("ChatAdapter", String.format(
                "Binding message at position %d: content='%s', isUser=%b, personalityId='%s'",
                position,
                message.getMessage(),
                message.isUser(),
                message.getPersonalityId()
            ));

            messageHolder.messageText.setText(message.getMessage());
            
            if (message.isUser()) {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_sent);
                messageHolder.messageContainer.setGravity(Gravity.END);
                messageHolder.avatarImage.setVisibility(View.GONE);
            } else {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
                messageHolder.messageContainer.setGravity(Gravity.START);
                messageHolder.avatarImage.setVisibility(View.VISIBLE);
                
                Log.d("ChatAdapter", "Message position " + position + ":");
                Log.d("ChatAdapter", "  Message: " + message.getMessage());
                Log.d("ChatAdapter", "  Personality ID: " + message.getPersonalityId());
                
                AIPersonality messagePersonality = message.getPersonalityId() != null ?
                        AIPersonalityConfig.getPersonalityById(message.getPersonalityId()) :
                        currentPersonality;
                
                Log.d("ChatAdapter", "  Using personality: " + messagePersonality.getName());
                Log.d("ChatAdapter", "  Avatar: " + messagePersonality.getAvatar());
                
                try {
                    String avatarName = messagePersonality.getAvatar();
                    int resourceId = holder.itemView.getContext().getResources()
                            .getIdentifier(avatarName, "drawable", 
                                    holder.itemView.getContext().getPackageName());
                    
                    Log.d("ChatAdapter", "  Resource ID: " + resourceId);
                    
                    if (resourceId != 0) {
                        Glide.with(holder.itemView.getContext())
                            .load(resourceId)
                            .circleCrop()
                            .into(messageHolder.avatarImage);
                    } else {
                        Log.e("ChatAdapter", "  Avatar resource not found: " + avatarName);
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
        if (loadingDots == 0) {
            currentLoadingView.setText(".");
        } else if (loadingDots == 1) {
            currentLoadingView.setText("..");
        } else {
            currentLoadingView.setText("...");
        }
        loadingDots = (loadingDots + 1) % 3;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {
        TextView loadingText;
        TextView loadingDots;
        
        LoadingViewHolder(View itemView) {
            super(itemView);
            loadingText = itemView.findViewById(R.id.loadingText);
            loadingDots = itemView.findViewById(R.id.loadingDots);
        }
        
        void bind(ChatMessage message) {
            loadingText.setText(message.getMessage().isEmpty() ? 
                "AI思考中" : message.getMessage());
            currentLoadingView = loadingDots;
            updateLoadingDots();
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