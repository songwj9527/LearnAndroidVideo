package com.songwj.openvideo.opengl.renders;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.songwj.openvideo.camera.Camera1Manager;
import com.songwj.openvideo.opengl.Camera1FilterGLSurfaceView;
import com.songwj.openvideo.opengl.filter.CameraFilter;
import com.songwj.openvideo.opengl.filter.DuskColorFilter;
import com.songwj.openvideo.opengl.filter.CubeFilter;
import com.songwj.openvideo.opengl.filter.ScreenFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractChainRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;
import com.songwj.openvideo.opengl.filter.base.FilterContext;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera1FilterRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "Camera1RecordRender";

    private Camera1FilterGLSurfaceView glSurfaceView;
    private int[] cameraTextrueId = new int[1];
    private SurfaceTexture cameraTexture = null;

    private List<AbstractChainRectFilter> filterList = new ArrayList<>();
    private FilterChain filterChain;

    private float[] mvpMatrix = new float[16];

    private OnRenderListener onRenderListener = null;

    public Camera1FilterRender(Camera1FilterGLSurfaceView glSurfaceView) {
        this.glSurfaceView = glSurfaceView;
        this.onRenderListener = glSurfaceView;
        FilterContext filterContext = new FilterContext();
        filterChain = new FilterChain(filterContext, 0, filterList);
    }

    public void addFilters(List<AbstractChainRectFilter> filters) {
        filterList.clear();
        filterList.addAll(filters);
    }

    public void addFilter(AbstractChainRectFilter filter) {
        filterList.add(filter);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0f, 0f, 0f, 0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        //------开启混合，即半透明---------
        // 开启很混合模式
        GLES30.glEnable(GLES30.GL_BLEND);
        // 配置混合算法
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glGenTextures(1, cameraTextrueId, 0);
        cameraTexture = new SurfaceTexture(cameraTextrueId[0]);
        cameraTexture.setOnFrameAvailableListener(this);
        Camera1Manager.getInstance().updatePreviewTexture(cameraTexture);
        Camera1Manager.getInstance().resumePreview();

        filterChain.init();
        if (onRenderListener != null) {
            onRenderListener.onRenderCreated(EGL14.eglGetCurrentContext());
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (filterChain != null) {
            filterChain.setSize(width, height);
        }
        if (onRenderListener != null) {
            onRenderListener.onRenderChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        cameraTexture.updateTexImage();//通过此方法更新接收到的预览数据
        cameraTexture.getTransformMatrix(mvpMatrix);

        if (filterChain != null) {
            filterChain.setCameraMatrix(mvpMatrix, mvpMatrix.length);
            int textureId = filterChain.proceed(cameraTextrueId[0]);
            if (onRenderListener != null) {
                onRenderListener.onDrawFrame(textureId, cameraTexture.getTimestamp());
            }
        }
    }

    public void onSurfaceDestroy() {
        if (filterChain != null) {
            filterChain.release();
            filterChain = null;
        }
        glSurfaceView = null;
        onRenderListener = null;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }

    public interface OnRenderListener {
        public void onRenderCreated(EGLContext eglContext);
        public void onRenderChanged(int width, int height);
        public void onDrawFrame(int textureId, long timestamp);
    }
}
