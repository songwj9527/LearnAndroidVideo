package com.songwj.openvideo.opengl;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.songwj.openvideo.opengl.renders.Camera1FilterRender;

public class Camera1FilterGLSurfaceView extends AspectRatioGLSurfaceView implements Camera1FilterRender.OnRenderListener  {
    protected Camera1FilterRender render = null;
    protected EGLContext eglContext = null;
    protected int width = 0, height = 0;

    public Camera1FilterGLSurfaceView(Context context) {
        super(context);
    }

    public Camera1FilterGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("Camera1FilterGLSurfaceView", "surfaceDestroyed()");
        if (render == null) {
            render.onSurfaceDestroy();
            render = null;
        }
        super.surfaceDestroyed(holder);
    }

    @Override
    public void onRenderCreated(EGLContext eglContext) {
        this.eglContext = eglContext;
    }

    @Override
    public void onRenderChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(int textureId, long timestamp) {

    }
}
