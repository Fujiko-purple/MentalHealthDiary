package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.Log;

import com.example.mentalhealthdiary.R;

/**
 * 甘雨AI界面风格
 */
public class GanyuStyle implements AIPersonalityStyle {
    private Context context;
    
    public GanyuStyle(Context context) {
        this.context = context;
    }
    
    @Override
    public int getChatBubbleDrawable(boolean isUser) {
        if (isUser) {
            return R.drawable.chat_bubble_sent;
        } else {
            return context.getResources().getIdentifier(
                context.getString(R.string.ganyu_bubble_bg), 
                "drawable", 
                context.getPackageName()
            );
        }
    }
    
    @Override
    public int getInputBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.ganyu_input_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public int getBackgroundDrawable() {
        return context.getResources().getIdentifier(
            context.getString(R.string.ganyu_chat_bg), 
            "drawable", 
            context.getPackageName()
        );
    }
    
    @Override
    public ColorStateList getPrimaryColor() {
        return ColorStateList.valueOf(context.getResources().getColor(R.color.ganyu_primary));
    }
    
    @Override
    public int getHintTextColor() {
        return context.getResources().getColor(R.color.ganyu_secondary);
    }
    
    @Override
    public Typeface getTypeface(Context context) {
        try {
            return Typeface.createFromAsset(
                context.getAssets(), 
                context.getString(R.string.ganyu_font)
            );
        } catch (Exception e) {
            Log.e("GanyuStyle", "无法加载甘雨字体", e);
            return Typeface.DEFAULT;
        }
    }
    
    @Override
    public String getInputHint() {
        return context.getString(R.string.ganyu_input_hint);
    }
    
    @Override
    public String getSendButtonText() {
        return context.getString(R.string.ganyu_send_button);
    }
    

    @Override
    public String transformText(String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return originalText;
        }

        if (originalText.startsWith("璃月之约·") || originalText.contains("霜华矢") ||
                originalText.contains("循仙之仪")) {
            return originalText;
        }

        String[] qilinTones = {
                "喏～", "呀～", "唷～", "...（垂眸）", "（理鬓）"
        };

        String tone = qilinTones[(int)(Math.random() * qilinTones.length)];

        originalText = originalText.replaceAll("(?i)我", "甘雨");
        originalText = originalText.replaceAll("(?i)认为", "以为");
        originalText = originalText.replaceAll("(?i)工作", "契约之责");
        originalText = originalText.replaceAll("(?i)休息", "暂离尘嚣");

        if (originalText.endsWith("。") || originalText.endsWith(".")) {
            originalText = originalText.substring(0, originalText.length()-1)
                    + "· " + tone;
        } else if (originalText.endsWith("!") || originalText.endsWith("！")) {
            originalText = originalText.substring(0, originalText.length()-1)
                    + "（寒天冰棱乍现）" + tone;
        } else if (!originalText.matches(".*[～）]$")) {
            originalText = originalText + " " + tone;
        }

        String[] paragraphs = originalText.split("\n\n");
        if (paragraphs.length > 1) {
            for (int i = 0; i < paragraphs.length - 1; i++) {
                if (!paragraphs[i].matches(".*[～）]$")) {
                    String newTone = qilinTones[(int)(Math.random() * qilinTones.length)];
                    paragraphs[i] = paragraphs[i] + " " + newTone;
                }
            }
            originalText = String.join("\n\n", paragraphs);
        }

        return "璃月之约·" + originalText;
    }
    
    @Override
    public int getButtonCornerRadius() {
        return (int) context.getResources().getDimension(R.dimen.ganyu_button_radius);
    }
    
    @Override
    public int getButtonStrokeWidth() {
        return (int) context.getResources().getDimension(R.dimen.ganyu_button_stroke_width);
    }
} 