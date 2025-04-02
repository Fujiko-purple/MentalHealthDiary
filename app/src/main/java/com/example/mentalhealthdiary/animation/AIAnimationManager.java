package com.example.mentalhealthdiary.animation;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.example.mentalhealthdiary.style.AIPersonalityStyle;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * AI动画管理器，负责根据AI风格展示特色动画
 */
public class AIAnimationManager {
    private static final String TAG = "AIAnimationManager";
    
    private Context context;
    private ViewGroup containerView;
    private AIPersonalityStyle currentStyle;
    private Handler animationHandler;
    private Random random = new Random();
    private boolean isAnimationEnabled = true;
    
    private Runnable animationRunnable;
    
    public AIAnimationManager(Context context, ViewGroup containerView) {
        this.context = context;
        this.containerView = containerView;
        this.animationHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置当前AI风格并更新动画
     */
    public void setAIStyle(AIPersonalityStyle style) {
        this.currentStyle = style;
        stopAnimations();
        
        if (style == null || style.getAnimationResources().length == 0) {
            Log.d(TAG, "此AI风格没有动画资源");
            return;
        }
        
        scheduleNextAnimation();
    }
    
    /**
     * 启用或禁用动画
     */
    public void setAnimationEnabled(boolean enabled) {
        this.isAnimationEnabled = enabled;
        if (!enabled) {
            stopAnimations();
        } else if (currentStyle != null) {
            scheduleNextAnimation();
        }
    }
    
    /**
     * 停止所有动画
     */
    public void stopAnimations() {
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
    }
    
    /**
     * 安排下一个动画
     */
    private void scheduleNextAnimation() {
        if (!isAnimationEnabled || currentStyle == null) return;
        
        // 计算下一次动画的延迟（基于频率）
        float frequency = currentStyle.getAnimationFrequency();
        long delayMillis = (long) (TimeUnit.MINUTES.toMillis(1) / frequency);
        // 添加随机性，让动画出现时间不那么规律
        delayMillis = (long) (delayMillis * (0.8 + random.nextFloat() * 0.4));
        
        animationRunnable = () -> {
            playRandomAnimation();
            scheduleNextAnimation(); // 递归安排下一个动画
        };
        
        animationHandler.postDelayed(animationRunnable, delayMillis);
        Log.d(TAG, "下一个动画将在 " + delayMillis/1000 + " 秒后播放");
    }
    
    /**
     * 播放随机动画
     */
    private void playRandomAnimation() {
        if (currentStyle == null || !isAnimationEnabled) return;
        
        // 获取资源
        int[] animResources = currentStyle.getAnimationResources();
        
        if (animResources.length == 0) return;
        
        // 随机选择一个动画资源
        int animResId = animResources[random.nextInt(animResources.length)];
        
        // 创建动画视图
        ImageView animView = new ImageView(context);
        animView.setImageResource(animResId);
        
        // 设置动画位置
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        containerView.addView(animView, params);
        
        // 根据位置策略设置位置
        setViewPosition(animView, currentStyle.getAnimationPositionStrategy());
        
        // 播放动画
        Animation animation = AnimationUtils.loadAnimation(
                context, android.R.anim.fade_in);
        animation.setDuration(1000);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                // 动画结束后淡出并移除视图
                Animation fadeOut = AnimationUtils.loadAnimation(
                        context, android.R.anim.fade_out);
                fadeOut.setDuration(500);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        containerView.removeView(animView);
                    }
                    
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                animView.startAnimation(fadeOut);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        animView.startAnimation(animation);
    }
    
    /**
     * 根据策略设置视图位置
     */
    private void setViewPosition(View view, AnimationPositionStrategy strategy) {
        int containerWidth = containerView.getWidth();
        int containerHeight = containerView.getHeight();
        
        if (containerWidth == 0 || containerHeight == 0) {
            // 容器尚未测量，使用默认位置
            view.setX(50);
            view.setY(50);
            return;
        }
        
        float x, y;
        
        switch (strategy) {
            case RANDOM:
                x = random.nextInt(containerWidth - 200);
                y = random.nextInt(containerHeight - 200);
                break;
            case TOP:
                x = random.nextInt(containerWidth - 200);
                y = 50;
                break;
            case BOTTOM:
                x = random.nextInt(containerWidth - 200);
                y = containerHeight - 250;
                break;
            case LEFT:
                x = 50;
                y = random.nextInt(containerHeight - 200);
                break;
            case RIGHT:
                x = containerWidth - 250;
                y = random.nextInt(containerHeight - 200);
                break;
            case CENTER:
            default:
                x = containerWidth / 2 - 100;
                y = containerHeight / 2 - 100;
                break;
        }
        
        view.setX(x);
        view.setY(y);
    }
}