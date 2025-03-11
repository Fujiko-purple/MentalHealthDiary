package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class ApiConfig {
    private static final String DEFAULT_BASE_URL = "https://love.qinyan.xyz/v1";
    private static final String DEFAULT_API_KEY = "sk-Lp3wOKexoo6cXZ78844857D9Dc1946F18385A83cB24eB3A0";
    private static final String DEFAULT_MODEL = "claude-3-5-sonnet-20241022";
    
    public static String getApiKey(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("custom_api_key", DEFAULT_API_KEY);
    }
    
    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("custom_api_base", DEFAULT_BASE_URL);
    }
    
    public static String getModelName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("custom_model_name", DEFAULT_MODEL);
    }
    
    public static boolean isCustomApiEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("use_custom_api", false);
    }
} 