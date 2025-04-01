package com.example.mentalhealthdiary.style;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * AI风格工厂，负责创建和缓存各种AI风格
 */
public class AIStyleFactory {
    private static final Map<String, AIPersonalityStyle> styleCache = new HashMap<>();
    
    /**
     * 获取指定AI人格的界面风格
     */
    public static AIPersonalityStyle getStyle(String personalityId, Context context) {
        String cacheKey = personalityId + "_" + context.hashCode();
        
        // 先检查缓存
        if (styleCache.containsKey(cacheKey)) {
            return styleCache.get(cacheKey);
        }
        
        // 创建新的风格实例
        AIPersonalityStyle style;
        switch (personalityId) {
            case "cat_girl":
                style = new CatGirlStyle(context);
                break;
            // 在这里添加其他AI风格...
            default:
                style = new DefaultAIStyle(context);
        }
        
        // 缓存并返回
        styleCache.put(cacheKey, style);
        return style;
    }
}