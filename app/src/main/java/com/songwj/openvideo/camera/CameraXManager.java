package com.songwj.openvideo.camera;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXManager implements ImageAnalysis.Analyzer {
    private static final String TAG = "CameraXManager";

    private CameraXManager() {

    }

    static private class Singleton {
        static private CameraXManager instance = new CameraXManager();
    }

    public static CameraXManager getInstance() {
        return Singleton.instance;
    }

    enum State {
        NONE,
        OPENING,
        OPENED,
        PREVIEW,
    }

    private volatile ExecutorService executorService = null;

    private AppCompatActivity appCompatActivity;
    private Object lock = new Object();
    private volatile State state = State.NONE;
    private ProcessCameraProvider cameraProvider = null;
    private int cameraId = CameraSelector.LENS_FACING_FRONT;
    private Preview preview = null;
    private ImageCapture imageCapture = null;

    private OnPreparedListener onPreparedListener = null;
    private OnImageAnalysisListener onImageAnalysisListener = null;
    private OnErrorListener onErrorListener = null;

    public void openCamera(AppCompatActivity appCompatActivity) {
        if (appCompatActivity == null) {
            if (onErrorListener != null) {
                onErrorListener.onError(-1000,"context is null.");
            }
            return;
        }
        if (executorService == null) {
            synchronized (lock) {
                if (executorService == null) {
                    executorService = Executors.newSingleThreadExecutor();
                }
            }
        }
        synchronized (lock) {
            if (state != State.NONE) {
                return;
            }
            this.appCompatActivity = appCompatActivity;
            state = State.OPENING;
            ListenableFuture<ProcessCameraProvider> providerListenableFuture =  ProcessCameraProvider.getInstance(appCompatActivity);
            providerListenableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (state != State.OPENING) {
                            return;
                        }
                    }
                    try {
                        cameraProvider = providerListenableFuture.get();

                        //1 图像预览接口
                        preview = new Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//                    .setTargetResolution(Size(1920, 1080)) // 最大预览无法达到(1920 x 1080)时，会自动适配输尺寸
                                .build();
                        //2 图像分析接口
                        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//                    .setTargetResolution(Size(1280, 720)) // 最大只能输出无法达到(1920 x 1080)时，会自动适配输出尺寸
                                .build();
                        imageAnalyzer.setAnalyzer(executorService, CameraXManager.this);

                        //3 拍照 接口
                        imageCapture = new ImageCapture.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//                    .setTargetResolution(Size(1920, 1080)) // 最大预览无法达到(1920 x 1080)时，会自动适配输尺寸
                                .build();

                        //4 选择相机
                        CameraSelector cameraSelector = new CameraSelector.Builder()
                                .requireLensFacing(cameraId)
                                .build();

                        synchronized (lock) {
                            if (state != State.OPENING) {
                                return;
                            }
                            //5 把我们需要的这三个接口安装到相机管理器的主线路上，实现截取数据的目的
                            cameraProvider.unbindAll();
                            Camera camera = cameraProvider.bindToLifecycle(
                                    CameraXManager.this.appCompatActivity,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalyzer
                            );
                            state = State.OPENED;
                            if (onPreparedListener != null) {
                                onPreparedListener.onPrepared(cameraId, camera.getCameraInfo().getSensorRotationDegrees());
                            }

                            Log.e(TAG, "camera info:\n");
                            Log.e(TAG, "    exposureState: ${camera.cameraInfo.exposureState?.isExposureCompensationSupported}\n");
                            Log.e(TAG, "    sensorRotationDegrees: ${camera.cameraInfo.sensorRotationDegrees}\n");
                            Log.e(TAG, "    torchState: ${camera.cameraInfo.torchState.value}\n");
                            Log.e(TAG, "    zoomState: ${camera.cameraInfo.zoomState.value}\n");
                            Log.e(TAG, "    hasFlashUnit: ${camera.cameraInfo.hasFlashUnit()}\n");
                        }
                    } catch (ExecutionException e) {
                        Log.e(TAG, "run: binding lifecycle failed");
                        e.printStackTrace();
                        synchronized (lock) {
                            state = State.NONE;
                            preview = null;
                            imageCapture = null;
                        }
                        if (onErrorListener != null) {
                            onErrorListener.onError(-1001, "run: binding lifecycle failed.");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        synchronized (lock) {
                            state = State.NONE;
                            preview = null;
                            imageCapture = null;
                        }
                        if (onErrorListener != null) {
                            onErrorListener.onError(-1002, "run: other error.");
                        }
                    }
                }
            }, ContextCompat.getMainExecutor(appCompatActivity));
        }
    }

    public void setPreviewView(PreviewView previewView) {
        synchronized (lock) {
            if (state.ordinal() > State.OPENING.ordinal() && preview != null) {
                state = State.PREVIEW;
                if (previewView == null) {
                    preview.setSurfaceProvider(null);
                } else {
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                }
            }
        }
    }

    public void switchCamera(AppCompatActivity appCompatActivity) {
        closeCamera();
        cameraId = (cameraId == CameraSelector.LENS_FACING_FRONT) ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
        openCamera(appCompatActivity);
    }

    public void closeCamera() {
        synchronized (lock) {
//            if (state.ordinal() > State.NONE.ordinal()) {
//                if (cameraProvider != null) {
//                    cameraProvider.shutdown();
//                    cameraProvider = null;
//                }
//                preview = null;
//                imageCapture = null;
//                state = State.NONE;
//            }
//            appCompatActivity = null;
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            preview = null;
            imageCapture = null;
            state = State.NONE;
            appCompatActivity = null;
        }
    }

    public void releaseCamera() {
        synchronized (lock) {
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            preview = null;
            imageCapture = null;
            state = State.NONE;
            appCompatActivity = null;
        }
    }

    @Override
    public void analyze(@NonNull @NotNull ImageProxy image) {
        if (onImageAnalysisListener != null) {
            onImageAnalysisListener.onImageAnalyze(image);
        }
        image.close();
    }

    public void takeCapture(String filePath) {
        if (TextUtils.isEmpty(filePath)
                || !(filePath.endsWith(".jpg") || filePath.endsWith(".JPG")
                || filePath.endsWith(".jpeg") || filePath.endsWith(".JPEG")
                || filePath.endsWith(".png") || filePath.endsWith(".PNG")
                || filePath.endsWith(".webp") || filePath.endsWith(".WEBP"))) {
            return;
        }
        synchronized (lock) {
            if (state.ordinal() > State.OPENING.ordinal()) {
                if (imageCapture != null) {
                    // 定义 拍摄imageCapture实例
                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(new File(filePath)).build();
                    imageCapture.takePicture(outputFileOptions, executorService, new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            //保存成功
                            Log.e(TAG, "onImageSaved()");
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            //"保存失败"+
                            Log.e(TAG, "onImageSave onError(): " + exception.getMessage());
                        }
                    });
                }
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    public void setOnImageAnalysisListener(OnImageAnalysisListener onImageAnalysisListener) {
        this.onImageAnalysisListener = onImageAnalysisListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public interface OnPreparedListener {
        public void onPrepared(int cameraId, int sensorRotationDegrees);
    }

    public interface OnImageAnalysisListener {
        public void onImageAnalyze(ImageProxy image);
    }

    public interface OnErrorListener {
        public void onError(int errorCode, String errorMessage);
    }
}
