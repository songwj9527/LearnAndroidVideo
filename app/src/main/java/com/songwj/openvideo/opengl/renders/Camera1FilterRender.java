package com.songwj.openvideo.opengl.renders;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.songwj.openvideo.camera.Camera1Manager;
import com.songwj.openvideo.opengl.filter.CameraFilter;
import com.songwj.openvideo.opengl.filter.ScreenFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;
import com.songwj.openvideo.opengl.filter.base.FilterContext;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Camera1FilterRender implements GLSurfaceView.Renderer {
    private static final String TAG = "Camera1RecordRender";

    private int[] cameraTextrueId = new int[1];
    private SurfaceTexture cameraTexture = null;
    private SurfaceTexture.OnFrameAvailableListener listener = null;

    private FilterChain filterChain;

    private float[] mvpMatrix = new float[16];

    public Camera1FilterRender(SurfaceTexture.OnFrameAvailableListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e(TAG, "onSurfaceCreated()");
        GLES30.glClearColor(0f, 0f, 0f, 0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        //------开启混合，即半透明---------
        // 开启很混合模式
        GLES30.glEnable(GLES30.GL_BLEND);
        // 配置混合算法
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glGenTextures(1, cameraTextrueId, 0);
        cameraTexture = new SurfaceTexture(cameraTextrueId[0]);
        cameraTexture.setOnFrameAvailableListener(listener);
        Camera1Manager.getInstance().updatePreviewTexture(cameraTexture);
        Camera1Manager.getInstance().resumePreview();

        FilterContext filterContext = new FilterContext();
        List<AbstractRectFilter> filterList = new ArrayList<>();
        filterList.add(new CameraFilter());
        filterList.add(new ScreenFilter());
        filterChain = new FilterChain(filterContext, 0, filterList);
        filterChain.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.e(TAG, "onSurfaceChanged()");
        if (filterChain != null) {
            filterChain.setSize(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.e(TAG, "onDrawFrame()");
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        cameraTexture.updateTexImage();//通过此方法更新接收到的预览数据
        cameraTexture.getTransformMatrix(mvpMatrix);

        if (filterChain != null) {
            filterChain.setCameraMatrix(mvpMatrix, mvpMatrix.length);
            filterChain.proceed(cameraTextrueId[0]);
        }
    }

    public void onSurfaceDestroy() {
        if (filterChain != null) {
            filterChain.release();
            filterChain = null;
        }
    }
}
