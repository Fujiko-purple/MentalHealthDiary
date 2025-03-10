package com.example.mentalhealthdiary;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.ChatAdapter;
import com.example.mentalhealthdiary.api.ChatApiClient;
import com.example.mentalhealthdiary.api.model.ChatRequest;
import com.example.mentalhealthdiary.api.model.ChatResponse;
import com.example.mentalhealthdiary.config.RemoteConfig;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private MaterialButton sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // 初始化欢迎消息
        if (messages.isEmpty()) {
            messages.add(new ChatMessage(
                "您好，我是心理健康助手小安，持有国家二级心理咨询师资质。\n" +
                "🤗 无论您遇到情绪困扰、压力问题还是情感困惑，我都会在这里倾听。\n" +
                "🔒 对话内容将严格保密，您可以放心倾诉～",
                false
            ));
        }

        // 初始化视图
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // 设置RecyclerView
        adapter = new ChatAdapter(messages);
        chatRecyclerView.setAdapter(adapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendButton.setOnClickListener(v -> {
            sendButton.setEnabled(false);  // 禁用按钮防止重复点击
            String userMessage = messageInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                // 添加用户消息
                messages.add(new ChatMessage(userMessage, true));
                adapter.notifyItemInserted(messages.size() - 1);
                messageInput.setText("");
                
                // 添加加载状态
                messages.add(new ChatMessage("", false, true));
                int loadingPos = messages.size() - 1;
                adapter.notifyItemInserted(loadingPos);
                
                // 滚动到底部
                chatRecyclerView.scrollToPosition(loadingPos);
                
                // 发送请求
                sendToAI(userMessage, loadingPos);
            }
        });
    }

    private void sendToAI(String userMessage, int loadingPos) {
        List<ChatRequest.Message> apiMessages = new ArrayList<>();
        
        // 添加系统预设消息
        apiMessages.add(new ChatRequest.Message("system", 
            "你是一个专业的心理健康助手，具备心理咨询师资质。请用温暖、共情的语气，结合认知行为疗法等专业方法进行对话。"
            + "回答要简明扼要（不超过300字），适当使用emoji增加亲和力。"
            + "用户可能有抑郁、焦虑等情绪问题，需保持高度敏感和同理心。"));
        
        apiMessages.add(new ChatRequest.Message("user", userMessage));
        
        // 使用配置的模型名称
        String modelName = RemoteConfig.getCustomModelName();
        ChatRequest request = new ChatRequest(apiMessages, modelName);
        
        ChatApiClient.getInstance(this).sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                runOnUiThread(() -> sendButton.setEnabled(true));
                // 移除加载状态
                messages.remove(loadingPos);
                adapter.notifyItemRemoved(loadingPos);
                
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = response.body().choices.get(0).message.content;
                    messages.add(new ChatMessage(aiResponse, false));
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                } else {
                    showError("AI响应错误: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                runOnUiThread(() -> sendButton.setEnabled(true));
                messages.remove(loadingPos);
                adapter.notifyItemRemoved(loadingPos);
                showError("网络请求失败: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 