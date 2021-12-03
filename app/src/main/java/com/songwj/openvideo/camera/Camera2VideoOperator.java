package com.songwj.openvideo.camera;

import android.hardware.camera2.CaptureRequest;

public class Camera2VideoOperator extends Camera2Operator {

    public Camera2VideoOperator() {
        TAG = "Camera2VideoOperater";
    }

    @Override
    protected void sendPreviewRequest() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        if (builder != null) {
            sendRepeatingRequest(getPreviewRequest(builder), null, cameraHandler);
        }
    }
}
