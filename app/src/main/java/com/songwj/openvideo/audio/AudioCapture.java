package com.songwj.openvideo.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

public class AudioCapture implements Runnable {
    private String TAG = "AudioRecorder";
    //默认参数
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIGS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    private AudioRecord audioRecord = null;
    private int bufferSize = 0;

    private Thread audioRecordThread = null;
    private volatile boolean isRunning = false;

    private OnAudioCaptureListener onAudioCaptureListener = null;

    public AudioCapture() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIGS, AUDIO_FORMAT);
    }

    public void start() {
        if (isRunning) {
            Log.e(TAG, "音频录制已经开启");
            return;
        }
        isRunning = true;
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            isRunning = false;
            Log.e(TAG, "无效参数");
            return;
        }
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIGS, AUDIO_FORMAT, bufferSize);
        audioRecordThread = new Thread(this);
        audioRecordThread.start();
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            if (audioRecordThread != null) {
                audioRecordThread.interrupt();
                audioRecordThread = null;
            }
        }
    }

    @Override
    public void run() {
        try {
            audioRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            isRunning = false;
            release();
            return;
        }
        try {
            while (isRunning && !Thread.interrupted()) {
                byte[] buffer = new byte[bufferSize];
                int readSize = audioRecord.read(buffer, 0, bufferSize);
                if (readSize > 0) {
                    if (onAudioCaptureListener != null) {
                        onAudioCaptureListener.onAudioFrameUpdate(buffer, readSize);
                    }
                }
                //延迟写入 SystemClock  --  Android专用
                SystemClock.sleep(9);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isRunning = false;
            audioRecord.stop();
            release();
        }
    }

    private void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    public void setOnAudioCaptureListener(OnAudioCaptureListener onAudioCaptureListener) {
        this.onAudioCaptureListener = onAudioCaptureListener;
    }

    public interface OnAudioCaptureListener {
        /**
         * 音频采集回调数据源
         * @param data ：音频采集回调数据源
         * @param dataSize :每次读取数据的大小
         */
        void onAudioFrameUpdate(byte[] data, int dataSize);
    }
}
