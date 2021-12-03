package com.songwj.openvideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.songwj.openvideo.MyApplication;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Camera2 中主要的API类：
 *
 * CameraManager类 : 摄像头管理类，用于检测、打开系统摄像头，通过getCameraCharacteristics(cameraId)可以获取摄像头特征。
 *
 * CameraCharacteristics类：相机特性类，例如，是否支持自动调焦，是否支持zoom，是否支持闪光灯一系列特征。
 *       LENS_FACING: 获取摄像头方向。LENS_FACING_FRONT是前摄像头，LENS_FACING_BACK是后摄像头，LENS_FACING_EXTERNAL是外接摄像头。
 *       SENSOR_ORIENTATION：获取摄像头拍照的方向。
 *       FLASH_INFO_AVAILABLE：获取是否支持闪光灯。
 *       SCALER_AVAILABLE_MAX_DIGITAL_ZOOM：获取最大的数字调焦值，也就是zoom最大值。
 *       LENS_INFO_MINIMUM_FOCUS_DISTANCE：获取最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
 *       INFO_SUPPORTED_HARDWARE_LEVEL：获取摄像头支持某些特性的程度。
 *
 * CameraDevice类： 相机设备,类似早期的camera类。
 *
 * CameraCaptureSession类：用于创建预览、拍照的Session类。通过它的setRepeatingRequest()方法控制预览界面 , 通过它的capture()方法控制拍照动作或者录像动作。

 * CameraRequest类：一次捕获的请求，可以设置一些列的参数，用于控制预览和拍照参数，例如：对焦模式，曝光模式，zoom参数等等。
 */
public abstract class Camera2Operator {
    protected String TAG = "Camera2Operater";

    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARED = 1;
    public static final int STATE_DEVICE_OPENED = 2;
    public static final int STATE_DEVICE_PREVIEW = 3;
    public static final int STATE_WAITING_LOCK = 4;
    public static final int STATE_WAITING_PRE_CAPTURE = 5;
    public static final int STATE_WAITING_NON_PRE_CAPTURE = 6;
    public static final int STATE_PICTURE_TAKEN = 7;

    protected Object stateLock = new Object();
    protected volatile int state = STATE_IDLE;

    protected CameraManager cameraManager = null; // 摄像头管理器
    protected CameraCharacteristics characteristics = null; // 相机特性类
    protected int cameraId = CameraCharacteristics.LENS_FACING_BACK; // 摄像头id
    protected Integer cameraOrientation = null; // 摄像头拍照的方向
    protected Size previewSize = null; // 最合适的预览尺寸
    protected Range<Integer> previewFpsRange = null; // 预览帧率

    protected boolean isFlashSupported = false; // 是否支持闪光灯
    protected boolean isAutoFocusSupported = false; // 是否支持自动对焦
    protected Float maxDigitalZoom = 0.0f; // 最大的数字调焦值
    protected Float minFocusDistance = 0.0f; // 最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。

    protected Rect cameraRect = null; // 摄像头坐标区域（为点击屏幕对焦准备）

    protected Handler cameraHandler = null;
    protected HandlerThread cameraHandlerThread = null;

    protected CameraDevice cameraDevice = null;

    protected SurfaceTexture surfaceTexture = null;
    protected Surface surface = null;
    protected CaptureRequest.Builder previewBuilder = null;
    protected CameraCaptureSession previewSession = null;

    protected int imageFormat = ImageFormat.YUV_420_888; // ImageFormat.NV21;
    protected ImageReader imageReader = null;

    public Size getPreviewSize() {
        if (previewSize == null) {
            CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
                // 最合适的预览尺寸
                return Camera2Utils.getBestPreviewSize(characteristics, new Size(1920, 1080));
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return previewSize;
    }

    public Size getSwitchPreviewSize() {
        int tempId = (cameraId == CameraCharacteristics.LENS_FACING_BACK ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK);
        CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + tempId);
            // 最合适的预览尺寸
            return Camera2Utils.getBestPreviewSize(characteristics, new Size(1920, 1080));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return new Size(1920, 1080);
    }

    public int getCameraOrientation() {
        if (cameraOrientation == null) {
            CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
                // 摄像头拍照的方向(旋转多少角度拍照的)
                Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (orientation == null) {
                    orientation = 0;
                }
                return orientation;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return cameraOrientation == null ? 0 : cameraOrientation;
    }

    public int getSwitchCameraOrientation() {
        int tempId = (cameraId == CameraCharacteristics.LENS_FACING_BACK ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK);
        CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + tempId);
            // 摄像头拍照的方向(旋转多少角度拍照的)
            Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (orientation == null) {
                orientation = 0;
            }
            return orientation;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (this.surfaceTexture != surfaceTexture) {
            this.surfaceTexture = surfaceTexture;
            synchronized (stateLock) {
                if (state > STATE_PREPARED) {
                    createPreviewSession();
                    if (state < STATE_DEVICE_PREVIEW) {
                        state = STATE_DEVICE_PREVIEW;
                    }
                }
            }
        }
    }

