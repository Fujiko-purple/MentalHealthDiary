package com.example.mentalhealthdiary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.mentalhealthdiary.service.TipsWorker;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_container);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        }
        
        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        // 添加测试按钮点击事件
        findViewById(R.id.test_notification_button).setOnClickListener(v -> {
            // 立即发送一条测试通知
            sendTestNotification();
        });
    }

    private void sendTestNotification() {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请先授予通知权限", Toast.LENGTH_SHORT).show();
                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1
                );
                return;
            }
        }

        // 创建一个一次性的工作请求
        OneTimeWorkRequest testTipRequest =
            new OneTimeWorkRequest.Builder(TipsWorker.class)
            .setInputData(new Data.Builder()
                .putBoolean("is_scheduled", false)
                .build())
            .build();

        // 立即执行
        WorkManager.getInstance(this)
            .enqueue(testTipRequest);

        Toast.makeText(this, "已发送测试通知", Toast.LENGTH_SHORT).show();
    }
    
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();
            validateApiConfig();
        }

        private void validateApiConfig() {
            EditTextPreference apiKeyPref = findPreference("custom_api_key");
            apiKeyPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String key = (String) newValue;
                if (key.contains(" ")) {
                    Toast.makeText(getContext(), "API密钥不能包含空格", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            });
            
            // 类似验证其他字段...
        }
    }
} 