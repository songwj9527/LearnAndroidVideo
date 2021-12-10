package com.songwj.openvideo.opengl.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.songwj.openvideo.opengl.filter.base.AbstractFboRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;
import com.songwj.openvideo.opengl.filter.base.FilterContext;

public class CameraFilter extends AbstractRectFilter {
    private int vMatrixHandler = -1;

    public CameraFilter() {
        super("uniform mat4 vMatrix;\n" +
                        "attribute vec4 vPositionCoord;\n" + //NDK坐标点
                        "attribute vec2 vTextureCoord;\n" +
                        "varying   vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                        " void main() {\n" +
                        "    gl_Position = vPositionCoord;\n" +
                        "    aTextureCoord = (vMatrix * vec4(vTextureCoord, 1.0, 1.0)).xy;\n" +
                        " }",
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "uniform samplerExternalOES vTexture;\n" + // samplerExternalOES: 图片， 采样器
                        "varying vec2 aTextureCoord;\n" +
                        "void main() {\n" +
                        "    vec4 rgba = texture2D(vTexture, aTextureCoord);\n" +
                        "    gl_FragColor = rgba;\n" +
                        "}");
    }

    @Override
    public void onCreated() {
        super.onCreated();
        if (program != -1) {
            vMatrixHandler = GLES30.glGetUniformLocation(program, "vMatrix");
        }
    }

    @Override
    protected void activeTexture(int textureId) {
        //激活指定纹理单元
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        //将激活的纹理单元传递到着色器里面
        GLES30.glUniform1i(vTextureHandler, 0);
        //配置边缘过渡参数
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    @Override
    protected void beforeDraw(int textureId, FilterChain filterChain) {
        FilterContext context = filterChain.getContext();
        GLES30.glUniformMatrix4fv(vMatrixHandler, 1, false, context.cameraMatrix, 0);
    }

    @Override
    protected void afterDraw(int textureId, FilterChain filterChain) {

    }
}
