package com.example.morph_02.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.morph_02.ui.DetectionItem;

// 与Room数据库映射的实体类（对应检测记录）
@Entity(tableName = "detection_records")
public class DetectionEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp; // 时间戳
    private String label;   // 声音类别（如“敲门声”）
    private float confidence; // 识别置信度
    private float intensity;  // 声音强度
    private boolean reminded; // 是否已提醒

    // 构造器（不含id，自增）
    public DetectionEntity(long timestamp, String label, float confidence, float intensity, boolean reminded) {
        this.timestamp = timestamp;
        this.label = label;
        this.confidence = confidence;
        this.intensity = intensity;
        this.reminded = reminded;
    }

    // Getter & Setter（Room需要）
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = intensity; }
    public boolean isReminded() { return reminded; }
    public void setReminded(boolean reminded) { this.reminded = reminded; }

    // 转换为UI层的DetectionItem（方便列表展示）
    public DetectionItem toDetectionItem() {
        return new DetectionItem(timestamp, label, confidence, intensity, reminded);
    }
}