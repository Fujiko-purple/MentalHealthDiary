package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class ApiConfig {
    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/";
    private static final String DEFAULT_MODEL = "deepseek-ai/DeepSeek-R1";
    
    public static String getApiKey(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("custom_api_key", "");
    }
    
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String baseUrl = prefs.getBoolean("use_custom_api", false) 
            ? prefs.getString("custom_api_base", DEFAULT_BASE_URL)
            : DEFAULT_BASE_URL;
            
        // 处理自定义 API 的路径
        if (prefs.getBoolean("use_custom_api", false)) {
            // 移除末尾的斜杠
            baseUrl = baseUrl.replaceAll("/+$", "");
            
            // 如果 URL 包含 v3/openai，移除它
            baseUrl = baseUrl.replaceAll("/v3/openai/*$", "");
            
            // 确保以斜杠结尾
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
        }
        
        return baseUrl;
    }
    
    public static String getModelName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_custom_api", false)
            ? prefs.getString("custom_model_name", DEFAULT_MODEL)
            : DEFAULT_MODEL;
    }
    
    public static boolean isCustomApiEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_custom_api", false);
    }
} 