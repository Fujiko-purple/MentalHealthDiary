package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class ApiConfig {
    private static final String DEFAULT_BASE_URL = "https://api.ppinfra.com/v3/openai/";
    private static final String DEFAULT_MODEL = "deepseek/deepseek-r1-turbo";
    
    public static String getApiKey(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("custom_api_key", "sk_sugjpsC0Bk8C__-Dokz2WIW4D125z5qxDZh28mcSNu4");
    }
    
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String baseUrl = prefs.getBoolean("use_custom_api", false) 
            ? prefs.getString("custom_api_base", DEFAULT_BASE_URL)
            : DEFAULT_BASE_URL;
            
        // 确保 URL 以斜杠结尾
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
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