package com.example.mentalhealthdiary.animation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * 优化版猫爪视图 - 更加透明，防止遮挡
 */
public class CatPawView extends View {
    private Paint mainPaint; // 主色调画笔
    private Paint highlightPaint; // 高光画笔
    private Paint outlinePaint; // 轮廓画笔
    private Path pawPath; // 爪印路径
    private boolean isLeftPaw; // 是否是左爪
    private float padding; // 内边距
    private int alpha; // 保存透明度值
    
    /**
     * 创建猫爪视图
     * @param context 上下文
     * @param pawColor 爪印主色
     * @param isLeftPaw 是否是左爪
     * @param alpha 透明度
     */
    public CatPawView(Context context, int pawColor, boolean isLeftPaw, int alpha) {
        super(context);
        this.isLeftPaw = isLeftPaw;
        this.alpha = alpha; // 保存alpha作为类成员变量
        init(pawColor, alpha);
    }
    
    private void init(int pawColor, int alpha) {
        // 计算亮色和暗色版本
        int lighterColor = lightenColor(pawColor, 0.3f);
        int darkerColor = darkenColor(pawColor, 0.2f);
        
        // 设置主色调画笔 - 增加透明度
        mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainPaint.setColor(pawColor);
        mainPaint.setStyle(Paint.Style.FILL);
        mainPaint.setAlpha(alpha);
        
        // 设置高光画笔 - 保持较高透明度
        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(lighterColor);
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setAlpha(Math.min(alpha + 20, 255)); // 高光稍微明显一点
        
        // 设置轮廓画笔 - 轮廓略微明显以保持形状可见
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setColor(darkerColor);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(1.5f); // 稍微细一点的线条
        outlinePaint.setAlpha(Math.min(alpha + 30, 255)); // 轮廓稍微明显一点
        
        // 创建爪印路径
        pawPath = new Path();
        
        // 设置内边距
        padding = 2f;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 获取视图尺寸
        float width = getWidth();
        float height = getHeight();
        
        // 清空路径并重新创建
        pawPath.reset();
        
        // 绘制猫爪 - 主要爪垫部分
        float centerX = width / 2;
        float centerY = height / 2;
        float pawRadius = Math.min(width, height) * 0.35f;
        
        // 主爪垫
        RectF mainPadRect = new RectF(
                centerX - pawRadius,
                centerY - pawRadius,
                centerX + pawRadius,
                centerY + pawRadius + pawRadius * 0.2f // 略微椭圆
        );
        
        // 绘制主爪垫
        canvas.drawOval(mainPadRect, mainPaint);
        
        // 绘制主爪垫轮廓
        canvas.drawOval(mainPadRect, outlinePaint);
        
        // 爪尖位置计算
        float toeSpacing = pawRadius * 0.6f;
        float toeOffsetY = -pawRadius * 0.9f;
        
        // 确定爪尖位置 - 根据左/右爪微调
        float[] toeOffsetsX;
        if (isLeftPaw) {
            toeOffsetsX = new float[] { -toeSpacing, 0, toeSpacing };
        } else {
            toeOffsetsX = new float[] { -toeSpacing, 0, toeSpacing };
        }
        
        // 绘制三个爪尖
        for (int i = 0; i < 3; i++) {
            float toeX = centerX + toeOffsetsX[i];
            float toeY = centerY + toeOffsetY;
            float toeRadius = pawRadius * 0.45f;
            
            // 绘制爪尖椭圆
            RectF toeRect = new RectF(
                    toeX - toeRadius,
                    toeY - toeRadius,
                    toeX + toeRadius,
                    toeY + toeRadius
            );
            canvas.drawOval(toeRect, mainPaint);
            canvas.drawOval(toeRect, outlinePaint);
            
            // 为每个爪尖添加高光
            float highlightRadius = toeRadius * 0.4f;
            float highlightOffsetX = toeRadius * 0.1f;
            float highlightOffsetY = -toeRadius * 0.1f;
            
            canvas.drawCircle(
                    toeX + highlightOffsetX,
                    toeY + highlightOffsetY,
                    highlightRadius,
                    highlightPaint
            );
        }
        
        // 为主爪垫添加高光
        float mainHighlightRadius = pawRadius * 0.35f;
        canvas.drawCircle(
                centerX - pawRadius * 0.15f,
                centerY - pawRadius * 0.15f,
                mainHighlightRadius,
                highlightPaint
        );
        
        // 添加肉球纹理 - 用更透明的细线条表示
        Paint texturePaint = new Paint(outlinePaint);
        texturePaint.setStrokeWidth(1f);
        texturePaint.setAlpha(alpha / 3); // 纹理线条更透明（原来是alpha/2）
        
        // 添加几条简单的弧线作为纹理
        for (int i = 0; i < 3; i++) {
            float startX = centerX - pawRadius * 0.5f + pawRadius * i * 0.5f;
            float startY = centerY + pawRadius * 0.3f;
            float endX = startX;
            float endY = startY + pawRadius * 0.3f;
            
            RectF arcRect = new RectF(
                    startX - pawRadius * 0.3f,
                    startY,
                    startX + pawRadius * 0.3f,
                    endY
            );
            
            canvas.drawArc(arcRect, 0, 180, false, texturePaint);
        }
    }
    
    // 辅助方法：增亮颜色
    private int lightenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0, hsv[1] - factor); // 降低饱和度
        hsv[2] = Math.min(1.0f, hsv[2] + factor); // 提高亮度
        return Color.HSVToColor(hsv);
    }
    
    // 辅助方法：变暗颜色
    private int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0, hsv[2] - factor); // 降低亮度
        return Color.HSVToColor(hsv);
    }
} 