package com.songwj.openvideo.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.songwj.openvideo.opengl.renders.Camera1FilterRender;

public class Camera1RecordGLSurfaceView extends AspectRatioGLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {
    private Camera1FilterRender render = null;
    public Camera1RecordGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        render = new Camera1FilterRender(this);
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public Camera1RecordGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        render = new Camera1FilterRender(this);
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        Log.e("Camera1RecordGLSurfaceView", "surfaceCreated()");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        Log.e("Camera1RecordGLSurfaceView", "surfaceChanged()");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("Camera1RecordGLSurfaceView", "surfaceDestroyed()");
        if (render == null) {
            render.onSurfaceDestroy();
            render = null;
        }
        super.surfaceDestroyed(holder);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.e("Camera1RecordGLSurfaceView", "onFrameAvailable()");
        requestRender();
    }
}
