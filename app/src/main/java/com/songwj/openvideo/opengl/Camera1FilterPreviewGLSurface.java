package com.songwj.openvideo.opengl;

import android.content.Context;
import android.util.AttributeSet;

import com.songwj.openvideo.opengl.Camera1FilterGLSurfaceView;
import com.songwj.openvideo.opengl.filter.CameraFilter;
import com.songwj.openvideo.opengl.filter.CubeFilter;
import com.songwj.openvideo.opengl.filter.DuskColorFilter;
import com.songwj.openvideo.opengl.filter.ScreenFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.renders.Camera1FilterRender;

public class Camera1FilterPreviewGLSurface extends Camera1FilterGLSurfaceView {

    public Camera1FilterPreviewGLSurface(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        render = new Camera1FilterRender(this);
        render.addFilter(new CameraFilter());
        render.addFilter(new DuskColorFilter());
        render.addFilter(new CubeFilter());
        render.addFilter(new ScreenFilter());
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public Camera1FilterPreviewGLSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        render = new Camera1FilterRender(this);
        render.addFilter(new CameraFilter());
        render.addFilter(new DuskColorFilter());
        render.addFilter(new CubeFilter());
        render.addFilter(new ScreenFilter());
        setRenderer(render);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
