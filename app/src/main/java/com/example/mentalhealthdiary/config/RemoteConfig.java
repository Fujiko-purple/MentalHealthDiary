package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.mentalhealthdiary.BuildConfig;
import androidx.preference.PreferenceManager;

public class RemoteConfig {
    private static String apiBaseUrl = ApiConfig.BASE_URL;
    private static String apiKey = ApiConfig.API_KEY;
    private static String modelName = ApiConfig.MODEL_NAME;
    
    // 新增自定义配置字段
    private static String customApiKey = "";
    private static String customApiBase = "";
    private static String customModelName = "";

    public static void updateConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // 读取所有配置项
        customApiKey = prefs.getString("custom_api_key", "");
        customApiBase = prefs.getString("custom_api_base", "");
        customModelName = prefs.getString("custom_model_name", "");
        
        // 原有配置逻辑保持不变...
    }

    // 新增获取方法
    public static String getCustomApiKey() {
        return !customApiKey.isEmpty() ? customApiKey : ApiConfig.API_KEY;
    }

    public static String getCustomApiBase() {
        return !customApiBase.isEmpty() ? customApiBase : ApiConfig.BASE_URL;
    }

    public static String getCustomModelName() {
        return !customModelName.isEmpty() ? customModelName : ApiConfig.MODEL_NAME;
    }

    // 原有方法保持不变...
} 