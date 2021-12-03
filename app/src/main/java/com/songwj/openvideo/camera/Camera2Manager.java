package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCharacteristics;
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

    synchronized Mode getMode() {
        return mode;
    }

    synchronized public void switchMode(Mode mode) {
        this.mode = mode;
        if (operator != null) {
            operator.releaseCamera();
            operator = null;
        }
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

    synchronized public boolean openCamera() {
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
        if (operator != null) {
            return operator.openCamera();
        }
        return false;
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

    synchronized public void releaseCamera() {
        if (operator != null) {
            operator.releaseCamera();
            operator = null;
        }
    }

    public Size getPreviewSize() {
        if (operator != null) {
            return operator.getPreviewSize();
        }
        return null;
    }

    public Size getSwitchPreviewSize() {
        if (operator != null) {
            return operator.getSwitchPreviewSize();
        }
        return null;
    }

    public int getCameraOrientation() {
        if (operator != null) {
            return operator.getCameraOrientation();
        }
        return 0;
    }

    public int getSwitchCameraOrientation() {
        if (operator != null) {
            return operator.getSwitchCameraOrientation();
        }
        return 0;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (operator != null) {
            operator.setSurfaceTexture(surfaceTexture);
        }
    }

    synchronized public void setOnImageAvailableOutputListener(Camera2Operator.OnImageAvailableOutputListener listener) {
        if (operator != null && mode == Mode.OUTPUT_YUV420) {
            operator.setOnImageAvailableOutputListener(listener);
        }
    }

//    synchronized public void takePictureYUV420(String capturePath, byte[] y, byte[] u, byte[] v, int stride, Size size) {
//        if (TextUtils.isEmpty(capturePath)
//                || y == null
//                || u == null
//                || v == null
//                || size == null) {
//            return;
//        }
//        if ((state == Camera2State.DEVICE_OPENED
//                || state == Camera2State.DEVICE_PREVIEW)
//                && cameraHandler != null) {
//            byte[] nv21 = new byte[size.getWidth() * size.getHeight() * 3 / 2];
//            // YUV转换成NV21
//            CameraFrameUtils.yuvToNv21(y, u, v, nv21, stride, size.getHeight());
//            cameraHandler.post(new CaptureYUV420Runnable(capturePath, nv21, size, cameraId, cameraOrientation))
//        }
//    }

    class CaptureYUV420Runnable implements Runnable {
        private int cameraId;
        private int cameraOrientation;
        private byte[] nv21;
        private Size size;
        private String capturePath;

        public CaptureYUV420Runnable(String capturePath, byte[] nv21, Size size, int cameraId, int cameraOrientation) {
            this.capturePath = capturePath;
            this.nv21 = nv21;
            this.size = size;
            this.cameraId = cameraId;
            this.cameraOrientation = cameraOrientation;
        }
        @Override
        public void run() {
            int width = size.getWidth();
            int height = size.getHeight();
            byte[] dest = new byte[width * height * 3 / 2];
            // 默认摄像头图像传感器的坐标系（图像）有旋转角度的，所以想要旋转相应角度，才是屏幕正常显示的坐标（图像）
            CameraFrameUtils.nv21Rotate(nv21, dest, width, height, cameraOrientation);
            if (cameraOrientation == 270 || cameraOrientation == 90) {
                width = size.getHeight();
                height = size.getWidth();
            }
            // 反转镜像
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
