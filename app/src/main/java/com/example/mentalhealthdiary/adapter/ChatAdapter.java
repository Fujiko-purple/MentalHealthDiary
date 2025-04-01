package com.example.mentalhealthdiary.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_LOADING = 2;
    private static final int TYPE_MESSAGE_USER = 3;
    private List<ChatMessage> messages;
    private AIPersonality currentPersonality;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int ANIMATION_INTERVAL = 1000;
    private static final int FADE_DURATION = 300;
    private Context context;
    private OnMessageEditListener messageEditListener;
    private boolean waitingResponse = false;
    private Typeface catGirlFont = null;

    // 添加接口定义
    public interface OnMessageEditListener {
        void onMessageEdited(int position, String newMessage);
    }

    public ChatAdapter(List<ChatMessage> messages, AIPersonality personality, Context context) {
        this.messages = messages;
        this.currentPersonality = personality;
        this.context = context;
    }

    public void setOnMessageEditListener(OnMessageEditListener listener) {
        this.messageEditListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isLoading()) {
            return TYPE_LOADING;
        } else if (message.isUser()) {
            return TYPE_MESSAGE_USER;
        } else {
            return TYPE_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            // 根据消息类型选择不同的布局
            int layoutRes = viewType == TYPE_MESSAGE_USER ? 
                    R.layout.item_chat_message_user : 
                    R.layout.item_chat_message;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(layoutRes, parent, false);
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
                
                // 关键修改：检查是否有任何加载消息存在于列表中
                boolean hasLoadingMessage = checkForLoadingMessage();
                
                // 只有在没有加载消息且不在等待响应时才允许编辑
                if (!hasLoadingMessage && !waitingResponse) {
                    // 设置长按监听
                    messageHolder.messageText.setOnLongClickListener(v -> {
                        // 显示编辑框和确认按钮，隐藏文本框
                        messageHolder.messageText.setVisibility(View.GONE);
                        messageHolder.editContainer.setVisibility(View.VISIBLE);
                        messageHolder.messageEditText.setText(message.getMessage());
                        messageHolder.messageEditText.requestFocus();
                        messageHolder.messageEditText.setSelection(message.getMessage().length());

                        // 显示软键盘
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(messageHolder.messageEditText, InputMethodManager.SHOW_IMPLICIT);

                        return true;
                    });

                    // 设置确认按钮点击监听
                    messageHolder.confirmEditButton.setOnClickListener(v -> {
                        String newMessage = messageHolder.messageEditText.getText().toString().trim();
                        if (!newMessage.isEmpty() && !newMessage.equals(message.getMessage())) {
                            // 创建新消息对象
                            ChatMessage editedMessage = new ChatMessage(newMessage, true);
                            
                            // 删除当前位置的消息
                            messages.remove(position);
                            notifyItemRemoved(position);
                            
                            // 在列表末尾添加新消息
                            messages.add(editedMessage);
                            notifyItemInserted(messages.size() - 1);
                            
                            // 通知编辑完成
                            if (messageEditListener != null) {
                                messageEditListener.onMessageEdited(messages.size() - 1, newMessage);
                            }
                        }
                        
                        // 隐藏编辑框和确认按钮，显示文本框
                        messageHolder.messageText.setVisibility(View.VISIBLE);
                        messageHolder.editContainer.setVisibility(View.GONE);
                        
                        // 隐藏软键盘
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(messageHolder.messageEditText.getWindowToken(), 0);
                    });

                    // 设置取消按钮点击监听
                    messageHolder.cancelEditButton.setOnClickListener(v -> {
                        // 恢复原始文本
                        messageHolder.messageEditText.setText(message.getMessage());
                        
                        // 隐藏编辑框和按钮，显示文本框
                        messageHolder.messageText.setVisibility(View.VISIBLE);
                        messageHolder.editContainer.setVisibility(View.GONE);
                        
                        // 隐藏软键盘
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(messageHolder.messageEditText.getWindowToken(), 0);
                    });
                } else {
                    // 禁用编辑功能
                    messageHolder.messageText.setOnLongClickListener(null);
                    messageHolder.messageText.setLongClickable(false);
                    // 确保编辑容器是隐藏的
                    messageHolder.editContainer.setVisibility(View.GONE);
                    messageHolder.messageText.setVisibility(View.VISIBLE);
                }
            } else {
                // 根据AI性格选择不同的气泡背景
                if ("cat_girl".equals(message.getPersonalityId())) {
                    // 猫娘专属气泡
                    messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_cat_girl);
                    
                    // 检查是否是思考中的消息
                    if (message.getMessage().contains("思考中")) {
                        messageHolder.messageText.setText(message.getMessage());
                    } else {
                        // 直接显示原始消息，不再转换
                        // 如果仍想保留转换功能作为备份，可以加个条件判断
                        if (!message.getMessage().contains("喵～") && !message.getMessage().contains("呜喵～")) {
                            messageHolder.messageText.setText(transformToCatGirlStyle(message.getMessage()));
                        } else {
                            messageHolder.messageText.setText(message.getMessage());
                        }
                    }
                } else {
                    // 默认AI气泡
                    messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
                    messageHolder.messageText.setText(message.getMessage());
                }
                
                // AI消息不需要长按编辑功能
                messageHolder.messageText.setOnLongClickListener(null);
            }

            // 应用字体
            if (catGirlFont != null && "cat_girl".equals(currentPersonality.getId())) {
                messageHolder.messageText.setTypeface(catGirlFont);
            } else {
                messageHolder.messageText.setTypeface(Typeface.DEFAULT);
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
                            mainHandler.postDelayed(this, 500); // 每0.5秒更新一次动画帧
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
        EditText messageEditText;
        ImageView avatarImage;
        View editContainer;
        ImageButton confirmEditButton;
        ImageButton cancelEditButton;
        
        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageEditText = itemView.findViewById(R.id.messageEditText);
            editContainer = itemView.findViewById(R.id.editContainer);
            confirmEditButton = itemView.findViewById(R.id.confirmEditButton);
            cancelEditButton = itemView.findViewById(R.id.cancelEditButton);
            if (itemView.findViewById(R.id.avatarImage) != null) {
                avatarImage = itemView.findViewById(R.id.avatarImage);
            }
        }
        
        void bindMessage(ChatMessage message, AIPersonality personality) {
            messageText.setText(message.getMessage());
            if (messageEditText != null) {
                messageEditText.setText(message.getMessage());
            }
            
            if (message.isUser()) {
                if (avatarImage != null) {
                    avatarImage.setVisibility(View.GONE);
                }
            } else {
                if (avatarImage != null) {
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
                            avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                        }
                    } catch (Exception e) {
                        Log.e("ChatAdapter", "Error loading avatar", e);
                        avatarImage.setImageResource(R.drawable.ic_ai_assistant);
                    }
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

    // 添加设置等待状态的方法
    public void setWaitingResponse(boolean waiting) {
        this.waitingResponse = waiting;
        notifyDataSetChanged(); // 刷新所有项以更新可编辑状态
    }

    // 添加一个辅助方法来检查是否有加载消息
    private boolean checkForLoadingMessage() {
        for (ChatMessage msg : messages) {
            if (msg.isLoading()) {
                return true;
            }
        }
        return false;
    }

    // 添加转换文本为猫娘风格的方法
    private String transformToCatGirlStyle(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // 已经有猫娘风格的不再处理
        if (message.startsWith("喵～") || message.endsWith("喵～") || 
            message.contains("呜喵") || message.contains("nya")) {
            return message;
        }
        
        // 随机使用的猫娘语气词
        String[] catSounds = {
            "喵～", "喵喵～", "喵呜～", "nya～", "呜喵～"
        };
        
        // 随机选择一个猫叫声
        String catSound = catSounds[(int)(Math.random() * catSounds.length)];
        
        // 替换常见词语为猫娘风格
        message = message.replaceAll("(?i)我认为", "人家认为");
        message = message.replaceAll("(?i)我觉得", "猫猫觉得");
        message = message.replaceAll("(?i)我想", "人家想");
        
        // 处理句子结尾
        if (message.endsWith("。") || message.endsWith(".")) {
            message = message.substring(0, message.length()-1) + "～ " + catSound;
        } else if (message.endsWith("!") || message.endsWith("！")) {
            message = message.substring(0, message.length()-1) + "！" + catSound;
        } else if (!message.endsWith("～")) {
            // 如果不是以上情况，且不已经以波浪号结尾，添加猫叫和波浪号
            message = message + " " + catSound;
        }
        
        // 段落处理 - 每个段落结尾添加猫叫
        String[] paragraphs = message.split("\n\n");
        if (paragraphs.length > 1) {
            for (int i = 0; i < paragraphs.length - 1; i++) {
                // 不是以猫叫结束的段落添加猫叫
                if (!paragraphs[i].endsWith("喵～") && !paragraphs[i].endsWith("nya～") &&
                    !paragraphs[i].contains("呜喵～")) {
                    String randomCatSound = catSounds[(int)(Math.random() * catSounds.length)];
                    paragraphs[i] = paragraphs[i] + " " + randomCatSound;
                }
            }
            message = String.join("\n\n", paragraphs);
        }
        
        return message;
    }

    // 设置猫娘字体的方法
    public void setCatGirlFont(Typeface font) {
        this.catGirlFont = font;
    }
} 