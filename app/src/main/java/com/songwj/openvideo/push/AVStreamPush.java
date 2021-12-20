package com.songwj.openvideo.push;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.AttributeSet;
import android.view.TextureView;

import com.songwj.openvideo.TextureRenderView;
import com.songwj.openvideo.camera.Camera2Manager;

public class AVStreamPush extends TextureRenderView implements TextureView.SurfaceTextureListener {
    public AVStreamPush(Context context) {
        super(context);
        setSurfaceAspectRatioMode(SurfaceAspectRatioMode.EQUAL_PROPORTION_FILL_PARENT);
        prepare();
    }

    public AVStreamPush(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSurfaceAspectRatioMode(SurfaceAspectRatioMode.EQUAL_PROPORTION_FILL_PARENT);
        prepare();
    }

    public AVStreamPush(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceAspectRatioMode(SurfaceAspectRatioMode.EQUAL_PROPORTION_FILL_PARENT);
        prepare();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AVStreamPush(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackgroundResource(android.R.color.darker_gray);
        setSurfaceAspectRatioMode(SurfaceAspectRatioMode.EQUAL_PROPORTION_FILL_PARENT);
        prepare();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceTexture = surface;
        Camera2Manager.getInstance().setSurfaceTexture(surface);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surface.release();
        return true;
    }

    private SurfaceTexture surfaceTexture = null;
    private boolean isCameraOpened = false;
    private void prepare() {
        Camera2Manager.getInstance().releaseCamera();
        Camera2Manager.getInstance().switchMode(Camera2Manager.Mode.OUTPUT_YUV420);
        isCameraOpened = Camera2Manager.getInstance().openCamera();
        setSurfaceTextureListener(this);
    }

    synchronized public void switchCamera() {
        isCameraOpened = Camera2Manager.getInstance().swichCamera();
        Camera2Manager.getInstance().setSurfaceTexture(surfaceTexture);
    }

    synchronized public void startPush() {

    }

    synchronized public void stopPush() {

    }

    synchronized public void release() {
        isCameraOpened = false;
        Camera2Manager.getInstance().releaseCamera();
    }
}
