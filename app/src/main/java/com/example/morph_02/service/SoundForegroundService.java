package com.example.morph_02.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.morph_02.R;
import com.example.morph_02.ml.AudioClassifierManager;
import com.example.morph_02.ui.AlertManager;
import com.example.morph_02.ui.DetectionItem;
import com.example.morph_02.repository.DetectionRepository;
import com.example.morph_02.data.AppDatabase;

// 优先级1修复：稳定保活、生命周期安全、版本适配
public class SoundForegroundService extends Service {
    // 无硬编码：常量封装
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "sound_detection_channel";
    private static final String CHANNEL_NAME = "环境音预警服务";
    private static final String NOTIFICATION_TITLE = "环境音监听中";
    private static final String NOTIFICATION_CONTENT = "实时识别敲门声、婴儿哭声等";

    private AudioClassifierManager classifierManager;
    private DetectionRepository repository;
    private boolean isServiceDestroyed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceDestroyed = false;
        // 初始化数据库仓库
        repository = DetectionRepository.getInstance(AppDatabase.getInstance(this));
        // 初始化音频识别（真实模型）
        classifierManager = new AudioClassifierManager(this);
        // 注册识别回调
        // 在 SoundForegroundService 的 onCreate 方法里，修改 setDetectionListener
        classifierManager.setDetectionListener(new AudioClassifierManager.DetectionListener() {
            @Override
            public void onDetected(DetectionItem item) {
                handleDetectionResult(item);
            }

            @Override
            public void onError(String message) {
                // 权限错误时，可在这里弹出通知或提示用户
                Log.e("SoundService", "识别错误：" + message);
                // 可选：停止服务并通知用户
                // stopSelf();
            }
        });
        // 创建通知渠道
        createNotificationChannel();
    }

    // 识别结果：存储 + 预警
    private void handleDetectionResult(DetectionItem item) {
        if (isServiceDestroyed) return;
        repository.insertDetectionRecord(item);
        AlertManager.getInstance(getApplicationContext()).showAlert(item);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 优先级1核心：服务保活（被系统杀死后自动重启）
        if (intent == null && !isServiceDestroyed) {
            Intent restartIntent = new Intent(this, SoundForegroundService.class);
            startService(restartIntent);
        }

        // 启动前台服务（Android12+ 完美适配）
        Notification notification = buildServiceNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 启动音频识别
        classifierManager.startListening();
        // 保活标记
        return START_STICKY;
    }

    // 构建前台服务通知
    private Notification buildServiceNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_CONTENT)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // 创建通知渠道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // 优先级1核心：安全释放资源，避免内存泄漏/崩溃
    @Override
    public void onDestroy() {
        isServiceDestroyed = true;
        if (classifierManager != null) {
            classifierManager.stopListening();
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}