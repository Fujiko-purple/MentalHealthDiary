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
     * 播放猫爪印序列动画 - 修改主方法，添加爱心路径
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
            
            case 3: // 圆形/椭圆路径
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
                // 计算每个爪印指向圆心的角度
                float dx = x - containerWidth / 2; // 需要在循环前记录当前使用的圆心
                float dy = y - containerHeight / 2;
                float angleToCenter = (float) Math.toDegrees(Math.atan2(dy, dx));
                
                // 如果是顺时针，爪印应该垂直于圆的切线
                finalRotation = angleToCenter + 90;
                
                // 根据爪印朝向调整角度
                if (!pawsFacingDown) {
                    finalRotation += 180; // 如果朝上，翻转180度
                }
                
                // 左右爪的微调
                finalRotation += isLeftPaw ? -10 : 10;
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
            
            // 延迟显示每个爪印
            int finalI = i;
            pawView.animate()
                .alpha(0.8f) // 更高的不透明度
                .setStartDelay(200 * i) // 增大时间间隔，让"画爱心"效果更明显
                .setDuration(200)
                .withEndAction(() -> {
                    // 爱心路径的爪印持续时间更长
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        pawView.animate()
                            .alpha(0f)
                            .setDuration(800)
                            .withEndAction(() -> containerView.removeView(pawView))
                            .start();
                    }, 2500); // 所有爪印都停留较长时间
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

    // 生成弧形路径坐标
    private void generateArcPath(float[] xPositions, float[] yPositions, 
                              int width, int height, boolean fromLeft, 
                              boolean fromTop, int count) {
        // 弧的中心
        float centerX = width / 2;
        float centerY = fromTop ? -height * 0.3f : height * 1.3f; // 减小偏移
        
        // 弧的半径
        float radiusX = width * 0.3f; // 减小半径，使爪印更紧凑
        float radiusY = height * 0.4f; // 减小半径，使爪印更紧凑
        
        // 起始角度和角度范围
        float startAngle, sweepAngle;
        
        if (fromLeft) {
            startAngle = fromTop ? 180 : 0;
            sweepAngle = 90; // 增大角度范围，使弧更完整
        } else {
            startAngle = fromTop ? 0 : 180;
            sweepAngle = -90; // 增大角度范围，使弧更完整
        }
        
        // 生成坐标 - 确保间隔均匀
        for (int i = 0; i < count; i++) {
            float angle = (float) Math.toRadians(startAngle + sweepAngle * i / (count - 1));
            xPositions[i] = centerX + radiusX * (float) Math.cos(angle);
            yPositions[i] = centerY + radiusY * (float) Math.sin(angle);
        }
    }

    // 进一步优化圆形路径
    private void generateCircularPath(float[] xPositions, float[] yPositions, 
                                   int width, int height, boolean clockwise, int count) {
        // 更多样化的圆形位置
        float centerX, centerY;
        
        // 随机选择圆形模式 - 0=边缘小圆, 1=中央大圆, 2=椭圆形
        int circleMode = random.nextInt(3);
        
        // 根据模式设置圆的参数
        float radiusX, radiusY;
        float sweepAngle;
        
        switch (circleMode) {
            case 0: // 边缘小圆 - 更小、更紧凑的圆形
                // 在左侧或右侧
                if (random.nextBoolean()) {
                    centerX = width * 0.15f; // 左侧
                } else {
                    centerX = width * 0.85f; // 右侧
                }
                centerY = height * (0.3f + random.nextFloat() * 0.4f);
                
                // 小半径
                radiusX = width * 0.08f;
                radiusY = width * 0.08f; // 保持圆形
                
                // 完整圆形
                sweepAngle = 360;
                break;
                
            case 1: // 中央大圆 - 在屏幕中央的较大圆形
                centerX = width * 0.5f;
                centerY = height * 0.45f;
                
                // 中等半径
                radiusX = width * 0.2f;
                radiusY = width * 0.2f * ((float)height/width); // 调整纵横比
                
                // 部分圆弧
                sweepAngle = 180 + random.nextFloat() * 90; // 180-270度的弧
                break;
                
            case 2: // 椭圆形 - 扁平椭圆
            default:
                // 随机水平位置
                centerX = width * (0.3f + random.nextFloat() * 0.4f);
                centerY = height * (0.25f + random.nextFloat() * 0.5f);
                
                // 扁平椭圆
                radiusX = width * 0.25f;
                radiusY = height * 0.1f;
                
                // 部分椭圆
                sweepAngle = 270; // 270度的弧
                break;
        }
        
        // 确定是顺时针还是逆时针
        boolean isClockwise = random.nextBoolean();
        
        // 调整爪印数量以适应圆形大小
        int effectiveCount = Math.min(count, (int)(sweepAngle / 45) + 3); // 避免过于密集
        
        // 起始角度 - 随机选择
        float startAngle = random.nextFloat() * 360;
        
        // 生成圆形路径 - 均匀分布
        for (int i = 0; i < effectiveCount; i++) {
            float angleStep = sweepAngle / (effectiveCount - 1);
            float currentAngle = startAngle;
            
            if (isClockwise) {
                currentAngle += i * angleStep;
            } else {
                currentAngle -= i * angleStep;
            }
            
            float angle = (float) Math.toRadians(currentAngle);
            xPositions[i] = centerX + radiusX * (float) Math.cos(angle);
            yPositions[i] = centerY + radiusY * (float) Math.sin(angle);
        }
        
        // 如果实际使用的爪印少于数组大小，将剩余位置设为可见区域之外
        for (int i = effectiveCount; i < count; i++) {
            xPositions[i] = -100;
            yPositions[i] = -100;
        }
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