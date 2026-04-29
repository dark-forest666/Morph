// java
package com.example.morph_02.ui;

public class DetectionItem {
    private long timestamp;
    private String label;
    private float confidence;
    private float intensity;
    private boolean reminded;

    public DetectionItem(long timestamp, String label, float confidence, float intensity, boolean reminded) {
        this.timestamp = timestamp;
        this.label = label;
        this.confidence = confidence;
        this.intensity = intensity;
        this.reminded = reminded;
    }

    public long getTimestamp() { return timestamp; }
    public String getLabel() { return label; }
    public float getConfidence() { return confidence; }
    public float getIntensity() { return intensity; }
    public boolean isReminded() { return reminded; }
}