package com.example.mentalhealthdiary.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class ApiConfig {
    public static final String DEFAULT_API_KEY = "";
    public static final String DEFAULT_BASE_URL = "";
    public static final String DEFAULT_MODEL = "";
    
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
        return prefs.getBoolean("use_custom_api", true);
    }
    
    public static void setCustomApiEnabled(Context context, boolean enabled) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("use_custom_api", enabled);
        editor.apply();
    }
} 