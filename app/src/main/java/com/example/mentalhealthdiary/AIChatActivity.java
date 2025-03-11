package com.example.mentalhealthdiary;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.ChatAdapter;
import com.example.mentalhealthdiary.config.RemoteConfig;
import com.example.mentalhealthdiary.config.ApiConfig;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.service.ChatRequest;
import com.example.mentalhealthdiary.service.ChatResponse;
import com.example.mentalhealthdiary.service.ChatService;
import com.example.mentalhealthdiary.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private ChatService chatService;
    private boolean serviceBound = false;
    private Handler thinkingTimeHandler = new Handler(Looper.getMainLooper());
    private Runnable thinkingTimeRunnable;
    private long thinkingStartTime;
    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ChatService.ACTION_CHAT_RESPONSE:
                        String response = intent.getStringExtra(ChatService.EXTRA_RESPONSE);
                        long chatId = intent.getLongExtra(ChatService.EXTRA_CHAT_ID, -1);
                        
                        // 获取思考用时
                        long thinkingTime = (System.currentTimeMillis() - adapter.getThinkingStartTime()) / 1000;
                        
                        // 移除加载消息并停止动画
                        adapter.stopThinkingAnimation();
                        
                        // 在响应前添加思考用时
                        String responseWithTime = String.format("(思考用时：%d秒)\n\n%s", thinkingTime, response);
                        
                        // 添加 AI 响应
                        messages.add(new ChatMessage(responseWithTime, false, currentPersonality.getId()));
                        adapter.notifyItemInserted(messages.size() - 1);
                        
                        // 保存消息
                        saveMessage(responseWithTime, false, currentPersonality.getId());
                        sendButton.setEnabled(true);
                        
                        // 滚动到底部
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                        break;
                        
                    case ChatService.ACTION_CHAT_ERROR:
                        // 获取思考用时
                        thinkingTime = (System.currentTimeMillis() - adapter.getThinkingStartTime()) / 1000;
                        
                        // 错误时也要停止动画
                        adapter.stopThinkingAnimation();
                        String error = intent.getStringExtra(ChatService.EXTRA_ERROR);
                        
                        // 在错误信息前添加思考用时
                        String errorWithTime = String.format("(思考用时：%d秒)\n\n❌ %s", thinkingTime, error);
                        showError(errorWithTime);
                        sendButton.setEnabled(true);
                        
                        // 滚动到底部
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                        break;
                }
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChatService.ChatBinder binder = (ChatService.ChatBinder) service;
            chatService = binder.getService();
            serviceBound = true;
            
            if (currentHistoryId != -1 && chatService.isThinking(currentHistoryId)) {
                boolean hasLoading = false;
                for (ChatMessage msg : messages) {
                    if (msg.isLoading()) {
                        hasLoading = true;
                        break;
                    }
                }
                
                if (!hasLoading) {
                    messages.add(new ChatMessage("", false, true));
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            chatService = null;
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadCurrentPersonality();

        adapter = new ChatAdapter(messages, currentPersonality);
        chatRecyclerView.setAdapter(adapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (messages.isEmpty()) {
            messages.add(new ChatMessage(currentPersonality.getWelcomeMessage(), false));
            adapter.notifyDataSetChanged();
            chatRecyclerView.post(() -> {
                chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            });
        }

        database = AppDatabase.getInstance(this);
        
        currentHistoryId = getIntent().getLongExtra("chat_history_id", -1);
        
        if (currentHistoryId == -1) {
            currentHistoryId = PreferenceManager.getLastChatId(this);
        }
        
        if (currentHistoryId != -1) {
            messages.add(new ChatMessage("正在加载对话...", false, true));
            adapter.notifyItemInserted(0);
            
            loadExistingChat(currentHistoryId);
        } else {
            createNewChat();
        }

        sendButton.setOnClickListener(v -> {
            String userMessage = messageInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                clearLoadingStates();
                
                messages.add(new ChatMessage(userMessage, true));
                adapter.notifyItemInserted(messages.size() - 1);
                messageInput.setText("");
                
                messages.add(new ChatMessage("", false, true));
                int loadingPos = messages.size() - 1;
                adapter.notifyItemInserted(loadingPos);
                
                chatRecyclerView.scrollToPosition(loadingPos);
                
                sendMessage(userMessage);
            }
        });

        Intent serviceIntent = new Intent(this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ChatService.ACTION_CHAT_START);
        filter.addAction(ChatService.ACTION_CHAT_RESPONSE);
        filter.addAction(ChatService.ACTION_CHAT_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(chatReceiver, filter);
    }

    private void loadCurrentPersonality() {
        String personalityId = PreferenceManager.getCurrentPersonalityId(this);
        Log.d("AIChatActivity", "Loading personality ID: " + personalityId);
        
        if (personalityId == null) {
            personalityId = "default";
            PreferenceManager.saveCurrentPersonalityId(this, personalityId);
        }
        
        currentPersonality = AIPersonalityConfig.getPersonalityById(personalityId);
        Log.d("AIChatActivity", "Loaded personality: " + 
              (currentPersonality != null ? currentPersonality.getName() + ", ID: " + currentPersonality.getId() : "null"));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentPersonality.getName());
        }
    }

    private void updatePersonality(AIPersonality personality) {
        if (personality != null) {
            currentPersonality = personality;
            
            // 更新标题栏
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(personality.getName());
            }
            
            // 更新聊天适配器
            adapter.setCurrentPersonality(personality);
            
            // 保存当前对话，使用新的性格ID
            saveCurrentChat();
            
            // 保存选择的性格ID
            PreferenceManager.saveCurrentPersonalityId(this, personality.getId());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadCurrentPersonality();
            Log.d("AIChatActivity", "Personality changed to: " + 
                  (currentPersonality != null ? currentPersonality.getName() : "null"));
            startNewChat();
        }
    }

    private void startNewChat() {
        // 先保存当前对话
        if (currentHistoryId != -1) {
            saveCurrentChat();
        }
        
        // 创建新对话
        createNewChat();
    }

    private void sendMessage(String message) {
        if (chatService != null) {
            // 保存用户消息
            saveMessage(message, true, currentPersonality.getId());
            
            // 构建消息历史
            List<ChatRequest.Message> apiMessages = new ArrayList<>();
            
            // 添加系统提示语
            if (currentPersonality != null) {
                apiMessages.add(new ChatRequest.Message(
                    "system",
                    currentPersonality.getSystemPrompt()
                ));
            }
            
            // 添加用户消息
            apiMessages.add(new ChatRequest.Message("user", message));
            
            // 创建请求，使用 ApiConfig 获取模型名称
            String modelName = ApiConfig.getModelName(this);
            if (modelName.isEmpty()) {
                showError("请先在设置中配置 API");
                return;
            }
            
            ChatRequest request = new ChatRequest(
                apiMessages,
                modelName
            );
            
            // 添加加载消息
            ChatMessage loadingMessage = ChatMessage.createLoadingMessage();
            messages.add(loadingMessage);
            adapter.stopThinkingAnimation();  // 使用 stopThinkingAnimation 替代 resetThinkingTimer
            adapter.notifyItemInserted(messages.size() - 1);
            
            // 发送请求
            chatService.sendChatRequest(request, currentHistoryId);
            messageInput.setText("");
        }
    }

    private void showError(String message) {
        removeThinkingMessage();
        
        messages.add(new ChatMessage("❌ " + message, false));
        adapter.notifyItemInserted(messages.size() - 1);
        
        chatRecyclerView.post(() -> {
            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    private void loadExistingChat(long historyId) {
        executorService.execute(() -> {
            try {
                ChatHistory history = database.chatHistoryDao().getHistoryById(historyId);
                if (history != null && history.getMessages() != null) {
                    Log.d("ChatDebug", "Loading messages from JSON: " + history.getMessages());
                    
                    Gson gson = new GsonBuilder()
                        .serializeNulls()
                        .create();
                    Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                    List<ChatMessage> historyMessages = gson.fromJson(history.getMessages(), type);
                    
                    // 使用历史记录中保存的 personalityId，而不是当前选择的
                    String chatPersonalityId = history.getPersonalityId();
                    Log.d("ChatDebug", "Loading chat with personality ID: " + chatPersonalityId);
                    
                    if (chatPersonalityId != null) {
                        AIPersonality chatPersonality = AIPersonalityConfig.getPersonalityById(chatPersonalityId);
                        if (chatPersonality != null) {
                            Log.d("ChatDebug", "Updating current personality to: " + chatPersonality.getName());
                            currentPersonality = chatPersonality;
                        }
                    }

                    runOnUiThread(() -> {
                        messages.clear();
                        if (historyMessages != null && !historyMessages.isEmpty()) {
                            messages.addAll(historyMessages);
                            
                            adapter.setCurrentPersonality(currentPersonality);
                            
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(currentPersonality.getName());
                            }
                        } else {
                            messages.add(new ChatMessage(
                                currentPersonality.getWelcomeMessage(),
                                false,
                                currentPersonality.getId()
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    });
                } else {
                    runOnUiThread(this::createNewChat);
                }
            } catch (Exception e) {
                Log.e("ChatDebug", "Error loading chat: ", e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    showError("加载对话时出错");
                    createNewChat();
                });
            }
        });
    }

    private void createNewChat() {
        executorService.execute(() -> {
            try {
                // 创建新的聊天历史记录
                ChatHistory history = new ChatHistory(
                    new Date(),
                    "新对话",
                    "[]",
                    currentPersonality.getId()  // 使用当前选择的 AI 性格
                );
                
                long newHistoryId = database.chatHistoryDao().insert(history);
                currentHistoryId = newHistoryId;
                
                // 清空消息列表
                runOnUiThread(() -> {
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    
                    // 添加欢迎消息
                    ChatMessage welcomeMessage = new ChatMessage(
                        currentPersonality.getWelcomeMessage(),
                        false,
                        currentPersonality.getId()  // 使用当前选择的 AI 性格
                    );
                    messages.add(welcomeMessage);
                    adapter.notifyDataSetChanged();
                    
                    chatRecyclerView.post(() -> {
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                    
                    saveMessage(welcomeMessage.getMessage(), false, currentPersonality.getId());
                });
            } catch (Exception e) {
                Log.e("ChatDebug", "创建新对话失败", e);
                e.printStackTrace();
            }
        });
    }

    private void saveCurrentChat() {
        if (currentHistoryId != -1 && messages != null && !messages.isEmpty()) {
            executorService.execute(() -> {
                try {
                    String title = generateChatTitle();
                    String messagesJson = convertMessagesToJson(messages);
                    
                    // 获取原始对话
                    ChatHistory existingHistory = database.chatHistoryDao().getHistoryById(currentHistoryId);
                    
                    // 只有在创建新对话时才使用当前性格ID，否则保持原有性格ID
                    String personalityId = existingHistory != null ? 
                        existingHistory.getPersonalityId() : currentPersonality.getId();
                    
                    Log.d("ChatDebug", "保存聊天历史，对话ID: " + currentHistoryId + 
                        ", 使用性格ID: " + personalityId);
                    
                    ChatHistory history = new ChatHistory(
                        new Date(),
                        title, 
                        messagesJson,
                        personalityId  // 使用正确的性格ID
                    );
                    history.setId(currentHistoryId);
                    database.chatHistoryDao().update(history);
                    
                    PreferenceManager.saveLastChatId(this, currentHistoryId);
                } catch (Exception e) {
                    Log.e("ChatDebug", "保存对话失败", e);
                    e.printStackTrace();
                }
            });
        }
    }

    private String generateChatTitle() {
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
        Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();
        String json = gson.toJson(messages);
        Log.d("ChatDebug", "Converting messages to JSON: " + json);
        return json;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.saveLastChatId(this, currentHistoryId);
        saveCurrentChat();
    }

    private void clearLoadingStates() {
        removeThinkingMessage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        if (chatReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(chatReceiver);
        }
        
        if (executorService != null) {
            try {
                saveCurrentChat();
                Thread.sleep(100);
                executorService.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        stopThinkingTimeUpdate();
    }

    private void saveMessage(String content, boolean isUser, String personalityId) {
        executorService.execute(() -> {
            try {
                ChatMessage message = new ChatMessage(content, isUser, personalityId);
                message.setChatId(currentHistoryId);
                message.setTimestamp(System.currentTimeMillis());
                
                AppDatabase.getInstance(this).chatMessageDao().insert(message);
                Log.d("ChatDebug", "成功保存消息: " + content);
            } catch (Exception e) {
                Log.e("ChatDebug", "保存消息失败: " + e.getMessage());
                e.printStackTrace();
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

    private void startThinkingTimeUpdate() {
        if (thinkingTimeRunnable != null) {
            thinkingTimeHandler.removeCallbacks(thinkingTimeRunnable);
        }
        
        thinkingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - thinkingStartTime;
                int seconds = (int) (elapsedTime / 1000);
                
                if (!messages.isEmpty() && messages.get(messages.size() - 1).isLoading()) {
                    ChatMessage loadingMessage = messages.get(messages.size() - 1);
                    loadingMessage.setMessage(String.format("AI思考中 (%d秒)", seconds));
                    adapter.notifyItemChanged(messages.size() - 1);
                }
                
                thinkingTimeHandler.postDelayed(this, 1000);
            }
        };
        
        thinkingTimeHandler.post(thinkingTimeRunnable);
    }
    
    private void stopThinkingTimeUpdate() {
        if (thinkingTimeRunnable != null) {
            thinkingTimeHandler.removeCallbacks(thinkingTimeRunnable);
            thinkingTimeRunnable = null;
        }
    }

    private void removeThinkingMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isLoading()) {
                messages.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }
} 