    private boolean openCamera(int cameraId) {
        boolean ret = false;
        cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            return ret;
        }
        synchronized (stateLock) {
            if (state == STATE_IDLE) {
                state = STATE_PREPARED;
            }
        }
        if (state == STATE_PREPARED) {
            try {
                characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
                if (characteristics != null) {
                    // 摄像头拍照的方向(旋转多少角度拍照的)
                    cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    if (cameraOrientation == null) {
                        cameraOrientation = 0;
                    }
                    Log.d(TAG, "cameraOrientation: "+ cameraOrientation);

                    // 最合适的预览尺寸
                    previewSize = Camera2Utils.getBestPreviewSize(characteristics, new Size(1920, 1080));
                    Log.d(TAG, "previewSize: "+ previewSize.getWidth() + ", " +previewSize.getHeight());

                    // 摄像头预览帧率
                    previewFpsRange = Camera2Utils.getBestPreviewFps(characteristics, 30);
                    if (previewFpsRange != null) {
                        Log.d(TAG, "preview fps range: " + previewFpsRange.toString());
                    }

                    // 获取硬件层面支持的Camera2功能等级
                    int hardwareSupportedLevel = Camera2Utils.getHardwareSupportedLevel(characteristics);

                    // 是否支持闪光灯
                    isFlashSupported = Camera2Utils.isFlashSupported(characteristics);
                    // 是否支持自动对焦
                    isAutoFocusSupported = Camera2Utils.checkAutoFocus(characteristics);
                    // 获取最大的数字调焦值，也就是zoom最大值。
                    maxDigitalZoom = Camera2Utils.getMaxDigitalZoom(characteristics);
                    if (maxDigitalZoom == null) {
                        maxDigitalZoom = 0.0f;
                    }
                    // 获取最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
                    minFocusDistance = Camera2Utils.getMinimumFocusDistance(characteristics);
                    if (minFocusDistance == null) {
                        minFocusDistance = 0.0f;
                    }

                    // 获取摄像头坐标区域（为点击屏幕对焦准备）
                    cameraRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                    cameraHandlerThread = new HandlerThread("camera2-manager");
                    cameraHandlerThread.start();
                    cameraHandler = new Handler(cameraHandlerThread.getLooper());

                    createImageReader();

                    cameraManager.openCamera(("" + cameraId), deviceStateCallback, cameraHandler);
                    ret = true;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
                synchronized (stateLock) {
                    releaseHander();
                    releaseImageReader();

                    state = STATE_IDLE;
                }
            }
        }
        return ret;
    }

    private void createImageReader() {
        releaseImageReader();
        imageReader = ImageReader.newInstance(
                previewSize.getWidth(),
                previewSize.getHeight(),
                imageFormat,
                2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, cameraHandler);
    }

    public boolean openCamera() {
        return openCamera(cameraId);
    }

    public void startPreview() {
        synchronized (stateLock) {
            if (state > STATE_PREPARED) {
                releasePreviewSession();
                createPreviewSession();
                if (state < STATE_DEVICE_PREVIEW) {
                    state = STATE_DEVICE_PREVIEW;
                }
            }
        }
    }

    public void stopPreview() {
        synchronized (stateLock) {
            if (state > STATE_PREPARED) {
                releasePreviewSession();
            }
        }
    }

    public void closeCamera() {
        synchronized (stateLock) {
            if (state != STATE_IDLE) {
                releasePreviewSession();
                releaseImageReader();
                releaseDevice();
                releaseHander();

                characteristics = null;
                cameraOrientation = null;
                previewSize = null;
                previewFpsRange = null;

                isFlashSupported = false;
                isAutoFocusSupported = false;
                maxDigitalZoom = 0.0f;
                minFocusDistance = 0.0f;
                cameraRect = null;

                state = STATE_IDLE;
            }
        }
    }

    public boolean swichCamera() {
        boolean ret = false;
        int tempId = (cameraId == CameraCharacteristics.LENS_FACING_BACK ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK);
        Log.e(TAG, "swichCamera() 0");
        closeCamera();
        Log.e(TAG, "swichCamera() 1");
        if (openCamera(tempId)) {
            cameraId = tempId;
            ret = true;
            Log.e(TAG, "swichCamera() 2");
        }
        return ret;
    }

    public void releaseCamera() {
        closeCamera();
        surfaceTexture = null;
        outputListener = null;
        Log.e(TAG, "releaseCamera()");
    }

    private void releaseHander() {
        if (cameraHandler != null) {
            cameraHandler.removeCallbacksAndMessages(null);
            cameraHandlerThread.getLooper().quitSafely();
            cameraHandlerThread = null;
            cameraHandler = null;
        }
    }

    private void releaseImageReader() {
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
    }

    private void releasePreviewSession() {
        if (previewSession != null) {
            previewSession.close();
            previewSession = null;
            surface = null;
            previewBuilder = null;
        }
    }

