package com.example.mentalhealthdiary.animation;

import android.content.Context;
import android.graphics.Color;
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
    
    // 添加成员变量，用于保存当前使用的圆心位置
    private float currentCircleCenterX;
    private float currentCircleCenterY;
    
    // 添加成员变量记录圆形行走方向
    private boolean currentCircleClockwise;
    
    // 添加成员变量保存每个点的角度
    private float[] currentPointAngles;
    
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
        
        if (style != null) {
            Log.d(TAG, "设置AI风格: " + style.getClass().getSimpleName());
            scheduleNextAnimation();
        }
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
     * 播放猫爪印序列动画 - 优化所有路径的显示逻辑
     */
    private void playPawprintSequence() {
        if (!isAnimationEnabled) return;
        
        Log.d(TAG, "播放猫爪序列动画");
        
        // 确定容器尺寸
        int containerWidth = containerView.getWidth();
        int containerHeight = containerView.getHeight();
        
        if (containerWidth <= 0 || containerHeight <= 0) {
            Log.e(TAG, "容器尺寸无效，无法显示动画");
            return;
        }
        
        // 猫爪印设置
        final int BASIC_PAWPRINT_COUNT = 6;
        int pawprintCount;
        final int PAWPRINT_SIZE = 45;
        final int STEP_DISTANCE = 60;
        
        // 修改路径选择逻辑，减少爱心出现频率
        int pathType;
        
        // 只有10%的概率出现爱心路径
        if (random.nextInt(10) == 0) {
            pathType = 5; // 爱心路径 (原S形索引5)
        } else {
            // 其他90%的情况随机选择其他4种路径
            pathType = random.nextInt(4); // 0-3之间的路径
        }
        
        Log.d(TAG, "选择路径类型: " + pathType);
        
        // 根据路径类型调整爪印数量，确保完整性
        switch (pathType) {
            case 0: // 直线路径
            case 1: // 斜线路径
                pawprintCount = BASIC_PAWPRINT_COUNT;
                break;
            case 2: // 弧形路径
                pawprintCount = BASIC_PAWPRINT_COUNT + 3;
                break;
            case 3: // 圆形路径
                pawprintCount = BASIC_PAWPRINT_COUNT + 6;
                break;
            case 5: // 爱心路径
                pawprintCount = BASIC_PAWPRINT_COUNT + 8;
                break;
            default:
                pawprintCount = BASIC_PAWPRINT_COUNT;
        }
        
        // 随机决定是从左侧还是右侧、是否朝下
        boolean fromLeft = random.nextBoolean();
        boolean pawsFacingDown = random.nextBoolean();
        boolean fromTopToBottom = pawsFacingDown;
        
        // 准备爪印位置坐标数组
        float[] xPositions = new float[pawprintCount];
        float[] yPositions = new float[pawprintCount];
        
        // 根据路径类型计算坐标
        switch (pathType) {
            case 0: // 直线路径
                generateStraightPath(xPositions, yPositions, containerWidth, containerHeight, 
                                    fromLeft, fromTopToBottom, pawprintCount, STEP_DISTANCE);
                break;
            
            case 1: // 斜线路径
                generateDiagonalPath(xPositions, yPositions, containerWidth, containerHeight, 
                                   fromLeft, fromTopToBottom, pawprintCount, STEP_DISTANCE);
                break;
            
            case 2: // 弧形路径
                generateArcPath(xPositions, yPositions, containerWidth, containerHeight,
                              fromLeft, fromTopToBottom, pawprintCount);
                break;
            
            case 3: // 圆形路径
                generateCircularPath(xPositions, yPositions, containerWidth, containerHeight, 
                                   fromLeft, pawprintCount);
                break;
            
            case 5: // 爱心路径
                generateHeartPath(xPositions, yPositions, containerWidth, containerHeight, 
                                pawprintCount);
                break;
        }
        
        // 猫爪颜色
        final int[] pawColors = new int[] {
            Color.parseColor("#FF80AB"),  // 淡粉色
            Color.parseColor("#F48FB1"),  // 粉红色
            Color.parseColor("#F06292")   // 中粉色
        };
        
        // 创建多个猫爪印，交替左右爪
        for (int i = 0; i < pawprintCount; i++) {
            boolean isLeftPaw = i % 2 == 0; // 奇偶交替
            
            // 如果是从右侧开始，则翻转左右爪逻辑
            if (!fromLeft) {
                isLeftPaw = !isLeftPaw;
            }
            
            // 从坐标数组获取位置
            float x = xPositions[i];
            float y = yPositions[i];
            
            // 计算当前爪印的旋转角度 - 基于路径方向
            float pathAngle = 0;
            
            // 如果不是第一个爪印，计算与前一个爪印的角度
            if (i > 0) {
                float dx = x - xPositions[i-1];
                float dy = y - yPositions[i-1];
                pathAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
            }
            
            // 随机选择猫爪颜色
            int pawColor = pawColors[random.nextInt(pawColors.length)];
            
            // 创建猫爪视图
            CatPawView pawView = new CatPawView(context, pawColor, isLeftPaw, 150);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(PAWPRINT_SIZE, PAWPRINT_SIZE);
            containerView.addView(pawView, params);
            
            // 设置位置
            pawView.setX(x);
            pawView.setY(y);
            
            // 设置旋转 - 综合考虑路径方向和猫爪朝向
            float baseRotation = pawsFacingDown ? 180 : 0;
            
            // 计算最终旋转角度 - 调整爪印使其沿着路径方向
            float finalRotation;
            
            if (pathType == 3) { // 圆形路径特殊处理
                // 获取当前点的角度 - 使用保存的角度数组
                float pointAngle = currentPointAngles[i];
                
                // 计算切线方向 - 垂直于半径方向
                float tangentAngle;
                if (currentCircleClockwise) {
                    tangentAngle = pointAngle - 90; // 顺时针方向的切线
                } else {
                    tangentAngle = pointAngle + 90; // 逆时针方向的切线
                }
                
                // 最终旋转角度 - 根据猫爪朝向进一步调整
                finalRotation = tangentAngle;
                
                // 左右爪的差异
                finalRotation += isLeftPaw ? -15 : 15;
                
                // 调整爪印向前的方向
                if (!pawsFacingDown) {
                    finalRotation += 180;
                }
            } else if (pathType == 5) { // 爱心路径特殊处理
                // 计算爪印沿爱心曲线的切线方向
                float tangentAngle;
                
                if (i > 0) {
                    // 计算当前点与前一个点的连线角度
                    float dx = x - xPositions[i-1];
                    float dy = y - yPositions[i-1];
                    tangentAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
                } else if (i < pawprintCount - 1) {
                    // 对第一个点，使用第一个点与第二个点的连线角度
                    float dx = xPositions[i+1] - x;
                    float dy = yPositions[i+1] - y;
                    tangentAngle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 180;
                } else {
                    tangentAngle = 0;
                }
                
                // 设置爪印沿切线方向
                finalRotation = tangentAngle + 90;
                
                // 左右爪的微调
                finalRotation += isLeftPaw ? -10 : 10;
            } else {
                // 直线、斜线和弧形路径
                float directionRotation = fromLeft ? -20 : 20;
                float pawRotation = isLeftPaw ? -10 : 10;
                finalRotation = baseRotation + directionRotation + pawRotation;
                // 添加少量随机性
                finalRotation += (random.nextFloat() * 6 - 3);
            }
            
            pawView.setRotation(finalRotation);
            
            // 初始透明
            pawView.setAlpha(0f);
            
            // 设置Z轴高度确保爪印在最上层
            pawView.setZ(1000f);
            
            // 为所有路径类型使用相同的序列化显示逻辑
            int finalI = i;
            
            // 应用相同的"按序出现"动画，让所有路径类型都符合猫行走的逻辑
            pawView.animate()
                .alpha(0.8f)
                .setStartDelay(150 * i) // 所有爪印类型都按顺序出现
                .setDuration(200)
                .withEndAction(() -> {
                    // 延迟淡出，让整个爪印序列更容易看清
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        pawView.animate()
                            .alpha(0f)
                            .setDuration(800)
                            .withEndAction(() -> containerView.removeView(pawView))
                            .start();
                    }, 2000 + (pawprintCount - finalI) * 100); // 第一个出现的最后消失
                })
                .start();
        }
    }

    // 生成直线路径坐标
    private void generateStraightPath(float[] xPositions, float[] yPositions, 
                                    int width, int height, boolean fromLeft, 
                                    boolean fromTop, int count, int stepDistance) {
        float insetFromEdge = width * 0.15f;
        float startX = fromLeft ? insetFromEdge : width - insetFromEdge;
        
        // 随机选择起始Y位置
        float startY = height * (0.1f + random.nextFloat() * 0.7f);
        
        // 如果是从下到上，调整起始点为序列末端
        if (!fromTop) {
            startY = startY + stepDistance * (count - 1);
        }
        
        for (int i = 0; i < count; i++) {
            // X轴上微小的左右摆动
            xPositions[i] = startX + (fromLeft ? 1 : -1) * (i % 2) * (stepDistance * 0.35f);
            
            // Y轴上的移动
            if (fromTop) {
                yPositions[i] = startY + i * stepDistance;
            } else {
                yPositions[i] = startY - i * stepDistance;
            }
        }
    }

    // 生成斜线路径坐标
    private void generateDiagonalPath(float[] xPositions, float[] yPositions, 
                                   int width, int height, boolean fromLeft, 
                                   boolean fromTop, int count, int stepDistance) {
        // 基准路径点 - 起点和终点
        float startX, startY;
        float endX, endY;
        
        // 确保起点非常靠近边缘
        if (fromLeft) {
            startX = width * 0.02f; 
            endX = width * 0.2f;   // 缩短路径长度，使爪印更紧凑
        } else {
            startX = width * 0.98f; 
            endX = width * 0.8f;   // 缩短路径长度，使爪印更紧凑
        }
        
        if (fromTop) {
            startY = height * 0.05f;
            endY = height * 0.4f;   // 缩短纵向距离
        } else {
            startY = height * 0.65f;
            endY = height * 0.3f;   // 缩短纵向距离
        }
        
        // 计算主路径 - 缩短步长使爪印更紧凑
        float xStep = (endX - startX) / (count - 1);
        float yStep = (endY - startY) / (count - 1);
        
        // 生成爪印坐标 - 一左一右交替但间距更小
        for (int i = 0; i < count; i++) {
            float baseX = startX + i * xStep;
            float baseY = startY + i * yStep;
            
            // 增加左右交替的偏移 - 但减小偏移量
            float sideOffset = (i % 2 == 0) ? -1 : 1;
            if (!fromLeft) sideOffset *= -1;
            
            // 减小横向偏移量，让爪印更紧凑
            float xOffset = sideOffset * stepDistance * 0.1f; // 减小为原来的一半
            
            // 应用坐标，减小随机性
            xPositions[i] = baseX + xOffset + (random.nextFloat() * 3 - 1.5f);
            yPositions[i] = baseY + (random.nextFloat() * 3 - 1.5f);
        }
    }

    /**
     * 生成弧形路径 - 模拟真实猫咪转弯路径
     */
    private void generateArcPath(float[] xPositions, float[] yPositions, 
                             int width, int height, boolean fromLeft, boolean fromTop, int count) {
        // 弧形路径应该像猫在转弯，不是机械的弧线
        
        // 设置起点
        float startX, startY;
        if (fromLeft) {
            startX = width * 0.1f;
        } else {
            startX = width * 0.9f;
        }
        startY = height * (0.3f + random.nextFloat() * 0.3f);
        
        // 设置终点 - 猫转弯后的方向
        float endX, endY;
        if (fromLeft) {
            // 从左向右转弯，转向上或下
            endX = width * (0.6f + random.nextFloat() * 0.3f);
            if (fromTop) {
                endY = startY + height * (0.1f + random.nextFloat() * 0.1f); // 向下转
            } else {
                endY = startY - height * (0.1f + random.nextFloat() * 0.1f); // 向上转
            }
        } else {
            // 从右向左转弯，转向上或下
            endX = width * (0.1f + random.nextFloat() * 0.3f);
            if (fromTop) {
                endY = startY + height * (0.1f + random.nextFloat() * 0.1f); // 向下转
            } else {
                endY = startY - height * (0.1f + random.nextFloat() * 0.1f); // 向上转
            }
        }
        
        // 设置控制点 - 确定弧度
        float controlX = (startX + endX) / 2;
        float controlY;
        
        if (fromTop) {
            controlY = Math.min(startY, endY) - height * 0.1f; // 弧向上凸
        } else {
            controlY = Math.max(startY, endY) + height * 0.1f; // 弧向下凸
        }
        
        // 步幅变化 - 猫走路不会步幅完全一致
        float[] stepVariation = new float[count];
        for (int i = 0; i < count; i++) {
            // 微小的步幅变化
            stepVariation[i] = random.nextFloat() * 5 - 2.5f;
        }
        
        // 左右摇摆 - 猫走路时身体有轻微摇摆
        float[] sideOffset = new float[count];
        for (int i = 0; i < count; i++) {
            // 左右交替的小偏移
            sideOffset[i] = ((i % 2) * 2 - 1) * (3 + random.nextFloat() * 3);
        }
        
        // 生成二次贝塞尔曲线，带自然变化
        for (int i = 0; i < count; i++) {
            // 参数t从0到1，表示沿曲线的进度
            float t = (float) i / (count - 1);
            
            // 二次贝塞尔曲线公式
            float mt = 1 - t;
            float baseX = mt*mt * startX + 2*mt*t * controlX + t*t * endX;
            float baseY = mt*mt * startY + 2*mt*t * controlY + t*t * endY;
            
            // 应用自然变化
            xPositions[i] = baseX + sideOffset[i];
            yPositions[i] = baseY + stepVariation[i];
        }
    }

    /**
     * 生成圆形路径 - 完全重新设计猫爪朝向逻辑
     */
    private void generateCircularPath(float[] xPositions, float[] yPositions, 
                               int width, int height, boolean fromLeft, int count) {
        // 设置圆心位置
        float centerX = width * (0.3f + random.nextFloat() * 0.4f);
        float centerY = height * (0.3f + random.nextFloat() * 0.3f);
        
        // 保存圆心位置
        this.currentCircleCenterX = centerX;
        this.currentCircleCenterY = centerY;
        
        // 使用适中的半径
        float radius = Math.min(width, height) * 0.15f;
        
        // 根据起点决定行走方向 - 确保从左/右出现
        boolean clockwise;
        if (fromLeft) {
            clockwise = true;  // 从左侧开始是顺时针方向
        } else {
            clockwise = false; // 从右侧开始是逆时针方向
        }
        
        // 记录行走方向
        this.currentCircleClockwise = clockwise;
        
        // 记录每个点的角度，用于后续计算切线角度
        float[] pointAngles = new float[count];
        
        // 根据起点决定第一个爪印的位置
        float startAngle;
        if (fromLeft) {
            startAngle = 180; // 从左侧开始
        } else {
            startAngle = 0;   // 从右侧开始
        }
        
        // 生成点序列
        float sweepAngle = 270; // 不是完整圆
        float angleStep = sweepAngle / (count - 1);
        
        for (int i = 0; i < count; i++) {
            // 计算当前角度
            float currentAngle;
            if (clockwise) {
                currentAngle = startAngle + angleStep * i;
            } else {
                currentAngle = startAngle - angleStep * i;
            }
            
            // 保存每个点的角度，用于后续计算旋转角度
            pointAngles[i] = currentAngle;
            
            // 转换为弧度计算坐标
            float angleRad = (float) Math.toRadians(currentAngle);
            xPositions[i] = centerX + radius * (float) Math.cos(angleRad);
            yPositions[i] = centerY + radius * (float) Math.sin(angleRad);
        }
        
        // 保存点角度数组
        this.currentPointAngles = pointAngles;
    }

    // 生成S形路径坐标
    private void generateSPath(float[] xPositions, float[] yPositions, 
                            int width, int height, boolean fromLeft, 
                            boolean fromTop, int count) {
        // 压缩S形，使其更紧凑且左右交替
        // S形曲线控制点
        float x1, x2, x3, x4;
        
        // 更靠近屏幕边缘
        if (fromLeft) {
            // 左侧S形
            x1 = width * 0.02f;
            x2 = width * 0.10f;
            x3 = width * 0.02f;
            x4 = width * 0.10f;
        } else {
            // 右侧S形
            x1 = width * 0.98f;
            x2 = width * 0.90f;
            x3 = width * 0.98f;
            x4 = width * 0.90f;
        }
        
        // 垂直分布 - 让S形更短
        float topY = height * 0.1f;
        float bottomY = height * 0.5f; // 减小纵向范围
        float range = bottomY - topY;
        
        float y1, y2, y3, y4;
        
        if (fromTop) {
            y1 = topY;
            y2 = topY + range * 0.3f;
            y3 = topY + range * 0.7f;
            y4 = bottomY;
        } else {
            y1 = bottomY;
            y2 = bottomY - range * 0.3f;
            y3 = bottomY - range * 0.7f;
            y4 = topY;
        }
        
        // 使用贝塞尔曲线生成S形路径
        for (int i = 0; i < count; i++) {
            float t = (float) i / (count - 1);
            
            // 三次贝塞尔曲线公式
            float mt = 1 - t;
            float baseX = mt*mt*mt * x1 + 3*mt*mt*t * x2 + 3*mt*t*t * x3 + t*t*t * x4;
            float baseY = mt*mt*mt * y1 + 3*mt*mt*t * y2 + 3*mt*t*t * y3 + t*t*t * y4;
            
            // 增加左右交替的偏移
            float sideOffset = (i % 2 == 0) ? -1 : 1;
            float xOffset = sideOffset * 5.0f; // 小偏移，模拟猫步交替
            
            xPositions[i] = baseX + xOffset;
            yPositions[i] = baseY;
        }
    }
    
    /**
     * 生成顺序出现的爱心形状猫爪路径 - 从底部开始画爱心
     */
    private void generateHeartPath(float[] xPositions, float[] yPositions, 
                               int width, int height, int count) {
        // 爱心中心位置
        float centerX = width / 2;
        float centerY = height * 0.4f;
        
        // 爱心大小 - 适中大小更明显
        float scale = Math.min(width, height) * 0.22f;
        
        // 确保有足够多的爪印
        int effectiveCount = Math.min(count, 18); // 更多爪印形成平滑爱心
        
        // 设置起始点 - 从底部的V形顶点开始
        int startPointIndex = 0; // 从爱心底部中间开始
        
        // 生成爱心路径 - 从底部开始，顺时针方向绘制
        for (int i = 0; i < effectiveCount; i++) {
            // 调整参数范围使轨迹从底部开始
            // 参数t控制在爱心曲线上的位置
            // 我们将起始点设在底部，然后顺时针移动
            float t = (float) (Math.PI + Math.PI * 2 * i / effectiveCount);
            
            float x, y;
            
            // 爱心公式 - 极坐标方程更适合顺序绘制
            float r = (float) (Math.sin(t) * Math.sqrt(Math.abs(Math.cos(t))) / 
                      (Math.sin(t) + 1.4) - 2 * Math.sin(t) + 2);
                      
            // 转换为笛卡尔坐标
            x = (float) (r * Math.cos(t));
            y = (float) (r * Math.sin(t));
            
            // 缩放并平移到屏幕中央
            // 翻转和旋转以获得正确朝向的爱心
            xPositions[i] = centerX - scale * x;
            yPositions[i] = centerY - scale * y;
        }
        
        // 如果有剩余位置，填充到屏幕外
        for (int i = effectiveCount; i < count; i++) {
            xPositions[i] = -100;
            yPositions[i] = -100;
        }
    }
    
    /**
     * 修改scheduleNextAnimation方法，支持猫爪动画
     */
    private void scheduleNextAnimation() {
        if (!isAnimationEnabled || currentStyle == null) return;
        
        // 计算下一次动画的延迟（基于频率）
        float frequency = currentStyle.getAnimationFrequency();
        long delayMillis = (long) (TimeUnit.MINUTES.toMillis(1) / frequency);
        // 添加随机性，让动画出现时间不那么规律
        delayMillis = (long) (delayMillis * (0.8 + random.nextFloat() * 0.4));
        
        Log.d(TAG, "安排下一次动画，延迟: " + delayMillis + "ms");
        
        animationRunnable = () -> {
            // 根据风格选择动画类型
            if (currentStyle != null && 
                currentStyle.getClass().getSimpleName().contains("CatGirl")) {
                Log.d(TAG, "执行猫爪动画序列");
                playPawprintSequence();
            } else {
                Log.d(TAG, "执行普通随机动画");
                playRandomAnimation();
            }
            scheduleNextAnimation(); // 递归安排下一个动画
        };
        
        animationHandler.postDelayed(animationRunnable, delayMillis);
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

    /**
     * 公共测试方法，用于手动触发猫爪动画测试
     */
}