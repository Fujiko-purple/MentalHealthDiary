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
        
        SharedPreferences.Editor editor = PreferenceManager
            .getDefaultSharedPreferences(context).edit();
        
        // 确保启用自定义API
        editor.putBoolean("use_custom_api", true);
        
        // 保存配置 - 使用正确的key名称
        editor.putString("custom_api_key", apiKey);       // 使用 "custom_api_key" 作为key
        editor.putString("custom_api_base", apiBase);     // 使用 "custom_api_base" 作为key
        editor.putString("custom_model_name", modelName); // 使用 "custom_model_name" 作为key
        editor.apply();
        
        // 更新内存中的值
        customApiKey = apiKey;
        customApiBase = apiBase;
        customModelName = modelName;
        
        // 重置API客户端实例，使新配置生效
        ChatApiClient.resetInstance();
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