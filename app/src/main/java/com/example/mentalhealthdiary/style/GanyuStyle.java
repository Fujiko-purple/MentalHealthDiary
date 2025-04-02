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
        // 甘雨特有的文本转换逻辑
        if (originalText == null || originalText.isEmpty()) {
            return originalText;
        }
        
        // 这里可以添加甘雨特有的语言风格转换
        // 比如添加一些璃月特色的词汇、口吻等
        
        return originalText;
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