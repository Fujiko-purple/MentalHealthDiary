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
    
    private ChatApiClient(Context context) {
        this.context = context.getApplicationContext();
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
        
        call.enqueue(callback);
    }
} 