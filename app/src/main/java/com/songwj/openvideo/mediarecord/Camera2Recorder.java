package com.songwj.openvideo.mediarecord;

import android.text.TextUtils;
import android.util.Log;

import com.songwj.openvideo.audio.AudioCapture;
import com.songwj.openvideo.mediacodec.encoder.AudioEncoder;
import com.songwj.openvideo.mediacodec.encoder.VideoEncoder;
import com.songwj.openvideo.mediacodec.muxer.MMuxer;

public class Camera2Recorder implements MMuxer.IMuxerStateListener, AudioCapture.OnAudioCaptureListener {
    private String filePath = "";
    private int videoWidth = 0;
    private int videoHeight = 0;

    private MMuxer mmuxer = null;

    private VideoEncoder videoEncoder = null;
    private boolean isVideoEncoderPrepared = false;

    private AudioEncoder audioEncoder = null;
    private boolean isAudioEncoderPrepared = false;
    private AudioCapture audioCapture = null;

    private boolean isPrepared = false;
    private volatile boolean isStarted = false;

    public Camera2Recorder(String filePath, int videoWidth, int videoHeight) {
        this.filePath = filePath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    synchronized public boolean start() {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        if (isStarted) {
            return true;
        }
        Log.e("Camera2Recorder", "filePath: " + filePath + "\n"
                + "videoWidth: " + videoWidth + ", videoHeight: " + videoHeight);
        try {
            mmuxer = new MMuxer(filePath);
            mmuxer.setStateListener(this);
            audioEncoder = new AudioEncoder(mmuxer);
            audioEncoder.start();
            audioCapture = new AudioCapture();
            audioCapture.setOnAudioCaptureListener(this);
            audioCapture.start();
            videoEncoder = new VideoEncoder(mmuxer, videoWidth, videoHeight, true);
            videoEncoder.start();
            isStarted = true;
        } catch (Exception e) {
            e.printStackTrace();
            if (mmuxer != null) {
                mmuxer.setStateListener(null);
                mmuxer.stopMuxer();
                mmuxer = null;
            }
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
                videoEncoder = null;
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
                audioEncoder = null;
            }
            if (audioCapture != null) {
                audioCapture.setOnAudioCaptureListener(null);
                audioCapture.stop();
                audioEncoder = null;
            }
        }
        return isStarted;
    }

    synchronized public void stop() {
        if (isStarted) {
            if (mmuxer != null) {
                mmuxer.stopMuxerPrepare();
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
            }
            if (audioCapture != null) {
                audioCapture.stop();
                audioCapture.setOnAudioCaptureListener(null);
            }
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
            }
            videoEncoder = null;
            audioCapture = null;
            audioEncoder = null;
            mmuxer = null;
            isStarted = false;
        }
    }

    @Override
    public void onMuxerStart() {
        isPrepared = true;
        if (videoEncoder != null) {
            videoEncoder.startEncode();
        }
        if (audioEncoder != null) {
            audioEncoder.startEncode();
        }
    }

    @Override
    public void onMuxerFinish() {
        if (isStarted) {
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
                videoEncoder = null;
            }
            if (audioCapture != null) {
                audioCapture.setOnAudioCaptureListener(null);
                audioCapture.stop();
                audioCapture = null;
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
                audioEncoder = null;
            }
            if (mmuxer != null) {
                mmuxer = null;
            }
            isStarted = false;
        }
    }

    @Override
    public void onAudioFrameUpdate(byte[] data, int dataSize) {
//        if (isStarted && isPrepared) {
        if (isStarted) {
            Log.i("Camera2Recorder", "onAudioFrameUpdate()");
            if (audioEncoder != null) {
                audioEncoder.dequeueFrame(data, dataSize);
            }
        }
    }

    public void onVideoFrameUpdate(byte[] data) {
//        if (isStarted && isPrepared) {
        if (isStarted) {
            Log.i("Camera2Recorder", "onVideoFrameUpdate()");
            if (videoEncoder != null) {
                videoEncoder.dequeueFrame(data, computePresentationTime(frameIndex++, 30));
            }
        }
    }

    private long frameIndex = 0L;
    private long computePresentationTime(long frameIndex, int frameRate) {
        //帧率是frameRate  132是偏移量 frameIndex单位是微秒(us)
        return 132 + frameIndex * 1000000 / frameRate;
    }
}
