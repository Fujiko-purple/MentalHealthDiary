package com.example.mentalhealthdiary.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.example.mentalhealthdiary.animation.AnimationPositionStrategy;

/**
 * AI人格界面风格接口
 * 定义所有AI界面风格必须实现的方法
 */
public interface AIPersonalityStyle {
    // 获取气泡背景样式
    @DrawableRes int getChatBubbleDrawable(boolean isUser);
    
    // 获取输入框背景
    @DrawableRes int getInputBackgroundDrawable();
    
    // 获取背景样式
    @DrawableRes int getBackgroundDrawable();
    
    // 获取主色调
    ColorStateList getPrimaryColor();
    
    // 获取提示文字颜色
    @ColorInt int getHintTextColor();
    
    // 获取字体
    Typeface getTypeface(Context context);
    
    // 获取输入提示文字
    String getInputHint();
    
    // 获取发送按钮文字
    String getSendButtonText();
    
    // 文本转换方法(可选)
    String transformText(String originalText);
    
    // 添加按钮样式控制方法
    int getButtonCornerRadius();
    int getButtonStrokeWidth();

    /**
     * 获取AI特色动画资源
     * @return 动画资源ID数组，可以是drawable、动画或原始资源
     */
    int[] getAnimationResources();

    /**
     * 获取动画显示频率（每分钟次数）
     * @return 动画显示频率
     */
    float getAnimationFrequency();

    /**
     * 获取动画显示位置策略
     * @return 动画位置策略
     */
    AnimationPositionStrategy getAnimationPositionStrategy();
} 