package com.songwj.openvideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Camera2Manager {
    private final String TAG = "Camera2Manager";

    private Camera2Manager() {

    }

    static private class Singleton {
        static private Camera2Manager instance = new Camera2Manager();
    }

    public static Camera2Manager getInstance() {
        return Singleton.instance;
    }

    public enum Mode {
        PREVIEW,
        TAKE_PICTURE,
        VIDEO_RECORD,
        OUTPUT_YUV420,
    }


    private Mode mode = Mode.PREVIEW;
    private Camera2Operator operator = null;


    private void createOperator() {
        if (operator == null) {
            if (mode == Mode.VIDEO_RECORD) {
                operator = new Camera2VideoOperator();
            } else if (mode == Mode.TAKE_PICTURE) {
                operator = new Camera2PictureOperator();
            } else if (mode == Mode.OUTPUT_YUV420) {
                operator = new Camera2OutputOperator();
            } else {
                operator = new Camera2PreviewOperator();
            }
        }
    }

    synchronized public void switchMode(Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            if (operator != null) {
                operator.releaseCamera();
                operator = null;
            }
        }
        createOperator();
    }

    synchronized Mode getMode() {
        return mode;
    }

    synchronized public boolean openCamera() {
        createOperator();
        if (operator != null) {
            return operator.openCamera();
        }
        return false;
    }

    synchronized public boolean isFrontCamera() {
        if (operator != null) {
            return operator.isFrontCamera();
        }
        return true;
    }

    synchronized public void startPreview() {
        if (operator != null) {
            operator.startPreview();
        }
    }

    synchronized public void stopPreview() {
        if (operator != null) {
            operator.stopPreview();
        }
    }

    synchronized public void closeCamera() {
        if (operator != null) {
            operator.closeCamera();
        }
    }

    synchronized public boolean swichCamera() {
        if (operator != null) {
            return operator.swichCamera();
        }
        return false;
    }

    synchronized public boolean setFlashMode(Camera2Operator.FlashMode flashMode) {
        if (operator != null) {
            return operator.setFlashMode(flashMode);
        }
        return false;
    }

    synchronized public void releaseCamera() {
        if (operator != null) {
            operator.releaseCamera();
            operator = null;
        }
    }

    synchronized public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (operator != null) {
            operator.setSurfaceTexture(surfaceTexture);
        }
    }

    synchronized public void setTouchFocusRegions(MeteringRectangle focusRect, MeteringRectangle meterRect) {
        if (operator != null) {
            operator.setTouchFocusRegions(focusRect, meterRect);
        }
    }

    synchronized public void resetTouchToFocus() {
        if (operator != null) {
            operator.resetTouchToFocus();
        }
    }

    synchronized public void setFocusDistance(int distance) {
        if (operator != null) {
            operator.setFocusDistance(distance);
        }
    }

    synchronized public void setOnImageAvailableOutputListener(Camera2Operator.OnImageAvailableOutputListener outputListener) {
        if (mode == Mode.OUTPUT_YUV420 && operator != null) {
            operator.setOnImageAvailableOutputListener(outputListener);
        }
    }

    synchronized public void setRequestCallback(Camera2Operator.RequestCallback callback) {
        operator.setRequestCallback(callback);
    }

    synchronized public int getCameraId() {
        if (operator != null) {
            return operator.getCameraId();
        }
        return CameraCharacteristics.LENS_FACING_BACK;
    }

    synchronized public int getCameraOrientation() {
        if (operator != null) {
            return operator.getCameraOrientation();
        }
        return 90;
    }

    synchronized public Size getPreviewSize() {
        if (operator != null) {
            return operator.getPreviewSize();
        }
        return null;
    }

    synchronized public void takePicture(String filePath, Camera2Operator.TakePictureCallback callback) {
        if (TextUtils.isEmpty(filePath)
                || operator == null
                || mode != Mode.TAKE_PICTURE) {
            return;
        }
        if (operator instanceof Camera2PictureOperator) {
            ((Camera2PictureOperator) operator).setTakePictureCallback(callback);
            ((Camera2PictureOperator) operator).takePicture(filePath);
        }
    }

    synchronized public void startRecord(String filePath, Camera2Operator.RecordVideoCallback callback) {
        if (TextUtils.isEmpty(filePath)
                || operator == null
                || mode != Mode.VIDEO_RECORD
                || !(operator instanceof Camera2VideoOperator)) {
            return;
        }
        ((Camera2VideoOperator) operator).setRecordVideoCallback(callback);
        ((Camera2VideoOperator) operator).startRecord(filePath);
    }

    synchronized public void stopRecord() {
        if (operator == null
                || mode != Mode.VIDEO_RECORD
                || !(operator instanceof Camera2VideoOperator)) {
            return;
        }
        ((Camera2VideoOperator) operator).stopRecord();
    }

    synchronized public void takePictureYUV420(String capturePath, byte[] y, byte[] u, byte[] v, int stride, Size size, int cameraId, int cameraOrientation) {
        if (TextUtils.isEmpty(capturePath)
                || y == null
                || u == null
                || v == null
                || size == null) {
            return;
        }
        byte[] nv21 = new byte[size.getWidth() * size.getHeight() * 3 / 2];
        // YUV转换成NV21
        CameraFrameUtils.YUV420ToNV21(y, u, v, nv21, stride, size.getHeight());
        new Thread(new CaptureYUV420Runnable(capturePath, nv21, size, cameraId, cameraOrientation)).start();
    }

    synchronized public void takePictureYUV420(String capturePath, byte[] yuv, Size size, int cameraId, int cameraOrientation) {
        new Thread(new CaptureYUV420Runnable(capturePath, yuv, size, cameraId, cameraOrientation)).start();
    }

    synchronized public void takePictureYUV420(Handler handler, CaptureYUV420Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.post(runnable);
    }

    public static class CaptureYUV420Runnable implements Runnable {
        private int cameraId;
        private int cameraOrientation;
        private byte[] yuv;
        private Size size;
        private String capturePath;

        public CaptureYUV420Runnable(String capturePath, byte[] yuv, Size size, int cameraId, int cameraOrientation) {
            this.capturePath = capturePath;
            this.yuv = yuv;
            this.size = size;
            this.cameraId = cameraId;
            this.cameraOrientation = cameraOrientation;
        }
        @Override
        public void run() {
            int width = size.getWidth();
            int height = size.getHeight();
            byte[] nv21 = new byte[width * height * 3 / 2];
            byte[] dest = new byte[width * height * 3 / 2];

            // 将YUV420转换成NV21
            CameraFrameUtils.YUV420ToNV21(yuv, nv21, width, height);

            // 默认摄像头图像传感器的坐标系（图像）有旋转角度的，所以想要旋转相应角度，才是屏幕正常显示的坐标（图像）
            CameraFrameUtils.nv21Rotate(nv21, dest, width, height, cameraOrientation);
            if (cameraOrientation == 270 || cameraOrientation == 90) {
                width = size.getHeight();
                height = size.getWidth();
            }
            // 反转镜像(小米手机CameraCharacteristics.LENS_FACING_BACK对应的是前置摄像头，正常应该判断LENS_FACING_FRONT)
            if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                CameraFrameUtils.nv21Reversed(dest, nv21, width, height);
                dest = nv21;
            }

            File captureFile = new File(capturePath);
            if (!captureFile.exists()) {
                try {
                    captureFile.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(captureFile);
                    YuvImage image = new YuvImage(
                            dest,
                            ImageFormat.NV21,
                            width,
                            height,
                            null); //将NV21 data保存成YuvImage
                    image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
