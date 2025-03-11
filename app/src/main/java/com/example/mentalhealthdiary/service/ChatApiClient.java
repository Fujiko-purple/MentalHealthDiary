package com.example.mentalhealthdiary.service;

import android.content.Context;
import com.example.mentalhealthdiary.config.ApiConfig;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import android.util.Log;

public class ChatApiClient {
    private static ChatApiClient instance;
    private final Context context;
    private final ChatApi chatApi;
    
    public static void resetInstance() {
        instance = null;
    }
    
    private ChatApiClient(Context context) {
        this.context = context.getApplicationContext();
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 创建重试拦截器
        Interceptor retryInterceptor = chain -> {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            
            for (int retryCount = 0; retryCount < 3; retryCount++) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }
                } catch (IOException e) {
                    exception = e;
                    if (response != null) {
                        response.close();
                    }
                    Log.d("ChatApiClient", "重试请求，次数: " + (retryCount + 1));
                    try {
                        Thread.sleep(5000); // 等待5秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
            
            if (exception != null) {
                throw exception;
            }
            return response;
        };
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(retryInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)     // 连接超时
            .readTimeout(60, TimeUnit.SECONDS)        // 读取超时
            .writeTimeout(60, TimeUnit.SECONDS)       // 写入超时
            .retryOnConnectionFailure(true)           // 允许重试
            .build();
            
        String baseUrl = ApiConfig.getBaseUrl(context);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
            
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
            
        chatApi = retrofit.create(ChatApi.class);
    }
    
    public static synchronized ChatApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ChatApiClient(context);
        }
        return instance;
    }
    
    public Call<ChatResponse> sendMessage(ChatRequest request) {
        String baseUrl = ApiConfig.getBaseUrl(context);
        String apiKey = ApiConfig.getApiKey(context);
        
        Log.d("ChatApiClient", "发送请求到: " + baseUrl);
        Log.d("ChatApiClient", "使用的 API Key: " + apiKey.substring(0, 4) + "****");
        
        return chatApi.chat(
            "Bearer " + apiKey,
            request
        );
    }
} 