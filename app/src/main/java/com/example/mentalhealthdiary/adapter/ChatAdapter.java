package com.example.mentalhealthdiary.adapter;

import android.os.Handler;
import android.os.Looper;
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
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int ANIMATION_INTERVAL = 1000;
    private static final int FADE_DURATION = 300;

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
        } else {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;
            messageHolder.bindMessage(message, currentPersonality);
            
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
            // 删除旧的加载动画代码
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {
        TextView loadingText;
        TextView timerText;
        private Runnable animationRunnable;
        private Runnable timerRunnable;
        
        LoadingViewHolder(View itemView) {
            super(itemView);
            loadingText = itemView.findViewById(R.id.loadingText);
            timerText = itemView.findViewById(R.id.timerText);
        }
        
        void bind(ChatMessage message) {
            if (message.getThinkingStartTime() == 0) {
                message.setThinkingStartTime(System.currentTimeMillis());
            }
            
            // 使用消息中保存的 personalityId，而不是当前的 personality
            String personalityId = message.getPersonalityId();
            if (personalityId == null || personalityId.isEmpty()) {
                personalityId = currentPersonality != null ? currentPersonality.getId() : "default";
            }
            
            startThinkingAnimation(personalityId);
            startTimer(message);
        }

        private void startTimer(ChatMessage message) {
            stopTimer();
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (timerText != null && itemView.getWindowToken() != null) {
                            long elapsedTime = (System.currentTimeMillis() - message.getThinkingStartTime()) / 1000;
                            timerText.setText(String.format("思考用时: %ds", elapsedTime));
                            mainHandler.postDelayed(this, 1000);
                        }
                    } catch (Exception e) {
                        Log.e("ChatAdapter", "Timer error", e);
                    }
                }
            };
            mainHandler.post(timerRunnable);
        }

        private void startThinkingAnimation(String personalityId) {
            stopThinkingAnimation();
            
            animationRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (loadingText != null && itemView.getWindowToken() != null) {
                            String thinkingFrame = ChatMessage.getNextThinkingFrame(personalityId);
                            loadingText.setText(thinkingFrame);
                            mainHandler.postDelayed(this, 2000); // 每2秒更新一次动画帧
                        }
                    } catch (Exception e) {
                        Log.e("ChatAdapter", "Animation error", e);
                    }
                }
            };
            mainHandler.post(animationRunnable);
        }
        
        void stopThinkingAnimation() {
            if (animationRunnable != null) {
                mainHandler.removeCallbacks(animationRunnable);
                animationRunnable = null;
            }
            stopTimer();
        }
        
        private void stopTimer() {
            if (timerRunnable != null) {
                mainHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
            }
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView avatarImage;
        LinearLayout messageContainer;
        private final AIPersonality currentPersonality;
        
        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            currentPersonality = null;
        }
        
        void bindMessage(ChatMessage message, AIPersonality personality) {
            messageText.setText(message.getMessage());
            
            if (message.isUser()) {
                messageContainer.setGravity(Gravity.END);
                avatarImage.setVisibility(View.GONE);
            } else {
                messageContainer.setGravity(Gravity.START);
                avatarImage.setVisibility(View.VISIBLE);
                
                AIPersonality messagePersonality = message.getPersonalityId() != null ?
                        AIPersonalityConfig.getPersonalityById(message.getPersonalityId()) :
                        personality;
                
                try {
                    String avatarName = messagePersonality.getAvatar();
                    int resourceId = itemView.getContext().getResources()
                            .getIdentifier(avatarName, "drawable", 
                                    itemView.getContext().getPackageName());
                    
                    if (resourceId != 0) {
                        Glide.with(itemView.getContext())
                            .load(resourceId)
                            .circleCrop()
                            .into(avatarImage);
                    } else {
                        Log.e("ChatAdapter", "  Avatar resource not found: " + avatarName);
                        avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                    }
                } catch (Exception e) {
                    Log.e("ChatAdapter", "Error loading avatar", e);
                    avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                }
            }
        }
    }

    public void setCurrentPersonality(AIPersonality personality) {
        Log.d("ChatAdapter", "Setting personality: " + 
              (personality != null ? personality.getName() + ", ID: " + personality.getId() : "null"));
        this.currentPersonality = personality;
    }

    // 在收到 AI 响应时停止动画
    public void stopThinkingAnimation() {
        if (messages != null && !messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            if (messages.get(lastIndex).isLoading()) {
                messages.remove(lastIndex);
                notifyItemRemoved(lastIndex);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).stopThinkingAnimation();
        }
    }
    
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mainHandler.removeCallbacksAndMessages(null);
    }

    // 添加获取思考开始时间的方法
    public long getThinkingStartTime() {
        return messages.get(messages.size() - 1).getThinkingStartTime();
    }

    // 添加 setter 方法
    public void setThinkingStartTime(long time) {
        for (ChatMessage message : messages) {
            message.setThinkingStartTime(time);
        }
    }
} 