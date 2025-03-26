package com.example.mentalhealthdiary;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.database.BreathingSession;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BreathingActivity extends AppCompatActivity {
    private ImageView breathingCircle;
    private TextView guidanceText;
    private Button startButton;
    private CountDownTimer breathingTimer;
    private CountDownTimer durationTimer;
    private boolean isBreathing = false;
    private AnimatorSet breathingAnimation;
    private TextView timerText;
    private int sessionSeconds = 0;
    private boolean isPreparingToStart = false;
    private CountDownTimer prepTimer;
    private MediaPlayer mediaPlayer;
    private TextView musicFeedbackText;

    // 音符动画相关变量
    private ViewGroup rootLayout;
    private Random random = new Random();
    private Handler noteHandler = new Handler();
    private Runnable noteRunnable;
    private boolean isShowingNotes = false;

    private static final String CHANNEL_ID = "breathing_reminder_channel";
    private static final String CHANNEL_NAME = "呼吸练习提醒";
    private static final String CHANNEL_DESC = "提醒您进行每日呼吸练习";

    // 更新呼吸模式枚举
    private enum BreathingMode {
        NORMAL(4, 4, "标准呼吸 4-4", "平衡身心"),      // 平静呼吸
        FOCUS(4, 6, "专注呼吸 4-6", "提升专注"),      // 专注呼吸
        ENERGIZING(6, 2, "提神呼吸 6-2", "提升能量"),  // 提神呼吸
        CALMING(4, 8, "安眠呼吸 4-8", "助于入睡");     // 安眠呼吸

        final int inhaleSeconds;
        final int exhaleSeconds;
        final String description;
        final String benefit;

        BreathingMode(int inhale, int exhale, String desc, String benefit) {
            this.inhaleSeconds = inhale;
            this.exhaleSeconds = exhale;
            this.description = desc;
            this.benefit = benefit;
        }
    }

    private BreathingMode currentMode = BreathingMode.NORMAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        // 设置返回按钮
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("正念呼吸");

        // 初始化视图
        breathingCircle = findViewById(R.id.breathingCircle);
        guidanceText = findViewById(R.id.guidanceText);
        startButton = findViewById(R.id.startButton);
        timerText = findViewById(R.id.timerText);
        musicFeedbackText = findViewById(R.id.musicFeedbackText);
        
        // 初始化音乐反馈文本
        if (musicFeedbackText != null) {
            musicFeedbackText.setVisibility(View.GONE);
        }

        // 初始化MediaPlayer
        initializeMediaPlayer();
        
        setupBreathingAnimation();
        setupUI();

        startButton.setOnClickListener(v -> {
            if (!isBreathing) {
                startBreathing();
            } else {
                stopBreathingExercise();
            }
        });

        // 在 onCreate 中更新 Spinner 设置
        Spinner modeSpinner = findViewById(R.id.breathingModeSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item,  // 使用系统默认布局
            Arrays.stream(BreathingMode.values())
                  .map(mode -> mode.description)
                  .toArray(String[]::new));

        // 设置下拉项的布局和样式
        adapter.setDropDownViewResource(R.layout.item_breathing_mode);
        modeSpinner.setAdapter(adapter);

        // 更新模式选择监听器
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMode = BreathingMode.values()[position];
                updateBreathingMode(position);
                onModeSelected(currentMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 设置下拉框动画
        modeSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || 
                      event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start();
            }
            return false;
        });

        // 找到副标题文本
        TextView subtitleText = findViewById(R.id.subtitleText);
        
        // 添加文字阴影效果
        subtitleText.setShadowLayer(3, 1, 1, Color.parseColor("#50000000"));
        
        // 添加淡入动画
        subtitleText.setAlpha(0f);
        subtitleText.animate()
            .alpha(1f)
            .setDuration(1500)
            .start();

        // 获取根布局用于添加音符
        rootLayout = findViewById(R.id.breathing_root_layout);
    }

    private void setupBreathingAnimation() {
        // 缩放动画
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(breathingCircle, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", 1f, 1.5f);
        
        // 透明度动画
        ObjectAnimator alpha = ObjectAnimator.ofFloat(breathingCircle, "alpha", 0.7f, 1f);
        
        // 设置每个动画的重复
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        
        breathingAnimation = new AnimatorSet();
        breathingAnimation.playTogether(scaleX, scaleY, alpha);
        breathingAnimation.setDuration(8000);
        breathingAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    private void setupUI() {
        // 设置引导文本颜色为深青色
        guidanceText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        // 设置计时器文本样式
        timerText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        timerText.setAlpha(0.8f);
        
        // 设置开始按钮样式
        startButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700)));
        startButton.setTextColor(Color.WHITE);
    }

    private void startBreathing() {
        // 如果正在准备或正在呼吸，直接返回
        if (isPreparingToStart || isBreathing) {
            return;
        }

        isPreparingToStart = true;
        guidanceText.setText("准备开始...");
        
        // 取消可能存在的计时器
        if (prepTimer != null) {
            prepTimer.cancel();
        }

        prepTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                guidanceText.setText("准备开始..." + secondsLeft);
            }

            @Override
            public void onFinish() {
                isPreparingToStart = false;
                if (!isBreathing) { // 确保没有在呼吸状态才开始
                    startBreathingExercise();
                }
            }
        }.start();
    }

    private void startBreathingExercise() {
        if (isBreathing) {
            return;
        }

        isBreathing = true;
        startButton.setText("停止");
        sessionSeconds = 0; // 确保从0开始计时
        
        // 开始动画
        breathingAnimation.start();
        
        // 更新引导文本
        updateGuidanceText(true);
        
        // 启动呼吸引导计时器
        startGuidanceTimer();
        
        // 播放音乐并显示反馈
        startBackgroundMusic();
        
        // 添加练习时长计时
        if (durationTimer != null) {
            durationTimer.cancel();
        }
        
        durationTimer = new CountDownTimer(3600000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isBreathing) { // 只有在呼吸状态才增加时间
                    sessionSeconds++;
                    int minutes = sessionSeconds / 60;
                    int seconds = sessionSeconds % 60;
                    String timeText = String.format("练习时长: %02d:%02d", minutes, seconds);
                    timerText.setText(timeText);
                }
            }

            @Override
            public void onFinish() {
                stopBreathingExercise();
            }
        }.start();
    }

    private void stopBreathingExercise() {
        // 如果正在准备阶段，取消准备
        if (isPreparingToStart && prepTimer != null) {
            prepTimer.cancel();
            isPreparingToStart = false;
        }

        if (!isBreathing) {
            return;
        }

        isBreathing = false;
        startButton.setText("开始");
        
        // 停止动画
        breathingAnimation.cancel();
        breathingCircle.setScaleX(1f);
        breathingCircle.setScaleY(1f);
        breathingCircle.setAlpha(0.7f);
        
        // 停止音乐并隐藏反馈
        stopBackgroundMusic();
        
        // 停止所有计时器
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
        if (durationTimer != null) {
            durationTimer.cancel();
        }
        
        // 保存练习记录
        if (sessionSeconds >= 5) {
            saveBreathingSession();
        }
        
        // 重置引导文本和计时器
        guidanceText.setText("跟随圆圈呼吸\n吸气4秒，呼气4秒");
        sessionSeconds = 0;
        timerText.setText("");
    }

    private void saveBreathingSession() {
        final int finalDuration = sessionSeconds; // 捕获当前值
        Log.d("BreathingActivity", "准备保存会话，时长: " + finalDuration + "秒");
        
        new Thread(() -> {
            try {
                BreathingSession session = new BreathingSession();
                session.timestamp = System.currentTimeMillis();
                session.duration = finalDuration;
                
                // 保存前验证
                Log.d("BreathingActivity", "即将保存的会话: " + session.toString());
                
                AppDatabase.getInstance(this).breathingSessionDao().insert(session);
                
                // 保存后立即验证
                List<BreathingSession> sessions = AppDatabase.getInstance(this)
                    .breathingSessionDao()
                    .getAllSessions();
                
                Log.d("BreathingActivity", "保存后的所有会话:");
                for (BreathingSession saved : sessions) {
                    Log.d("BreathingActivity", "已保存会话: " + saved.toString());
                }
                
                // 验证总时长
                int totalDuration = AppDatabase.getInstance(this)
                    .breathingSessionDao()
                    .getTotalDuration();
                Log.d("BreathingActivity", "数据库中的总时长: " + totalDuration + "秒");
                
            } catch (Exception e) {
                Log.e("BreathingActivity", "保存会话时出错", e);
            }
        }).start();
    }

    private void startGuidanceTimer() {
        // 直接启动呼吸计时器，不再需要额外的准备时间
        breathingTimer = new CountDownTimer(8000, 4000) {
            boolean inhale = true;
            
            @Override
            public void onTick(long millisUntilFinished) {
                updateGuidanceText(inhale);
                inhale = !inhale;
            }

            @Override
            public void onFinish() {
                start();
            }
        }.start();
    }

    private void updateGuidanceText(boolean inhale) {
        // 取消之前的动画
        guidanceText.animate().cancel();
        
        // 设置初始透明度
        guidanceText.setAlpha(0f);
        
        if (inhale) {
            guidanceText.setTextColor(getResources().getColor(R.color.teal_700));
            guidanceText.setText("吸气...");
        } else {
            guidanceText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            guidanceText.setText("呼气...");
        }
        
        // 统一处理动画
        guidanceText.animate()
            .alpha(1f)
            .setDuration(800)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, BreathingHistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
        if (durationTimer != null) {
            durationTimer.cancel();
        }
        if (breathingAnimation != null) {
            breathingAnimation.cancel();
        }
        // 释放MediaPlayer资源
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e("BreathingActivity", "释放MediaPlayer失败", e);
            }
        }
        // 如果正在进行练习且超过30秒，保存记录
        if (isBreathing && sessionSeconds >= 30) {
            saveBreathingSession();
        }
        
        // 清理音符动画
        if (noteHandler != null && noteRunnable != null) {
            noteHandler.removeCallbacks(noteRunnable);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_breathing, menu);
        return true;
    }

    private void scheduleReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // 设置每天固定时间提醒
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9); // 早上9点
        calendar.set(Calendar.MINUTE, 0);

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        );
    }

    private void updateBreathingAnimation() {
        if (breathingAnimation != null) {
            breathingAnimation.cancel();

        }
        
        int totalDuration = currentMode.inhaleSeconds + currentMode.exhaleSeconds;
        breathingAnimation.setDuration(totalDuration * 1000L);
        
        if (!isBreathing) {
            breathingCircle.setScaleX(1f);
            breathingCircle.setScaleY(1f);
            breathingCircle.setAlpha(0.7f);
        } else {
            breathingAnimation.start();
        }
        
        guidanceText.setText(String.format("吸气%d秒，呼气%d秒", 
            currentMode.inhaleSeconds, 
            currentMode.exhaleSeconds));
    }

    // 修改onModeSelected方法，使其在呼吸练习中也能切换模式
    private void onModeSelected(BreathingMode mode) {
        // 保存之前的呼吸状态
        boolean wasBreathing = isBreathing;
        
        // 如果正在呼吸，先暂停当前的呼吸练习
        if (isBreathing) {
            // 暂停但不完全停止
            if (breathingAnimation != null) {
                breathingAnimation.cancel();
            }
            if (breathingTimer != null) {
                breathingTimer.cancel();
            }
        }
        
        // 更新当前模式
        currentMode = mode;
        updateBreathingAnimation();
        
        // 如果之前在呼吸，使用新模式重新开始呼吸
        if (wasBreathing) {
            // 更新音乐
            updateBackgroundMusic();
            
            // 重新开始动画和计时器
            breathingAnimation.start();
            startGuidanceTimer();
        }
        
        // 显示模式效果提示
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, 
            mode.description + "\n" + mode.benefit, 
            Snackbar.LENGTH_LONG)
            .setAction("了解更多", v -> showModeInfoDialog(mode))
            .show();
    }

    // 显示模式详细信息
    private void showModeInfoDialog(BreathingMode mode) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_breathing_mode_info, null);
        
        TextView titleText = dialogView.findViewById(R.id.modeTitleText);
        TextView rhythmText = dialogView.findViewById(R.id.modeRhythmText);
        TextView benefitText = dialogView.findViewById(R.id.modeBenefitText);
        TextView guideText = dialogView.findViewById(R.id.modeGuideText);
        TextView benefitTitleText = (TextView) dialogView.findViewById(R.id.benefitTitleText);
        
        // 首先设置颜色
        int textColor;
        switch (mode) {
            case NORMAL: // 平静呼吸
                textColor = getResources().getColor(R.color.calm_breathing);  // 蓝色
                break;
            case FOCUS: // 专注呼吸
                textColor = getResources().getColor(R.color.focus_breathing);  // 紫色
                break;
            case ENERGIZING: // 提神呼吸
                textColor = getResources().getColor(R.color.deep_breathing);  // 橙色
                break;
            case CALMING: // 安眠呼吸
                textColor = getResources().getColor(R.color.relax_breathing);  // 绿色
                break;
            default:
                textColor = getResources().getColor(R.color.calm_breathing);
                break;
        }
        
        // 然后设置图标
        int iconRes;
        switch (mode) {
            case NORMAL:
                iconRes = R.drawable.ic_benefits;
                break;
            case FOCUS:
                iconRes = R.drawable.ic_breathing_focus;
                break;
            case ENERGIZING:
                iconRes = R.drawable.ic_breathing_energy;
                break;
            case CALMING:
                iconRes = R.drawable.ic_breathing_sleep;
                break;
            default:
                iconRes = R.drawable.ic_benefits;
                break;
        }
        
        // 设置图标并应用颜色
        benefitTitleText.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        Drawable[] drawables = benefitTitleText.getCompoundDrawables();
        if (drawables[0] != null) {
            drawables[0].setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
        }
        
        // 设置标题、节奏文本和主要功效标题的颜色
        titleText.setTextColor(textColor);
        rhythmText.setTextColor(textColor);
        benefitTitleText.setTextColor(textColor);
        
        // 设置主要功效文本的颜色（使用较浅的主题色）
        int benefitTextColor = adjustAlpha(textColor, 0.75f);
        benefitText.setTextColor(benefitTextColor);
        
        // 设置练习指导标题和内容的颜色（使用柔和的灰绿色，象征平静和自然）
        int guideColor = getResources().getColor(R.color.mindful_guide);
        TextView guideTitleText = dialogView.findViewById(R.id.guideTitleText);
        guideTitleText.setTextColor(guideColor);
        guideText.setTextColor(adjustAlpha(guideColor, 0.8f));
        
        // 设置练习指导图标的颜色
        Drawable guideIcon = getResources().getDrawable(R.drawable.ic_guide).mutate();
        guideIcon.setColorFilter(guideColor, PorterDuff.Mode.SRC_IN);
        guideTitleText.setCompoundDrawablesWithIntrinsicBounds(guideIcon, null, null, null);
        guideTitleText.setCompoundDrawablePadding(8);  // 保持原有的padding
        
        titleText.setText(mode.description);
        rhythmText.setText(String.format("呼吸节奏：吸气 %d 秒，呼气 %d 秒", 
            mode.inhaleSeconds, mode.exhaleSeconds));
        
        // 设置不同模式的具体效果说明
        String benefitDetail;
        String guideDetail;
        switch (mode) {
            case NORMAL:
                benefitDetail = "• 帮助平衡身心\n• 缓解日常压力\n• 提升专注力\n• 改善睡眠质量\n\n🎵 背景音乐: Call of silence";
                guideDetail = "找到舒适的坐姿，保持背部挺直。跟随圆圈的节奏，" +
                            "通过鼻子缓慢吸气，感受气息充满胸腹，然后轻柔地呼出。";
                break;
            case CALMING:
                benefitDetail = "• 帮助入睡\n• 减轻失眠\n• 平静心绪\n• 改善睡眠质量\n\n🎵 背景音乐: 皎洁的笑颜";
                guideDetail = "可以采用躺姿，放松全身肌肉。" +
                            "将注意力集中在呼吸上，让思绪随着呼吸渐渐平静。";
                break;
            case ENERGIZING:
                benefitDetail = "• 提升能量水平\n• 增强清醒度\n• 改善注意力\n• 提高工作效率\n\n🎵 背景音乐: 钢琴曲";
                guideDetail = "较长的吸气和短促的呼气能激活身体系统。" +
                            "保持正确的呼吸节奏，感受能量在体内流动。";
                break;
            case FOCUS:
                benefitDetail = "• 提升专注力\n• 增强思维清晰度\n• 改善学习效率\n• 减少分心走神\n\n🎵 背景音乐: Nuit Silencieuse";
                guideDetail = "找到舒适的坐姿，保持背部挺直。跟随圆圈的节奏，" +
                            "通过鼻子缓慢吸气，感受气息充满胸腹，然后轻柔地呼出。";
                break;
            default:
                benefitDetail = mode.benefit + "\n\n🎵 背景音乐: " + getMusicNameForMode(mode);
                guideDetail = "保持自然的呼吸节奏，关注当下的呼吸感受。";
        }
        
        benefitText.setText(benefitDetail);
        guideText.setText(guideDetail);
        
        // 隐藏单独的音乐信息文本视图，因为我们已经将其整合到功效文本中
        TextView musicInfoText = dialogView.findViewById(R.id.musicInfoText);
        if (musicInfoText != null) {
            musicInfoText.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("开始练习", null)  // 先设置为null
            .setNegativeButton("关闭", null)
            .create();

        // 设置对话框背景和动画
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        dialog.show();

        // 获取按钮并设置样式
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        
        // 设置按钮样式
        positiveButton.setTextColor(textColor);
        negativeButton.setTextColor(Color.GRAY);
        
        // 设置点击事件
        positiveButton.setOnClickListener(v -> {
            if (!isBreathing) {
                startBreathing();
                dialog.dismiss();
            }
        });
    }

    private void initializeBreathingPatterns() {
        // 使用已有的 breathingModeSpinner 而不是 patternSpinner
        Spinner modeSpinner = findViewById(R.id.breathingModeSpinner);
        
        Map<String, BreathingPattern> patterns = new HashMap<>();
        patterns.put("478呼吸法", new BreathingPattern(4, 7, 8, "缓解焦虑"));
        patterns.put("方块呼吸", new BreathingPattern(4, 4, 4, "平静心情"));
        patterns.put("4-4-4-4呼吸", new BreathingPattern(4, 4, 4, "提升专注力"));
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            R.layout.item_breathing_mode,  // 使用自定义布局
            new ArrayList<>(patterns.keySet())
        );
        adapter.setDropDownViewResource(R.layout.item_breathing_mode_dropdown);
        modeSpinner.setAdapter(adapter);
        
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPattern = (String) parent.getItemAtPosition(position);
                BreathingPattern pattern = patterns.get(selectedPattern);
                if (pattern != null) {
                    updateBreathingPattern(pattern);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateBreathingPattern(BreathingPattern pattern) {
        // 更新呼吸动画时间
        if (breathingAnimation != null) {
            breathingAnimation.setDuration((pattern.inhale + pattern.exhale) * 1000L);
        }
        
        // 更新提示文本
        guidanceText.setText(String.format("吸气%d秒，屏息%d秒，呼气%d秒\n%s", 
            pattern.inhale, pattern.hold, pattern.exhale, pattern.benefit));
    }

    // 添加呼吸模式类
    private static class BreathingPattern {
        final int inhale;
        final int hold;
        final int exhale;
        final String benefit;
        
        BreathingPattern(int inhale, int hold, int exhale, String benefit) {
            this.inhale = inhale;
            this.hold = hold;
            this.exhale = exhale;
            this.benefit = benefit;
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(Context context) {
        createNotificationChannel();

        Intent intent = new Intent(context, BreathingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("呼吸练习提醒")
            .setContentText("现在是放松身心的好时候，让我们进行一次呼吸练习吧")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());
    }

    private void updateBreathingMode(int position) {
        // 根据不同的呼吸模式设置不同的颜色
        int textColor;
        switch (position) {
            case 0: // 平静呼吸
                textColor = getResources().getColor(R.color.calm_breathing);  // 蓝色
                currentMode = BreathingMode.NORMAL;
                break;
            case 1: // 专注呼吸
                textColor = getResources().getColor(R.color.focus_breathing);  // 紫色
                currentMode = BreathingMode.FOCUS;
                break;
            case 2: // 提神呼吸
                textColor = getResources().getColor(R.color.deep_breathing);  // 橙色
                currentMode = BreathingMode.ENERGIZING;
                break;
            case 3: // 安眠呼吸
                textColor = getResources().getColor(R.color.relax_breathing);  // 绿色
                currentMode = BreathingMode.CALMING;
                break;
            default:
                textColor = getResources().getColor(R.color.calm_breathing);
                currentMode = BreathingMode.NORMAL;
                break;
        }
        
        if (guidanceText != null) {
            guidanceText.setTextColor(textColor);
        }

        // 根据不同模式设置不同的引导语
        String subtitle;
        switch (position) {
            case 0: // 平静呼吸
                subtitle = "让心灵沉淀，找回内在平静";
                break;
            case 1: // 专注呼吸
                subtitle = "收敛思绪，提升专注力";
                break;
            case 2: // 提神呼吸
                subtitle = "唤醒身心，激发内在能量";
                break;
            case 3: // 安眠呼吸
                subtitle = "放松身心，安抚入眠";
                break;
            default:
                subtitle = "让心灵沉淀，找回内在平静";
                break;
        }
        
        // 找到副标题文本
        TextView subtitleText = findViewById(R.id.subtitleText);
        
        // 设置文字并添加动画效果
        subtitleText.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction(() -> {
                subtitleText.setText(subtitle);
                // 设置半透明的对应颜色
                int subtitleColor = adjustAlpha(textColor, 0.9f);
                subtitleText.setTextColor(subtitleColor);
                // 文字淡入动画
                subtitleText.animate()
                    .alpha(0.8f)
                    .setDuration(300)
                    .start();
            })
            .start();
        
        // 如果正在练习中，更新音乐
        if (isBreathing && mediaPlayer != null) {
            updateBackgroundMusic();
        }
    }

    private void updateBackgroundMusic() {
        try {
            // 确保MediaPlayer已初始化
            if (mediaPlayer == null) {
                initializeMediaPlayer();
            } else {
                // 重新创建MediaPlayer以确保从头开始播放
                mediaPlayer.release();
                initializeMediaPlayer();
            }
            
            // 开始播放音乐
            if (mediaPlayer != null) {
                Log.d("BreathingActivity", "开始播放音乐: " + getMusicFeedbackForMode(currentMode));
                mediaPlayer.seekTo(0); // 确保从头开始播放
                mediaPlayer.start();
                
                // 显示音乐反馈
                if (musicFeedbackText != null) {
                    // 根据当前模式设置不同的反馈文本
                    String musicFeedback = getMusicFeedbackForMode(currentMode);
                    musicFeedbackText.setText(musicFeedback);
                    
                    // 设置音乐反馈框的颜色与当前呼吸模式匹配
                    int textColor;
                    switch (currentMode) {
                        case NORMAL:
                            textColor = getResources().getColor(R.color.calm_breathing);
                            break;
                        case FOCUS:
                            textColor = getResources().getColor(R.color.focus_breathing);
                            break;
                        case ENERGIZING:
                            textColor = getResources().getColor(R.color.deep_breathing);
                            break;
                        case CALMING:
                            textColor = getResources().getColor(R.color.relax_breathing);
                            break;
                        default:
                            textColor = getResources().getColor(R.color.calm_breathing);
                    }
                    
                    // 为音乐图标设置颜色
                    Drawable[] drawables = musicFeedbackText.getCompoundDrawables();
                    if (drawables[0] != null) {
                        drawables[0].setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                    }
                    
                    // 淡入动画显示反馈
                    musicFeedbackText.setAlpha(0f);
                    musicFeedbackText.setVisibility(View.VISIBLE);
                    musicFeedbackText.animate()
                        .alpha(0.8f)
                        .setDuration(1000)
                        .start();
                    
                    // 添加轻微的脉动动画
                    startMusicFeedbackPulsation();
                    
                    // 开始音符动画
                    startMusicNoteAnimation();
                }
            } else {
                Log.d("BreathingActivity", "MediaPlayer为null");
            }
        } catch (Exception e) {
            Log.e("BreathingActivity", "播放音乐失败", e);
        }
    }

    // 添加轻微的脉动动画，与呼吸节奏相协调
    private void startMusicFeedbackPulsation() {
        if (musicFeedbackText == null) return;
        
        // 取消可能存在的动画
        musicFeedbackText.clearAnimation();
        
        // 创建轻微的缩放动画
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 1.03f);
        pulseAnimator.setDuration(currentMode.inhaleSeconds * 1000);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        pulseAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            musicFeedbackText.setScaleX(value);
            musicFeedbackText.setScaleY(value);
        });
        
        pulseAnimator.start();
        
        Log.d("BreathingActivity", "音乐反馈框脉动动画已启动");
    }

    // 停止音乐反馈动画
    private void stopMusicFeedbackAnimation() {
        if (musicFeedbackText != null) {
            musicFeedbackText.clearAnimation();
            musicFeedbackText.setScaleX(1f);
            musicFeedbackText.setScaleY(1f);
        }
    }

    // 辅助方法：调整颜色的透明度
    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void initializeMediaPlayer() {
        try {
            // 根据当前模式选择合适的音乐
            int musicResId = getMusicResourceForMode(currentMode);
            mediaPlayer = MediaPlayer.create(this, musicResId);
            mediaPlayer.setLooping(true); // 循环播放
            mediaPlayer.setVolume(1.0f, 1.0f); // 设置最大音量
        } catch (Exception e) {
            Log.e("BreathingActivity", "初始化MediaPlayer失败", e);
        }
    }

    private int getMusicResourceForMode(BreathingMode mode) {

        switch (mode) {
            case NORMAL:
                return R.raw.calm_breathing;
            case FOCUS:
                return R.raw.focus_breathing;
            case ENERGIZING:
                return R.raw.energizing_breathing;
            case CALMING:
                return R.raw.calming_breathing;
            default:
                return R.raw.calm_breathing;
        }

    }

    private void startBackgroundMusic() {
        try {
            // 确保MediaPlayer已初始化
            if (mediaPlayer == null) {
                initializeMediaPlayer();
            } else {
                // 重新创建MediaPlayer以确保从头开始播放
                mediaPlayer.release();
                initializeMediaPlayer();
            }
            
            // 开始播放音乐
            if (mediaPlayer != null) {
                Log.d("BreathingActivity", "开始播放音乐: " + getMusicFeedbackForMode(currentMode));
                mediaPlayer.seekTo(0); // 确保从头开始播放
                mediaPlayer.start();
                
                // 显示音乐反馈
                if (musicFeedbackText != null) {
                    // 根据当前模式设置不同的反馈文本
                    String musicFeedback = getMusicFeedbackForMode(currentMode);
                    musicFeedbackText.setText(musicFeedback);
                    
                    // 设置音乐反馈框的颜色与当前呼吸模式匹配
                    int textColor;
                    switch (currentMode) {
                        case NORMAL:
                            textColor = getResources().getColor(R.color.calm_breathing);
                            break;
                        case FOCUS:
                            textColor = getResources().getColor(R.color.focus_breathing);
                            break;
                        case ENERGIZING:
                            textColor = getResources().getColor(R.color.deep_breathing);
                            break;
                        case CALMING:
                            textColor = getResources().getColor(R.color.relax_breathing);
                            break;
                        default:
                            textColor = getResources().getColor(R.color.calm_breathing);
                    }
                    
                    // 为音乐图标设置颜色
                    Drawable[] drawables = musicFeedbackText.getCompoundDrawables();
                    if (drawables[0] != null) {
                        drawables[0].setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                    }
                    
                    // 淡入动画显示反馈
                    musicFeedbackText.setAlpha(0f);
                    musicFeedbackText.setVisibility(View.VISIBLE);
                    musicFeedbackText.animate()
                        .alpha(0.8f)
                        .setDuration(1000)
                        .start();
                    
                    // 添加轻微的脉动动画
                    startMusicFeedbackPulsation();
                    
                    // 开始音符动画
                    startMusicNoteAnimation();
                }
            } else {
                Log.d("BreathingActivity", "MediaPlayer为null");
            }
        } catch (Exception e) {
            Log.e("BreathingActivity", "播放音乐失败", e);
        }
    }

    private void stopBackgroundMusic() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                
                // 停止音符动画
                if (noteHandler != null && noteRunnable != null) {
                    noteHandler.removeCallbacks(noteRunnable);
                    isShowingNotes = false;
                }
                
                // 停止脉动动画
                stopMusicFeedbackAnimation();
                
                // 隐藏音乐反馈
                if (musicFeedbackText != null) {
                    musicFeedbackText.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(() -> musicFeedbackText.setVisibility(View.GONE))
                        .start();
                }
            }
        } catch (Exception e) {
            Log.e("BreathingActivity", "停止音乐失败", e);
        }
    }

    private String getMusicFeedbackForMode(BreathingMode mode) {
        // 根据不同的呼吸模式返回不同的音乐反馈文本
        switch (mode) {
            case NORMAL:
                return "正在播放：Call of silence";
            case FOCUS:
                return "正在播放：Nuit Silencieuse";
            case ENERGIZING:
                return "正在播放：钢琴曲";
            case CALMING:
                return "正在播放：皎洁的笑颜";
            default:
                return "正在播放：冥想音乐";
        }
    }

    // 添加一个新方法来获取音乐名称
    private String getMusicNameForMode(BreathingMode mode) {
        switch (mode) {
            case NORMAL:
                return "Call of silence";
            case FOCUS:
                return "Nuit Silencieuse";
            case ENERGIZING:
                return "钢琴曲";
            case CALMING:
                return "皎洁的笑颜";
            default:
                return "冥想音乐";
        }
    }

    // 开始音符动画
    private void startMusicNoteAnimation() {
        if (rootLayout == null || isShowingNotes) return;
        
        isShowingNotes = true;
        Log.d("BreathingActivity", "开始音符动画");
        
        // 创建定时任务，随机生成音符
        noteRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBreathing && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // 一次添加多个音符，形成更丰富的效果
                    int noteCount = random.nextInt(3) + 1; // 随机生成1-3个音符
                    for (int i = 0; i < noteCount; i++) {
                        // 延迟一点时间添加每个音符，使其看起来更自然
                        final int delay = i * 150;
                        noteHandler.postDelayed(() -> addMusicNote(), delay);
                    }
                    
                    // 根据当前模式设置音符生成频率
                    int delay;
                    switch (currentMode) {
                        case ENERGIZING:
                            delay = 1200 + random.nextInt(800); // 更频繁
                            break;
                        case CALMING:
                            delay = 2500 + random.nextInt(1000); // 较少
                            break;
                        default:
                            delay = 1800 + random.nextInt(1000); // 中等
                    }
                    
                    // 安排下一组音符
                    noteHandler.postDelayed(this, delay);
                    Log.d("BreathingActivity", "安排下一组音符，延迟: " + delay + "ms");
                } else {
                    isShowingNotes = false;
                    Log.d("BreathingActivity", "停止音符动画");
                }
            }
        };
        
        // 立即添加第一组音符，然后开始定时生成
        int initialNotes = random.nextInt(2) + 2; // 2-3个初始音符
        for (int i = 0; i < initialNotes; i++) {
            noteHandler.postDelayed(() -> addMusicNote(), i * 200);
        }
        
        // 开始生成音符
        noteHandler.postDelayed(noteRunnable, 1000);
    }

    // 修改addMusicNote方法，创建大小不同的音符
    private void addMusicNote() {
        runOnUiThread(() -> {
            try {
                if (musicFeedbackText == null || !musicFeedbackText.isShown()) return;
                
                // 创建新的ImageView作为音符
                ImageView noteView = new ImageView(this);
                
                // 随机选择音符图标 - 可以创建几种不同的音符图标
                int noteType = random.nextInt(2);
                int noteResId = noteType == 0 ? 
                        R.drawable.ic_music_note_small : 
                        R.drawable.ic_music_note_small2;
                noteView.setImageResource(noteResId);
                
                // 设置音符颜色（与当前呼吸模式匹配）
                int noteColor;
                switch (currentMode) {
                    case NORMAL:
                        noteColor = getResources().getColor(R.color.calm_breathing);
                        break;
                    case FOCUS:
                        noteColor = getResources().getColor(R.color.focus_breathing);
                        break;
                    case ENERGIZING:
                        noteColor = getResources().getColor(R.color.deep_breathing);
                        break;
                    case CALMING:
                        noteColor = getResources().getColor(R.color.relax_breathing);
                        break;
                    default:
                        noteColor = getResources().getColor(R.color.calm_breathing);
                }
                
                // 随机调整颜色亮度，使音符颜色略有变化
                float brightness = 0.8f + random.nextFloat() * 0.4f; // 0.8-1.2
                noteColor = adjustBrightness(noteColor, brightness);
                
                noteView.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN);
                
                // 随机大小 (50%-150% 的原始大小)
                float scale = 0.5f + random.nextFloat(); // 0.5-1.5
                noteView.setScaleX(scale);
                noteView.setScaleY(scale);
                
                // 设置布局参数
                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                );
                
                // 获取音乐反馈文本的位置
                int[] location = new int[2];
                musicFeedbackText.getLocationInWindow(location);
                
                // 设置音符的初始位置 - 从音乐反馈框内部或周围随机位置出现
                int startX, startY;
                boolean fromInside = random.nextBoolean(); // 50%几率从内部生成
                
                if (fromInside) {
                    // 从音乐反馈框内部随机位置生成
                    startX = location[0] + random.nextInt(musicFeedbackText.getWidth() - 20);
                    startY = location[1] + random.nextInt(musicFeedbackText.getHeight() - 10);
                } else {
                    // 从音乐反馈框周围生成
                    int side = random.nextInt(4); // 0:上, 1:右, 2:下, 3:左
                    switch (side) {
                        case 0: // 上方
                            startX = location[0] + random.nextInt(musicFeedbackText.getWidth());
                            startY = location[1] - 10 - random.nextInt(20);
                            break;
                        case 1: // 右侧
                            startX = location[0] + musicFeedbackText.getWidth() + random.nextInt(20);
                            startY = location[1] + random.nextInt(musicFeedbackText.getHeight());
                            break;
                        case 2: // 下方
                            startX = location[0] + random.nextInt(musicFeedbackText.getWidth());
                            startY = location[1] + musicFeedbackText.getHeight() + random.nextInt(20);
                            break;
                        default: // 左侧
                            startX = location[0] - 10 - random.nextInt(20);
                            startY = location[1] + random.nextInt(musicFeedbackText.getHeight());
                            break;
                    }
                }
                
                params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                params.leftMargin = startX;
                params.topMargin = startY;
                
                noteView.setLayoutParams(params);
                noteView.setAlpha(0f);
                
                // 添加到布局
                rootLayout.addView(noteView);
                
                // 创建随机的上升路径和淡出动画
                float xOffset = random.nextInt(80) - 40; // -40到40
                float yOffset = -80 - random.nextInt(60); // -80到-140
                int duration = 1500 + random.nextInt(1500); // 1.5-3秒
                
                noteView.animate()
                        .alpha(0.7f)
                        .translationYBy(yOffset)
                        .translationXBy(xOffset)
                        .setDuration(duration)
                        .withEndAction(() -> {
                            // 淡出并移除
                            noteView.animate()
                                    .alpha(0f)
                                    .translationYBy(yOffset/2)
                                    .setDuration(duration/2)
                                    .withEndAction(() -> rootLayout.removeView(noteView))
                                    .start();
                        })
                        .start();
                    
                Log.d("BreathingActivity", "音符已添加到屏幕");
            } catch (Exception e) {
                Log.e("BreathingActivity", "添加音符失败", e);
            }
        });
    }

    // 调整颜色亮度的辅助方法
    private int adjustBrightness(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }
} 