package com.example.mentalhealthdiary.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.mentalhealthdiary.R;
import java.util.Calendar;
import android.util.Log;

public class TipsWorker extends Worker {
    private static final String CHANNEL_ID = "tips_channel";
    private static final int NOTIFICATION_ID = 1;
    
    public TipsWorker(Context context, WorkerParameters params) {
        super(context, params);
        createNotificationChannel();
    }

    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // 如果是定时任务且推送被禁用，则不发送
        boolean isScheduled = getInputData().getBoolean("is_scheduled", true);
        if (isScheduled && !prefs.getBoolean("enable_tips", true)) {
            return Result.success();
        }

        // 如果是定时任务，检查时间是否符合设置
        if (isScheduled) {
            int preferredHour = prefs.getInt("tips_time", 9);
            Calendar now = Calendar.getInstance();
            if (now.get(Calendar.HOUR_OF_DAY) != preferredHour) {
                return Result.success();
            }
        }

        // 发送通知
        sendTipNotification(getRandomTip());
        return Result.success();
    }

    private String getRandomTip() {
        String[] tips = {
            "深呼吸是缓解压力的好方法",
            "保持规律作息有助于心理健康",
            "与朋友聊天可以改善心情",
            "适度运动能提升心理韧性",
            "记录感恩日记有助于保持积极心态",
            "给自己一个拥抱，接纳当下的自己",
            "尝试冥想，让心灵沉淀下来",
            "倾听音乐可以调节情绪",
            "设定小目标，一步步实现它",
            "保持乐观，相信明天会更好"
        };
        return tips[(int) (Math.random() * tips.length)];
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("TipsWorker", "Creating notification channel");
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "心理小贴士",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("每日心理健康提示");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager manager = getApplicationContext()
                .getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e("TipsWorker", "NotificationManager is null");
                return;
            }
            manager.createNotificationChannel(channel);
            Log.d("TipsWorker", "Notification channel created");
        }
    }

    private void sendTipNotification(String tip) {
        Log.d("TipsWorker", "Sending notification: " + tip);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            getApplicationContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日心理小贴士")
            .setContentText(tip)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) 
            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e("TipsWorker", "NotificationManager is null");
            return;
        }
        manager.notify(NOTIFICATION_ID, builder.build());
        Log.d("TipsWorker", "Notification sent");
    }
} 