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
                "通过正念呼吸，让心灵回归平静"
            ),
            new OnboardingItem(
                R.drawable.ic_mood_tracking,
                "情绪记录",
                "记录每一天的心情变化，了解自己的情绪轨迹"
            ),
            new OnboardingItem(
                R.drawable.ic_achievement,
                "成长足迹",
                "在正念修习的道路上，见证自己的每一步进步"
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