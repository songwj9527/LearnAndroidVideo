package com.songwj.openvideo.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.songwj.openvideo.opengl.renders.Camera1PreviewRender;
import com.songwj.openvideo.opengl.renders.CubeRender;

import java.util.jar.Attributes;

public class Camera1PreviewGLSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {
    private Camera1PreviewRender render = null;

    public Camera1PreviewGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        render = new Camera1PreviewRender(context, this);
        render.addObjectRender(new CubeRender());
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public Camera1PreviewGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        render = new Camera1PreviewRender(context, this);
        render.addObjectRender(new CubeRender());
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        if (render != null) {
            render.release();
            render = null;
        }
    }
}
