// java
package com.example.morph_02.ui;

public class DetectionItem {
    private long id;          // 新增：数据库主键
    private long timestamp;
    private String label;
    private float confidence;
    private float intensity;
    private boolean reminded;

    public DetectionItem(long id, long timestamp, String label, float confidence, float intensity, boolean reminded) {
        this.id = id;
        this.timestamp = timestamp;
        this.label = label;
        this.confidence = confidence;
        this.intensity = intensity;
        this.reminded = reminded;
    }

    // 兼容旧构造器（不带 id），但建议统一使用新构造器
    public DetectionItem(long timestamp, String label, float confidence, float intensity, boolean reminded) {
        this(0, timestamp, label, confidence, intensity, reminded);
    }

    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getLabel() { return label; }
    public float getConfidence() { return confidence; }
    public float getIntensity() { return intensity; }
    public boolean isReminded() { return reminded; }
}