package com.songwj.openvideo.opengl.filter.base;

public class FilterContext {
    // EGL矩阵
    public float[] cameraMatrix  = new float[16];
    // 屏幕宽和高
    public int width = 0, height = 0;

    public FilterContext setCameraMatrix(float[] cameraMatrix, int length) {
        if (length == 16) {
            for (int i = 0; i < 16; i++) {
                this.cameraMatrix[i] = cameraMatrix[i];
            }
            return this;
        }
        this.cameraMatrix = cameraMatrix;
        return this;
    }


    public FilterContext setWidth(int width) {
        this.width = width;
        return this;
    }

    public FilterContext setHeight(int height) {
        this.height = height;
        return this;
    }
}
