package com.example.mentalhealthdiary.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "MentalHealthDiaryPrefs";
    private static final String KEY_LAST_CHAT_ID = "last_chat_id";
    
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
} 