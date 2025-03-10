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
        apiMessages.add(new ChatRequest.Message("user", userMessage));
        
        ChatRequest request = new ChatRequest(apiMessages, "deepseek-ai/DeepSeek-R1");
        
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