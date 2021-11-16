package com.songwj.openvideo.opengl.filter.base;

public class FilterContext {
    // 投影矩阵
    public float[] projectMatrix = new float[16];
    // 相机矩阵
    public float[] cameraMatrix  = new float[16];
    // 屏幕宽和高
    public int width = 0, height = 0;

    public float[] getProjectMatrix() {
        return projectMatrix;
    }

    public FilterContext setProjectMatrix(float[] projectMatrix, int length) {
        if (length == 16) {
            for (int i = 0; i < 16; i++) {
                this.projectMatrix[i] = projectMatrix[i];
            }
            return this;
        }
        this.projectMatrix = projectMatrix;
        return this;
    }

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
