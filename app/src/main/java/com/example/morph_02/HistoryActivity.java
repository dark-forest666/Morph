package com.example.morph_02;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.morph_02.data.AppDatabase;
import com.example.morph_02.repository.DetectionRepository;
import com.example.morph_02.ui.DetectionAdapter;
import com.example.morph_02.ui.DetectionItem;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerDetections;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private DetectionAdapter adapter;
    private DetectionRepository repository;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 初始化Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("历史记录");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回按钮

        // 初始化视图
        recyclerDetections = findViewById(R.id.recycler_detections);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        tvEmpty = findViewById(R.id.tv_empty);

        // 初始化仓库
        AppDatabase db = AppDatabase.getInstance(this);
        repository = DetectionRepository.getInstance(db);

        // 初始化适配器
        adapter = new DetectionAdapter(null);
        recyclerDetections.setLayoutManager(new LinearLayoutManager(this));
        recyclerDetections.setAdapter(adapter);

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(() -> {
            handler.postDelayed(() -> {
                loadData();
                swipeRefresh.setRefreshing(false);
            }, 800);
        });

        // 清空按钮
        findViewById(R.id.fab_clear).setOnClickListener(v -> {
            repository.clearAllRecords();
            adapter.clear();
            updateEmptyView();
            Toast.makeText(this, "已清空历史记录", Toast.LENGTH_SHORT).show();
        });

        // 首次加载数据
        loadData();
    }

    // 从数据库加载真实数据
    private void loadData() {
        List<DetectionItem> items = repository.getAllDetectionItems();
        adapter.setItems(items);
        updateEmptyView();
    }

    // 更新空视图显示
    private void updateEmptyView() {
        boolean empty = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerDetections.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // 返回按钮监听
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}