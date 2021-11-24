package com.songwj.openvideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;

import com.songwj.openvideo.MyApplication;

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
class Camera2Manager {
    private final String TAG = "Camera2Manager";

    private Camera2Manager() {

    }
    static private class Singleton {
        static private Camera2Manager instance = new Camera2Manager();
    }

    public static Camera2Manager getInstance() {
        return Singleton.instance;
    }

    private int cameraId = CameraCharacteristics.LENS_FACING_FRONT;
    private Integer cameraOrientation = 0; // 摄像头拍照的方向
    private Boolean isFlashSupported = false; // 是否支持闪光灯
    private Float maxDigitalZoom = 0.0f; // 最大的数字调焦值
    private Float minFocusDistance = 0.0f; // 最小的调焦距离，某些手机上获取到的该values为null或者0.0。前摄像头大部分有固定焦距，无法调节。
    private Rect cameraRect = null; // 摄像头坐标区域（为点击屏幕对焦准备）
    private Size previewSize = null;

    private int displayOrientation = 0;

    private CameraDevice cameraDevice = null;
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private Handler backgroundHandler = null;
    private HandlerThread backgroundThread = null;


    public boolean openCamera() {
        boolean ret = false;
        CameraManager cameraManager = (CameraManager) MyApplication.Companion.getInstance().getSystemService(Context.CAMERA_SERVICE);
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
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("" + cameraId);
            if (characteristics != null) {
                // 摄像头拍照的方向(旋转多少角度拍照的)
                cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (cameraOrientation == null) {
                    cameraOrientation = 0;
                }

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

                // 管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // 最合适的尺寸
                Size[] outputSizes = configurationMap.getOutputSizes(SurfaceTexture.class);

                backgroundThread = new HandlerThread("camera2-manager");
                backgroundThread.start();
                backgroundHandler = new Handler(backgroundThread.getLooper());
                cameraManager.openCamera("" + cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            if (backgroundHandler != null) {
                backgroundHandler.getLooper().quitSafely();
                backgroundThread = null;
                backgroundHandler = null;
            }
        }
        return ret;
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
}
