package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CaptureRequest;

public class Camera2OutputOperator extends Camera2Operator {

    public Camera2OutputOperator() {
        TAG = "Camera2OutputOperater";
        imageFormat = ImageFormat.YUV_420_888;
    }

    @Override
    protected void sendPreviewRequest() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        if (builder != null) {
            sendRepeatingRequest(getPreviewRequest(builder), null, cameraHandler);
        }
    }
}
