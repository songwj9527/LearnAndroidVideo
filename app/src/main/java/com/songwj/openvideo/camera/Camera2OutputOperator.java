package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Log;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

public class Camera2OutputOperator extends Camera2Operator {

    public Camera2OutputOperator() {
        TAG = "Camera2OutputOperater";
        imageFormat = ImageFormat.YUV_420_888;
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (this.surfaceTexture != surfaceTexture) {
            this.surfaceTexture = surfaceTexture;
            synchronized (stateLock) {
                if (state > STATE_OPENIND) {
                    createPreviewSession();
                    if (state < STATE_DEVICE_PREVIEW) {
                        state = STATE_DEVICE_PREVIEW;
                    }
                }
            }
        }
    }

    @Override
    protected void createPreviewSession() {
        // 设置预览的宽高
        if (cameraDevice != null
                && state > STATE_OPENIND) {
            releasePreviewSession();

            List<Surface> surfaces;
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                surface = new Surface(surfaceTexture);
                surfaces = Arrays.asList(surface, imageReader.getSurface());
            } else {
                surfaces = Arrays.asList(imageReader.getSurface());
            }
            try {
                //该对象负责管理处理预览请求和拍照请求
                cameraDevice.createCaptureSession(
                        surfaces,
                        previewSessionStateCallback,
                        cameraHandler
                );
                Log.d(TAG, "createPreviewSession()");
            } catch (CameraAccessException e) {
                e.printStackTrace();
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
                if (imageReader != null) {
                    previewBuilder.addTarget(imageReader.getSurface());
                }
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return previewBuilder;
    }

    @Override
    protected void processPreCapture(CaptureResult result) {

    }
}
