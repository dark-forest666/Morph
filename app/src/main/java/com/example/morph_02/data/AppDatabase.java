package com.example.morph_02.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

// 数据库主类（版本号后续更新需递增）
@Database(entities = {DetectionEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    // 暴露DAO接口
    public abstract DetectionDao detectionDao();

    // 单例获取数据库实例
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "detection_db" // 数据库文件名
                            ).allowMainThreadQueries() // 简易版（真实项目需用异步/协程）
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}