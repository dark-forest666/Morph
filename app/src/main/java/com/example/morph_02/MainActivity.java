package com.example.morph_02;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.morph_02.service.SoundForegroundService;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMISSIONS = 1001;
    private TextView tvStatus, tvCurrentSound, tvConfidence;
    private Button btnStart, btnStop, btnHistory;
    private boolean isServiceRunning = false;

    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.RECORD_AUDIO
            };
        }
    }

    private final BroadcastReceiver detectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String label = intent.getStringExtra("label");
            float confidence = intent.getFloatExtra("confidence", 0f);
            tvCurrentSound.setText("当前识别：" + label);
            tvConfidence.setText(String.format("置信度：%.0f%%", confidence * 100));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tv_status);
        tvCurrentSound = findViewById(R.id.tv_current_sound);
        tvConfidence = findViewById(R.id.tv_confidence);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history);

        updateButtonState();

        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                startService(new Intent(this, SoundForegroundService.class));
                isServiceRunning = true;
                updateButtonState();
                tvStatus.setText("服务状态：运行中");
                Toast.makeText(this, "环境音预警服务已启动", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions();
            }
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, SoundForegroundService.class));
            isServiceRunning = false;
            updateButtonState();
            tvStatus.setText("服务状态：已停止");
            tvCurrentSound.setText("当前识别：--");
            tvConfidence.setText("置信度：--");
            Toast.makeText(this, "环境音预警服务已停止", Toast.LENGTH_SHORT).show();
        });

        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(detectionReceiver,
                new IntentFilter("com.example.morph_02.NEW_DETECTION"));
        // 检查服务是否实际在运行（可选：通过 ActivityManager 检查）
        isServiceRunning = isServiceRunning(); // 自定义方法
        updateButtonState();
        tvStatus.setText(isServiceRunning ? "服务状态：运行中" : "服务状态：已停止");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(detectionReceiver);
    }

    private boolean isServiceRunning() {
        // 简单判断：如果服务进程存在？也可以保留内部变量，但最好从实际状态恢复
        // 这里简化：返回之前保存的 isServiceRunning（不完全准确）
        return isServiceRunning;
    }

    private boolean checkPermissions() {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                btnStart.performClick();
            } else {
                Toast.makeText(this, "需要录音和通知权限才能运行", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateButtonState() {
        btnStart.setEnabled(!isServiceRunning);
        btnStop.setEnabled(isServiceRunning);
    }
}