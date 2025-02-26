package com.example.mentalhealthdiary.service;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class TipsWorkManager {
    private static final String TIPS_WORK_NAME = "daily_tips_work";

    public static void scheduleDailyTips(Context context) {
        // 创建每日定时任务
        Data inputData = new Data.Builder()
            .putBoolean("is_scheduled", true)
            .build();

        PeriodicWorkRequest tipsWorkRequest =
            new PeriodicWorkRequest.Builder(TipsWorker.class, 24, TimeUnit.HOURS)
                .setInputData(inputData)
                .build();

        // 确保只有一个实例在运行
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                TIPS_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                tipsWorkRequest
            );
    }

    public static void cancelDailyTips(Context context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(TIPS_WORK_NAME);
    }
} 