package com.example.mentalhealthdiary;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mentalhealthdiary.database.AppDatabase;
import com.example.mentalhealthdiary.database.BreathingSession;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 更新呼吸模式枚举
    private enum BreathingMode {
        NORMAL(4, 4, "标准呼吸 4-4", "平衡身心"),
        RELAXING(4, 8, "放松呼吸 4-8", "缓解压力"),
        ENERGIZING(6, 2, "提神呼吸 6-2", "提升能量"),
        CALMING(4, 6, "安眠呼吸 4-6", "助于入睡");

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
        // 如果正在进行练习且超过30秒，保存记录
        if (isBreathing && sessionSeconds >= 30) {
            saveBreathingSession();
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

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

    // 添加模式切换时的提示
    private void onModeSelected(BreathingMode mode) {
        currentMode = mode;
        updateBreathingAnimation();
        
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
        new AlertDialog.Builder(this)
            .setTitle(mode.description)
            .setMessage("呼吸节奏: 吸气" + mode.inhaleSeconds + "秒，呼气" + mode.exhaleSeconds + "秒\n\n" +
                       "适用场景: " + mode.benefit + "\n\n" +
                       "练习建议: 找到舒适的姿势，保持背部挺直。随着圆圈的变化调整呼吸节奏，" +
                       "吸气时圆圈扩大，呼气时圆圈收缩。")
            .setPositiveButton("开始练习", (dialog, which) -> {
                if (!isBreathing) {
                    startBreathingExercise();
                }
            })
            .setNegativeButton("关闭", null)
            .show();
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
} 