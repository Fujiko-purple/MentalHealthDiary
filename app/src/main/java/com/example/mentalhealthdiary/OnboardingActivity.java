package com.example.mentalhealthdiary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View[] indicators;
    private OnboardingItem[] onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // 初始化引导页数据
        onboardingItems = new OnboardingItem[]{
            new OnboardingItem(
                R.drawable.ic_meditation,
                "静心呼吸",
                "让我们一起深呼吸，\n感受此刻的平静与美好"
            ),
            new OnboardingItem(
                R.drawable.ic_mood_tracking,
                "倾听内心",
                "每一个情绪都值得被关注，\n记录下你的心情，让我陪你一起成长"
            ),
            new OnboardingItem(
                R.drawable.ic_nature,
                "拥抱自然",
                "聆听鸟鸣，感受微风，\n让心灵在大自然中找到归属"
            ),
            new OnboardingItem(
                R.drawable.ic_achievement,
                "点滴进步",
                "每一次练习都是一份珍贵的礼物，\n让我们一起见证美好的改变"
            )
        };

        // 设置ViewPager
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new OnboardingAdapter());
        
        // 设置指示器
        setupIndicators();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
            }
        });

        // 设置开始按钮
        MaterialButton startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            // 标记已经查看过引导页
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("has_seen_onboarding", true).apply();
            
            // 跳转到主页面
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 添加页面切换动画效果
        viewPager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float absPos = Math.abs(position);
                
                // 淡入淡出效果
                page.setAlpha(1 - (absPos * 0.5f));
                
                // 缩放效果
                float scale = 1 - (absPos * 0.2f);
                page.setScaleX(scale);
                page.setScaleY(scale);
                
                // 平移效果
                page.setTranslationX(-position * page.getWidth() / 8);
            }
        });
    }

    private void setupIndicators() {
        ViewGroup indicatorContainer = findViewById(R.id.indicatorContainer);
        indicators = new View[onboardingItems.length];
        
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(size, size);
            params.setMargins(size/2, 0, size/2, 0);
            indicators[i].setLayoutParams(params);
            indicators[i].setBackgroundResource(R.drawable.indicator_inactive);
            indicatorContainer.addView(indicators[i]);
        }
        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(
                i == position ? R.drawable.indicator_active : R.drawable.indicator_inactive
            );
        }
    }

    private static class OnboardingItem {
        int imageRes;
        String title;
        String description;

        OnboardingItem(int imageRes, String title, String description) {
            this.imageRes = imageRes;
            this.title = title;
            this.description = description;
        }
    }

    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnboardingItem item = onboardingItems[position];
            holder.imageView.setImageResource(item.imageRes);
            holder.titleText.setText(item.title);
            holder.descriptionText.setText(item.description);
        }

        @Override
        public int getItemCount() {
            return onboardingItems.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView titleText;
            TextView descriptionText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                titleText = itemView.findViewById(R.id.titleText);
                descriptionText = itemView.findViewById(R.id.descriptionText);
            }
        }
    }
} 