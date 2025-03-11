package com.example.mentalhealthdiary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.mentalhealthdiary.config.ApiConfig;
import com.example.mentalhealthdiary.service.ChatApiClient;
import com.example.mentalhealthdiary.service.ChatRequest;
import com.example.mentalhealthdiary.service.ChatResponse;
import com.example.mentalhealthdiary.service.TipsWorker;
import java.util.Collections;
import android.app.ProgressDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;
import android.content.Context;

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
        private ProgressDialog dialog;
        
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
            // 使用 getActivity() 替代 this 来获取 Context
            Context context = getActivity();
            if (context == null) return;

            // 添加日志输出
            Log.d("ApiConfig", "useCustomApi: " + ApiConfig.isCustomApiEnabled(context));
            Log.d("ApiConfig", "apiBase: " + ApiConfig.getBaseUrl(context));
            Log.d("ApiConfig", "apiKey: " + ApiConfig.getApiKey(context));
            Log.d("ApiConfig", "modelName: " + ApiConfig.getModelName(context));
            
            EditTextPreference apiKeyPref = findPreference("custom_api_key");
            EditTextPreference apiBasePref = findPreference("custom_api_base");
            EditTextPreference modelNamePref = findPreference("custom_model_name");
            SwitchPreference useCustomApiPref = findPreference("use_custom_api");
            Preference testApiPref = findPreference("test_api");
            
            // API开关监听
            useCustomApiPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                updateApiPreferencesState(enabled);
                ChatApiClient.resetInstance();  // 重置实例
                return true;
            });
            
            // API密钥验证
            apiKeyPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String key = (String) newValue;
                if (key.trim().isEmpty()) {
                    showError("API密钥不能为空");
                    return false;
                }
                ChatApiClient.resetInstance();  // 重置实例
                return true;
            });
            
            // API地址验证
            apiBasePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String url = (String) newValue;
                if (url.trim().isEmpty()) {
                    showError("API地址不能为空");
                    return false;
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    showError("API地址必须以http://或https://开头");
                    return false;
                }
                if (!url.endsWith("/")) {
                    url += "/";
                }
                ChatApiClient.resetInstance();  // 重置实例
                return true;
            });
            
            // 测试API按钮点击事件
            testApiPref.setOnPreferenceClickListener(preference -> {
                if (!validateApiSettings()) {
                    return true;
                }
                
                if (dialog != null && dialog.isShowing()) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                dialog = new ProgressDialog(getActivity());
                dialog.setMessage("正在测试API连接...");
                dialog.setCancelable(false);
                
                try {
                    dialog.show();
                    
                    // 创建测试请求
                    ChatRequest testRequest = new ChatRequest(
                        Collections.singletonList(
                            new ChatRequest.Message("user", "测试消息")
                        ),
                        ApiConfig.getModelName(getActivity())
                    );
                    
                    // 执行API测试
                    Call<ChatResponse> call = ChatApiClient.getInstance(getActivity())
                        .sendMessage(testRequest);
                        
                    call.enqueue(new Callback<ChatResponse>() {
                        @Override
                        public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                safelyDismissDialog();
                                return;
                            }
                            
                            safelyDismissDialog();
                            
                            if (response.isSuccessful() && response.body() != null) {
                                safelyShowSuccess("API连接测试成功！");
                            } else {
                                safelyShowError("API测试失败: " + response.code());
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<ChatResponse> call, Throwable t) {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                safelyDismissDialog();
                                return;
                            }
                            
                            safelyDismissDialog();
                            safelyShowError("API连接失败: " + t.getMessage());
                        }
                    });
                } catch (Exception e) {
                    safelyDismissDialog();
                    safelyShowError("发生错误: " + e.getMessage());
                }
                
                return true;
            });
        }

        private void updateApiPreferencesState(boolean enabled) {
            findPreference("custom_api_key").setEnabled(enabled);
            findPreference("custom_api_base").setEnabled(enabled);
            findPreference("custom_model_name").setEnabled(enabled);
            findPreference("test_api").setEnabled(enabled);
        }

        private boolean validateApiSettings() {
            String apiKey = ApiConfig.getApiKey(getActivity());
            String baseUrl = ApiConfig.getBaseUrl(getActivity());
            String modelName = ApiConfig.getModelName(getActivity());
            
            if (apiKey.trim().isEmpty()) {
                showError("请先配置API密钥");
                return false;
            }
            
            if (baseUrl.trim().isEmpty()) {
                showError("请先配置API地址");
                return false;
            }
            
            if (modelName.trim().isEmpty()) {
                showError("请先配置模型名称");
                return false;
            }
            
            return true;
        }

        private void showError(String message) {
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                    .setTitle("错误")
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show();
            }
        }

        private void showSuccess(String message) {
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                    .setTitle("成功")
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show();
            }
        }

        private void safelyDismissDialog() {
            if (dialog != null && dialog.isShowing()) {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dialog = null;
        }
        
        private void safelyShowError(final String message) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    try {
                        new AlertDialog.Builder(getActivity())
                            .setTitle("错误")
                            .setMessage(message)
                            .setPositiveButton("确定", null)
                            .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        
        private void safelyShowSuccess(final String message) {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    try {
                        new AlertDialog.Builder(getActivity())
                            .setTitle("成功")
                            .setMessage(message)
                            .setPositiveButton("确定", null)
                            .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            safelyDismissDialog();
        }
    }
} 