package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

/**
 * 字体加载辅助类，统一处理字体加载和日志
 */
public class FontHelper {
    private static final String TAG = "FontHelper";
    
    /**
     * 从assets加载字体并生成统一日志
     * @param context 上下文
     * @param fontPath 字体路径
     * @param aiName AI名称（用于日志）
     * @return 加载的字体，失败时返回默认字体
     */
    public static Typeface loadTypeface(Context context, String fontPath, String aiName) {
        try {
            Log.d(TAG, aiName + "尝试加载字体: " + fontPath);
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
            Log.d(TAG, aiName + "字体加载成功!");
            return typeface;
        } catch (Exception e) {
            Log.e(TAG, aiName + "字体加载失败: " + e.getMessage(), e);
            return Typeface.DEFAULT;
        }
    }
} 