package com.example.morph_02.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.morph_02.R;

public class NotificationHelper {
    // 通知渠道ID（自定义，需唯一）
    private static final String CHANNEL_ID = "sound_service_channel";
    // 前台服务通知ID（自定义）
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;

    /**
     * 创建通知渠道（Android 8.0+ 必须）
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "声音服务通知";
            String description = "前台服务麦克风监听通知";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // 注册渠道到系统
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建前台服务通知
     */
    public static Notification buildForegroundServiceNotification(Context context) {
        // 先确保渠道已创建
        createNotificationChannel(context);
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("麦克风监听中")
                .setContentText("服务正在运行...")
                .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的通知图标
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true) // 前台服务通知不可手动关闭
                .build();
    }

    /**
     * 检查并请求通知权限（Android 13+ 必须）
     */
    public static boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // 低版本无需显式请求通知权限
        return true;
    }

    /**
     * 启动前台服务时调用：构建并返回通知
     */
    public static Notification getForegroundNotification(Context context) {
        return buildForegroundServiceNotification(context);
    }
}