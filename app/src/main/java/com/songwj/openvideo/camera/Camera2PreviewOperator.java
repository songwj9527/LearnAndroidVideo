package com.songwj.openvideo.camera;

import android.hardware.camera2.CaptureRequest;

public class Camera2PreviewOperator extends Camera2Operator {

    public Camera2PreviewOperator() {
        TAG = "Camera2PreviewOperater";
    }

    @Override
    protected void sendPreviewRequest() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        if (builder != null) {
            sendRepeatingRequest(getPreviewRequest(builder), null, cameraHandler);
        }
    }
}
