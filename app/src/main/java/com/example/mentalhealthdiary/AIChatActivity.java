package com.example.mentalhealthdiary;

import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mentalhealthdiary.adapter.ChatAdapter;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private MaterialButton sendButton;
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // 初始化视图
        initViews();
        setupRecyclerView();
        
        // 添加 AI 的欢迎语
        addWelcomeMessage();
        
        // 设置发送按钮点击事件
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        
        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI 心理助手");
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void addWelcomeMessage() {
        String welcomeMessage = "你好，我是你的 AI 心理咨询师。我会倾听你的心声，为你提供专业的心理支持和建议。\n\n" +
                              "你可以和我分享你的：\n" +
                              "• 日常烦恼和困扰\n" +
                              "• 情绪起伏和感受\n" +
                              "• 人际关系问题\n" +
                              "• 学习工作压力\n\n" +
                              "让我们开始对话吧！";
        
        chatMessages.add(new ChatMessage(welcomeMessage, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        // 添加用户消息
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        
        // 清空输入框
        messageInput.setText("");
        
        // 显示加载消息
        chatMessages.add(ChatMessage.createLoadingMessage());
        int loadingMessageIndex = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(loadingMessageIndex);
        
        // 滚动到最新消息
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        // 模拟 AI 回复
        chatRecyclerView.postDelayed(() -> {
            // 移除加载消息
            chatMessages.remove(loadingMessageIndex);
            chatAdapter.notifyItemRemoved(loadingMessageIndex);
            
            // 添加 AI 回复
            String aiResponse = generateAIResponse(message);
            chatMessages.add(new ChatMessage(aiResponse, false));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            
            // 滚动到最新消息
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }, 1000);
    }

    private String generateAIResponse(String userMessage) {
        // 这里是一个简单的模拟回复逻辑
        // 实际应用中，您需要接入真实的 AI API
        if (userMessage.contains("难过") || userMessage.contains("伤心")) {
            return "我理解你现在的心情。让我们一起来分析这种感受，看看是什么原因导致的。你愿意和我详细说说发生了什么吗？";
        } else if (userMessage.contains("压力") || userMessage.contains("焦虑")) {
            return "压力和焦虑是很常见的情绪反应。我建议你可以尝试一些放松技巧，比如深呼吸或冥想。你平时是怎么缓解压力的呢？";
        } else if (userMessage.contains("谢谢")) {
            return "不用谢，很高兴能帮到你。记住，我随时都在这里，如果你需要倾诉或建议，随时可以来找我。";
        } else {
            return "谢谢你的分享。作为你的AI心理咨询师，我会认真倾听你的每一句话。你愿意多告诉我一些相关的细节吗？这样我可以更好地理解和帮助你。";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 