package com.songwj.openvideo.opengl.filter.base;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.songwj.openvideo.opengl.renders.GLDataUtil;
import com.songwj.openvideo.opengl.renders.ShaderUtils;

import java.nio.FloatBuffer;

abstract public class AbstractRectFilter {
    // 顶点坐标
    public float[] vertexCoords = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    // 纹理坐标
    public float[] textureCoords = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };
    // 顶点坐标buffer
    protected FloatBuffer vertexBuffer;
    // 纹理坐标buffer
    protected FloatBuffer textureBuffer;
    // 顶点坐标索引
    protected int vPositionCoordHandler = -1;
    // 纹理坐标索引
    protected int vTextureCoordHandler = -1;
    // 纹理索引
    protected int vTextureHandler = -1;
    // 渲染程序
    protected int program = -1;

    private String vertexShaderStr;
    private String fragmentShaderStr;

    // 投影矩阵
    protected float[] projectMatrix = new float[16];
    // 相机矩阵
    protected float[] cameraMatrix  = new float[16];
    // 视图宽高
    protected int width = 0, height = 0;

    public AbstractRectFilter(String vertexShaderStr, String fragmentShaderStr) {
        // 顶点着色器坐标保存
        vertexBuffer = GLDataUtil.createFloatBuffer(vertexCoords);
        // 片元着色器坐标保存
        textureBuffer = GLDataUtil.createFloatBuffer(textureCoords);

        this.vertexShaderStr = vertexShaderStr;
        this.fragmentShaderStr = fragmentShaderStr;
    }

    public void onSurfaceCreated() {
        Matrix.setIdentityM(projectMatrix, 0);
        Matrix.setIdentityM(cameraMatrix, 0);

        //编译顶点着色程序
        int vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr);
        //编译片段着色程序
        int fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr);
        //连接程序
        program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId);
        vPositionCoordHandler = GLES30.glGetAttribLocation(program,"vPositionCoord");
        vTextureCoordHandler = GLES20.glGetAttribLocation(program,"vTextureCoord");
        vTextureHandler = GLES20.glGetUniformLocation(program, "vTexture");
    }

    public void onSurfaceChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int onDrawFrame(int textureId) {
        if (program == -1) {
            return textureId;
        }
        Log.e("AbstractRectFilter", "onDrawFrame()");
        // 1- 设置视图的尺寸和位置
        GLES30.glUseProgram(program);
        GLES30.glViewport(0, 0, width, height);
        //2- 执行绘图操作
        //左乘矩阵
        //x,y 所以数据size 是2
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(vPositionCoordHandler, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        //启用顶点坐标句柄
        GLES30.glEnableVertexAttribArray(vPositionCoordHandler);

        //纹理坐标是x,y 所以数据size是 2
        textureBuffer.position(0);
        GLES30.glVertexAttribPointer(vTextureCoordHandler, 2, GLES30.GL_FLOAT, false, 0, textureBuffer);
        //启用纹理坐标句柄
        GLES30.glEnableVertexAttribArray(vTextureCoordHandler);

        //启动纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // 0: 图层ID  GL_TEXTURE0
        // 1: 图层ID  GL_TEXTURE1
        GLES30.glUniform1i(vTextureHandler, 0);

        beforeDraw();

        // 进行绘图
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCoords.length / 2);

        afterDraw();

        // 为何 texture=0？
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        return textureId;
    }

    public int onDrawFrame(int textureId, FilterChain filterChain) {
        return onDrawFrame(textureId);
    }

    public void onSurfaceDestroy() {
        if(program != -1){
            GLES30.glDisableVertexAttribArray(vTextureCoordHandler);
            GLES30.glDisableVertexAttribArray(vTextureCoordHandler);
            GLES30.glDeleteProgram(program);
        }
    }

    protected abstract void beforeDraw();
    protected abstract void afterDraw();
}
