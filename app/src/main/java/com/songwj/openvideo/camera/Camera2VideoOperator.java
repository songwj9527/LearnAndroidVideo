package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Camera2VideoOperator extends Camera2Operator {
    private volatile boolean isRecording = false;
    private MediaRecorder mediaRecorder = null;
    private File currentRecordFile = null;
    private CaptureRequest.Builder videoBuilder = null;
    private RecordVideoCallback recordVideoCallback = null;

    public Camera2VideoOperator() {
        TAG = "Camera2VideoOperater";
        imageFormat = ImageFormat.JPEG;
    }

    @Override
    protected void createPreviewSession() {
        synchronized (stateLock) {
            if (!isRecording) {
                super.createPreviewSession();
            } else {
                createVideoSession();
            }
        }
    }

    @Override
    protected CaptureRequest.Builder getPreviewBuilder() {
        // 创建预览需要的CaptureRequest.Builder
        if (previewBuilder == null) {
            try {
                previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                if (surface != null) {
                    previewBuilder.addTarget(surface);
                }
                // 设置了此处，imageReader就会一直调用onImageAvailable(ImageReader reader)传递帧数据
//                if (imageReader != null) {
//                    previewBuilder.addTarget(imageReader.getSurface());
//                }
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return previewBuilder;
    }

    @Override
    protected void processPreCapture(CaptureResult result) {

    }

    @Override
    public void closeCamera() {
        stopRecord();
        super.closeCamera();
        videoBuilder = null;
    }

    @Override
    public void releaseCamera() {
        stopRecord();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        super.releaseCamera();
        recordVideoCallback = null;
    }

    public void setRecordVideoCallback(RecordVideoCallback callback) {
        this.recordVideoCallback = callback;
    }

    public void startRecord(String filePath) {
        if (TextUtils.isEmpty(filePath) || (!filePath.endsWith(".mp4") && !filePath.endsWith(".MP4"))) {
            Log.e(TAG, "startRecord(): " + filePath + "为空或者格式错误");
            if (recordVideoCallback != null) {
                recordVideoCallback.onError();
            }
            return;
        }
        synchronized (stateLock) {
            if (state >= STATE_DEVICE_OPENED) {
                createRecorderAndSession(filePath, cameraOrientation);
            } else {
                Log.e(TAG, "startRecord(): state < STATE_DEVICE_OPENED");
                if (recordVideoCallback != null) {
                    recordVideoCallback.onError();
                }
            }
        }
    }

    private void createRecorderAndSession(String filePath, int deviceRotation) {
        if (!createMediaRecorder(filePath, deviceRotation)) {
            Log.e(TAG, "startRecord(): createMediaRecorder 失败");
            if (recordVideoCallback != null) {
                recordVideoCallback.onError();
            }
            return;
        }
        isRecording = true;
        createVideoSession();
    }

    private void createVideoSession() {
        releasePreviewSession();
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            surface = new Surface(surfaceTexture);
            try {
                cameraDevice.createCaptureSession(Arrays.asList(surface, mediaRecorder.getSurface()), videoSessionStateCb, cameraHandler);
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
                Log.e(TAG, "create capture session:" + e.getMessage());
                mediaRecorder.reset();
                isRecording = false;
                if (recordVideoCallback != null) {
                    recordVideoCallback.onError();
                }
            }
        }
    }

    private boolean createMediaRecorder(String filePath, int deviceRotation) {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        currentRecordFile = new File(filePath);
        if (currentRecordFile == null) {
            Log.e(TAG, " get video file failed");
            return false;
        }
        boolean ret = false;
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(currentRecordFile.getAbsolutePath());
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        int rotation = Camera2Utils.getJpgRotation(characteristics, deviceRotation);
        mediaRecorder.setOrientationHint(deviceRotation);
        try {
            mediaRecorder.prepare();
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error prepare video record:" + e.getMessage());
            mediaRecorder.reset();
        }
        return ret;
    }

    public void stopRecord() {
        synchronized (stateLock) {
            if (isRecording) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    currentRecordFile = null;
                    if (recordVideoCallback != null) {
                        recordVideoCallback.onStoped();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "media recorder stop: " + e.getMessage());
                    mediaRecorder.reset();
                    if (currentRecordFile.exists() && currentRecordFile.delete()) {
                        Log.w(TAG, "video file delete success");
                    }
                    currentRecordFile = null;
                    if (recordVideoCallback != null) {
                        recordVideoCallback.onError();
                    }
                }
                isRecording = false;
            }
            createPreviewSession();
        }
    }

    //session callback
    private CameraCaptureSession.StateCallback videoSessionStateCb = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "videoSessionStateCb: " + session.getDevice().getId());
            synchronized (stateLock) {
                if (isRecording) {
                    previewSession = session;
                    sendVideoPreviewRequest();
                    try {
                        mediaRecorder.start();
                        if (recordVideoCallback != null) {
                            recordVideoCallback.onStarted();
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        Log.e(TAG, "start record failed msg:" + e.getMessage());
                        mediaRecorder.reset();
                        if (currentRecordFile.exists() && currentRecordFile.delete()) {
                            Log.w(TAG, "video file delete success");
                        }
                        currentRecordFile = null;
                        isRecording = false;
                        createPreviewSession();
                        if (recordVideoCallback != null) {
                            recordVideoCallback.onError();
                        }
                    }
                }
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "videoSessionStateCb fail id:" + session.getDevice().getId());
        }
    };

    private void sendVideoPreviewRequest() {
        CaptureRequest.Builder builder = getVideoBuilder();
        CaptureRequest request = getPreviewRequest(builder);
        sendRepeatingRequest(request, previewCaptureCallback, cameraHandler);
    }

    private CaptureRequest.Builder getVideoBuilder() {
        try {
            videoBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            videoBuilder.addTarget(surface);
            videoBuilder.addTarget(mediaRecorder.getSurface());
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
        return videoBuilder;
    }
}
