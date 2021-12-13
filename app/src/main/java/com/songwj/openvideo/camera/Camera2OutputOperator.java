package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;

public class Camera2OutputOperator extends Camera2Operator {

    public Camera2OutputOperator() {
        TAG = "Camera2OutputOperater";
        imageFormat = ImageFormat.YUV_420_888;
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
