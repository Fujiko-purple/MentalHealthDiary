package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.example.mentalhealthdiary.service.ChatApiClient;

public class RemoteConfig {
    private static final String DEFAULT_API_KEY = "sk_MJSWP6reQeFVutaYaskScEwyUXAreZX0R_zZue8Vkx8";
    private static final String DEFAULT_BASE_URL = "https://api.ppinfra.com/v3/openai";
    private static final String DEFAULT_MODEL = "deepseek/deepseek-r1/community";


    private static Context context;
    private static String customApiKey = "";
    private static String customApiBase = "";
    private static String customModelName = "";
    
    public static void init(Context appContext) {
        context = appContext.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // 从 SharedPreferences 加载自定义配置，使用默认值
        customApiKey = prefs.getString("custom_api_key", DEFAULT_API_KEY);
        customApiBase = prefs.getString("custom_api_base", DEFAULT_BASE_URL);
        customModelName = prefs.getString("custom_model_name", DEFAULT_MODEL);
        
        // 确保设置了默认值
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.contains("custom_api_key")) {
            editor.putString("custom_api_key", DEFAULT_API_KEY);
        }
        if (!prefs.contains("custom_api_base")) {
            editor.putString("custom_api_base", DEFAULT_BASE_URL);
        }
        if (!prefs.contains("custom_model_name")) {
            editor.putString("custom_model_name", DEFAULT_MODEL);
        }
        // 只在第一次初始化时设置默认值
        if (!prefs.contains("use_custom_api")) {
            editor.putBoolean("use_custom_api", true);
        }
        editor.apply();
    }
    
    public static void updateConfig(String apiKey, String apiBase, String modelName) {
        if (context == null) return;
        
        SharedPreferences.Editor editor = PreferenceManager
            .getDefaultSharedPreferences(context).edit();
        
        editor.putString("custom_api_key", apiKey);
        editor.putString("custom_api_base", apiBase);
        editor.putString("custom_model_name", modelName);
        editor.putBoolean("use_custom_api", true);  // 确保启用
        editor.apply();
        
        // 更新内存中的值
        customApiKey = apiKey;
        customApiBase = apiBase;
        customModelName = modelName;
        
        // 重置API客户端实例
        ChatApiClient.resetInstance();
    }
    
    public static String getApiKey() {
        return customApiKey;  // 直接返回内存中的值
    }
    
    public static String getApiBaseUrl() {
        return customApiBase;  // 直接返回内存中的值
    }
    
    public static String getModelName() {
        return customModelName;  // 直接返回内存中的值
    }
    
    public static boolean isCustomApiEnabled() {
        if (context == null) return false;
        return PreferenceManager
            .getDefaultSharedPreferences(context)
            .getBoolean("use_custom_api", false);  // 默认为 false
    }

    public static void setCustomApiEnabled(boolean enabled) {
        if (context == null) return;
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putBoolean("use_custom_api", enabled)
            .apply();
    }
} 