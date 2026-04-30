package com.example.morph_02.ml;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.morph_02.ui.DetectionItem;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class AudioClassifierManager {
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL = android.media.AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;
    public static final int MODEL_INPUT_LENGTH = 15600;
    public static final String MODEL_NAME = "yamnet.tflite";
    public static final String LABELS_NAME = "class_names.csv";
    public static final int DETECT_INTERVAL = 1000; // 改为1秒，避免丢失事件
    private static final String TAG = "AudioClassifier";

    private AudioRecord audioRecord;
    private boolean isListening;
    private final Handler mainHandler;
    private DetectionListener listener;
    private final Interpreter tflite;
    private final List<String> classNames;
    private final Context context;

    public interface DetectionListener {
        void onDetected(DetectionItem item);
        void onError(String message);
    }

    public AudioClassifierManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.tflite = createInterpreter(context, MODEL_NAME);
        this.classNames = loadLabels(context, LABELS_NAME);
        Log.d(TAG, "模型初始化完成，标签数量：" + (classNames != null ? classNames.size() : 0));
    }

    public void setDetectionListener(DetectionListener listener) {
        this.listener = listener;
    }

    private boolean hasRecordAudioPermission() {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private Interpreter createInterpreter(Context context, String modelName) {
        try {
            InputStream inputStream = context.getAssets().open(modelName);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length);
            byteBuffer.order(ByteOrder.nativeOrder());
            byteBuffer.put(buffer);
            byteBuffer.rewind();

            return new Interpreter(byteBuffer);
        } catch (Exception e) {
            Log.e(TAG, "模型加载失败", e);
            return null;
        }
    }

    private List<String> loadLabels(Context context, String labelsName) {
        List<String> labels = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsName)))) {
            String line;
            // 修复：不跳过任何行，直接解析
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // 标准 YAMNet CSV 格式：索引,mid,display_name
                if (parts.length >= 3) {
                    // 移除可能存在的引号
                    String displayName = parts[2].replace("\"", "").trim();
                    labels.add(displayName);
                } else if (parts.length == 2) {
                    labels.add(parts[1].replace("\"", "").trim());
                }
            }
            Log.d(TAG, "成功加载标签：" + labels.size());
        } catch (Exception e) {
            Log.e(TAG, "标签加载失败", e);
        }
        return labels;
    }

    public void startListening() {
        if (!hasRecordAudioPermission()) {
            Log.e(TAG, "无录音权限");
            if (listener != null) mainHandler.post(() -> listener.onError("无录音权限"));
            return;
        }
        if (isListening || tflite == null) {
            Log.e(TAG, "已经在监听中或模型未加载");
            return;
        }
        isListening = true;

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT);
        // 确保缓冲区至少能放 MODEL_INPUT_LENGTH * 2
        if (bufferSize < MODEL_INPUT_LENGTH * 2) {
            bufferSize = MODEL_INPUT_LENGTH * 2;
        }

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord 初始化失败");
            }
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "创建 AudioRecord 失败", e);
            isListening = false;
            if (listener != null) mainHandler.post(() -> listener.onError("无法访问麦克风"));
            return;
        }

        new Thread(() -> {
            try {
                audioRecord.startRecording();
                // 使用环形缓冲区累积音频数据
                short[] ringBuffer = new short[MODEL_INPUT_LENGTH];
                int offset = 0;

                while (isListening && hasRecordAudioPermission()) {
                    int shortToRead = MODEL_INPUT_LENGTH - offset;
                    int read = audioRecord.read(ringBuffer, offset, shortToRead);
                    if (read > 0) {
                        offset += read;
                        if (offset >= MODEL_INPUT_LENGTH) {
                            // 拿到了完整一帧，进行推理
                            float[][] inputWaveform = new float[1][MODEL_INPUT_LENGTH];
                            for (int i = 0; i < MODEL_INPUT_LENGTH; i++) {
                                inputWaveform[0][i] = ringBuffer[i] / 32768.0f;
                            }

                            float[][] scores = new float[1][521];
                            tflite.run(inputWaveform, scores);

                            int topIndex = getTopClassIndex(scores[0]);
                            float confidence = scores[0][topIndex];
                            String label = (classNames != null && topIndex < classNames.size())
                                    ? classNames.get(topIndex) : "未知声音";
                            float intensity = calculateIntensity(ringBuffer, MODEL_INPUT_LENGTH);

                            Log.d(TAG, String.format("识别结果: %s (%.2f) 强度 %.2f", label, confidence, intensity));
                            postResult(label, confidence, intensity);

                            // 重置偏移，准备下一帧（可重叠一半，但简单起见完全重叠）
                            offset = 0;
                            // 可选：滑动窗口（保留后一半与前一半重叠），更平滑，但为简化直接重置
                            // 延迟后再继续采集
                            Thread.sleep(DETECT_INTERVAL);
                        }
                    } else {
                        // 读取错误或超时，短暂休眠
                        Thread.sleep(10);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "音频线程异常", e);
                if (listener != null) mainHandler.post(() -> listener.onError("识别线程崩溃"));
            } finally {
                stopListening();
            }
        }).start();
    }

    private int getTopClassIndex(float[] scores) {
        int maxIdx = 0;
        float maxScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private float calculateIntensity(short[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) sum += Math.abs(buffer[i]);
        return (float) sum / length / 32768.0f;
    }

    private void postResult(String label, float confidence, float intensity) {
        mainHandler.post(() -> {
            if (listener != null) {
                DetectionItem item = new DetectionItem(System.currentTimeMillis(), label, confidence, intensity, false);
                listener.onDetected(item);
            }
        });
    }

    public void stopListening() {
        isListening = false;
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
    }
}