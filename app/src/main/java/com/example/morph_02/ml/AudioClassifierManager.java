package com.example.morph_02.ml;

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

// 修复：添加权限检查 + 异常处理，解决SecurityException和Lint警告
public class AudioClassifierManager {
    // 👇 完全对齐YAMNet教程参数（无硬编码）
    public static final int SAMPLE_RATE = 16000;    // 教程指定16kHz
    public static final int CHANNEL = android.media.AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;
    public static final int MODEL_INPUT_LENGTH = 15600; // YAMNet固定输入长度
    public static final String MODEL_NAME = "yamnet.tflite";
    public static final String LABELS_NAME = "class_names.csv"; // 直接用csv，不用改后缀
    public static final int DETECT_INTERVAL = 2000;
    private static final String TAG = "AudioClassifier";

    private AudioRecord audioRecord;
    private boolean isListening;
    private final Handler mainHandler;
    private DetectionListener listener;
    private final Interpreter tflite;
    private final List<String> classNames;
    private final Context context;

    // 回调接口（不变，兼容你的服务）
    public interface DetectionListener {
        void onDetected(DetectionItem item);
        void onError(String message); // 新增错误回调，方便上层处理权限问题
    }

    // 构造：加载模型+标签（对应教程：加载model、加载class_names）
    public AudioClassifierManager(Context context) {
        this.context = context.getApplicationContext(); // 用ApplicationContext避免内存泄漏
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.tflite = createInterpreter(context, MODEL_NAME);
        this.classNames = loadLabels(context, LABELS_NAME);
    }

    public void setDetectionListener(DetectionListener listener) {
        this.listener = listener;
    }

    // ====================== 新增：权限检查方法（解决Lint警告） ======================
    private boolean hasRecordAudioPermission() {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    // ====================== 核心1：加载TFLite模型（对应教程 model = hub.load(...)） ======================
    private Interpreter createInterpreter(Context context, String modelName) {
        try {
            InputStream inputStream = context.getAssets().open(modelName);
            FileChannel fileChannel = ((FileInputStream) inputStream).getChannel();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            return new Interpreter(buffer);
        } catch (Exception e) {
            Log.e(TAG, "模型加载失败", e);
            return null;
        }
    }

    // ====================== 核心2：加载标签（对应教程 class_names_from_csv） ======================
    private List<String> loadLabels(Context context, String labelsName) {
        List<String> labels = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsName)))) {
            String line;
            br.readLine(); // 跳过表头（csv第一行）
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) labels.add(parts[2]); // display_name
            }
        } catch (Exception e) {
            Log.e(TAG, "标签加载失败", e);
        }
        return labels;
    }

    // ====================== 核心3：启动录音+推理（修复权限问题） ======================
    public void startListening() {
        // 先检查权限：无权限直接返回，不启动录音
        if (!hasRecordAudioPermission()) {
            Log.e(TAG, "无录音权限，无法启动音频识别");
            if (listener != null) {
                mainHandler.post(() -> listener.onError("无录音权限，请先授予权限"));
            }
            return;
        }

        if (isListening || tflite == null) return;
        isListening = true;

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT);
        // 捕获SecurityException：权限被撤销时不会崩溃
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, bufferSize);
        } catch (SecurityException e) {
            Log.e(TAG, "创建AudioRecord失败，权限被拒绝", e);
            isListening = false;
            if (listener != null) {
                mainHandler.post(() -> listener.onError("录音权限被拒绝，无法启动识别"));
            }
            return;
        }

        new Thread(() -> {
            try {
                // 再次检查权限，避免启动后被用户撤销
                if (!hasRecordAudioPermission()) {
                    throw new SecurityException("录音权限被撤销");
                }
                audioRecord.startRecording();
                short[] audioBuffer = new short[MODEL_INPUT_LENGTH];

                while (isListening && hasRecordAudioPermission()) { // 循环中持续检查权限
                    int read = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    if (read <= 0) continue;

                    // 👇 YAMNet教程关键：归一化到 [-1, 1]
                    float[] inputWaveform = normalizeAudio(audioBuffer, read);

                    // 👇 模型推理（对应教程 scores, embeddings, spectrogram = model(waveform)）
                    float[][] scores = new float[1][521];
                    tflite.run(inputWaveform, scores);

                    // 👇 取最高分类别（对应教程 infered_class = class_names[...]）
                    int topClassIndex = getTopClassIndex(scores[0]);
                    String label = classNames != null && topClassIndex < classNames.size()
                            ? classNames.get(topClassIndex)
                            : "未知声音";
                    float confidence = scores[0][topClassIndex];
                    float intensity = calculateIntensity(audioBuffer, read);

                    // 回调结果（直接用你的DetectionItem）
                    postResult(label, confidence, intensity);

                    Thread.sleep(DETECT_INTERVAL);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "录音权限异常，停止识别", e);
                if (listener != null) {
                    mainHandler.post(() -> listener.onError("录音权限异常，识别已停止"));
                }
            } catch (Exception e) {
                Log.e(TAG, "音频识别线程异常", e);
            } finally {
                // 确保资源释放
                stopListening();
            }
        }).start();
    }

    // 音频归一化（严格对齐YAMNet教程：wav_data / tf.int16.max）
    private float[] normalizeAudio(short[] buffer, int length) {
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = buffer[i] / 32768.0f; // 对应Python tf.int16.max
        }
        return result;
    }

    // 获取最高分类别索引
    private int getTopClassIndex(float[] scores) {
        int maxIndex = 0;
        float maxScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    // 计算声音强度
    private float calculateIntensity(short[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) sum += Math.abs(buffer[i]);
        return (float) sum / length / 32768.0f;
    }

    // 主线程回调
    private void postResult(String label, float confidence, float intensity) {
        mainHandler.post(() -> {
            if (listener != null) {
                DetectionItem item = new DetectionItem(
                        System.currentTimeMillis(), label, confidence, intensity, false);
                listener.onDetected(item);
            }
        });
    }

    // 停止并释放资源
    public void stopListening() {
        isListening = false;
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
    }
}