package com.example.mentalhealthdiary;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 2000; // 2秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
} 