package com.songwj.openvideo.opengl.renders;

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


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        for (AbsObjectRender objRender:objectRenders){
            objRender.initProgram();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        for (AbsObjectRender objRender:objectRenders){
            objRender.setProjectAndCameraMatrix(projectMatrix, cameraMatrix);
            objRender.setScreenSize(width, height);
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
