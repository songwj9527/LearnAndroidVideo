package com.songwj.openvideo.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import androidx.annotation.NonNull;

public class Camera2PictureOperator extends Camera2Operator {
    private int latestAfState = -1;

    public Camera2PictureOperator() {
        TAG = "Camera2PictureOperator";
    }

    @Override
    protected void sendPreviewRequest() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        if (builder != null) {
            sendRepeatingRequest(getPreviewRequest(builder), previewCaptureCallback, cameraHandler);
        }
    }

    private CameraCaptureSession.CaptureCallback previewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            if (cameraDevice == null) {
                return;
            }
            updateAfState(partialResult);
            processPreCapture(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (cameraDevice == null) {
                return;
            }
            updateAfState(result);
            processPreCapture(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.w(TAG, "Preview onCaptureFailed reason:" + failure.getReason());
            if (cameraDevice == null) {
                return;
            }
        }
    };

    private void updateAfState(CaptureResult result) {
        Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
        if (state != null && latestAfState != state) {
            latestAfState = state;
            Log.d(TAG, "updateAfState:" + state);
        }
    }

    private void processPreCapture(CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
    }
}
