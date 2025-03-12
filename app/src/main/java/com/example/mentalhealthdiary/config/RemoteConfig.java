package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.example.mentalhealthdiary.service.ChatApiClient;

public class RemoteConfig {
    private static Context context;
    private static String customApiKey = "";
    private static String customApiBase = "";
    private static String customModelName = "";
    
    public static void init(Context appContext) {
        context = appContext.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // 从 SharedPreferences 加载自定义配置
        customApiKey = prefs.getString("custom_api_key", "");
        customApiBase = prefs.getString("custom_api_base", "");
        customModelName = prefs.getString("custom_model_name", "");
    }
    
    public static void updateConfig(String apiKey, String apiBase, String modelName) {
        if (context == null) return;
        
        // 只有在启用自定义 API 时才更新配置
        if (ApiConfig.isCustomApiEnabled(context)) {
            SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
            
            editor.putString("custom_api_key", apiKey);
            editor.putString("custom_api_base", apiBase);
            editor.putString("custom_model_name", modelName);
            editor.apply();
            
            // 更新内存中的值
            customApiKey = apiKey;
            customApiBase = apiBase;
            customModelName = modelName;
            
            // 重置API客户端实例
            ChatApiClient.resetInstance();
        }
    }
    
    public static String getApiKey() {
        return ApiConfig.getApiKey(context);
    }
    
    public static String getApiBaseUrl() {
        return ApiConfig.getBaseUrl(context);
    }
    
    public static String getModelName() {
        return ApiConfig.getModelName(context);
    }
    
    public static boolean isCustomApiEnabled() {
        return ApiConfig.isCustomApiEnabled(context);
    }
} 