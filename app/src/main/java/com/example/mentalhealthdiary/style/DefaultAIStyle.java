package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;

import com.example.mentalhealthdiary.R;

/**
 * 默认AI界面风格
 */
public class DefaultAIStyle implements AIPersonalityStyle {
    private Context context;
    
    public DefaultAIStyle(Context context) {
        this.context = context;
    }
    
    @Override
    public int getChatBubbleDrawable(boolean isUser) {
        return isUser ? R.drawable.chat_bubble_sent : R.drawable.chat_bubble_received;
    }
    
    @Override
    public int getInputBackgroundDrawable() {
        return R.drawable.chat_input_background;
    }
    
    @Override
    public int getBackgroundDrawable() {
        return android.R.color.white;
    }
    
    @Override
    public ColorStateList getPrimaryColor() {
        return ColorStateList.valueOf(context.getResources().getColor(R.color.colorPrimary));
    }
    
    @Override
    public int getHintTextColor() {
        return Color.GRAY;
    }
    
    @Override
    public Typeface getTypeface(Context context) {
        return Typeface.DEFAULT;
    }
    
    @Override
    public String getInputHint() {
        return context.getString(R.string.input_message_hint);
    }
    
    @Override
    public String getSendButtonText() {
        return context.getString(R.string.send);
    }
    
    @Override
    public String transformText(String originalText) {
        return originalText; // 默认不转换
    }
    
    @Override
    public int getButtonCornerRadius() {
        return 4; // 默认圆角
    }
    
    @Override
    public int getButtonStrokeWidth() {
        return 1; // 默认边框宽度
    }
} 