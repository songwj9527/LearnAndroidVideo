package com.songwj.openvideo.opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.songwj.openvideo.opengl.renders.Camera1FilterRender;

public class Camera1RecordGLSurfaceView extends AspectRatioGLSurfaceView {
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
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("Camera1RecordGLSurfaceView", "surfaceDestroyed()");
        if (render == null) {
            render.onSurfaceDestroy();
            render = null;
        }
        super.surfaceDestroyed(holder);
    }
}
