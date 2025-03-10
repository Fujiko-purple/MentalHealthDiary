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
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AIPersonality currentPersonality;

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

        // 设置 Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI 心理助手");
        }

        database = AppDatabase.getInstance(this);
        
        // 获取传入的历史记录ID
        currentHistoryId = getIntent().getLongExtra("chat_history_id", -1);
        
        // 如果没有传入历史记录ID，检查是否有上次对话
        if (currentHistoryId == -1) {
            currentHistoryId = PreferenceManager.getLastChatId(this);
        }
        
        if (currentHistoryId != -1) {
            // 显示加载提示
            messages.add(new ChatMessage("正在加载对话...", false, true));
            adapter.notifyItemInserted(0);
            
            // 加载已有对话
            loadExistingChat(currentHistoryId);
        } else {
            createNewChat();
        }

        // 加载当前选择的AI性格
        loadCurrentPersonality();

        // 设置发送按钮点击事件
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

    private void loadCurrentPersonality() {
        String personalityId = PreferenceManager.getCurrentPersonalityId(this);
        if (personalityId == null) {
            personalityId = "default"; // 使用默认性格
            PreferenceManager.saveCurrentPersonalityId(this, personalityId);
        }
        currentPersonality = AIPersonalityConfig.getPersonalityById(personalityId);
        updatePersonalityUI();
    }

    private void updatePersonalityUI() {
        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentPersonality.getName());
        }

        // 如果是新对话，显示欢迎消息
        if (messages.isEmpty()) {
            messages.add(new ChatMessage(currentPersonality.getWelcomeMessage(), false));
            adapter.notifyDataSetChanged();
        }
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
        } else if (item.getItemId() == R.id.action_personality) {
            startActivityForResult(new Intent(this, AIPersonalitySelectActivity.class), 1);
            return true;
        } else if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, ChatHistoryActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_new_chat) {
            startNewChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 性格已更改，重新加载性格并开始新对话
            loadCurrentPersonality();
            startNewChat();
        }
    }

    private void startNewChat() {
        // 清除当前对话ID
        currentHistoryId = -1;
        PreferenceManager.saveLastChatId(this, -1);
        
        // 清除消息列表
        messages.clear();
        // 添加新性格的欢迎消息
        messages.add(new ChatMessage(currentPersonality.getWelcomeMessage(), false));
        adapter.notifyDataSetChanged();
    }

    private void sendToAI(String userMessage, int loadingPos) {
        List<ChatRequest.Message> apiMessages = new ArrayList<>();
        
        // 添加当前性格的系统提示词
        apiMessages.add(new ChatRequest.Message("system", currentPersonality.getSystemPrompt()));
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

    private void loadExistingChat(long historyId) {
        executorService.execute(() -> {
            try {
                ChatHistory history = database.chatHistoryDao().getHistoryById(historyId);
                if (history != null && history.getMessages() != null) {
                    Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                    List<ChatMessage> historyMessages = new Gson().fromJson(history.getMessages(), type);
                    
                    runOnUiThread(() -> {
                        try {
                            messages.clear();
                            if (historyMessages != null && !historyMessages.isEmpty()) {
                                messages.addAll(historyMessages);
                            } else {
                                // 如果没有消息，显示当前性格的欢迎消息
                                messages.add(new ChatMessage(
                                    currentPersonality.getWelcomeMessage(),
                                    false
                                ));
                            }
                            adapter.notifyDataSetChanged();
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError("加载对话失败");
                        }
                    });
                } else {
                    runOnUiThread(() -> showError("找不到对话记录"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showError("加载对话时出错"));
            }
        });
    }

    private void createNewChat() {
        // 创建新的聊天历史记录
        executorService.execute(() -> {
            ChatHistory newHistory = new ChatHistory(new Date(), "新对话", "");
            currentHistoryId = database.chatHistoryDao().insert(newHistory);
            // 保存最后一次对话的ID
            PreferenceManager.saveLastChatId(this, currentHistoryId);
            
            // 添加当前性格的欢迎消息
            runOnUiThread(() -> {
                messages.clear(); // 确保清空任何可能的加载消息
                messages.add(new ChatMessage(
                    currentPersonality.getWelcomeMessage(),
                    false
                ));
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void saveCurrentChat() {
        if (currentHistoryId != -1 && messages != null && !messages.isEmpty()) {
            // 只有当有消息时才保存
            executorService.execute(() -> {
                try {
                    // 在后台线程中获取标题
                    String title = generateChatTitle();
                    String messagesJson = convertMessagesToJson(messages);
                    
                    ChatHistory history = new ChatHistory(new Date(), title, messagesJson);
                    history.setId(currentHistoryId);
                    database.chatHistoryDao().update(history);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private String generateChatTitle() {
        // 如果是已存在的对话，保持原有标题
        if (currentHistoryId != -1) {
            try {
                ChatHistory existingHistory = database.chatHistoryDao().getHistoryById(currentHistoryId);
                if (existingHistory != null && existingHistory.getTitle() != null 
                    && !existingHistory.getTitle().isEmpty()) {
                    return existingHistory.getTitle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 只有新对话才生成标题（使用第一条用户消息）
        for (ChatMessage msg : messages) {
            if (msg.isUser()) {
                String title = msg.getMessage();
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }
                return title;
            }
        }
        return "新对话";
    }

    private String convertMessagesToJson(List<ChatMessage> messages) {
        // 实现将消息列表转换为JSON字符串的逻辑
        return new Gson().toJson(messages);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存最后一次对话的ID（这个可以在主线程执行，因为是轻量级操作）
        PreferenceManager.saveLastChatId(this, currentHistoryId);
        // 在后台线程保存对话内容
        saveCurrentChat();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保最后一次保存完成
        if (executorService != null) {
            try {
                saveCurrentChat();
                Thread.sleep(100); // 给一点时间让保存操作完成
                executorService.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
} 