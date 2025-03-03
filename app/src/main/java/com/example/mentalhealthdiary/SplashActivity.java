package com.example.mentalhealthdiary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SplashActivity", "onCreate started");
        setContentView(R.layout.activity_splash);

        // 设置渐入动画
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);

        ImageView logoImage = findViewById(R.id.splashImage);
        TextView titleText = findViewById(R.id.splashTitle);
        TextView subtitleText = findViewById(R.id.splashSubtitle);

        logoImage.startAnimation(fadeIn);
        titleText.startAnimation(fadeIn);
        subtitleText.startAnimation(fadeIn);

        // 延迟后跳转到主界面
        new Handler().postDelayed(() -> {
            Log.d("SplashActivity", "Handler started");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            
            // 先清除之前的值
            prefs.edit().clear().apply();
            // 然后设置为 false
            prefs.edit().putBoolean("has_seen_onboarding", false).apply();
            
            boolean hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false);
            Log.d("SplashActivity", "hasSeenOnboarding: " + hasSeenOnboarding);
            
            Intent intent;
            if (hasSeenOnboarding) {
                Log.d("SplashActivity", "Starting MainActivity");
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                Log.d("SplashActivity", "Starting OnboardingActivity");
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            
            try {
                startActivity(intent);
                Log.d("SplashActivity", "Activity started successfully");
            } catch (Exception e) {
                Log.e("SplashActivity", "Error starting activity", e);
            }
            finish();
        }, SPLASH_DURATION);
    }
} 