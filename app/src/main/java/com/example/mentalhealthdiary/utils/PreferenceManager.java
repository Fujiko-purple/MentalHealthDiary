package com.example.mentalhealthdiary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferenceManager {
    private static final String PREF_NAME = "MentalHealthDiaryPrefs";
    private static final String KEY_LAST_CHAT_ID = "last_chat_id";
    private static final String KEY_CURRENT_PERSONALITY_ID = "current_personality_id";
    private static final String PREF_QUICK_MESSAGE_USED = "quick_message_used_";
    
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static void saveLastChatId(Context context, long chatId) {
        getPreferences(context)
            .edit()
            .putLong(KEY_LAST_CHAT_ID, chatId)
            .commit();
    }
    
    public static long getLastChatId(Context context) {
        return getPreferences(context).getLong(KEY_LAST_CHAT_ID, -1);
    }
    
    public static void saveCurrentPersonalityId(Context context, String personalityId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_PERSONALITY_ID, personalityId).apply();
    }
    
    public static String getCurrentPersonalityId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENT_PERSONALITY_ID, "default");
    }
    
    public static boolean isQuickMessageUsed(Context context, long chatId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_QUICK_MESSAGE_USED + chatId, false);
    }
    
    public static void setQuickMessageUsed(Context context, long chatId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_QUICK_MESSAGE_USED + chatId, true).apply();
    }
} 