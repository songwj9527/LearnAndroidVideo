package com.songwj.openvideo.opengl.renders;

import java.nio.FloatBuffer;

/**
 * 【说明】：添加其他绘制元素时的抽象类
 */
public abstract class AbsObjectRender {

    //投影矩阵
    protected float[] projectMatrix = new float[16];
    //相机矩阵
    protected float[] cameraMatrix  = new float[16];
    //顶点数组buffer
    private FloatBuffer vertexBuffer;
    //颜色数组buffer
    private FloatBuffer colorBuffer;
    //渲染程序
    protected int program = 0;
    //屏幕宽和高
    protected int width =0;
    protected int height =0;

    /**
     * 设置屏幕宽和高
     * @param width
     * @param heigt
     */
    public void setScreenSize(int width, int heigt){
        this.width = width;
        this.height = heigt;
    }

    /**
     * 说明】： 在onSurfaceCreated中调用,program要在onSurfaceCreated中调用才能成功
     */
    abstract public void initProgram();

    /**
     *
     * @return
     */
    public boolean isPrepared(){
        return !(program == 0);
    }

    /**
     * 【说明】：在onSurfaceChanged中调用，保存投影矩阵和相机矩阵
     * @param projectMatrix
     * @param cameraMatrix
     */
    public void setProjectAndCameraMatrix(float[] projectMatrix,float[] cameraMatrix){
        this.projectMatrix = projectMatrix;
        this.cameraMatrix = cameraMatrix;
    }

    /**
    *【说明】：在onDrawFrame中调用
    */
    abstract public void onDrawFrame();

    /**
     * 释放资源
     */
    abstract public void release();
}
