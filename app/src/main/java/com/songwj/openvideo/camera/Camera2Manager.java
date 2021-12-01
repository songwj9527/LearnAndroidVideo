package com.songwj.openvideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.songwj.openvideo.MyApplication;

import java.util.Arrays;

/**
 * Camera2 中主要的API类：
 *
 * CameraManager类 : 摄像头管理类，用于检测、打开系统摄像头，通过getCameraCharacteristics(cameraId)可以获取摄像头特征。
 *
 * CameraCharacteristics类：相机特性类，例如，是否支持自动调焦，是否支持zoom，是否支持闪光灯一系列特征。
 *
 * CameraDevice类： 相机设备,类似早期的camera类。
 *
 * CameraCaptureSession类：用于创建预览、拍照的Session类。通过它的setRepeatingRequest()方法控制预览界面 , 通过它的capture()方法控制拍照动作或者录像动作。

 * CameraRequest类：一次捕获的请求，可以设置一些列的参数，用于控制预览和拍照参数，例如：对焦模式，曝光模式，zoom参数等等。
 */
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

    public enum Camera2State {
        IDLE,
        DEVICE_PREPARED,
        DEVICE_OPENED,
        DEVICE_PREVIEW,
        DEVICE_CLOSED,
    }

    private CameraManager cameraManager = null; // 摄像头管理器
    private CameraCharacteristics characteristics = null;
    private int cameraId = CameraCharacteristics.LENS_FACING_BACK; // 摄像头id
    private Size previewSize = null; // 最合适的预览尺寸
    private Integer cameraOrientation = null; // 摄像头拍照的方向
    private Boolean isFlashSupported = false; // 是否支持闪光灯
    private Float maxDigitalZoom = 0.0f; // 最大的数字调焦值
    private Float minFocusDistance = 0.0f; // 最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
    private Rect cameraRect = null; // 摄像头坐标区域（为点击屏幕对焦准备）

    private int displayOrientation = 0;

    private Camera2State state = Camera2State.IDLE;

    private Handler cameraHandler = null;
    private HandlerThread cameraHandlerThread = null;

    private CameraDevice cameraDevice = null;

    private SurfaceTexture surfaceTexture = null;
    private Surface surface = null;
    private CaptureRequest.Builder previewBuilder = null;
    private CameraCaptureSession cameraSession = null;

    private ImageReader imageReader = null;
    private OnPreviewYUVFrameListener onPreviewYUVFrameListener = null;

    private CaptureRequest.Builder captureBuilder = null;

    private int latestAfState = -1;

    public Size getPreviewSize() {
        if (previewSize == null) {
            CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
                // 最合适的预览尺寸
                return getBestPreviewSize(characteristics, (cameraId == CameraCharacteristics.LENS_FACING_FRONT) ? new Size(1920, 1080) : new Size(1280, 720));
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
            return getBestPreviewSize(characteristics, (tempId == CameraCharacteristics.LENS_FACING_FRONT) ? new Size(1920, 1080) : new Size(1280, 720));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return (tempId == CameraCharacteristics.LENS_FACING_FRONT) ? new Size(1920, 1080) : new Size(1280, 720);
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
            createCameraPreviewSession();
        }
    }

    synchronized public boolean openCamera(int cameraId) {
        boolean ret = false;
        cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            return ret;
        }
        try {
            /**
             * LENS_FACING: 获取摄像头方向。LENS_FACING_FRONT是前摄像头，LENS_FACING_BACK是后摄像头，LENS_FACING_EXTERNAL是外接摄像头。
             * SENSOR_ORIENTATION：获取摄像头拍照的方向。
             * FLASH_INFO_AVAILABLE：获取是否支持闪光灯。
             * SCALER_AVAILABLE_MAX_DIGITAL_ZOOM：获取最大的数字调焦值，也就是zoom最大值。
             * LENS_INFO_MINIMUM_FOCUS_DISTANCE：获取最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
             * INFO_SUPPORTED_HARDWARE_LEVEL：获取摄像头支持某些特性的程度。
             */
            characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
            if (characteristics != null) {
                // 最合适的预览尺寸
                previewSize = getBestPreviewSize(characteristics, (cameraId == CameraCharacteristics.LENS_FACING_FRONT) ? new Size(1920, 1080) : new Size(1280, 720));
                Log.d(TAG, "previewSize: "+ previewSize.getWidth() + ", " +previewSize.getHeight());

                // 摄像头拍照的方向(旋转多少角度拍照的)
                cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (cameraOrientation == null) {
                    cameraOrientation = 0;
                }
                Log.d(TAG, "cameraOrientation: "+ cameraOrientation);

                // 获取硬件层面支持的Camera2功能等级
                int hardwareSupportedLevel = getHardwareSupportedLevel(characteristics);
                // 是否支持闪光灯
                isFlashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (isFlashSupported == null) {
                    isFlashSupported = false;
                }
                // 获取最大的数字调焦值，也就是zoom最大值。
                maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                if (maxDigitalZoom == null) {
                    maxDigitalZoom = 0.0f;
                }
                // 获取最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
                minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                if (minFocusDistance == null) {
                    minFocusDistance = 0.0f;
                }
                // 获取摄像头坐标区域（为点击屏幕对焦准备）
                cameraRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                cameraHandlerThread = new HandlerThread("camera2-manager");
                cameraHandlerThread.start();
                cameraHandler = new Handler(cameraHandlerThread.getLooper());

                createImageReader();

                state = Camera2State.DEVICE_PREPARED;
                cameraManager.openCamera(("" + cameraId), deviceStateCallback, cameraHandler);
                ret = true;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            if (cameraHandler != null) {
                cameraHandler.removeCallbacksAndMessages(null);
                cameraHandlerThread.getLooper().quitSafely();
                cameraHandlerThread = null;
                cameraHandler = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        }
        return ret;
    }

    public boolean openCamera() {
        return openCamera(cameraId);
    }

    synchronized public void closeCamera() {
        if (cameraSession != null) {
            cameraSession.close();
            cameraSession = null;
        }
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (cameraHandler != null) {
            cameraHandler.removeCallbacksAndMessages(null);
            cameraHandlerThread.getLooper().quitSafely();
            cameraHandlerThread = null;
            cameraHandler = null;
        }

        characteristics = null;
        previewSize = null;
        cameraOrientation = null;
        isFlashSupported = false;
        maxDigitalZoom = 0.0f;
        minFocusDistance = 0.0f;
        cameraRect = null;

        displayOrientation = 0;

        surface = null;
        previewBuilder = null;

        captureBuilder = null;

        state = Camera2State.IDLE;
    }

    synchronized public boolean swichCamera() {
        SurfaceTexture tempSurfaceTexture = surfaceTexture;
        if (state != Camera2State.IDLE) {
            closeCamera();
        }
        boolean ret = false;
        int tempId = (cameraId == CameraCharacteristics.LENS_FACING_BACK ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK);
        if (openCamera(tempId)) {
            cameraId = tempId;
            ret = true;
        }
        return ret;
    }

    public void releaseCamera() {
        closeCamera();
        surfaceTexture = null;
        onPreviewYUVFrameListener = null;
    }

    private String[] getCameraIds(CameraManager cameraManager) {
        String[] ret = null;
        try {
            ret = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private int getHardwareSupportedLevel(CameraCharacteristics characteristics) {
        Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == null) {
            Log.e(TAG, "can not get INFO_SUPPORTED_HARDWARE_LEVEL");
            return -1;
        }
        /**
         * LEVEL_LEGACY: 向后兼容模式, 如果是此等级, 所有设备都会支持，基本没有额外功能, 也就是和过时的Camera API支持的特性是一致的;
         * LEVEL_LIMITED: 有最基本的功能, 还支持一些额外的高级功能, 这些高级功能是LEVEL_FULL的子集;
         * LEVEL_FULL: 全方位的硬件支持，支持对每一帧数据进行控制、允许手动控制全高清的摄像、支持连拍模式以及其他新特性;
         * LEVEL_3: 支持YUV后处理和Raw格式图片拍摄, 还支持额外的输出流配置
         * LEVEL_EXTERNAL: API28中加入的, 应该是外接的摄像头, 功能和LIMITED类似
         *
         * 各个等级从支持的功能多少排序为: LEGACY < LIMITED < FULL < LEVEL_3
         */
        switch (deviceLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                Log.w(TAG, "hardware supported level:LEVEL_LEGACY");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                Log.w(TAG, "hardware supported level:LEVEL_LIMITED");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                Log.w(TAG, "hardware supported level:LEVEL_FULL");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                Log.w(TAG, "hardware supported level:LEVEL_3");
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                Log.w(TAG, "hardware supported level:LEVEL_3");
                break;
        }
        return deviceLevel;
    }

    private Size getBestPreviewSize(CameraCharacteristics characteristics, Size targetSize) {
        if (characteristics == null) {
            return targetSize;
        }

        // 管理摄像头支持的所有输出格式和尺寸
        StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (configurationMap == null) {
            return targetSize;
        }

        // 适配的尺寸数组
        Size[] outputSizes = configurationMap.getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            return targetSize;
        }

        if (targetSize == null) {
            return outputSizes[0];
        }

        Size bestSize = null;
        for (Size size : outputSizes) {
            if (size.getWidth() == targetSize.getWidth()
                    && size.getHeight() == targetSize.getHeight()) {
                bestSize = size;
                break;
            }
        }
        if (bestSize == null) {
            float ratoi = (targetSize.getWidth() * 1.0f) / (targetSize.getHeight() * 1.0f);
            for (Size size : outputSizes) {
                Log.d(TAG, "size: " + size.getWidth() + ", " + size.getHeight());
                float currentRatoi = (size.getWidth() * 1.0f) / (size.getHeight() * 1.0f);
                float currentAbs = Math.abs(currentRatoi - ratoi);
                int currentWidthAbs = Math.abs(size.getWidth() - targetSize.getWidth());
                int currentHeightAbs =  Math.abs(size.getHeight() - targetSize.getHeight());
                if (bestSize == null) {
                    bestSize = size;
                } else {
                    float prevRatoi = (bestSize.getWidth() * 1.0f) / (bestSize.getHeight() * 1.0f);
                    float prevAbs = Math.abs(prevRatoi - ratoi);
                    int prevWidthAbs = Math.abs(bestSize.getWidth() - targetSize.getWidth());
                    int prevHeightAbs =  Math.abs(bestSize.getHeight() - targetSize.getHeight());
                    if (currentAbs <= prevAbs
                            && (currentWidthAbs < prevWidthAbs || currentHeightAbs < prevHeightAbs)) {
                        bestSize = size;
                    }
                }
                Log.d(TAG, "bestSize: " + bestSize.getWidth() + ", " + bestSize.getHeight());
            }
        }
        return bestSize;
    }

    private void createImageReader() {
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        imageReader = ImageReader.newInstance(
                previewSize.getWidth(),
                previewSize.getHeight(),
                ImageFormat.YUV_420_888,
                1);
        imageReader.setOnImageAvailableListener(onImageYUV420888AvailableListener, cameraHandler);
    }

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, " CameraDevice onOpened():" + camera.getId());
            state = Camera2State.DEVICE_OPENED;
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " CameraDevice onDisconnected():" + camera.getId());
            state = Camera2State.DEVICE_CLOSED;
            camera.close();
            cameraDevice = null;
            if (cameraSession != null) {
                cameraSession.close();
                cameraSession = null;
            }
            if (imageReader != null) {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
                imageReader = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, " CameraDevice onError():" + camera.getId() + ", " + error);
            state = Camera2State.DEVICE_CLOSED;
            camera.close();
            cameraDevice = null;
            if (cameraSession != null) {
                cameraSession.close();
                cameraSession = null;
            }
            if (imageReader != null) {
                imageReader.setOnImageAvailableListener(null, null);
                imageReader.close();
                imageReader = null;
            }
        }
    };

    synchronized private void createCameraPreviewSession() {
        // 设置预览的宽高
        if (cameraDevice != null
                && surfaceTexture != null
                && (state == Camera2State.DEVICE_OPENED || state == Camera2State.DEVICE_PREVIEW)) {
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            surface = new Surface(surfaceTexture);

            if (cameraSession != null) {
                cameraSession.close();
                cameraSession = null;
            }

            try {
                //该对象负责管理处理预览请求和拍照请求
                cameraDevice.createCaptureSession(
                        Arrays.asList(surface, imageReader.getSurface()),
                        sessionStateCallback,
                        cameraHandler
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            state = Camera2State.DEVICE_PREVIEW;
        }
    }
    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, " session onConfigured()");
            if (cameraDevice == null) {
                return;
            }
            cameraSession = session;
            sendPreviewRequest();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, " session onConfigureFailed()");
        }

    };

    private void sendPreviewRequest() {
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
            updateAfState(partialResult);
            processPreCapture(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            updateAfState(result);
            processPreCapture(result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.w(TAG, "onCaptureFailed reason:" + failure.getReason());
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


    private ImageReader.OnImageAvailableListener onImageYUV420888AvailableListener = new ImageReader.OnImageAvailableListener() {
        private byte[] y = null;
        private byte[] u = null;
        private byte[] v = null;

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                if (planes != null) {
                    if (y == null) {
                        y = new byte[(planes[0].getBuffer().limit() - planes[0].getBuffer().position())];
                    }
                    if (u == null) {
                        u = new byte[(planes[1].getBuffer().limit() - planes[1].getBuffer().position())];
                    }
                    if (v == null) {
                        v = new byte[(planes[2].getBuffer().limit() - planes[2].getBuffer().position())];
                    }
                    if (planes[0].getBuffer().remaining() == y.length) {
                        // 分别填到 yuv
                        planes[0].getBuffer().get(y);
                        planes[1].getBuffer().get(u);
                        planes[2].getBuffer().get(v);
                    }
                    if (onPreviewYUVFrameListener != null) {
                        onPreviewYUVFrameListener.onPreviewFrameYUV(y, u, v, planes[0].getRowStride(), previewSize);
                    }
                }
                image.close();
            }
        }
    };
    public interface OnPreviewYUVFrameListener {
        public void onPreviewFrameYUV(byte[] y, byte[] u, byte[] v, int stride, Size size);
    }

    private void sendRepeatingRequest(CaptureRequest request,
                                      CameraCaptureSession.CaptureCallback callback,
                                      Handler handler) {
        try {
            cameraSession.setRepeatingRequest(request, callback, handler);
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "send repeating request error:" + e.getMessage());
        }
    }

    private CaptureRequest getPreviewRequest(CaptureRequest.Builder builder) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        int antiBMode = getValidAntiBandingMode(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    private CaptureRequest.Builder getPreviewBuilder() {
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

    /* ------------------------- private function------------------------- */
    private int getValidAFMode(int targetMode) {
        int[] allAFMode = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int mode : allAFMode) {
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
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support anti banding mode:" + targetMode
                + " use mode:" + allABMode[0]);
        return allABMode[0];
    }
}
