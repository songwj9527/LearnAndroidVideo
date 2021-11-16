package com.songwj.openvideo.opengl.renders;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Render基类，主要便于在画面上叠加其他渲染对象
 */
public class BaseAbsRender implements GLSurfaceView.Renderer {
    private static final String TAG = "BaseCameraRenderer";
    protected List<AbsObjectRender> objectRenders = new ArrayList<>();
    protected float[] projectMatrix = new float[16];
    protected float[] cameraMatrix  = new float[16];

    public void setObjectRender(AbsObjectRender absObjectRender){
        objectRenders.clear();
        objectRenders.add(absObjectRender);
    }

    public void setObjectRenders(List<AbsObjectRender> absObjectRenders){
        objectRenders.clear();
        objectRenders.addAll(absObjectRenders);
    }

    public void addObjectRender(AbsObjectRender absObjectRender){
        objectRenders.add(absObjectRender);
    }

    public void addObjectRender(AbsObjectRender absObjectRender, int index){
        if (index > objectRenders.size()) {
            objectRenders.add(absObjectRender);
            return;
        }
        objectRenders.add(index, absObjectRender);
    }

    public void deleteObjectRender(int index) {
        if (index >= 0 && index < objectRenders.size()) {
            objectRenders.remove(index);
        }
    }

    public int getObjectRenderSize() {
        return objectRenders.size();
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES30.glClearColor(0f, 0f, 0f, 0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        //------开启混合，即半透明---------
        // 开启很混合模式
        GLES30.glEnable(GLES30.GL_BLEND);
        // 配置混合算法
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        //------------------------------

        for (AbsObjectRender objRender:objectRenders){
            objRender.initProgram();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        for (AbsObjectRender objRender:objectRenders){
            objRender.setProjectAndCameraMatrix(projectMatrix, cameraMatrix);
            objRender.setScreenSize(width, height);
            objRender.initMatrix();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
//        Log.e(TAG, "onDrawFrame: onDrawFrame");
        for (AbsObjectRender objRender:objectRenders){
            if (!objRender.isPrepared()){   //初始化不成功，先进行初始化
                Log.e(TAG, "onDrawFrame: 初始化不成功，重新初始化");
                objRender.initProgram();
                objRender.setProjectAndCameraMatrix(projectMatrix, cameraMatrix);
            }
            objRender.onDrawFrame();
        }
    }

    public void release() {
        for (AbsObjectRender item : objectRenders) {
            item.release();
        }
        objectRenders.clear();
    }
}
