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
import com.example.mentalhealthdiary.api.ChatApiClient;
import com.example.mentalhealthdiary.api.model.ChatRequest;
import com.example.mentalhealthdiary.api.model.ChatResponse;
import com.example.mentalhealthdiary.config.RemoteConfig;
import com.example.mentalhealthdiary.config.AIPersonalityConfig;
import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.model.AIPersonality;
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
            long chatId = intent.getLongExtra(ChatService.EXTRA_CHAT_ID, -1);
            
            if (chatId != currentHistoryId) {
                return;
            }
            
            if (ChatService.ACTION_CHAT_START.equals(action)) {
                // AI 开始思考时禁用发送按钮
                sendButton.setEnabled(false);
                // 记录开始思考时间
                thinkingStartTime = System.currentTimeMillis();
                startThinkingTimeUpdate();
            } else if (ChatService.ACTION_CHAT_RESPONSE.equals(action)) {
                String response = intent.getStringExtra(ChatService.EXTRA_RESPONSE);
                
                // 计算思考总时长
                long thinkingTime = System.currentTimeMillis() - thinkingStartTime;
                int seconds = (int) (thinkingTime / 1000);
                
                // 移除加载状态
                clearLoadingStates();
                
                // 添加AI回复，并在开头显示思考时长
                String responseWithTime = String.format("(思考用时：%d秒)\n\n%s", seconds, response);
                messages.add(new ChatMessage(responseWithTime, false, currentPersonality.getId()));
                adapter.notifyItemInserted(messages.size() - 1);
                chatRecyclerView.scrollToPosition(messages.size() - 1);
                
                // 保存AI回复
                saveMessage(responseWithTime, false, currentPersonality.getId());
                
                // 收到回复后启用发送按钮
                sendButton.setEnabled(true);
                stopThinkingTimeUpdate();
            } else if (ChatService.ACTION_CHAT_ERROR.equals(action)) {
                String error = intent.getStringExtra(ChatService.EXTRA_ERROR);
                
                // 计算思考总时长
                long thinkingTime = System.currentTimeMillis() - thinkingStartTime;
                int seconds = (int) (thinkingTime / 1000);
                error = String.format("(思考用时：%d秒)\n%s", seconds, error);
                
                // 移除加载状态
                clearLoadingStates();
                
                // 显示错误
                showError(error);
                
                // 发生错误时也要启用发送按钮
                sendButton.setEnabled(true);
                stopThinkingTimeUpdate();
            }
        }
    };

    // 添加ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChatService.ChatBinder binder = (ChatService.ChatBinder) service;
            chatService = binder.getService();
            serviceBound = true;
            
            // 检查是否有正在进行的请求
            if (currentHistoryId != -1 && chatService.isThinking(currentHistoryId)) {
                // 显示加载状态
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

        // 初始化视图
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // 设置 Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 先加载当前AI性格
        loadCurrentPersonality();

        // 初始化适配器
        adapter = new ChatAdapter(messages, currentPersonality);
        chatRecyclerView.setAdapter(adapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 如果是新对话，显示欢迎消息
        if (messages.isEmpty()) {
            messages.add(new ChatMessage(currentPersonality.getWelcomeMessage(), false));
            adapter.notifyDataSetChanged();
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

        // 设置发送按钮点击事件
        sendButton.setOnClickListener(v -> {
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

        // 绑定Service
        Intent serviceIntent = new Intent(this, ChatService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // 注册广播接收器
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
            personalityId = "default";  // 确保这里设置的是 "default"
            PreferenceManager.saveCurrentPersonalityId(this, personalityId);
        }
        
        currentPersonality = AIPersonalityConfig.getPersonalityById(personalityId);
        Log.d("AIChatActivity", "Loaded personality: " + 
              (currentPersonality != null ? currentPersonality.getName() + ", ID: " + currentPersonality.getId() : "null"));
        
        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentPersonality.getName());
        }
    }

    // 当切换AI性格时调用此方法
    private void updatePersonality(AIPersonality newPersonality) {
        currentPersonality = newPersonality;
        if (adapter != null) {
            adapter.setCurrentPersonality(newPersonality);
            adapter.notifyDataSetChanged();
        }
        
        // 更新标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(newPersonality.getName());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 重新加载性格并开始新对话
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

    private void sendToAI(String userMessage, int loadingPos) {
        // 添加日志检查当前性格
        Log.d("AIChatActivity", "Sending message with personality: " + 
              currentPersonality.getName() + ", ID: " + currentPersonality.getId());
        
        // 保存用户消息
        saveMessage(userMessage, true, currentPersonality.getId());

        List<ChatRequest.Message> apiMessages = new ArrayList<>();
        
        // 获取历史消息中的所有对话，用于保持对话连贯性
        for (ChatMessage msg : messages) {
            if (!msg.isLoading()) {
                // 根据消息的 personalityId 获取对应的性格
                AIPersonality messagePersonality = msg.getPersonalityId() != null ?
                        AIPersonalityConfig.getPersonalityById(msg.getPersonalityId()) :
                        currentPersonality;
                        
                if (!msg.isUser() && apiMessages.isEmpty()) {
                    // 第一条 AI 消息前添加对应的系统提示词
                    apiMessages.add(new ChatRequest.Message(
                        "system", 
                        messagePersonality.getSystemPrompt()
                    ));
                }
                
                apiMessages.add(new ChatRequest.Message(
                    msg.isUser() ? "user" : "assistant",
                    msg.getMessage()
                ));
            }
        }
        
        // 使用配置的模型名称
        String modelName = RemoteConfig.getCustomModelName();
        ChatRequest request = new ChatRequest(apiMessages, modelName);
        
        // 使用Service发送请求
        if (serviceBound && chatService != null) {
            chatService.sendChatRequest(request, currentHistoryId);
        } else {
            // 如果Service未绑定，显示错误
            messages.remove(loadingPos);
            adapter.notifyItemRemoved(loadingPos);
            showError("服务未准备好，请稍后再试");
            sendButton.setEnabled(true);  // 这行保留，因为是即时错误
        }
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
                    Log.d("ChatDebug", "Loading messages from JSON: " + history.getMessages());
                    
                    Gson gson = new GsonBuilder()
                        .serializeNulls()
                        .create();
                    Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                    List<ChatMessage> historyMessages = gson.fromJson(history.getMessages(), type);
                    
                    // 找到第一条 AI 消息的性格ID
                    String chatPersonalityId = null;
                    if (historyMessages != null) {
                        for (ChatMessage msg : historyMessages) {
                            if (!msg.isUser() && msg.getPersonalityId() != null) {
                                chatPersonalityId = msg.getPersonalityId();
                                Log.d("ChatDebug", "Found chat personality ID: " + chatPersonalityId);
                                break;
                            }
                        }
                    }

                    // 如果找到了性格ID，更新当前性格
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
                            
                            // 更新适配器的当前性格
                            adapter.setCurrentPersonality(currentPersonality);
                            
                            // 更新标题
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
                ChatHistory newHistory = new ChatHistory(new Date(), "新对话", "");
                currentHistoryId = database.chatHistoryDao().insert(newHistory);
                
                PreferenceManager.saveLastChatId(this, currentHistoryId);
                
                runOnUiThread(() -> {
                    messages.clear();
                    // 添加欢迎消息时包含性格ID
                    ChatMessage welcomeMessage = new ChatMessage(
                        currentPersonality.getWelcomeMessage(),
                        false,
                        currentPersonality.getId()  // 添加性格ID
                    );
                    messages.add(welcomeMessage);
                    adapter.notifyDataSetChanged();
                    
                    saveMessage(welcomeMessage.getMessage(), false, currentPersonality.getId());
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showError("创建新对话失败"));
            }
        });
    }

    private void saveCurrentChat() {
        if (currentHistoryId != -1 && messages != null && !messages.isEmpty()) {
            executorService.execute(() -> {
                try {
                    // 添加日志检查消息
                    for (ChatMessage msg : messages) {
                        if (!msg.isUser()) {
                            Log.d("ChatDebug", "Saving AI message with personality ID: " + msg.getPersonalityId());
                        }
                    }

                    String title = generateChatTitle();
                    String messagesJson = convertMessagesToJson(messages);
                    
                    // 添加日志检查 JSON
                    Log.d("ChatDebug", "Messages JSON: " + messagesJson);

                    ChatHistory history = new ChatHistory(new Date(), title, messagesJson);
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
        Gson gson = new GsonBuilder()
            .serializeNulls()  // 这会确保 null 值也被序列化
            .create();
        String json = gson.toJson(messages);
        Log.d("ChatDebug", "Converting messages to JSON: " + json);  // 添加日志
        return json;
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
        
        // 解绑Service
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        // 注销广播接收器
        if (chatReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(chatReceiver);
        }
        
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
        
        stopThinkingTimeUpdate();
    }

    private void saveMessage(String content, boolean isUser, String personalityId) {
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
                
                // 更新最后一条消息（加载状态）的文本
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
} 