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
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
    
    public void testConnection(ChatRequest request, Callback<ChatResponse> callback) {
        Call<ChatResponse> call = chatApi.chat(
            "Bearer " + ApiConfig.getApiKey(context),
            request
        );
        
        // 打印完整的请求信息
        System.out.println("完整请求URL: " + call.request().url());
        System.out.println("请求方法: " + call.request().method());
        System.out.println("请求头: " + call.request().headers());
        System.out.println("请求体: " + request.toString());
        
        call.enqueue(callback);
    }
} 