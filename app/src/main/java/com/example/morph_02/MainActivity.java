package com.example.morph_02;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.morph_02.service.SoundForegroundService;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 1001;
    private TextView tvStatus;
    private Button btnStart, btnStop, btnHistory;
    private boolean isServiceRunning = false;

    // 所需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.VIBRATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS // 新增
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tv_status);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history); // 新增历史记录按钮

        // 初始化按钮状态
        updateButtonState();

        // 启动服务
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

        // 停止服务
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, SoundForegroundService.class));
            isServiceRunning = false;
            updateButtonState();
            tvStatus.setText("服务状态：已停止");
            Toast.makeText(this, "环境音预警服务已停止", Toast.LENGTH_SHORT).show();
        });

        // 跳转到历史记录页面
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    // 检查权限
    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 请求权限
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_PERMISSIONS);
    }

    // 权限申请结果回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                btnStart.performClick();
            } else {
                Toast.makeText(this, "请授予所有必要权限以使用功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 更新按钮状态
    private void updateButtonState() {
        btnStart.setEnabled(!isServiceRunning);
        btnStop.setEnabled(isServiceRunning);
    }
}