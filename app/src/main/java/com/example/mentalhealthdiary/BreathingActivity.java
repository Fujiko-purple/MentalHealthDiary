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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
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
    private View overlayView;

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
            
            // 设置文本样式
            musicFeedbackText.setTextColor(getResources().getColor(R.color.calm_breathing));
            
            // 创建一个新的GradientDrawable来设置背景
            android.graphics.drawable.GradientDrawable musicBackground = new android.graphics.drawable.GradientDrawable();
            musicBackground.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            musicBackground.setCornerRadius(16 * getResources().getDisplayMetrics().density); // 16dp
            musicBackground.setColor(Color.argb(220, 245, 249, 252)); // 非常淡的蓝色，带透明度
            
            // 设置新的背景
            musicFeedbackText.setBackground(musicBackground);
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

        // 增强Spinner的可见性
        modeSpinner.setBackgroundResource(R.drawable.spinner_background_enhanced);

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

        // 在onCreate方法中
        guidanceText.setBackground(getResources().getDrawable(R.drawable.guidance_text_background_improved));

        // 改为使用系统默认字体
        guidanceText.setTypeface(Typeface.DEFAULT_BOLD);

        // 在onCreate方法中
        View rhythmIndicator = findViewById(R.id.rhythmIndicator);
        TextView rhythmIndicatorHint = findViewById(R.id.rhythmIndicatorHint);

        // 初始状态下设置为半透明
        rhythmIndicator.setAlpha(0.5f);
        rhythmIndicatorHint.setAlpha(0.7f);

        // 在onCreate方法中添加
        setupInitialRhythmIndicator();
        
        // 在onCreate方法中添加，初始化呼吸圆形的提示
        setupInitialBreathingCircleHint();
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
        
        // 使节奏指示器完全可见
        View rhythmIndicator = findViewById(R.id.rhythmIndicator);
        TextView rhythmIndicatorHint = findViewById(R.id.rhythmIndicatorHint);
        
        rhythmIndicator.animate().alpha(1.0f).setDuration(500).start();
        rhythmIndicatorHint.animate().alpha(0.0f).setDuration(500).start(); // 隐藏提示文本
    }

    private void startBreathingExercise() {
        isBreathing = true;
        startButton.setText("停止练习");
        
        // 重置引导文本和计时器
        guidanceText.setText("跟随圆圈呼吸\n吸气" + currentMode.inhaleSeconds + "秒，呼气" + currentMode.exhaleSeconds + "秒");
        guidanceText.setGravity(android.view.Gravity.CENTER);
        guidanceText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        
        // 在这里播放背景音乐
        String musicName = getMusicFeedbackForMode(currentMode);
        startBackgroundMusic();
        updateMusicFeedback(musicName);
        
        // 其他代码...
        
        // 开始动画
        breathingAnimation.start();
        
        // 启动呼吸引导计时器
        startGuidanceTimer();
        
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
                    
                    // 更新音乐反馈文本
                    updateMusicFeedback(musicFeedback);
                    
                    // 确保启动音符动画
                    if (!isShowingNotes) {
                        startMusicNoteAnimation();
                    }
                }
            } else {
                Log.e("BreathingActivity", "MediaPlayer为null");
            }
        } catch (Exception e) {
            Log.e("BreathingActivity", "播放音乐失败", e);
        }
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
        guidanceText.setText("跟随圆圈呼吸\n吸气" + currentMode.inhaleSeconds + "秒，呼气" + currentMode.exhaleSeconds);
        guidanceText.setGravity(android.view.Gravity.CENTER);
        guidanceText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sessionSeconds = 0;
        timerText.setText("");

        // 恢复节奏指示器的初始状态
        View rhythmIndicator = findViewById(R.id.rhythmIndicator);
        TextView rhythmIndicatorHint = findViewById(R.id.rhythmIndicatorHint);
        
        rhythmIndicator.animate().alpha(0.5f).setDuration(500).start();
        rhythmIndicatorHint.animate().alpha(0.7f).setDuration(500).start(); // 显示提示文本
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
        // 获取节奏指示器视图
        View rhythmDot1 = findViewById(R.id.rhythmDot1);
        View rhythmDot2 = findViewById(R.id.rhythmDot2);
        
        // 获取背景视图
        View breathingBackground = findViewById(R.id.breathing_root_layout);
        
        // 设置初始状态
        rhythmDot1.setAlpha(0.3f);
        rhythmDot2.setAlpha(0.3f);
        
        breathingTimer = new CountDownTimer(Long.MAX_VALUE, 
                (currentMode.inhaleSeconds + currentMode.exhaleSeconds) * 1000) {
            boolean inhale = true;
            
            @Override
            public void onTick(long millisUntilFinished) {
                // 更新指导文本
                updateGuidanceTextForBreathing(inhale);
                
                // 更新节奏指示器
                updateRhythmIndicator(inhale);
                
                // 更新背景微妙变化
                updateBreathingBackground(inhale);
                
                inhale = !inhale;
            }
            
            @Override
            public void onFinish() {
                start();
            }
        };
        
        breathingTimer.start();
    }

    private void updateGuidanceText() {
        // 根据当前模式设置不同的文字颜色
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
        
        // 设置文字颜色
        guidanceText.setTextColor(textColor);
        
        // 设置文字阴影，增加可读性
        guidanceText.setShadowLayer(3, 1, 1, Color.parseColor("#33000000"));
        
        // 设置背景透明度
        Drawable background = guidanceText.getBackground();
        if (background != null) {
            background.setAlpha(180); // 0-255，值越小越透明
        }
        
        // 添加文字动画效果
        if (isBreathing) {
            // 呼吸时的文字淡入淡出效果
            guidanceText.animate()
                .alpha(0.9f)
                .setDuration(300)
                .start();
        }
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
        // 显示模式信息
        Snackbar.make(findViewById(R.id.breathing_root_layout), 
                "已选择: " + mode.description, 
                Snackbar.LENGTH_SHORT)
            .setAction("了解更多", v -> showModeInfoDialog(mode))
            .show();

        // 根据模式设置背景
        View rootLayout = findViewById(R.id.breathing_root_layout);
        switch (mode) {
            case NORMAL:
                rootLayout.setBackground(getResources().getDrawable(R.drawable.breathing_background_calm));
                break;
            case FOCUS:
                rootLayout.setBackground(getResources().getDrawable(R.drawable.breathing_background_focus));
                break;
            case ENERGIZING:
                rootLayout.setBackground(getResources().getDrawable(R.drawable.breathing_background_energizing));
                break;
            case CALMING:
                rootLayout.setBackground(getResources().getDrawable(R.drawable.breathing_background_calming));
                break;
        }
        
        // 获取Spinner并更新其样式
        Spinner modeSpinner = findViewById(R.id.breathingModeSpinner);
        
        // 根据当前模式设置Spinner边框颜色
        int borderColor;
        int alpha = 180; // 透明度，使颜色更柔和
        switch (mode) {
            case NORMAL:
                borderColor = Color.argb(alpha, Color.red(getResources().getColor(R.color.calm_breathing)), 
                                       Color.green(getResources().getColor(R.color.calm_breathing)), 
                                       Color.blue(getResources().getColor(R.color.calm_breathing)));
                break;
            case FOCUS:
                borderColor = Color.argb(alpha, Color.red(getResources().getColor(R.color.focus_breathing)), 
                                       Color.green(getResources().getColor(R.color.focus_breathing)), 
                                       Color.blue(getResources().getColor(R.color.focus_breathing)));
                break;
            case ENERGIZING:
                borderColor = Color.argb(alpha, Color.red(getResources().getColor(R.color.deep_breathing)), 
                                       Color.green(getResources().getColor(R.color.deep_breathing)), 
                                       Color.blue(getResources().getColor(R.color.deep_breathing)));
                break;
            case CALMING:
                borderColor = Color.argb(alpha, Color.red(getResources().getColor(R.color.relax_breathing)), 
                                       Color.green(getResources().getColor(R.color.relax_breathing)), 
                                       Color.blue(getResources().getColor(R.color.relax_breathing)));
                break;
            default:
                borderColor = Color.argb(alpha, Color.red(getResources().getColor(R.color.calm_breathing)), 
                                       Color.green(getResources().getColor(R.color.calm_breathing)), 
                                       Color.blue(getResources().getColor(R.color.calm_breathing)));
        }
        
        // 创建一个新的GradientDrawable来设置边框颜色
        android.graphics.drawable.GradientDrawable spinnerBackground = new android.graphics.drawable.GradientDrawable();
        spinnerBackground.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        spinnerBackground.setCornerRadius(24 * getResources().getDisplayMetrics().density); // 24dp
        spinnerBackground.setColor(Color.WHITE);
        spinnerBackground.setStroke(2, borderColor);
        
        // 不使用setPadding方法，而是直接设置背景
        modeSpinner.setBackground(spinnerBackground);
        
        // 使用内边距属性设置内边距
        modeSpinner.setPadding(
            (int)(16 * getResources().getDisplayMetrics().density),
            (int)(8 * getResources().getDisplayMetrics().density),
            (int)(16 * getResources().getDisplayMetrics().density),
            (int)(8 * getResources().getDisplayMetrics().density)
        );
        
        // 如果正在进行呼吸练习，更新音乐
        if (isBreathing && !isPreparingToStart && mediaPlayer != null && mediaPlayer.isPlaying()) {
            String musicName = getMusicFeedbackForMode(mode);
            startBackgroundMusic(); // 重新开始播放音乐
            updateMusicFeedback(musicName);
        }
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
                    
                    // 添加音符图标到文本
                    updateMusicFeedback(musicFeedback);
                    
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

    private void playBackgroundMusic(String musicName) {
        try {
            if (mediaPlayer != null) {
                stopBackgroundMusic();
            }
            
            // 根据音乐名称选择资源ID
            int musicResId;
            switch (musicName) {
                case "Call of silence":
                    musicResId = R.raw.calm_breathing;
                    break;
                case "Nuit Silencieuse":
                    musicResId = R.raw.focus_breathing;
                    break;
                case "皎洁的笑颜":
                    musicResId = R.raw.calming_breathing;
                    break;
                case "钢琴曲":
                    musicResId = R.raw.energizing_breathing;
                    break;
                default:
                    musicResId = R.raw.energizing_breathing;
                    musicName = "冥想音乐";
            }
            
            mediaPlayer = MediaPlayer.create(this, musicResId);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            
            // 更新音乐反馈
            updateMusicFeedback(musicName);
            
            // 启动音符动画
            startMusicNoteAnimation();
            
        } catch (Exception e) {
            Log.e("BreathingActivity", "播放背景音乐失败", e);
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

    // 修改addMusicNote方法，减少抖动并降低上升速度
    private void addMusicNote() {
        if (rootLayout == null || musicFeedbackText == null || !musicFeedbackText.isShown()) return;
        
        try {
            // 创建音符ImageView
            ImageView noteView = new ImageView(this);
            
            // 随机选择音符图标
            int[] noteResources = {
                R.drawable.ic_music_note_small,
                R.drawable.ic_music_note_small2,
                R.drawable.ic_music_note_small3,
                R.drawable.ic_music_note_small4,
                R.drawable.ic_music_note_small5
            };
            
            int noteResource = noteResources[random.nextInt(noteResources.length)];
            noteView.setImageResource(noteResource);
            
            // 设置音符颜色
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
            
            // 设置音符颜色滤镜
            noteView.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN);
            
            // 设置音符大小 - 随机大小使效果更自然
            float scale = 0.6f + random.nextFloat() * 0.4f; // 0.6-1.0倍大小，更小的音符
            int noteSize = (int)(20 * getResources().getDisplayMetrics().density * scale);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(noteSize, noteSize);
            noteView.setLayoutParams(params);
            
            // 获取音乐反馈文本的位置和尺寸
            int[] location = new int[2];
            musicFeedbackText.getLocationInWindow(location);
            int textX = location[0];
            int textY = location[1];
            int textWidth = musicFeedbackText.getWidth();
            int textHeight = musicFeedbackText.getHeight();
            
            // 确定音符生成位置 - 从文本框周围生成
            int startX, startY;
            
            // 主要从文本框上方和两侧生成音符，像气泡一样
            int position = random.nextInt(10);
            if (position < 6) { // 60%几率从上方生成
                startX = textX + random.nextInt(textWidth);
                startY = textY - noteSize - random.nextInt(5);
            } else if (position < 8) { // 20%几率从左侧生成
                startX = textX - noteSize - random.nextInt(5);
                startY = textY + random.nextInt(textHeight);
            } else { // 20%几率从右侧生成
                startX = textX + textWidth + random.nextInt(5);
                startY = textY + random.nextInt(textHeight);
            }
            
            // 设置音符初始位置
            noteView.setX(startX);
            noteView.setY(startY);
            
            // 设置初始透明度为0.1f，轻微可见
            noteView.setAlpha(0.1f);
            
            // 添加到布局
            rootLayout.addView(noteView);
            
            // 创建气泡上升动画
            // 计算上升距离 - 屏幕高度的1/4到1/3之间，减少上升距离
            int screenHeight = rootLayout.getHeight();
            if (screenHeight <= 0) {
                screenHeight = getResources().getDisplayMetrics().heightPixels;
            }
            
            float riseDistance = screenHeight * (0.25f + random.nextFloat() * 0.08f); // 屏幕高度的1/4到1/3
            
            // 计算水平漂移 - 更轻微的左右漂移
            float horizontalDrift = (random.nextFloat() * 2 - 1) * 20; // -20到20像素
            
            // 计算动画时长 - 更长的时间，使动画看起来更缓慢
            int duration = 5000 + random.nextInt(3000); // 5-8秒
            
            // 创建路径动画 - 使用ValueAnimator实现更平滑的动画
            ValueAnimator pathAnimator = ValueAnimator.ofFloat(0f, 1f);
            pathAnimator.setDuration(duration);
            pathAnimator.setInterpolator(new AccelerateDecelerateInterpolator()); // 加减速插值器，模拟气泡上升
            
            // 随机决定这个音符是否有抖动效果 - 减少抖动概率
            boolean hasWobble = random.nextInt(100) < 25; // 25%的音符有抖动效果，降低比例
            
            // 如果有抖动，设置抖动参数 - 减小抖动幅度
            final float amplitude = hasWobble ? (3 + random.nextFloat() * 7) : 0; // 3-10的抖动幅度，减小范围
            final float frequency = hasWobble ? (0.3f + random.nextFloat() * 0.7f) : 0; // 0.3-1.0的抖动频率，降低频率
            
            // 随机决定是否有轻微旋转 - 减少旋转概率
            boolean hasRotation = random.nextInt(100) < 20; // 20%的音符有旋转效果
            final float maxRotation = hasRotation ? (random.nextFloat() * 15 - 7.5f) : 0; // -7.5到7.5度的旋转，减小角度
            
            pathAnimator.addUpdateListener(animation -> {
                float fraction = animation.getAnimatedFraction();
                
                // 计算当前Y位置 - 匀速上升，但使用缓动函数使开始和结束更平滑
                float easeInOutFraction = (float)(Math.sin((fraction - 0.5f) * Math.PI) * 0.5f + 0.5f);
                float currentY = startY - riseDistance * easeInOutFraction;
                
                // 计算当前X位置
                float currentX;
                if (hasWobble) {
                    // 有抖动效果的音符 - 添加正弦波动，但频率更低
                    currentX = startX + horizontalDrift * fraction + 
                              amplitude * (float)Math.sin(frequency * Math.PI * fraction * 6);
                } else {
                    // 无抖动效果的音符 - 平滑漂移
                    currentX = startX + horizontalDrift * fraction;
                }
                
                // 更新位置
                noteView.setX(currentX);
                noteView.setY(currentY);
                
                // 如果有旋转效果，更新旋转角度 - 使旋转更缓慢
                if (hasRotation) {
                    noteView.setRotation(maxRotation * (float)Math.sin(Math.PI * fraction));
                }
                
                // 更新透明度 - 先淡入，然后在最后1/3的时间内淡出
                if (fraction < 0.2f) {
                    // 前20%时间淡入
                    noteView.setAlpha(fraction * 4); // 0.1 -> 0.8
                } else if (fraction > 0.7f) {
                    // 后30%时间淡出
                    noteView.setAlpha(0.8f * (1 - (fraction - 0.7f) / 0.3f));
                } else {
                    // 中间50%时间保持较高透明度
                    noteView.setAlpha(0.8f);
                }
            });
            
            // 动画结束后移除音符
            pathAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    rootLayout.removeView(noteView);
                }
            });
            
            // 启动动画
            pathAnimator.start();
            
        } catch (Exception e) {
            Log.e("BreathingActivity", "添加音符失败", e);
        }
    }

    // 修改startMusicNoteAnimation方法，降低音符生成频率
    private void startMusicNoteAnimation() {
        if (rootLayout == null || isShowingNotes) return;
        
        isShowingNotes = true;
        Log.d("BreathingActivity", "开始音符动画");
        
        // 创建定时任务，随机生成音符
        noteRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBreathing && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // 一次只添加1个音符，减少密度
                    addMusicNote();
                    
                    // 增加时间间隔，降低音符生成频率
                    int delay = random.nextInt(800) + 1500; // 1500-2300ms
                    noteHandler.postDelayed(this, delay);
                } else {
                    isShowingNotes = false;
                }
            }
        };
        
        // 立即开始第一次运行
        noteHandler.post(noteRunnable);
    }

    // 修改stopMusicNoteAnimation方法，确保正确停止动画
    private void stopMusicNoteAnimation() {
        if (noteHandler != null && noteRunnable != null) {
            noteHandler.removeCallbacks(noteRunnable);
            isShowingNotes = false;
            
            // 清除所有现有音符
            if (rootLayout != null) {
                for (int i = 0; i < rootLayout.getChildCount(); i++) {
                    View child = rootLayout.getChildAt(i);
                    if (child instanceof ImageView && child.getTag() != null && 
                        "music_note".equals(child.getTag())) {
                        rootLayout.removeView(child);
                        i--; // 调整索引，因为移除了一个元素
                    }
                }
            }
        }
    }

    // 调整颜色亮度的辅助方法
    private int adjustBrightness(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    private void updateGuidanceTextForBreathing(boolean isInhaling) {
        // 取消之前的动画
        guidanceText.animate().cancel();
        
        // 根据当前模式设置颜色
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
        
        if (isInhaling) {
            // 吸气阶段 - 文字放大效果
            String inhaleText = String.format("吸气 %d 秒", currentMode.inhaleSeconds);
            SpannableString spannableString = new SpannableString(inhaleText);
            
            // 设置"吸气"两个字的样式
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, 2, 
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 设置数字的样式
            spannableString.setSpan(new RelativeSizeSpan(1.2f), 3, 4, 
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            guidanceText.setText(spannableString);
            guidanceText.setTextColor(textColor);
            
            // 添加放大动画
            guidanceText.setScaleX(0.9f);
            guidanceText.setScaleY(0.9f);
            guidanceText.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .alpha(1.0f)
                    .setDuration(currentMode.inhaleSeconds * 1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            // 呼气阶段 - 文字缩小效果
            String exhaleText = String.format("呼气 %d 秒", currentMode.exhaleSeconds);
            SpannableString spannableString = new SpannableString(exhaleText);
            
            // 设置"呼气"两个字的样式
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, 2, 
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 设置数字的样式
            spannableString.setSpan(new RelativeSizeSpan(1.2f), 3, 4, 
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            guidanceText.setText(spannableString);
            guidanceText.setTextColor(textColor);
            
            // 添加缩小动画
            guidanceText.setScaleX(1.1f);
            guidanceText.setScaleY(1.1f);
            guidanceText.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0.8f)
                    .setDuration(currentMode.exhaleSeconds * 1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // 添加波浪效果
        if (isInhaling) {
            // 吸气阶段 - 文字波浪效果
            CharSequence text = guidanceText.getText();
            if (text.length() > 0) {
                SpannableString spannableString;
                if (text instanceof SpannableString) {
                    spannableString = (SpannableString) text;
                } else {
                    spannableString = new SpannableString(text);
                }
                
                for (int i = 0; i < text.length(); i++) {
                    float waveOffset = (float) Math.sin(i * 0.5) * 0.2f + 1.0f;
                    spannableString.setSpan(new ScaleXSpan(waveOffset), i, i + 1, 
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                
                guidanceText.setText(spannableString);
            }
        }
    }

    // 修改updateRhythmIndicator方法
    private void updateRhythmIndicator(boolean isInhaling) {
        ImageView rhythmDot1 = findViewById(R.id.rhythmDot1);
        ImageView rhythmDot2 = findViewById(R.id.rhythmDot2);
        ImageView rhythmDot3 = findViewById(R.id.rhythmDot3);
        ImageView rhythmDot4 = findViewById(R.id.rhythmDot4);
        
        // 根据当前模式设置颜色
        int activeColor;
        switch (currentMode) {
            case NORMAL:
                activeColor = getResources().getColor(R.color.calm_breathing);
                break;
            case FOCUS:
                activeColor = getResources().getColor(R.color.focus_breathing);
                break;
            case ENERGIZING:
                activeColor = getResources().getColor(R.color.deep_breathing);
                break;
            case CALMING:
                activeColor = getResources().getColor(R.color.relax_breathing);
                break;
            default:
                activeColor = getResources().getColor(R.color.calm_breathing);
        }
        
        if (isInhaling) {
            // 吸气阶段 - 第一个点高亮并放大，其他点逐渐变暗
            rhythmDot1.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
            rhythmDot2.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            rhythmDot3.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            rhythmDot4.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            
            rhythmDot1.animate().alpha(1f).scaleX(1.5f).scaleY(1.5f).setDuration(300).start();
            rhythmDot2.animate().alpha(0.7f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            rhythmDot3.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            rhythmDot4.animate().alpha(0.3f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        } else {
            // 呼气阶段 - 第四个点高亮并放大，其他点逐渐变暗
            rhythmDot1.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            rhythmDot2.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            rhythmDot3.setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
            rhythmDot4.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
            
            rhythmDot1.animate().alpha(0.3f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            rhythmDot2.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            rhythmDot3.animate().alpha(0.7f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            rhythmDot4.animate().alpha(1f).scaleX(1.5f).scaleY(1.5f).setDuration(300).start();
        }
    }

    // 修改背景变化方法
    private void updateBreathingBackground(boolean isInhaling) {
        // 获取根布局
        ViewGroup rootLayout = findViewById(R.id.breathing_root_layout);
        
        // 如果叠加层不存在，创建它
        if (overlayView == null) {
            overlayView = new View(this);
            
            // 设置叠加层布局参数
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT);
            
            overlayView.setLayoutParams(params);
            
            // 添加到根布局
            rootLayout.addView(overlayView, 0); // 添加到最底层
        }
        
        // 根据当前模式获取颜色
        int baseColor;
        switch (currentMode) {
            case NORMAL:
                baseColor = getResources().getColor(R.color.calm_breathing);
                break;
            case FOCUS:
                baseColor = getResources().getColor(R.color.focus_breathing);
                break;
            case ENERGIZING:
                baseColor = getResources().getColor(R.color.deep_breathing);
                break;
            case CALMING:
                baseColor = getResources().getColor(R.color.relax_breathing);
                break;
            default:
                baseColor = getResources().getColor(R.color.calm_breathing);
        }
        
        // 创建非常透明的颜色
        int overlayColor = Color.argb(
                isInhaling ? 10 : 5,  // 非常低的透明度
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor));
        
        // 设置叠加层颜色
        overlayView.setBackgroundColor(overlayColor);
        
        // 添加淡入淡出动画
        overlayView.animate()
                .alpha(isInhaling ? 1.0f : 0.5f)
                .setDuration(isInhaling ? 
                        currentMode.inhaleSeconds * 1000 : 
                        currentMode.exhaleSeconds * 1000)
                .start();
    }

    private void updateMusicFeedback(String musicName) {
        if (musicFeedbackText != null) {
            // 使用Unicode音符字符，这种方式在大多数设备上都能正确显示
            musicFeedbackText.setText("\uD83C\uDFB5 正在播放：" + musicName + "\uD83C\uDFB6");
            musicFeedbackText.setVisibility(View.VISIBLE);
            
            // 根据当前模式设置音乐反馈文本的样式
            int textColor;
            int backgroundColor;
            int alpha = 220; // 透明度，使背景更柔和
            
            switch (currentMode) {
                case NORMAL:
                    textColor = getResources().getColor(R.color.calm_breathing);
                    backgroundColor = Color.argb(alpha, 245, 249, 252); // 非常淡的蓝色
                    break;
                case FOCUS:
                    textColor = getResources().getColor(R.color.focus_breathing);
                    backgroundColor = Color.argb(alpha, 249, 245, 252); // 非常淡的紫色
                    break;
                case ENERGIZING:
                    textColor = getResources().getColor(R.color.deep_breathing);
                    backgroundColor = Color.argb(alpha, 252, 249, 245); // 非常淡的橙色
                    break;
                case CALMING:
                    textColor = getResources().getColor(R.color.relax_breathing);
                    backgroundColor = Color.argb(alpha, 245, 252, 247); // 非常淡的绿色
                    break;
                default:
                    textColor = getResources().getColor(R.color.calm_breathing);
                    backgroundColor = Color.argb(alpha, 245, 249, 252);
            }
            
            // 设置文本颜色
            musicFeedbackText.setTextColor(textColor);
            
            // 创建一个新的GradientDrawable来设置背景
            android.graphics.drawable.GradientDrawable musicBackground = new android.graphics.drawable.GradientDrawable();
            musicBackground.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            musicBackground.setCornerRadius(16 * getResources().getDisplayMetrics().density); // 16dp
            musicBackground.setColor(backgroundColor);
            
            // 设置新的背景
            musicFeedbackText.setBackground(musicBackground);
            
            // 添加淡入动画
            musicFeedbackText.setAlpha(0f);
            musicFeedbackText.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
        }
    }

    // 在onCreate方法末尾调用
    private void setupInitialRhythmIndicator() {
        ImageView rhythmDot1 = findViewById(R.id.rhythmDot1);
        ImageView rhythmDot2 = findViewById(R.id.rhythmDot2);
        ImageView rhythmDot3 = findViewById(R.id.rhythmDot3);
        ImageView rhythmDot4 = findViewById(R.id.rhythmDot4);
        
        // 设置初始颜色 - 直接对ImageView设置颜色过滤器
        int defaultColor = Color.LTGRAY;
        rhythmDot1.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        rhythmDot2.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        rhythmDot3.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        rhythmDot4.setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
        
        // 创建一个简单的演示动画，在用户开始练习前展示节奏指示器的作用
        new Handler().postDelayed(() -> {
            // 只在用户尚未开始呼吸练习时展示
            if (!isBreathing) {
                // 简单地展示一次呼吸周期
                rhythmDot1.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(500).start();
                
                new Handler().postDelayed(() -> {
                    rhythmDot1.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                    rhythmDot2.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(500).start();
                    
                    new Handler().postDelayed(() -> {
                        rhythmDot2.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                        rhythmDot3.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(500).start();
                        
                        new Handler().postDelayed(() -> {
                            rhythmDot3.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                            rhythmDot4.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(500).start();
                            
                            new Handler().postDelayed(() -> {
                                rhythmDot4.animate().alpha(0.5f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                            }, 500);
                        }, 500);
                    }, 500);
                }, 500);
            }
        }, 2000); // 在页面加载2秒后展示演示动画
    }

    // 修改呼吸圆形提示方法
    private void setupInitialBreathingCircleHint() {
        // 获取呼吸圆形和指导文本
        ImageView breathingCircle = findViewById(R.id.breathingCircle);
        TextView guidanceText = findViewById(R.id.guidanceText);
        TextView breathingStateText = findViewById(R.id.breathingStateText);
        
        // 清空中间的状态文本
        breathingStateText.setText("");
        
        // 设置上方的指导文本，确保居中显示
        guidanceText.setText("跟随圆圈呼吸\n吸气4秒，呼气4秒");
        guidanceText.setTextSize(18);
        guidanceText.setTextColor(getResources().getColor(R.color.calm_breathing));
        guidanceText.setShadowLayer(3, 1, 1, Color.parseColor("#80000000"));
        guidanceText.setAlpha(0.9f);
        guidanceText.setGravity(android.view.Gravity.CENTER); // 设置文本居中
        guidanceText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // 确保文本对齐居中
        
        // 添加一个轻微的脉动动画，提示这是呼吸指示器
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.08f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        pulseAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            breathingCircle.setScaleX(value);
            breathingCircle.setScaleY(value);
        });
        
        // 立即开始动画
        pulseAnimator.start();
        
        // 当用户点击开始按钮时，停止初始动画，开始正式的呼吸练习
        startButton.setOnClickListener(v -> {
            if (!isBreathing) {
                // 清除提示文本，恢复为空白
                guidanceText.setText("");
                // 停止脉动动画
                pulseAnimator.cancel();
                // 开始呼吸练习
                startBreathing();
            } else {
                stopBreathingExercise();
            }
        });
        
        // 添加点击呼吸圆形也可以开始练习的功能
        breathingCircle.setOnClickListener(v -> {
            if (!isBreathing && !isPreparingToStart) {
                // 清除提示文本
                guidanceText.setText("");
                // 停止脉动动画
                pulseAnimator.cancel();
                // 开始呼吸练习
                startBreathing();
            }
        });
    }
} 