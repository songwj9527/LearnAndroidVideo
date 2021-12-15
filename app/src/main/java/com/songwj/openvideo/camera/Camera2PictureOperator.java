package com.songwj.openvideo.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Camera2PictureOperator extends Camera2Operator {
    private TakePictureCallback takePictureCallback = null;
    private CaptureRequest.Builder captureBuilder = null;

    public Camera2PictureOperator() {
        TAG = "Camera2PictureOperator";
        imageFormat = ImageFormat.JPEG;
        outputListener = new OnImageAvailableOutputListener() {
            @Override
            public void onImageAvailable(byte[] image, Size size) {
                boolean success = false;
                if (TextUtils.isEmpty(captureFilePath)) {
                    sendPreviewRequest();
                    return;
                }
                if (state > STATE_OPENIND) {
                    File saveFile = new File(captureFilePath);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(cameraOrientation);
//                    // 缩放 当sy为-1时向上翻转 当sx为-1时向左翻转 sx、sy都为-1时相当于旋转180°
//                    matrix.postScale(1f, -1f);
//                    // 因为向上翻转了所以y要向下平移一个bitmap的高度
//                    matrix.postTranslate(0f, bitmap.getHeight());
                    // 反转镜像(小米手机CameraCharacteristics.LENS_FACING_BACK对应的是前置摄像头，正常应该判断LENS_FACING_FRONT)
//                    if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
                    if (isFrontCamera()) {
                        matrix.postScale(-1f, 1f);
                        matrix.postTranslate(0f, bitmap.getWidth());
                    }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    try {
                        saveFile.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
                        if (fileOutputStream != null) {
                            if (captureFilePath.endsWith(".jpg") || captureFilePath.endsWith(".jpeg")
                                    || captureFilePath.endsWith(".JPG") || captureFilePath.endsWith(".JPEG")) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                            } else {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                            }
                        }
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                sendPreviewRequest();
                if (takePictureCallback != null) {
                    if (success) {
                        takePictureCallback.onSuccess();
                    } else {
                        takePictureCallback.onFailure();
                    }
                }
            }
        };
    }

    @Override
    protected CaptureRequest.Builder getPreviewBuilder() {
        // 创建预览需要的CaptureRequest.Builder
        if (previewBuilder == null) {
            try {
                previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                if (surface != null) {
                    previewBuilder.addTarget(surface);
                }
                // 设置了此处，imageReader就会一直调用onImageAvailable(ImageReader reader)传递帧数据
//                if (imageReader != null) {
//                    previewBuilder.addTarget(imageReader.getSurface());
//                }
            } catch (CameraAccessException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return previewBuilder;
    }

    @Override
    protected void sendPreviewRequest() {
        captureBuilder = null;
        super.sendPreviewRequest();
    }

    @Override
    public void closeCamera() {
        super.closeCamera();
        captureBuilder = null;
    }

    @Override
    public void releaseCamera() {
        super.releaseCamera();
        takePictureCallback = null;
    }

    @Override
    protected void processPreCapture(CaptureResult result) {
        switch (state) {
            case STATE_DEVICE_PREVIEW: {
                // We have nothing to do when the camera preview is working normally.
                break;
            }
            //刚开始拍照，锁住，等待状态
            case STATE_WAITING_LOCK: {
                //当前自动对焦的状态
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    sendStillPictureRequest();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        state = STATE_PICTURE_TAKEN;
                        sendStillPictureRequest();
                    } else {
                        triggerAECaptureSequence();
                    }
                }
                break;
            }
            // //等待，预捕获
            case STATE_WAITING_PRE_CAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    state = STATE_WAITING_NON_PRE_CAPTURE;
                }
                break;
            }
            //已经完成预捕获，直接拍照。
            case STATE_WAITING_NON_PRE_CAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    state = STATE_PICTURE_TAKEN;
                    sendStillPictureRequest();
                }
                break;
            }
        }
    }

    public void setTakePictureCallback(TakePictureCallback callback) {
        this.takePictureCallback = callback;
    }
    private String captureFilePath = null;
    public void takePicture(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            if (takePictureCallback != null) {
                takePictureCallback.onFailure();
            }
            return;
        }
        captureFilePath = filePath;
        synchronized (stateLock) {
            if (state > STATE_OPENIND) {
                boolean isFlashOn = flashMode != FlashMode.OFF && flashMode != FlashMode.TORCH;
                if (canTriggerAf() && isFlashOn) {
                    triggerAFCaptureSequence();
                } else {
                    sendStillPictureRequest();
                }
            } else {
                if (takePictureCallback != null) {
                    takePictureCallback.onFailure();
                }
            }
        }
    }

    private boolean canTriggerAf() {
        int[] allAFMode = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return  allAFMode != null && allAFMode.length > 1;
    }

    /**
     * 拍照的第一步，锁住焦点。
     */
    private void triggerAFCaptureSequence() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        state = STATE_WAITING_LOCK;
        sendCaptureRequest(builder.build(), previewCaptureCallback, cameraHandler);
    }

    /**
     * 运行预捕获的序列，为捕获一个静态图片
     */
    private void triggerAECaptureSequence() {
        CaptureRequest.Builder builder = getPreviewBuilder();
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        state = STATE_WAITING_PRE_CAPTURE;
        sendCaptureRequest(builder.build(), previewCaptureCallback, cameraHandler);
    }

    /**
     * 拍照一个静态的图片
     */
    private void sendStillPictureRequest() {
        int jpegRotation = Camera2Utils.getJpgRotation(characteristics, cameraOrientation);
        CaptureRequest.Builder builder = getCaptureBuilder(false, imageReader.getSurface());
        Integer aeFlash = getPreviewBuilder().get(CaptureRequest.CONTROL_AE_MODE);
        Integer afMode = getPreviewBuilder().get(CaptureRequest.CONTROL_AF_MODE);
        Integer flashMode = getPreviewBuilder().get(CaptureRequest.FLASH_MODE);
        builder.set(CaptureRequest.CONTROL_AE_MODE, aeFlash);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.FLASH_MODE, flashMode);
        CaptureRequest request = getStillPictureRequest(
                getCaptureBuilder(false, imageReader.getSurface()),
                jpegRotation);
        sendCaptureRequestWithStop(request, captureCallback, cameraHandler);
    }

    private CaptureRequest.Builder getCaptureBuilder(boolean create, Surface surface) {
        if (create) {
            return createBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE, surface);
        } else {
            if (captureBuilder == null) {
                captureBuilder = createBuilder(CameraDevice.TEMPLATE_STILL_CAPTURE, surface);
            }
            return captureBuilder;
        }
    }

    private CaptureRequest.Builder createBuilder(int type, Surface surface) {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(type);
            builder.addTarget(surface);
            return builder;
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
        }
        return null;
    }

    private CaptureRequest getStillPictureRequest(CaptureRequest.Builder builder, int rotation) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
        return builder.build();
    }

    private void sendCaptureRequestWithStop(CaptureRequest request,
                                            CameraCaptureSession.CaptureCallback callback,
                                            Handler handler) {
        try {
            previewSession.stopRepeating();
            previewSession.abortCaptures();
            previewSession.capture(request, callback, handler);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, "send capture request error:" + e.getMessage());
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession
            .CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(TAG, "capture complete");
            resetTriggerState();
        }
    };
}
