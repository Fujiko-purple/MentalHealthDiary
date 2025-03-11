package com.example.mentalhealthdiary.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mentalhealthdiary.service.ChatApiClient;
import com.example.mentalhealthdiary.service.ChatRequest;
import com.example.mentalhealthdiary.service.ChatResponse;
import com.example.mentalhealthdiary.config.ApiConfig;
import com.example.mentalhealthdiary.service.ChatResponse.Choice;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    
    public void sendChatRequest(ChatRequest request, long chatId) {
        if (currentCall != null) {
            currentCall.cancel();
        }
        
        // 取消之前的超时检查（如果有的话）
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        
        handleChatRequest(request, chatId);
    }
    
    private void handleChatRequest(ChatRequest request, long chatId) {
        sendStartBroadcast(chatId);
        currentChatId = chatId;
        retryCount = 0;
        
        // 设置总体超时
        timeoutRunnable = () -> {
            if (currentCall != null && !currentCall.isCanceled()) {
                currentCall.cancel();
                retryHandler.removeCallbacksAndMessages(null); // 清除所有等待的重试
                currentChatId = -1;
                currentCall = null;
                sendErrorBroadcast(chatId, "请求超时，请重试");
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, THINKING_TIMEOUT);
        
        // 发送请求
        sendRequest(request, chatId);
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
        
        currentCall = ChatApiClient.getInstance(this).sendMessage(request);
        currentCall.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                if (call.isCanceled()) {
                    return;
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
    
    private void sendResponseBroadcast(long chatId, String aiResponse) {
        Intent intent = new Intent(ACTION_CHAT_RESPONSE);
        intent.putExtra(EXTRA_RESPONSE, aiResponse);
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
        return currentChatId == chatId && currentCall != null && !currentCall.isCanceled();
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
    }
} 