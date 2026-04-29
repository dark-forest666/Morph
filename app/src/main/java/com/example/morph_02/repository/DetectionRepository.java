// java
package com.example.morph_02.repository;

import com.example.morph_02.data.AppDatabase;
import com.example.morph_02.data.DetectionEntity;
import com.example.morph_02.ui.DetectionItem;

import java.util.ArrayList;
import java.util.List;

public class DetectionRepository {
    private final AppDatabase db;
    private static volatile DetectionRepository INSTANCE;

    private DetectionRepository(AppDatabase db) {
        this.db = db;
    }

    public static DetectionRepository getInstance(AppDatabase db) {
        if (INSTANCE == null) {
            synchronized (DetectionRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DetectionRepository(db);
                }
            }
        }
        return INSTANCE;
    }

    public long insertDetectionRecord(DetectionItem item) {
        DetectionEntity entity = new DetectionEntity(
                item.getTimestamp(),
                item.getLabel(),
                item.getConfidence(),
                item.getIntensity(),
                item.isReminded()
        );
        return db.detectionDao().insert(entity);
    }

    public boolean updateRemindedState(long recordId, boolean newState) {
        DetectionEntity entity = db.detectionDao().getRecordById(recordId);
        if (entity == null) return false;
        entity.setReminded(newState);
        return db.detectionDao().update(entity) > 0;
    }

    // 修复：手动将 DetectionEntity 转换为 UI 层的 DetectionItem，避免类型不匹配
    public List<DetectionItem> getAllDetectionItems() {
        List<DetectionEntity> entities = db.detectionDao().getAllRecords();
        List<DetectionItem> items = new ArrayList<>();
        if (entities == null) return items;
        for (DetectionEntity entity : entities) {
            DetectionItem uiItem = new DetectionItem(
                    entity.getTimestamp(),
                    entity.getLabel(),
                    entity.getConfidence(),
                    entity.getIntensity(),
                    entity.isReminded()
            );
            items.add(uiItem);
        }
        return items;
    }

    public void clearAllRecords() {
        db.detectionDao().deleteAll();
    }
}