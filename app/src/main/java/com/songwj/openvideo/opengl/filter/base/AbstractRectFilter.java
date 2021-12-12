package com.songwj.openvideo.opengl.filter.base;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.songwj.openvideo.opengl.utils.GLDataUtil;
import com.songwj.openvideo.opengl.utils.ShaderUtils;

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

    public AbstractRectFilter(String vertexShaderStr, String fragmentShaderStr) {
        // 顶点着色器坐标保存
        vertexBuffer = GLDataUtil.createFloatBuffer(vertexCoords);
        // 片元着色器坐标保存
        textureBuffer = GLDataUtil.createFloatBuffer(textureCoords);

        this.vertexShaderStr = vertexShaderStr;
        this.fragmentShaderStr = fragmentShaderStr;
    }

    public void onCreated() {
        //编译顶点着色程序
        int vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr);
        //编译片段着色程序
        int fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr);
        //连接程序
        program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId);
        if (program != -1) {
            vPositionCoordHandler = GLES30.glGetAttribLocation(program, "vPositionCoord");
            vTextureCoordHandler = GLES30.glGetAttribLocation(program, "vTextureCoord");
            vTextureHandler = GLES30.glGetUniformLocation(program, "vTexture");
        }
    }

    public int onDrawFrame(int textureId, FilterChain filterChain) {
        if (program == -1) {
            return textureId;
        }
        // 1- 设置视图的尺寸和位置
        GLES30.glUseProgram(program);
        GLES30.glViewport(0, 0, filterChain.getContext().width, filterChain.getContext().height);

//        //启动纹理
//        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
//        //绑定纹理
//        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
//
//        // 0: 图层ID  GL_TEXTURE0
//        // 1: 图层ID  GL_TEXTURE1
//        GLES30.glUniform1i(vTextureHandler, 0);
        activeTexture(textureId);
        doDraw(textureId, filterChain);
        return textureId;
    }

    protected abstract void activeTexture(int textureId);

    private void doDraw(int textureId, FilterChain filterChain) {
        //启用顶点坐标句柄
        GLES30.glEnableVertexAttribArray(vPositionCoordHandler);
        //启用纹理坐标句柄
        GLES30.glEnableVertexAttribArray(vTextureCoordHandler);

        //左乘矩阵
        //x,y 所以数据size 是2
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(vPositionCoordHandler, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        //纹理坐标是x,y 所以数据size是 2
        textureBuffer.position(0);
        GLES30.glVertexAttribPointer(vTextureCoordHandler, 2, GLES30.GL_FLOAT, false, 0, textureBuffer);

        beforeDraw(textureId, filterChain);

        // 进行绘图
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCoords.length / 2);

        afterDraw(textureId, filterChain);

        // 为何 texture=0？
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public void onDestroy() {
        if(program != -1){
            GLES30.glDisableVertexAttribArray(vTextureCoordHandler);
            GLES30.glDisableVertexAttribArray(vPositionCoordHandler);
            GLES30.glDeleteProgram(program);
            program = -1;
        }
    }


    protected abstract void beforeDraw(int textureId, FilterChain filterChain);
    protected abstract void afterDraw(int textureId, FilterChain filterChain);
}
