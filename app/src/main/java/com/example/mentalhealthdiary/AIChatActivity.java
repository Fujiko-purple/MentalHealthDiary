package com.example.mentalhealthdiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.ChatAdapter;
import com.example.mentalhealthdiary.api.ChatApiClient;
import com.example.mentalhealthdiary.api.model.ChatRequest;
import com.example.mentalhealthdiary.api.model.ChatResponse;
import com.example.mentalhealthdiary.config.RemoteConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
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
    private AppDatabase database;
    private long currentHistoryId = -1;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // 设置 Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI 心理助手");
        }

        database = AppDatabase.getInstance(this);
        
        // 检查是否是从历史记录打开的特定对话
        currentHistoryId = getIntent().getLongExtra("chat_history_id", -1);
        
        if (currentHistoryId == -1) {
            // 如果不是从历史记录打开的，则加载最后一次的对话
            currentHistoryId = PreferenceManager.getLastChatId(this);
        }
        
        if (currentHistoryId != -1) {
            loadChatHistory(currentHistoryId);
        } else {
            // 如果没有历史对话，显示欢迎消息
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
                // 清理所有旧的加载状态
                clearLoadingStates();
                
                // 添加用户消息
                messages.add(new ChatMessage(userMessage, true));
                adapter.notifyItemInserted(messages.size() - 1);
                messageInput.setText("");
                
                // 添加新的加载状态
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, ChatHistoryActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_new_chat) {
            // 清除当前对话ID
            currentHistoryId = -1;
            PreferenceManager.saveLastChatId(this, -1);
            
            // 清除消息列表
            messages.clear();
            // 添加欢迎消息
            messages.add(new ChatMessage(
                "您好，我是心理健康助手小安，持有国家二级心理咨询师资质。\n" +
                "🤗 无论您遇到情绪困扰、压力问题还是情感困惑，我都会在这里倾听。\n" +
                "🔒 对话内容将严格保密，您可以放心倾诉～",
                false
            ));
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    // 移除加载状态
                    messages.remove(loadingPos);
                    adapter.notifyItemRemoved(loadingPos);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String aiResponse = response.body().choices.get(0).message.content;
                        messages.add(new ChatMessage(aiResponse, false));
                        adapter.notifyItemInserted(messages.size() - 1);
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                    } else {
                        showError("AI响应错误: " + (response.code() == 429 ? "请求太频繁，请稍后再试" : 
                                response.code() == 503 ? "服务暂时不可用" : 
                                "错误代码 " + response.code()));
                    }
                });
            }

            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    showError(t.getMessage().contains("timeout") ? 
                        "请求超时，请检查网络连接" : 
                        "网络请求失败: " + t.getMessage());
                });
            }
        });
    }

    private void showError(String message) {
        // 只移除最后一个加载状态
        if (!messages.isEmpty() && messages.get(messages.size() - 1).isLoading()) {
            messages.remove(messages.size() - 1);
            adapter.notifyItemRemoved(messages.size());
        }
        
        // 添加错误消息
        messages.add(new ChatMessage("❌ " + message, false));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
    }

    private void loadChatHistory(long historyId) {
        new Thread(() -> {
            ChatHistory history = database.chatHistoryDao().getHistoryById(historyId);
            if (history != null) {
                try {
                    Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                    List<ChatMessage> historyMessages = new Gson().fromJson(history.getMessages(), type);
                    runOnUiThread(() -> {
                        messages.clear();
                        messages.addAll(historyMessages);
                        adapter.notifyDataSetChanged();
                        chatRecyclerView.scrollToPosition(messages.size() - 1);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void saveCurrentChat() {
        if (messages.isEmpty()) return;
        
        new Thread(() -> {
            // 如果是已存在的对话，保持原有标题
            String title = "新对话";
            if (currentHistoryId != -1) {
                ChatHistory existingHistory = database.chatHistoryDao().getHistoryById(currentHistoryId);
                if (existingHistory != null) {
                    title = existingHistory.getTitle();
                }
            } else {
                // 只有新对话才生成标题（使用第一条用户消息）
                for (ChatMessage msg : messages) {
                    if (msg.isUser()) {
                        title = msg.getMessage();
                        if (title.length() > 20) {
                            title = title.substring(0, 20) + "...";
                        }
                        break;
                    }
                }
            }
            
            // 将消息列表转换为JSON字符串
            String messagesJson = new Gson().toJson(messages);
            
            ChatHistory history = new ChatHistory(new Date(), title, messagesJson);
            if (currentHistoryId != -1) {
                history.setId(currentHistoryId);
                database.chatHistoryDao().update(history);
            } else {
                currentHistoryId = database.chatHistoryDao().insert(history);
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentChat();
        // 保存最后一次对话的ID
        PreferenceManager.saveLastChatId(this, currentHistoryId);
    }

    private void clearLoadingStates() {
        // 从后向前遍历，删除所有加载状态的消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isLoading()) {
                messages.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }
    }
} 