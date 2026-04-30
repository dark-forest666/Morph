package com.example.morph_02.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.morph_02.R;

public class AlertManager {
    private static final String CHANNEL_ID = "sound_alert_channel";
    private static final int NOTIFICATION_ID = 2001;
    private final Context context;
    private final NotificationManager notificationManager;

    private AlertManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "环境音预警",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("检测到重要环境音时发送通知");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static AlertManager getInstance(Context context) {
        return new AlertManager(context); // 简化单例，每次创建（实际可用静态变量）
    }

    public void showAlert(DetectionItem item) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⚠️ 环境音预警")
                .setContentText(String.format("%s (置信度: %.0f%%)", item.getLabel(), item.getConfidence() * 100))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000});

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}