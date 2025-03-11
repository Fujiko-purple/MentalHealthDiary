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
        
        // 修改日志拦截器的配置
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d("ChatApiClient", message);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC); // 改为 BASIC 级别
        
        // 创建重试拦截器
        Interceptor retryInterceptor = chain -> {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            
            for (int retryCount = 0; retryCount < 3; retryCount++) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    response = chain.proceed(request.newBuilder().build());
                    return response;
                } catch (IOException e) {
                    exception = e;
                    if (response != null) {
                        response.close();
                    }
                    if (retryCount == 2) throw e; // 最后一次重试失败时抛出异常
                    
                    try {
                        Thread.sleep(2000); // 等待2秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
            
            throw new IOException("所有重试都失败了");
        };
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)  // 先添加重试拦截器
            .addInterceptor(logging)           // 再添加日志拦截器
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
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
        Log.d("ChatApiClient", "使用的 API Key: " + (apiKey.length() > 4 ? apiKey.substring(0, 4) + "****" : "****"));
        
        return chatApi.chat(
            "Bearer " + apiKey,
            request
        );
    }
} 