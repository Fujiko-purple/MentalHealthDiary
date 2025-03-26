package com.example.mentalhealthdiary.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Build;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mentalhealthdiary.service.ChatApiClient;
import com.example.mentalhealthdiary.service.ChatRequest;
import com.example.mentalhealthdiary.service.ChatResponse;
import com.example.mentalhealthdiary.config.ApiConfig;
import com.example.mentalhealthdiary.service.ChatResponse.Choice;
import com.example.mentalhealthdiary.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

public class ChatService extends Service {
    // 广播动作常量
    public static final String ACTION_CHAT_RESPONSE = "com.example.mentalhealthdiary.ACTION_CHAT_RESPONSE";
    public static final String ACTION_CHAT_ERROR = "com.example.mentalhealthdiary.ACTION_CHAT_ERROR";
    public static final String ACTION_CHAT_START = "com.example.mentalhealthdiary.ACTION_CHAT_START";
    
    // 广播数据键
    public static final String EXTRA_RESPONSE = "response";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_CHAT_ID = "chat_id";
    
    private final IBinder binder = new ChatBinder();
    private Call<ChatResponse> currentCall;
    private long currentChatId = -1;
    
    private static final int MAX_RETRIES = 3;  // 最大重试次数
    private static final int RETRY_DELAY = 20000;  // 每次重试间隔20秒
    private static final long THINKING_TIMEOUT = 120000; // 增加到120秒超时
    
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private int retryCount = 0;
    
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "chat_service_channel";
    private static final String CHANNEL_NAME = "AI Chat Service";
    
    private Map<Long, Boolean> thinkingStates = new HashMap<>();
    private Map<Long, Call<ChatResponse>> activeRequests = new HashMap<>();
    private Map<Long, Long> thinkingStartTimes = new HashMap<>();

