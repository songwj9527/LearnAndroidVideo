package com.songwj.openvideo.opengl.filter;

import android.opengl.GLES30;

import com.songwj.openvideo.opengl.filter.base.AbstractChainRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractFboRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;

public class DuskColorFilter extends AbstractFboRectFilter {

    public DuskColorFilter() {
        super("attribute vec4 vPositionCoord;\n" + //NDK坐标点
                        "attribute vec2 vTextureCoord;\n" +
                        "varying vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                        " void main() {\n" +
                        "    gl_Position = vPositionCoord;\n" +
                        "    aTextureCoord = vTextureCoord;\n" +
                        " }",
                "precision mediump float;\n" +
                        "uniform sampler2D vTexture;\n" +
                        "varying vec2 aTextureCoord;\n" +
                        "void main() {\n" +
                        "    vec4 rgba = texture2D(vTexture, aTextureCoord);\n" +
                        // 滤镜效果
                        "    gl_FragColor = vec4(rgba.r * 0.875, rgba.g * 0.62, rgba.b * 0.863, rgba.a);\n" +
                        "}");
    }

    @Override
    protected void activeTexture(int textureId) {
        //激活指定纹理单元
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理ID到纹理单元
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        //将激活的纹理单元传递到着色器里面
        GLES30.glUniform1i(vTextureHandler, 0);
        //配置边缘过渡参数
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    @Override
    protected void beforeDraw(int textureId, FilterChain filterChain) {

    }

    @Override
    protected void afterDraw(int textureId, FilterChain filterChain) {

    }
}
