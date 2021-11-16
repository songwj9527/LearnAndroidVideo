package com.songwj.openvideo.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Camera1Manager implements Camera.PreviewCallback  {

    private Camera1Manager() {

    }
    static private class Singleton {
        static private Camera1Manager instance = new Camera1Manager();
    }

    public static Camera1Manager getInstance() {
        return Singleton.instance;
    }

    enum SurfaceMode {
        NONE,
        SURFACE_HOLDER,
        SURFACE_TEXTURE
    }

    private Activity activity = null;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private Camera camera = null;
    private Object previewSurface = null;
    private SurfaceMode surfaceMode = SurfaceMode.NONE;

    private Camera.Size cameraSize = null;
    private int cameraOrientation = 0;
    private int displayOrientation = 0;
    private byte[] previewBuffer = null;
    private PreviewFrameCallback previewCallback = null;
    private Camera.AutoFocusCallback autoFocusCallback = null;
    private Camera.AutoFocusMoveCallback autoFocusMoveCallback = null;

    public void bindActivity(Activity activity) {
        this.activity = activity;
    }

    public Camera.Size getCameraSize() {
        return cameraSize;
    }

    public void setPreviewCallback(PreviewFrameCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    public void setPreviewHolder(SurfaceHolder surfaceHolder) {
        this.previewSurface = surfaceHolder;
        this.surfaceMode = SurfaceMode.SURFACE_HOLDER;
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        this.previewSurface = surfaceTexture;
        this.surfaceMode = SurfaceMode.SURFACE_TEXTURE;
    }

    public boolean openCamera(int cameraId) {
        boolean result = false;
        try{
            // 打开摄像头
            camera = Camera.open(cameraId);
            Camera.Parameters parameters = camera.getParameters();
            cameraSize = parameters.getPreviewSize();
            // 设置相机捕捉的帧为NV21
            parameters.setPreviewFormat(ImageFormat.NV21);
            // 设置YUV预览图像的像素
            parameters.setPictureSize(cameraSize.width, cameraSize.height);
//            // 开启闪光灯
//            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            parameters.setPreviewSize(cameraSize.width, cameraSize.height);
            camera.setParameters(parameters);
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            cameraOrientation = info.orientation;
            if (activity != null) {
                displayOrientation = getCameraDisplayOrientation(activity, info);
                camera.setDisplayOrientation(displayOrientation);
            } else {
                displayOrientation = 90;
                camera.setDisplayOrientation(90);
            }

            switch(surfaceMode) {
                case SURFACE_HOLDER:
                    camera.setPreviewDisplay((SurfaceHolder) previewSurface);
                    break;
                case SURFACE_TEXTURE:
                    camera.setPreviewTexture((SurfaceTexture) previewSurface);
                    break;
                default:
                    break;
            }
            previewBuffer = new byte[cameraSize.width * cameraSize.height * 3 / 2];
            camera.addCallbackBuffer(previewBuffer);
            camera.setPreviewCallbackWithBuffer(this);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        }
        return result;
    }

    public boolean openCamera() {
        return openCamera(cameraId);
    }

    public void startPreview(){
        if (openCamera(cameraId)) {
            camera.startPreview();
        }
    }

    public void pausePreview() {
        if(camera != null){
            camera.stopPreview();
        }
    }

    public void resumePreview() {
        if(camera != null){
            camera.startPreview();
        }
    }

    public void stopPreview() {
        if(camera != null){
            camera.setPreviewCallbackWithBuffer(null);
            camera.stopPreview();
            camera.release();
            camera = null;
            cameraSize = null;
            previewBuffer = null;
            displayOrientation = 0;
        }
    }

    public void release() {
        stopPreview();
        surfaceMode = SurfaceMode.NONE;
        previewSurface = null;
        previewCallback = null;
        autoFocusCallback  = null;
        autoFocusMoveCallback = null;
        activity = null;
        displayOrientation = 0;
    }

    public void switchCamera() {
        int tempCameraId = (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        stopPreview();
        cameraId = tempCameraId;
        startPreview();
    }

    public int getCameraOrientation() {
        return cameraOrientation;
    }

    public int getCameraDisplayOrientation() {
        return displayOrientation;
    }

    public void updatePreviewHolder(SurfaceHolder surfaceHolder) {
        if (previewSurface != surfaceHolder) {
            this.previewSurface = surfaceHolder;
            this.surfaceMode = SurfaceMode.SURFACE_HOLDER;
            if (camera != null) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updatePreviewTexture(SurfaceTexture surfaceTexture) {
        if (this.previewSurface != surfaceTexture) {
            this.previewSurface = surfaceTexture;
            this.surfaceMode = SurfaceMode.SURFACE_TEXTURE;
            if (camera != null) {
                try {
                    camera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        previewBuffer = data;
        if (previewCallback != null) {
            previewCallback.onPreviewFrame(data, camera);
        }
        camera.addCallbackBuffer(data);
    }

    public void capture(byte[] data, String capturePath) {
        if (TextUtils.isEmpty(capturePath) || data == null || camera == null) {
            return;
        }
        byte[] dest = new byte[cameraSize.width * cameraSize.height * 3 / 2];
        // 默认摄像头图像传感器的坐标系（图像）逆时针90度，才是屏幕正常显示的坐标（图像）
        CameraUtils.nv21RotateTo270(data, dest, cameraSize.width, cameraSize.height);
//        byte[] nv12 = CameraUtils.nv21toNV12(data);
//        CameraUtils.portraitData2Raw(nv12, dest, cameraSize.width, cameraSize.height);
        File captureFile = new File(capturePath);
        if (!captureFile.exists()) {
            try {
                File old = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/capture_old_" + System.currentTimeMillis() + ".jpeg");
                old.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(old);
                YuvImage image = new YuvImage(
                        data,
                        ImageFormat.NV21,
                        cameraSize.width,
                        cameraSize.height,
                        null); //将NV21 data保存成YuvImage
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);

                captureFile.createNewFile();
                fileOutputStream = new FileOutputStream(captureFile);
                image = new YuvImage(
                        dest,
                        ImageFormat.NV21,
                        cameraSize.height,
                        cameraSize.width,
                        null); //将NV21 data保存成YuvImage
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getCameraDisplayOrientation(Activity activity, Camera.CameraInfo cameraInfo) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Log.e("Camera1Manager", "Window Default Display Rotation: " + rotation);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
        }
        int result;
        Log.e("Camera1Manager", "Camera Id: " + cameraInfo.facing);
        Log.e("Camera1Manager", "Camera Rotation: " + cameraInfo.orientation);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        Log.e("Camera1Manager", "Camera Display Rotation: " + result);
        return result;
    }

    public Camera.Size getLargePictureSize(Camera camera){
        if(camera != null){
            List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                float scale = (float)(sizes.get(i).height) / sizes.get(i).width;
                if(temp.width < sizes.get(i).width && scale < 0.6f && scale > 0.5f)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    public Camera.Size getLargePreviewSize(Camera camera){
        if(camera != null){
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            Camera.Size temp = sizes.get(0);
            Log.e("getLargePreviewSize", "0 , width = "+temp.width+", height = "+temp.height);
            for(int i = 1;i < sizes.size();i ++){
                Log.e("getLargePreviewSize", i+" , width = "+sizes.get(i).width+", height = "+sizes.get(i).height);
                if(temp.width < sizes.get(i).width)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    public Camera.Size getPropPreviewSize(List<Camera.Size> list, float th, int minWidth){
        CameraSizeComparator sizeComparator = new CameraSizeComparator();
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Camera.Size s:list){
            if((s.width >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }
    public Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth){
        CameraSizeComparator sizeComparator = new CameraSizeComparator();
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Camera.Size s:list){
            if((s.width >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;//如果没找到，就选最小的size
        }
        return list.get(i);
    }

    public boolean equalRate(Camera.Size s, float rate){
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.03)
        {
            return true;
        }
        else{
            return false;
        }
    }

    public  class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width > rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }

    }

    public interface PreviewFrameCallback {
        public void onPreviewFrame(byte[] data, Camera camera);
    }
}