    public class ChatBinder extends Binder {
        public ChatService getService() {
            return ChatService.this;
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // 创建服务时就启动前台服务
        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 确保服务启动时就进入前台模式
        startForegroundService();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 助手")
            .setContentText("服务正在运行中...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    public void sendChatRequest(ChatRequest request, long chatId) {
        // 记录思考开始时间
        startThinking(chatId);
        
        // 更新通知显示正在思考
        updateNotification("AI 助手正在思考", "正在生成回复...");
        
        thinkingStates.put(chatId, true);
        
        // 创建新的请求
        Call<ChatResponse> call = ChatApiClient.getInstance(this).sendMessage(request);
        
        // 保存活动请求
        activeRequests.put(chatId, call);
        
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                thinkingStates.remove(chatId);
                activeRequests.remove(chatId);
                
                // 如果没有正在进行的请求，更新通知
                if (activeRequests.isEmpty()) {
                    updateNotification("AI 助手", "服务正在运行中...");
                }
                
                // 打印响应状态和内容
                Log.d("ChatService", "响应码: " + response.code());
                if (!response.isSuccessful()) {
                    try {
                        Log.e("ChatService", "错误响应: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("ChatService", "读取错误响应失败", e);
                    }
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = extractResponseContent(response.body());
                    Log.d("ChatService", "AI响应: " + aiResponse);
                    
                    if (aiResponse != null) {
                        if (timeoutRunnable != null) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                        }
                        retryHandler.removeCallbacksAndMessages(null);
                        sendResponseBroadcast(chatId, aiResponse);
                        currentChatId = -1;
                        currentCall = null;
                    } else {
                        Log.e("ChatService", "AI响应为空");
                        retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                    }
                } else {
                    Log.e("ChatService", "响应不成功或为空");
                    retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    thinkingStates.remove(chatId);
                    activeRequests.remove(chatId);
                    
                    // 如果没有正在进行的请求，更新通知
                    if (activeRequests.isEmpty()) {
                        updateNotification("AI 助手", "服务正在运行中...");
                    }
                    
                    Log.e("ChatService", "请求失败", t);
                    retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                }
            }
        });
    }
    
    private void sendStartBroadcast(long chatId) {
        Intent startIntent = new Intent(ACTION_CHAT_START);
        startIntent.putExtra(EXTRA_CHAT_ID, chatId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(startIntent);
    }
    
    private String processAIResponse(String response) {
        if (response == null) return null;
        
        // 移除思考时间
        response = response.replaceAll("\\(思考用时：.*?秒\\)\n*", "");
        
        // 移除 <think> 标签及其内容，添加 Pattern.DOTALL 标志
        response = response.replaceAll("(?s)<think>.*?</think>\n*", "");
        
        // 移除其他可能的思考过程标记
        response = response.replaceAll("(?s)\\[思考中\\].*?\\[/思考中\\]\n*", "");
        response = response.replaceAll("(?s)\\(思考中\\).*?\\(/思考中\\)\n*", "");
        response = response.replaceAll("(?s)【思考】.*?【/思考】\n*", "");
        
        // 移除多余的空行
        response = response.replaceAll("(?m)^\\s*$[\n\r]{1,}", "\n");
        response = response.trim();
        
        Log.d("ChatService", "处理后的响应: " + response);
        return response;
    }
    
    private void sendResponseBroadcast(long chatId, String aiResponse) {
        // 处理 AI 响应
        String processedResponse = processAIResponse(aiResponse);
        
        Intent intent = new Intent(ACTION_CHAT_RESPONSE);
        intent.putExtra(EXTRA_RESPONSE, processedResponse);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    private void sendErrorBroadcast(long chatId, String errorMessage) {
        Intent intent = new Intent(ACTION_CHAT_ERROR);
        intent.putExtra(EXTRA_ERROR, errorMessage);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    private String extractResponseContent(ChatResponse response) {
        Log.d("ChatService", "解析响应: " + response);
        if (response != null) {
            Log.d("ChatService", "choices: " + response.choices);
            if (response.choices != null && !response.choices.isEmpty()) {
                ChatResponse.Choice choice = response.choices.get(0);
                Log.d("ChatService", "choice: " + choice);
                if (choice.message != null) {
                    Log.d("ChatService", "message: " + choice.message.content);
                    return choice.message.content;
                }
            }
        }
        return null;
    }
    
    public boolean isThinking(long chatId) {
        return thinkingStates.containsKey(chatId) && thinkingStates.get(chatId);
    }
    
    private void updateNotification(String title, String content) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        retryHandler.removeCallbacksAndMessages(null);
        if (currentCall != null) {
            currentCall.cancel();
        }
        // 取消所有正在进行的请求
        for (Call<ChatResponse> call : activeRequests.values()) {
            call.cancel();
        }
        activeRequests.clear();
        thinkingStates.clear();
        stopForeground(true);
    }

    private void sendRequest(ChatRequest request, long chatId) {
        if (retryCount >= MAX_RETRIES) {
            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }
            currentChatId = -1;
            currentCall = null;
            sendErrorBroadcast(chatId, "请求失败，请重试");
            return;
        }
        
        retryCount++;
        Log.d("ChatService", "发送请求，尝试次数：" + retryCount);
        
        // 打印请求内容
        Log.d("ChatService", "请求模型: " + request.model);
        Log.d("ChatService", "请求消息: " + request.messages.get(0).content);
        
        // 创建新的请求
        Call<ChatResponse> call = ChatApiClient.getInstance(this).sendMessage(request);
        
        // 保存活动请求
        activeRequests.put(chatId, call);
        
        call.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                thinkingStates.remove(chatId);
                activeRequests.remove(chatId);
                
                // 如果没有正在进行的请求，更新通知
                if (activeRequests.isEmpty()) {
                    updateNotification("AI 助手", "服务正在运行中...");
                }
                
                // 打印响应状态和内容
                Log.d("ChatService", "响应码: " + response.code());
                if (!response.isSuccessful()) {
                    try {
                        Log.e("ChatService", "错误响应: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("ChatService", "读取错误响应失败", e);
                    }
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    String aiResponse = extractResponseContent(response.body());
                    Log.d("ChatService", "AI响应: " + aiResponse);
                    
                    if (aiResponse != null) {
                        if (timeoutRunnable != null) {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                        }
                        retryHandler.removeCallbacksAndMessages(null);
                        sendResponseBroadcast(chatId, aiResponse);
                        currentChatId = -1;
                        currentCall = null;
                    } else {
                        Log.e("ChatService", "AI响应为空");
                        retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                    }
                } else {
                    Log.e("ChatService", "响应不成功或为空");
                    retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    thinkingStates.remove(chatId);
                    activeRequests.remove(chatId);
                    
                    // 如果没有正在进行的请求，更新通知
                    if (activeRequests.isEmpty()) {
                        updateNotification("AI 助手", "服务正在运行中...");
                    }
                    
                    Log.e("ChatService", "请求失败", t);
                    retryHandler.postDelayed(() -> sendRequest(request, chatId), RETRY_DELAY);
                }
            }
        });
    }

    // 开始思考时记录时间
    public void startThinking(long chatId) {
        thinkingStartTimes.put(chatId, System.currentTimeMillis());
    }

    // 获取思考已用时间（毫秒）
    public long getThinkingTime(long chatId) {
        Long startTime = thinkingStartTimes.get(chatId);
        if (startTime != null) {
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }

    // 在响应处理完成后清除时间记录
    private void handleResponse(String response, long chatId) {
        // 现有代码...
        
        // 清除思考时间记录
        thinkingStartTimes.remove(chatId);
    }
} 