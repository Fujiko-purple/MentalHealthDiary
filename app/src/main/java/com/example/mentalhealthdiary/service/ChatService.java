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

import com.example.mentalhealthdiary.api.ChatApiClient;
import com.example.mentalhealthdiary.api.model.ChatRequest;
import com.example.mentalhealthdiary.api.model.ChatResponse;

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
        
        retryCount = 0;  // 重置重试计数
        sendRequestWithRetry(request, chatId);
    }
    
    private void sendRequestWithRetry(ChatRequest request, long chatId) {
        currentChatId = chatId;
        // 发送开始思考的广播
        Intent startIntent = new Intent(ACTION_CHAT_START);
        startIntent.putExtra(EXTRA_CHAT_ID, chatId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(startIntent);
        
        Log.d("ChatService", "开始发送请求，重试次数：" + retryCount);
        
        currentCall = ChatApiClient.getInstance(this).sendMessage(request);
        currentCall.enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                currentChatId = -1;  // 重置当前聊天ID
                if (response.isSuccessful() && response.body() != null) {
                    retryCount = 0;  // 成功后重置重试计数
                    String aiResponse = response.body().choices.get(0).message.content;
                    
                    Intent intent = new Intent(ACTION_CHAT_RESPONSE);
                    intent.putExtra(EXTRA_RESPONSE, aiResponse);
                    intent.putExtra(EXTRA_CHAT_ID, chatId);
                    LocalBroadcastManager.getInstance(ChatService.this)
                        .sendBroadcast(intent);
                } else {
                    handleError(request, chatId, response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    Log.e("ChatService", "请求失败", t);
                    handleError(request, chatId, -1);
                }
            }
        });
    }
    
    private void handleError(ChatRequest request, long chatId, int responseCode) {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            // 使用指数退避策略，每次重试等待时间增加
            long delayMillis = 1000 * (long) Math.pow(2, retryCount - 1);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sendRequestWithRetry(request, chatId);
            }, delayMillis);
            
        } else {
            // 达到最大重试次数，发送错误广播
            currentChatId = -1;
            String error = "AI响应错误: " + 
                (responseCode == 429 ? "请求太频繁，请稍后再试" : 
                 responseCode == 503 ? "服务暂时不可用" : 
                 responseCode == -1 ? "网络连接不稳定，请检查网络设置" :
                 "错误代码 " + responseCode);
            
            Intent intent = new Intent(ACTION_CHAT_ERROR);
            intent.putExtra(EXTRA_ERROR, error);
            intent.putExtra(EXTRA_CHAT_ID, chatId);
            LocalBroadcastManager.getInstance(ChatService.this)
                .sendBroadcast(intent);
        }
    }
    
    public boolean isThinking(long chatId) {
        return currentChatId == chatId && currentCall != null && !currentCall.isCanceled();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentCall != null) {
            currentCall.cancel();
        }
    }
} 