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
        return ColorStateList.valueOf(Color.parseColor("#2196F3")); // 默认蓝色
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
        return "输入消息...";
    }
    
    @Override
    public String getSendButtonText() {
        return "发送";
    }
    
    @Override
    public String transformText(String originalText) {
        return originalText; // 默认不转换
    }
} 