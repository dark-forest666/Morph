package com.example.morph_02.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;

import com.example.morph_02.R;

// 提醒管理器（震动、亮屏、弹窗）
public class AlertManager {
    private static volatile AlertManager INSTANCE;
    private final Context context;
    private final Vibrator vibrator;
    private final PowerManager powerManager;

    private AlertManager(Context context) {
        this.context = context.getApplicationContext();
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public static AlertManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AlertManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AlertManager(context);
                }
            }
        }
        return INSTANCE;
    }

    // 显示提醒（震动+亮屏+弹窗）
    public void showAlert(DetectionItem item) {
        // 1. 触发震动（长震动，适配API 26+）
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 1000, 500, 1000, 500, 1000}, // 震动模式：停0ms→震1000ms→停500ms...
                        -1 // 不重复
                ));
            } else {
                vibrator.vibrate(new long[]{0, 1000, 500, 1000, 500, 1000}, -1);
            }
        }

        // 2. 亮屏（需要权限：android.permission.WAKE_LOCK）
        if (powerManager != null && !powerManager.isScreenOn()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "sound:alert:wakeup"
            );
            wakeLock.acquire(5000); // 亮屏5秒
            wakeLock.release();
        }

        // 3. 显示弹窗提醒（需确保在前台Activity显示，此处简化处理）
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            new AlertDialog.Builder(activity)
                    .setTitle("环境音预警")
                    .setMessage(String.format("检测到：%s\n置信度：%.2f\n强度：%.2f",
                            item.getLabel(), item.getConfidence(), item.getIntensity()))
                    .setPositiveButton("确认", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create()
                    .show();

            // 弹窗亮屏（可选）
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}