    private void releaseDevice() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, " CameraDevice onOpened():" + camera.getId());
            synchronized (stateLock) {
                if (state < STATE_PREPARED) {
                    return;
                }
                state = STATE_DEVICE_OPENED;
                cameraDevice = camera;
                createPreviewSession();
                if (state < STATE_DEVICE_PREVIEW) {
                    state = STATE_DEVICE_PREVIEW;
                }
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " CameraDevice onDisconnected():" + camera.getId());
            closeCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, " CameraDevice onError():" + camera.getId() + ", " + error);
            closeCamera();
        }
    };

    private void createPreviewSession() {
        // 设置预览的宽高
        if (cameraDevice != null
                && surfaceTexture != null
                && state > STATE_PREPARED) {
            releasePreviewSession();

            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            surface = new Surface(surfaceTexture);
            try {
                //该对象负责管理处理预览请求和拍照请求
                cameraDevice.createCaptureSession(
                        Arrays.asList(surface, imageReader.getSurface()),
                        previewSessionStateCallback,
                        cameraHandler
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraCaptureSession.StateCallback previewSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, " session onConfigured()");
            if (cameraDevice == null) {
                return;
            }
//            cameraSession = session;
//            sendPreviewRequest();
            synchronized (stateLock) {
                if (state > STATE_PREPARED) {
                    previewSession = session;
                    sendPreviewRequest();
                }
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, " session onConfigureFailed()");
        }

    };

    abstract protected void sendPreviewRequest();

    protected void sendRepeatingRequest(CaptureRequest request,
                                      CameraCaptureSession.CaptureCallback callback,
                                      Handler handler) {
        try {
            previewSession.setRepeatingRequest(request, callback, handler);
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "send repeating request error:" + e.getMessage());
        }
    }

    protected CaptureRequest.Builder getPreviewBuilder() {
        // 创建预览需要的CaptureRequest.Builder
        if (previewBuilder == null) {
            try {
                previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                if (surface != null) {
                    previewBuilder.addTarget(surface);
                }
                if (imageReader != null) {
                    previewBuilder.addTarget(imageReader.getSurface());
                }
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return previewBuilder;
    }

    protected CaptureRequest getPreviewRequest(CaptureRequest.Builder builder) {
        // 为相机预览设置连续对焦。
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        int antiBMode = getValidAntiBandingMode(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);

        // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
        if (previewFpsRange != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, previewFpsRange);
        }

        return builder.build();
    }

    private int getValidAFMode(int targetMode) {
        int[] allAFMode = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int mode : allAFMode) {
            Log.i(TAG, "support af mode:" + mode);
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support af mode:" + targetMode + " use mode:" + allAFMode[0]);
        return allAFMode[0];
    }

    private int getValidAntiBandingMode(int targetMode) {
        int[] allABMode = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
        for (int mode : allABMode) {
            Log.i(TAG, "support ae mode:" + mode);
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support anti banding mode:" + targetMode
                + " use mode:" + allABMode[0]);
        return allABMode[0];
    }


    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Log.d(TAG,"onImageAvailable(): " + imageReader.getWidth() + ", " + imageReader.getHeight());
                if (outputListener != null) {
                    Image.Plane[] planes = image.getPlanes();
                    if (planes != null) {
                        try {
//                            if (y == null) {
//                                y = new byte[(planes[0].getBuffer().limit() - planes[0].getBuffer().position())];
//                            }
//                            if (u == null) {
//                                u = new byte[(planes[1].getBuffer().limit() - planes[1].getBuffer().position())];
//                            }
//                            if (v == null) {
//                                v = new byte[(planes[2].getBuffer().limit() - planes[2].getBuffer().position())];
//                            }
                            int totalSize = 0;
                            for (Image.Plane plane : planes) {
                                totalSize += plane.getBuffer().remaining();
                            }
                            ByteBuffer totalBuffer = ByteBuffer.allocate(totalSize);
                            for (Image.Plane plane : image.getPlanes()) {
                                totalBuffer.put(plane.getBuffer());
                            }
                            if (outputListener != null) {
                                outputListener.onImageAvailable(totalBuffer.array(), new Size(image.getWidth(), image.getHeight()));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                image.close();
            }
        }
    };

    public void setOnImageAvailableOutputListener(OnImageAvailableOutputListener outputListener) {
        this.outputListener = outputListener;
    }

    protected OnImageAvailableOutputListener outputListener = null;
    public interface OnImageAvailableOutputListener {
        public void onImageAvailable(byte[] yuv, Size size);
    }

    protected byte[] getByteFromReader(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        int totalSize = 0;
        for (Image.Plane plane : image.getPlanes()) {
            totalSize += plane.getBuffer().remaining();
        }
        ByteBuffer totalBuffer = ByteBuffer.allocate(totalSize);
        for (Image.Plane plane : image.getPlanes()) {
            totalBuffer.put(plane.getBuffer());
        }
        image.close();
        return totalBuffer.array();
    }
}
