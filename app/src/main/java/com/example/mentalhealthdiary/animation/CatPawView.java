package com.example.mentalhealthdiary.animation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * 自定义猫爪视图，优化美观度
 */
public class CatPawView extends View {
    private Paint paint;
    private Paint strokePaint;
    private int pawColor;
    private boolean isLeftPaw; // 是否左爪
    private int alphaValue = 180; // 默认透明度值

    public CatPawView(Context context, int pawColor, boolean isLeftPaw) {
        this(context, pawColor, isLeftPaw, 180); // 默认透明度
    }

    public CatPawView(Context context, int pawColor, boolean isLeftPaw, int alpha) {
        super(context);
        this.pawColor = pawColor;
        this.isLeftPaw = isLeftPaw;
        this.alphaValue = alpha; // 允许自定义透明度
        init();
    }

    private void init() {
        // 主色填充画笔 - 添加自定义半透明效果
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 使用原色但增加半透明效果
        int alphaColor = Color.argb(
            alphaValue,  // 使用自定义透明度
            Color.red(pawColor),
            Color.green(pawColor),
            Color.blue(pawColor)
        );
        paint.setColor(alphaColor);
        paint.setStyle(Paint.Style.FILL);
        
        // 轮廓描边画笔 - 也添加半透明效果
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int strokeColor = adjustColorBrightness(pawColor, -0.3f);
        int alphaStrokeColor = Color.argb(
            Math.min(255, alphaValue + 20),  // 比填充稍微不透明一点
            Color.red(strokeColor),
            Color.green(strokeColor),
            Color.blue(strokeColor)
        );
        strokePaint.setColor(alphaStrokeColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.5f);
    }
    
    // 调整颜色亮度
    private int adjustColorBrightness(int color, float factor) {
        int r = (int) Math.min(255, Math.max(0, Color.red(color) + 255 * factor));
        int g = (int) Math.min(255, Math.max(0, Color.green(color) + 255 * factor));
        int b = (int) Math.min(255, Math.max(0, Color.blue(color) + 255 * factor));
        return Color.rgb(r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float width = getWidth();
        float height = getHeight();
        
        // 确保视图足够大以容纳爪印
        if (width < 10 || height < 10) return;
        
        // 添加轻微阴影效果
        paint.setShadowLayer(2f, 1f, 1f, Color.parseColor("#40000000"));
        
        float centerX = width / 2;
        float centerY = height / 2;
        float padRadius = Math.min(width, height) * 0.28f;
        
        // 绘制主脚垫（稍微偏椭圆）
        canvas.drawOval(
            centerX - padRadius, 
            centerY - padRadius * 0.9f, 
            centerX + padRadius, 
            centerY + padRadius * 1.1f, 
            paint);
        canvas.drawOval(
            centerX - padRadius, 
            centerY - padRadius * 0.9f, 
            centerX + padRadius, 
            centerY + padRadius * 1.1f, 
            strokePaint);

        // 绘制脚趾（更有形状）
        float toeRadius = padRadius * 0.5f;
        float toeDistanceY = padRadius * 0.9f;
        float toeDistanceX = padRadius * 0.7f;
        
        // 左趾
        drawToe(canvas, centerX - toeDistanceX, centerY - toeDistanceY, toeRadius, -15);
        // 中趾
        drawToe(canvas, centerX, centerY - toeDistanceY * 1.1f, toeRadius * 1.05f, 0);
        // 右趾
        drawToe(canvas, centerX + toeDistanceX, centerY - toeDistanceY, toeRadius, 15);
        
        // 小拇指（根据左右爪形状稍有不同）
        if (isLeftPaw) {
            drawToe(canvas, centerX + toeDistanceX * 0.8f, 
                   centerY + toeDistanceY * 0.3f, 
                   toeRadius * 0.6f, 45);
        } else {
            drawToe(canvas, centerX - toeDistanceX * 0.8f, 
                   centerY + toeDistanceY * 0.3f, 
                   toeRadius * 0.6f, -45);
        }
        
        // 添加内部纹理
        drawPadTexture(canvas, centerX, centerY, padRadius);
    }
    
    // 绘制椭圆形脚趾，可设置旋转角度
    private void drawToe(Canvas canvas, float x, float y, float radius, float angle) {
        canvas.save();
        canvas.rotate(angle, x, y);
        canvas.drawOval(
            x - radius, 
            y - radius * 1.2f, 
            x + radius, 
            y + radius * 0.8f, 
            paint);
        canvas.drawOval(
            x - radius, 
            y - radius * 1.2f, 
            x + radius, 
            y + radius * 0.8f, 
            strokePaint);
        canvas.restore();
    }
    
    // 绘制脚垫内部纹理
    private void drawPadTexture(Canvas canvas, float centerX, float centerY, float padRadius) {
        Paint texturePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        texturePaint.setColor(adjustColorBrightness(pawColor, -0.1f));
        texturePaint.setStyle(Paint.Style.STROKE);
        texturePaint.setStrokeWidth(1f);
        
        // 横向分割
        canvas.drawLine(
            centerX - padRadius * 0.5f,
            centerY, 
            centerX + padRadius * 0.5f, 
            centerY, 
            texturePaint);
            
        // 竖向分割
        canvas.drawLine(
            centerX, 
            centerY - padRadius * 0.5f, 
            centerX, 
            centerY + padRadius * 0.5f, 
            texturePaint);
    }
} 