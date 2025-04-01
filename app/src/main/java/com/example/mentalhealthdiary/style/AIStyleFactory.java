package com.example.mentalhealthdiary.style;

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
    public static AIPersonalityStyle getStyle(String personalityId) {
        // 先检查缓存
        if (styleCache.containsKey(personalityId)) {
            return styleCache.get(personalityId);
        }
        
        // 创建新的风格实例
        AIPersonalityStyle style;
        switch (personalityId) {
            case "cat_girl":
                style = new CatGirlStyle();
                break;
            // 在这里添加其他AI风格...
            default:
                style = new DefaultAIStyle();
        }
        
        // 缓存并返回
        styleCache.put(personalityId, style);
        return style;
    }
}