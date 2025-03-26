package com.example.mentalhealthdiary;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.adapter.ChatAdapter;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.config.ApiConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.AIPersonality;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.example.mentalhealthdiary.service.ChatRequest;
import com.example.mentalhealthdiary.service.ChatService;
import com.example.mentalhealthdiary.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                        
                        // 获取最后一条加载消息的思考时间
                        long thinkingTime = 0;
                        ChatMessage loadingMessage = null;
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            ChatMessage msg = messages.get(i);
                            if (msg.isLoading()) {
                                loadingMessage = msg;
                                break;
                            }
                        }
                        
                        if (loadingMessage != null) {
                            thinkingTime = (System.currentTimeMillis() - loadingMessage.getThinkingStartTime()) / 1000;
                        }
                        
                        // 移除加载消息并停止动画
                        removeThinkingMessage();
                        stopThinkingTimeUpdate();
                        
                        // 在响应前添加思考用时
                        String responseWithTime = String.format("(思考用时：%d秒)\n\n%s", thinkingTime, response);
                        
                        // 添加 AI 响应
                        messages.add(new ChatMessage(responseWithTime, false, currentPersonality.getId()));
                        adapter.notifyItemInserted(messages.size() - 1);
                        
                        saveMessage(responseWithTime, false, currentPersonality.getId());
                        
                        // 重置等待状态
                        isWaitingResponse = false;
                        adapter.setWaitingResponse(false);
                        updateSendButtonState();
                        
                        // 滚动到底部
                        chatRecyclerView.post(() -> {
                            chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                        break;
                        
                    case ChatService.ACTION_CHAT_ERROR:
                        // 获取思考用时
                        thinkingTime = 0;
                        loadingMessage = null;
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            ChatMessage msg = messages.get(i);
                            if (msg.isLoading()) {
                                loadingMessage = msg;
                                break;
                            }
                        }
                        
                        if (loadingMessage != null) {
                            thinkingTime = (System.currentTimeMillis() - loadingMessage.getThinkingStartTime()) / 1000;
                        }
                        
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
                        
                        // 同样需要重置状态
                        isWaitingResponse = false;
                        adapter.setWaitingResponse(false);
                        updateSendButtonState();
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
            
            // 服务连接后，立即更新状态
            updateWaitingState();
            updateSendButtonState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            chatService = null;
            serviceBound = false;
        }
    };

    private boolean isWaitingResponse = false;
    private ChipGroup quickMessageGroup;
    private List<String> quickMessages = Arrays.asList(
        "我今天心情很好",
        "我今天心情一般",
        "我今天心情不太好",
        "我想找人聊聊",
        "分析我最近的心情"
    );

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

        adapter = new ChatAdapter(messages, currentPersonality, this);
        adapter.setOnMessageEditListener((position, newMessage) -> {
            // 不需要删除后续消息，直接请求AI回复
            requestAIResponse(position);
        });
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
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty() && !isWaitingResponse) {
                // 先添加用户消息到界面
                messages.add(new ChatMessage(message, true));
                adapter.notifyItemInserted(messages.size() - 1);
                
                // 添加思考动画消息，并立即设置开始时间
                ChatMessage loadingMessage = ChatMessage.createLoadingMessage(currentPersonality.getId());
                loadingMessage.setThinkingStartTime(System.currentTimeMillis());
                messages.add(loadingMessage);
                int loadingPos = messages.size() - 1;
                adapter.notifyItemInserted(loadingPos);
                
                // 滚动到底部
                chatRecyclerView.scrollToPosition(loadingPos);
                
                // 开始思考时间更新
                startThinkingTimeUpdate(loadingMessage);
                
                // 发送消息
                sendMessage(message);
                messageInput.setText("");
                isWaitingResponse = true;
                updateSendButtonState();
            }
        });

        // 启动并绑定服务
        Intent serviceIntent = new Intent(this, ChatService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ChatService.ACTION_CHAT_START);
        filter.addAction(ChatService.ACTION_CHAT_RESPONSE);
        filter.addAction(ChatService.ACTION_CHAT_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(chatReceiver, filter);

        quickMessageGroup = findViewById(R.id.quickMessageGroup);
        setupQuickMessages();
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
            // 检查是否启用了自定义API且配置有效
            if (!ApiConfig.isCustomApiEnabled(this) || 
                ApiConfig.getApiKey(this).isEmpty() || 
                ApiConfig.getBaseUrl(this).isEmpty() || 
                ApiConfig.getModelName(this).isEmpty()) {
                showError("请先在设置中配置并启用 API");
                return;
            }
            
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
            
            // 发送请求
            chatService.sendChatRequest(request, currentHistoryId);
        }
    }

    private void showError(String message) {
        removeThinkingMessage();
        
        messages.add(new ChatMessage("❌ " + message, false, currentPersonality.getId()));
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
                    currentPersonality.getId()
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
                        currentPersonality.getId()
                    );
                    messages.add(welcomeMessage);
                    adapter.notifyDataSetChanged();
                    
                    chatRecyclerView.post(() -> {
                        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
                    });
                    
                    saveMessage(welcomeMessage.getMessage(), false, currentPersonality.getId());
                    
                    // 为新对话设置预备消息
                    setupQuickMessages();
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
        
        // 在 onPause 中解绑服务
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
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

    private void startThinkingTimeUpdate(ChatMessage loadingMessage) {
        if (thinkingTimeRunnable != null) {
            thinkingTimeHandler.removeCallbacks(thinkingTimeRunnable);
        }
        
        // 重置开始时间为当前时间
        loadingMessage.setThinkingStartTime(System.currentTimeMillis());
        
        // 使用个性化的思考动画
        String thinkingFrame = ChatMessage.getNextThinkingFrame(loadingMessage.getPersonalityId());
        loadingMessage.setMessage(thinkingFrame + " (0秒)");
        adapter.notifyItemChanged(messages.indexOf(loadingMessage));
        
        thinkingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!messages.isEmpty() && messages.contains(loadingMessage)) {
                    long elapsedTime = System.currentTimeMillis() - loadingMessage.getThinkingStartTime();
                    int seconds = (int) (elapsedTime / 1000);
                    // 获取新的思考帧
                    String newThinkingFrame = ChatMessage.getNextThinkingFrame(loadingMessage.getPersonalityId());
                    loadingMessage.setMessage(String.format("%s (%d秒)", newThinkingFrame, seconds));
                    adapter.notifyItemChanged(messages.indexOf(loadingMessage));
                    if (messages.contains(loadingMessage)) {
                        thinkingTimeHandler.postDelayed(this, 500); // 每0.5秒更新一次动画帧
                    }
                }
            }
        };
        
        // 立即开始第一次更新
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

    private void updateSendButtonState() {
        sendButton.setEnabled(!isWaitingResponse);
        sendButton.setAlpha(isWaitingResponse ? 0.5f : 1.0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 检查服务状态并重新绑定
        if (!serviceBound) {
            Intent serviceIntent = new Intent(this, ChatService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        
        // 更新等待状态
        updateWaitingState();
        updateSendButtonState();
        
        // 恢复思考中的消息状态
        restoreThinkingState();
        
        // 根据对话ID的使用状态决定是否显示预备消息
        if (!PreferenceManager.isQuickMessageUsed(this, currentHistoryId)) {
            setupQuickMessages();
        }
    }

    // 添加新方法：恢复思考状态
    private void restoreThinkingState() {
        // 检查是否有思考中的消息
        if (serviceBound && chatService != null && currentHistoryId != -1) {
            if (chatService.isThinking(currentHistoryId)) {
                // 找到最后一条加载消息
                ChatMessage loadingMessage = null;
                for (int i = messages.size() - 1; i >= 0; i--) {
                    if (messages.get(i).isLoading()) {
                        loadingMessage = messages.get(i);
                        break;
                    }
                }
                
                // 如果没有加载消息但服务正在思考，添加一个新的加载消息
                if (loadingMessage == null) {
                    loadingMessage = ChatMessage.createLoadingMessage(currentPersonality.getId());
                    loadingMessage.setThinkingStartTime(System.currentTimeMillis() - 
                        chatService.getThinkingTime(currentHistoryId));
                    messages.add(loadingMessage);
                    adapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                }
                
                // 重新启动思考时间更新
                startThinkingTimeUpdate(loadingMessage);
                isWaitingResponse = true;
                adapter.setWaitingResponse(true);
            }
        }
    }

    // 新增方法：检查是否有加载消息
    private boolean checkLoadingMessage() {
        if (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            if (lastMessage.isLoading()) {
                Log.d("ChatDebug", "最后一条消息是加载消息");
                return true;
            }
        }
        return false;
    }

    // 新增方法：更新等待状态
    private void updateWaitingState() {
        boolean hasLoadingMessage = checkLoadingMessage();
        
        if (serviceBound && chatService != null && currentHistoryId != -1) {
            boolean serviceThinking = chatService.isThinking(currentHistoryId);
            Log.d("ChatDebug", "服务状态: " + (serviceThinking ? "思考中" : "空闲"));
            
            // 如果服务正在思考但UI没有显示加载状态，恢复加载状态
            if (serviceThinking && !hasLoadingMessage) {
                restoreThinkingState();
            }
            // 如果服务不在思考但UI显示加载状态，清除加载状态
            else if (!serviceThinking && hasLoadingMessage) {
                removeThinkingMessage();
            }
            
            isWaitingResponse = serviceThinking;
        } else {
            Log.d("ChatDebug", "使用消息列表状态: " + hasLoadingMessage);
            isWaitingResponse = hasLoadingMessage;
        }
        
        // 更新适配器的等待状态
        adapter.setWaitingResponse(isWaitingResponse);
        updateSendButtonState();
    }

    private void requestAIResponse(int position) {
        // 获取编辑后的消息
        ChatMessage editedMessage = messages.get(messages.size() - 2); // 倒数第二条是编辑后的消息
        
        // 保存消息
        saveMessage(editedMessage.getMessage(), true, currentPersonality.getId());
        
        // 请求AI回复
        if (serviceBound && chatService != null) {
            // 添加加载状态
            ChatMessage loadingMessage = new ChatMessage("", false);
            loadingMessage.setLoading(true);
            loadingMessage.setThinkingStartTime(System.currentTimeMillis());
            messages.add(loadingMessage);
            adapter.notifyItemInserted(messages.size() - 1);
            
            // 滚动到底部
            chatRecyclerView.post(() -> {
                chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            });

            // 开始思考时间更新
            startThinkingTimeUpdate(loadingMessage);
            
            // 设置等待状态
            isWaitingResponse = true;
            adapter.setWaitingResponse(true);
            updateSendButtonState();
            
            // 发送消息
            sendMessage(editedMessage.getMessage());
        }
    }

    private void setupQuickMessages() {
        if (PreferenceManager.isQuickMessageUsed(this, currentHistoryId)) {
            quickMessageGroup.removeAllViews();
            return;
        }
        
        quickMessageGroup.removeAllViews();
        
        // 定义每个预消息的样式配置
        class QuickMessageStyle {
            final String message;
            final int textColor;
            final int rippleColor;

            QuickMessageStyle(String message, int textColor, int rippleColor) {
                this.message = message;
                this.textColor = textColor;
                this.rippleColor = rippleColor;
            }
        }

        // 配置每个预消息的样式
        List<QuickMessageStyle> styles = Arrays.asList(
            new QuickMessageStyle(
                "😊 我今天心情很好",
                getResources().getColor(R.color.mood_good_text),
                getResources().getColor(R.color.mood_good_ripple)
            ),
            new QuickMessageStyle(
                "😐 我今天心情一般",
                getResources().getColor(R.color.mood_neutral_text),
                getResources().getColor(R.color.mood_neutral_ripple)
            ),
            new QuickMessageStyle(
                "😔 我今天心情不太好",
                getResources().getColor(R.color.mood_bad_text),
                getResources().getColor(R.color.mood_bad_ripple)
            ),
            new QuickMessageStyle(
                "💭 我想找人聊聊",
                getResources().getColor(R.color.chat_text),
                getResources().getColor(R.color.chat_ripple)
            ),
            new QuickMessageStyle(
                "📊 分析我最近的心情",
                getResources().getColor(R.color.analysis_text_color),
                getResources().getColor(R.color.analysis_ripple_color)
            )
        );

        // 创建并添加Chips
        for (QuickMessageStyle style : styles) {
            Chip chip = new Chip(this);
            chip.setText(style.message);
            chip.setCheckable(true);
            
            // 应用样式
            chip.setTextColor(style.textColor);
            chip.setRippleColor(ColorStateList.valueOf(style.rippleColor));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (style.message.equals("📊 分析我最近的心情")) {
                        generateMoodAnalysisMessage();
                    } else {
                        messageInput.setText(style.message);
                        sendButton.performClick();
                    }
                    PreferenceManager.setQuickMessageUsed(this, currentHistoryId);
                    quickMessageGroup.removeAllViews();
                }
            });
            
            quickMessageGroup.addView(chip);
        }
    }

    // 修改 generateMoodAnalysisMessage() 方法
    private void generateMoodAnalysisMessage() {
        AppDatabase database = AppDatabase.getInstance(this);
        database.moodEntryDao().getAllEntries().observe(this, entries -> {
            String userMessage = "分析我的心情状况";
            messageInput.setText(userMessage);

            if (entries == null || entries.isEmpty()) {
                String noDataPrompt = 
                    "用户请求分析心情，但目前还没有任何心情记录。请：\n" +
                    "1. 简单说明记录心情的重要性\n" +
                    "2. 介绍如何开始记录心情（例如：每天用1-5分记录心情，5分最好）\n" +
                    "3. 鼓励用户开始第一次心情记录\n" +
                    "注意：回复要简短友善，限制100字以内";

                updatePersonalityPrompt(noDataPrompt);
                sendButton.performClick();
                return;
            }

            // 分析最近7天的数据
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            float avgMood = 0;
            int count = 0;
            int highestMood = 1;
            int lowestMood = 5;
            StringBuilder moodTrend = new StringBuilder();
            StringBuilder recentDiaries = new StringBuilder();
            
            int consecutiveLowMoodDays = 0;
            int currentConsecutiveDays = 0;
            int lastMoodScore = 0;

            // 收集最近的日记内容
            List<String> keyPhrases = new ArrayList<>();
            int diaryCount = 0;

            for (MoodEntry entry : entries) {
                if (entry.getDate().getTime() >= sevenDaysAgo) {
                    int currentScore = entry.getMoodScore();
                    avgMood += currentScore;
                    count++;
                    
                    // 更新统计数据
                    highestMood = Math.max(highestMood, currentScore);
                    lowestMood = Math.min(lowestMood, currentScore);
                    
                    // 检查心情趋势
                    if (currentScore <= 2) {
                        currentConsecutiveDays++;
                        consecutiveLowMoodDays = Math.max(consecutiveLowMoodDays, currentConsecutiveDays);
                    } else {
                        currentConsecutiveDays = 0;
                    }
                    
                    // 记录变化趋势
                    if (lastMoodScore != 0) {
                        if (currentScore < lastMoodScore) {
                            moodTrend.append("↓");
                        } else if (currentScore > lastMoodScore) {
                            moodTrend.append("↑");
                        } else {
                            moodTrend.append("→");
                        }
                    }
                    lastMoodScore = currentScore;

                    // 收集所有7天内的日记内容
                    if (entry.getDiaryContent() != null && !entry.getDiaryContent().trim().isEmpty()) {
                        keyPhrases.add(String.format(
                            "- %s（心情%d分）：%s",
                            new SimpleDateFormat("MM/dd", Locale.getDefault()).format(entry.getDate()),
                            currentScore,
                            // 如果内容超过100字则截断
                            entry.getDiaryContent().length() > 100 ? 
                                entry.getDiaryContent().substring(0, 100) + "..." : 
                                entry.getDiaryContent()
                        ));
                    }
                }
            }

            // 构建系统提示词
            String systemPrompt = String.format(
                "用户要求分析心情，请基于以下数据进行分析（回复限制300字）：\n\n" +
                "【数据统计】\n" +
                "- 近7天数据：均分%.1f分，最高%d分，最低%d分，记录%d天\n" +
                "- 情绪走势：%s\n" +
                "- 连续低落天数：%d天\n\n" +
                "【近7天日记记录】\n%s\n\n" +
                "分析要求：\n" +
                "1. 结合数据和日记内容，分析情绪变化的关键原因\n" +
                "2. 找出日记中反复出现的情绪触发点\n" +
                "3. 基于用户具体情况，给出针对性的改善建议\n" +
                "4. 保持积极鼓励的语气，肯定用户的进步",
                count > 0 ? avgMood / count : 0,
                highestMood,
                lowestMood,
                count,
                moodTrend.length() > 0 ? moodTrend.toString() : "暂无",
                consecutiveLowMoodDays,
                keyPhrases.isEmpty() ? "暂无日记内容" : String.join("\n", keyPhrases)
            );

            updatePersonalityPrompt(systemPrompt);
            sendButton.performClick();
        });
    }

    // 抽取更新性格提示词的方法
    private void updatePersonalityPrompt(String newPrompt) {
        if (currentPersonality != null) {
            String originalPrompt = currentPersonality.getSystemPrompt();
            currentPersonality = new AIPersonality(
                currentPersonality.getId(),
                currentPersonality.getName(),
                currentPersonality.getAvatar(),
                currentPersonality.getDescription(),
                newPrompt + "\n\n" + originalPrompt,
                currentPersonality.getWelcomeMessage(),
                currentPersonality.getModelName()
            );
        }
    }
} 