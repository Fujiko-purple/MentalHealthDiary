package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;

import com.example.mentalhealthdiary.R;

/**
 * 猫娘AI界面风格
 */
public class CatGirlStyle implements AIPersonalityStyle {
    private Context context;
    
    // 增加构造函数缓存context以获取资源
    public CatGirlStyle(Context context) {
        this.context = context;
    }
    
    @Override
    public int getChatBubbleDrawable(boolean isUser) {
        if (isUser) {
            return R.drawable.chat_bubble_sent;
        } else {
            return context.getResources().getIdentifier(
                context.getString(R.string.catgirl_bubble_bg), 
                "drawable", 
                context.getPackageName()
            );
        }
    }
    
    @Override
    public int getInputBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.catgirl_input_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public int getBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.catgirl_chat_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public ColorStateList getPrimaryColor() {
        return ColorStateList.valueOf(context.getResources().getColor(R.color.catgirl_primary));
    }
    
    @Override
    public int getHintTextColor() {
        return context.getResources().getColor(R.color.catgirl_secondary);
    }
    
    @Override
    public Typeface getTypeface(Context context) {
        return FontHelper.loadTypeface(
            context,
            context.getString(R.string.catgirl_font),
            "猫娘"
        );
    }
    
    @Override
    public String getInputHint() {
        return context.getString(R.string.catgirl_input_hint);
    }
    
    @Override
    public String getSendButtonText() {
        return context.getString(R.string.catgirl_send_button);
    }
    
    @Override
    public String transformText(String originalText) {
        // 从ChatAdapter中提取的猫娘文本转换逻辑
        if (originalText == null || originalText.isEmpty()) {
            return originalText;
        }
        
        // 已经有猫娘风格的不再处理
        if (originalText.startsWith("喵～") || originalText.endsWith("喵～") || 
            originalText.contains("呜喵") || originalText.contains("nya")) {
            return originalText;
        }
        
        // 随机使用的猫娘语气词
        String[] catSounds = {
            "喵～", "喵喵～", "喵呜～", "nya～", "呜喵～"
        };
        
        // 随机选择一个猫叫声
        String catSound = catSounds[(int)(Math.random() * catSounds.length)];
        
        // 替换常见词语为猫娘风格
        originalText = originalText.replaceAll("(?i)我认为", "人家认为");
        originalText = originalText.replaceAll("(?i)我觉得", "猫猫觉得");
        originalText = originalText.replaceAll("(?i)我想", "人家想");
        
        // 处理句子结尾
        if (originalText.endsWith("。") || originalText.endsWith(".")) {
            originalText = originalText.substring(0, originalText.length()-1) + "～ " + catSound;
        } else if (originalText.endsWith("!") || originalText.endsWith("！")) {
            originalText = originalText.substring(0, originalText.length()-1) + "！" + catSound;
        } else if (!originalText.endsWith("～")) {
            // 如果不是以上情况，且不已经以波浪号结尾，添加猫叫和波浪号
            originalText = originalText + " " + catSound;
        }
        
        // 段落处理 - 每个段落结尾添加猫叫
        String[] paragraphs = originalText.split("\n\n");
        if (paragraphs.length > 1) {
            for (int i = 0; i < paragraphs.length - 1; i++) {
                // 不是以猫叫结束的段落添加猫叫
                if (!paragraphs[i].endsWith("喵～") && !paragraphs[i].endsWith("nya～") &&
                    !paragraphs[i].contains("呜喵～")) {
                    String randomCatSound = catSounds[(int)(Math.random() * catSounds.length)];
                    paragraphs[i] = paragraphs[i] + " " + randomCatSound;
                }
            }
            originalText = String.join("\n\n", paragraphs);
        }
        
        return originalText;
    }
    
    @Override
    public int getButtonCornerRadius() {
        return (int) context.getResources().getDimension(R.dimen.catgirl_button_radius);
    }
    
    @Override
    public int getButtonStrokeWidth() {
        return (int) context.getResources().getDimension(R.dimen.catgirl_button_stroke_width);
    }
} 