package com.songwj.openvideo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.Comparator;

public class Camera2Utils {

    private static final String TAG = "Camera2Utils";

    /**
     * 获取所有摄像头id
     * @param cameraManager
     * @return
     */
    public static String[] getCameraIds(CameraManager cameraManager) {
        String[] ret = null;
        try {
            ret = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 匹配指定方向的摄像头，前还是后
     *
     * LENS_FACING_FRONT是前摄像头标志
     * @param cameraCharacteristics
     * @param direction
     * @return
     */
    public static  boolean matchCameraDirection(CameraCharacteristics cameraCharacteristics, int direction){
        //这里设置后摄像头
        Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        return  (facing != null && facing == direction) ? true : false;
    }

    /**
     * 获取硬件支持的等级
     * @param characteristics
     * @return
     */
    public static int getHardwareSupportedLevel(CameraCharacteristics characteristics) {
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

    /**
     * 获取匹配到的最佳预览尺寸
     * @param characteristics
     * @param targetSize
     * @return
     */
    public static Size getBestPreviewSize(CameraCharacteristics characteristics, Size targetSize) {
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

        Log.d(TAG, "PreviewSize: " + Arrays.toString(outputSizes));
        Size bestSize = null;
        float ratoi = (targetSize.getWidth() * 1.0f) / (targetSize.getHeight() * 1.0f);
        for (Size size : outputSizes) {
            if (size.getWidth() == targetSize.getWidth()
                    && size.getHeight() == targetSize.getHeight()) {
                bestSize = size;
                break;
            }
        }
        if (bestSize == null) {
            for (Size size : outputSizes) {
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
            }
        }
        Log.d(TAG, "bestSize: " + bestSize.getWidth() + ", " + bestSize.getHeight());
        return bestSize;
    }

    /**
     * 获取匹配到的最佳预览尺寸
     * @param characteristics
     * @param targetSize
     * @return
     */
    public static Size getBestLargePreviewSize(CameraCharacteristics characteristics, Size targetSize) {
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

        Log.d(TAG, "PreviewSize: " + Arrays.toString(outputSizes));
        Size bestSize = null;
        float ratoi = (targetSize.getWidth() * 1.0f) / (targetSize.getHeight() * 1.0f);
        for (Size size : outputSizes) {
            float currentRatoi = (size.getWidth() * 1.0f) / (size.getHeight() * 1.0f);
            float currentAbs = Math.abs(currentRatoi - ratoi);
            if ((size.getWidth() >= targetSize.getWidth() || size.getHeight() >= targetSize.getHeight())
                    && currentAbs <= 0.03f) {
                if (bestSize == null) {
                    bestSize = size;
                }
                else if (size.getWidth() > bestSize.getWidth() || size.getHeight() >= bestSize.getHeight()) {
                    bestSize = size;
                }
            }
        }
        if (bestSize == null) {
            bestSize = getBestPreviewSize(characteristics, targetSize);
        }
        return bestSize;
    }

    /**
     * 获取匹配到的最佳预览帧率
     * @param characteristics
     * @param fps
     * @return
     */
    public static Range<Integer> getBestPreviewFps(CameraCharacteristics characteristics, int fps) {
        Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (fpsRanges != null && fpsRanges.length > 0) {
            Log.d(TAG, "FpsRanges: " + Arrays.toString(fpsRanges));
            int abs_best = -1;
            Range<Integer> best = null;
            for (Range<Integer> range : fpsRanges) {
                int result = (range.getLower() + range.getUpper()) / 2;
                int abs = Math.abs(result - fps);
                if (abs_best == -1) {
                    abs_best = abs;
                    best = range;
                } else if (abs < abs_best) {
                    abs_best = abs;
                    best = range;
                }
            }
            return best;
        }
        return null;
    }

    /**
     * 检查是否支持闪光灯
     * @param characteristics
     * @return
     */
    public static boolean isFlashSupported(CameraCharacteristics characteristics) {
        Boolean isSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return isSupported == null ? false : isSupported;
    }

    /**
     * 检查是否支持设备自动对焦
     * <p>
     * 很多设备的前摄像头都有固定对焦距离，而没有自动对焦。
     *
     * @param characteristics
     * @return
     */
    public static boolean checkAutoFocus(CameraCharacteristics characteristics) {
        int[] afAvailableModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (afAvailableModes.length == 0 || (afAvailableModes.length == 1 && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)) {
            return  false;
        } else {
            return  true;
        }
    }

    /**
     * 检查相机支持哪几种focusMode
     * @param cameraCharacteristics
     */
    public int checkFocusMode(CameraCharacteristics cameraCharacteristics){
        int[] availableFocusModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int focusMode : availableFocusModes != null ? availableFocusModes : new int[0]) {
            if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_OFF) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_AUTO) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_MACRO) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_EDOF) {

            }
            return focusMode;
        }
        return CameraCharacteristics.CONTROL_AF_MODE_OFF;
    }

    /**
     * 获取相机支持最大的调焦距离
     * @param cameraCharacteristics
     * @return
     */
    public  static  Float getMinimumFocusDistance(CameraCharacteristics cameraCharacteristics){
        Float distance = null;
        try {
            distance = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return  distance;
    }

    /**
     * 获取最大的数字变焦值，也就是缩放值
     * @param cameraCharacteristics
     * @return
     */
    public static  Float getMaxDigitalZoom(CameraCharacteristics cameraCharacteristics){
        Float maxZoom = null;
        try {
            maxZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        }catch (Exception e){
            e.printStackTrace();
        }
        return maxZoom;
    }

    /**
     * 用于检查是否支持Camera 2
     *
     * 事实上，在各个厂商的的Android设备上，Camera2的各种特性并不都是可用的，
     * 需要通过characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)方法来根据返回值来获取支持的级别：
     *
     * 1. INFO_SUPPORTED_HARDWARE_LEVEL_FULL：全方位的硬件支持，允许手动控制全高清的摄像、支持连拍模式以及其他新特性。
     * 2. INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED：有限支持，这个需要单独查询。
     * 3. INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY：所有设备都会支持，也就是和过时的Camera API支持的特性是一致的。
     *
     *
     * @param mContext
     * @return
     */
    public static boolean hasCamera2(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notFull = true;
            if (idList.length == 0) {
                notFull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notFull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);
                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notFull = false;
                        break;
                    }
                }
            }
            return notFull;
        } catch (Throwable ignore) {
            return false;
        }
    }


    /**
     * 计算zoom所对应的rect
     * @param originReact 相机原始的rect
     * @param currentZoom 当前的zoom值
     * @return
     */
    public static Rect createZoomRect(Rect originReact, float currentZoom){
        Rect zoomRect = null;
        try {
            if (originReact == null){
                return zoomRect;
            }else{
                float ratio = ((float) 1) / currentZoom;
                int cropWidth = originReact.width() - Math.round(((float) originReact.width()) * ratio);
                int cropHeight = originReact.height() - Math.round(((float) originReact.height()) * ratio);
                zoomRect = new Rect(cropWidth/2, cropHeight/2, originReact.width() - cropWidth/2, originReact.height() - cropHeight/2);
            }
        }catch (Exception e){
            e.printStackTrace();
            zoomRect = null;
        }
        return zoomRect;
    }

    public static double RATIO_4X3 = 1.333333333;
    public static double RATIO_16X9 = 1.777777778;
    public static double ASPECT_TOLERANCE = 0.00001;
    public static final String SPLIT_TAG = "x";

    private static void sortCamera2Size(Size[] sizes) {
        Comparator<Size> comparator = new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return o2.getWidth() * o2.getHeight() - o1.getWidth() * o1.getHeight();
            }
        };
        Arrays.sort(sizes, comparator);
    }

    /**
     * get default picture size from support picture size list
     * @param map specific stream configuration map used for get supported picture size
     * @param format used for get supported picture size list from map
     * @return largest 4:3 picture size or largest size in supported list
     */
    public static Size getDefaultPictureSize(StreamConfigurationMap map, int format) {
        Size[] supportSize = map.getOutputSizes(format);
        sortCamera2Size(supportSize);
        for (Size size : supportSize) {
            if (ratioMatched4x3(size)) {
                return size;
            }
        }
        return supportSize[0];
    }

    public static boolean ratioMatched4x3(Size size) {
        return size.getWidth() * 3 == size.getHeight() * 4;
    }

    public static boolean videoRatioMatched16x9(Size size) {
        return size.getWidth() * 9 == size.getHeight() * 16;
    }

    /**
     * get default preview size from supported preview size list
     * @param map specific stream configuration map used for get supported preview size
     * @param displaySize devices screen size
     * @return preview size equals or less than screen size
     */
    public static Size getDefaultPreviewSize(StreamConfigurationMap map, Point displaySize) {
        Size[] supportSize = map.getOutputSizes(SurfaceTexture.class);
        sortCamera2Size(supportSize);
        for (Size size : supportSize) {
            if (!ratioMatched4x3(size)) {continue;}
            if ((size.getHeight() == displaySize.x)
                    || (size.getWidth() <= displaySize.y && size.getHeight() <= displaySize.x)) {
                return size;
            }
        }
        return supportSize[0];
    }

    public static Size getPreviewSizeByRatio(StreamConfigurationMap map, Point displaySize, double ratio) {
        Size[] supportSize = map.getOutputSizes(SurfaceTexture.class);
        sortCamera2Size(supportSize);
        for (Size size : supportSize) {
            boolean isRatioMatch = (int) (size.getHeight() * ratio) == size.getWidth();
            if (!isRatioMatch) {
                continue;
            }
            if ((size.getHeight() == displaySize.x)
                    || (size.getWidth() <= displaySize.y && size.getHeight() <= displaySize.x)) {
                return size;
            }
        }
        return supportSize[0];
    }

    /**
     * Get default video size from support video size list
     * @param map specific stream configuration map used for get supported picture size
     * @return size match screen size or largest size in supported list
     */
    public static Size getDefaultVideoSize(StreamConfigurationMap map, Point displaySize) {
        Size[] supportSize = map.getOutputSizes(MediaRecorder.class);
        sortCamera2Size(supportSize);
        for (Size size : supportSize) {
            if (videoRatioMatched16x9(size) && size.getHeight() <= displaySize.x) {
                return size;
            }
        }
        return supportSize[0];
    }

    /* size format is "width x height"*/
    public static String[] getPictureSizeList(StreamConfigurationMap map, int format) {
        Size[] supportSize = map.getOutputSizes(format);
        sortCamera2Size(supportSize);
        String[] sizeStr = new String[supportSize.length];
        for (int i = 0; i < supportSize.length; i++) {
            sizeStr[i] = supportSize[i].getWidth() + SPLIT_TAG + supportSize[i].getHeight();
        }
        return sizeStr;
    }

    public static String[] getPreviewSizeList(StreamConfigurationMap map) {
        Size[] supportSize = map.getOutputSizes(SurfaceTexture.class);
        sortCamera2Size(supportSize);
        String[] sizeStr = new String[supportSize.length];
        for (int i = 0; i < supportSize.length; i++) {
            sizeStr[i] = supportSize[i].getWidth() + SPLIT_TAG + supportSize[i].getHeight();
        }
        return sizeStr;
    }

    public static String[] getVideoSizeList(StreamConfigurationMap map) {
        Size[] supportSize = map.getOutputSizes(MediaRecorder.class);
        sortCamera2Size(supportSize);
        String[] sizeStr = new String[supportSize.length];
        for (int i = 0; i < supportSize.length; i++) {
            sizeStr[i] = supportSize[i].getWidth() + SPLIT_TAG + supportSize[i].getHeight();
        }
        return sizeStr;
    }

    public static Size getPreviewUiSize(Context context, Size previewSize) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        double ratio = previewSize.getWidth() / (double) previewSize.getHeight();
        int w = (int) Math.ceil(metrics.widthPixels * ratio);
        int h = metrics.widthPixels;
        return new Size(w, h);
    }

    public static int getJpgRotation(CameraCharacteristics c, int deviceRotation) {
        int result ;
        Integer sensorRotation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Integer lensFace = c.get(CameraCharacteristics.LENS_FACING);
        if (sensorRotation == null || lensFace == null) {
            Log.e(TAG, "can not get sensor rotation or lens face");
            return deviceRotation;
        }
        if (lensFace == CameraCharacteristics.LENS_FACING_BACK) {
            result = (sensorRotation + deviceRotation) % 360;
        } else {
            result = (sensorRotation - deviceRotation + 360) % 360;
        }
        return result;
    }

    public static Point getDisplaySize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        return point;
    }

    public static int getVirtualKeyHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        Point defaultPoint = new Point();
        Point realPoint = new Point();
        windowManager.getDefaultDisplay().getSize(defaultPoint);
        windowManager.getDefaultDisplay().getRealSize(realPoint);
        return realPoint.y - defaultPoint.y;
    }

    public static int getBottomBarHeight(int screenWidth) {
        return (int) (screenWidth * (RATIO_16X9 - RATIO_4X3));
    }

    public static int getDefaultPreviewSizeIndex(Context context, Size[] supportSize) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        Point realPoint = new Point();
        windowManager.getDefaultDisplay().getRealSize(realPoint);
        for (int i = 0; i < supportSize.length; i++) {
            if (!ratioMatched4x3(supportSize[i])) {
                continue;
            }
            if ((supportSize[i].getHeight() == realPoint.x) || (supportSize[i].getWidth()
                    <= realPoint.y && supportSize[i].getHeight() <= realPoint.x)) {
                return i;
            }
        }
        return 0;
    }

    public static String[][] getOutputFormat(int[] supportFormat) {
        String[][] formatStr = new String[2][supportFormat.length];
        for (int i = 0; i < supportFormat.length; i++) {
            formatStr[0][i] = format2String(supportFormat[i]);
            formatStr[1][i] = String.valueOf(supportFormat[i]);
        }
        return formatStr;
    }

    public static String hardwareLevel2Sting(int level) {
        switch (level) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                return "LEVEL_FULL";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                return "LEVEL_LEGACY";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                return "LEVEL_3";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                return "LEVEL_LIMITED";
            default:
                return "Unknown Level";
        }
    }

    public static String capabilities2String(int cap) {
        switch (cap) {
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE:
                return "BACKWARD_COMPATIBLE";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE:
                return "BURST_CAPTURE";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO:
                return "CONSTRAINED_HIGH_SPEED_VIDEO";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT:
                return "DEPTH_OUTPUT";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING:
                return "MANUAL_POST_PROCESSING";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR:
                return "MANUAL_SENSOR";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING:
                return "PRIVATE_REPROCESSING";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW:
                return "RAW";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS:
                return "READ_SENSOR_SETTINGS";
            case CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING:
                return "YUV_REPROCESSING";
            default:
                return "Unknown Capabilities";
        }
    }

    public static String format2String(int format) {
        switch (format) {
            case ImageFormat.RGB_565:
                return "RGB_565";
            case ImageFormat.NV16:
                return "NV16";
            case ImageFormat.YUY2:
                return "YUY2";
            case ImageFormat.YV12:
                return "YV12";
            case ImageFormat.JPEG:
                return "JPEG";
            case ImageFormat.NV21:
                return "NV21";
            case ImageFormat.YUV_420_888:
                return "YUV_420_888";
            case ImageFormat.YUV_422_888:
                return "YUV_422_888";
            case ImageFormat.YUV_444_888:
                return "YUV_444_888";
            case ImageFormat.FLEX_RGB_888:
                return "FLEX_RGB_888";
            case ImageFormat.FLEX_RGBA_8888:
                return "FLEX_RGBA_8888";
            case ImageFormat.RAW_SENSOR:
                return "RAW_SENSOR";
            case ImageFormat.RAW_PRIVATE:
                return "RAW_PRIVATE";
            case ImageFormat.RAW10:
                return "RAW10";
            case ImageFormat.RAW12:
                return "RAW12";
            case ImageFormat.DEPTH16:
                return "DEPTH16";
            case ImageFormat.DEPTH_POINT_CLOUD:
                return "DEPTH_POINT_CLOUD";
            case ImageFormat.PRIVATE:
                return "PRIVATE";
            default:
                return "ERROR FORMAT";
        }
    }
}