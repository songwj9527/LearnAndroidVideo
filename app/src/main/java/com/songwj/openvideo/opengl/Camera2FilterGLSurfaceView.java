package com.songwj.openvideo.opengl;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.songwj.openvideo.opengl.renders.Camera2FilterRender;

public class Camera2FilterGLSurfaceView extends AspectRatioGestureGLSurfaceView implements Camera2FilterRender.OnRenderListener  {
    protected Camera2FilterRender render = null;
    protected EGLContext eglContext = null;
    protected int width = 0, height = 0;

    public Camera2FilterGLSurfaceView(Context context) {
        super(context);
    }

    public Camera2FilterGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
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

    public void onRenderDestroy() {
        if (render != null) {
            render.onSurfaceDestroy();
            render = null;
        }
    }
}
