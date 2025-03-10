package com.example.mentalhealthdiary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferenceManager {
    private static final String PREF_NAME = "MentalHealthDiaryPrefs";
    private static final String KEY_LAST_CHAT_ID = "last_chat_id";
    private static final String KEY_CURRENT_PERSONALITY = "current_personality_id";
    
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
    
    public static void saveCurrentPersonalityId(Context context, String id) {
        Log.d("PreferenceManager", "Saving personality ID: " + id);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_PERSONALITY, id).apply();
    }
    
    public static String getCurrentPersonalityId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_CURRENT_PERSONALITY, null);
        Log.d("PreferenceManager", "Getting current personality ID: " + id);
        return id;
    }
} 