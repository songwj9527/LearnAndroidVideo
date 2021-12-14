package com.songwj.openvideo.opengl.filter;

import android.opengl.GLES20;
import android.opengl.GLES30;

import com.songwj.openvideo.opengl.filter.base.AbstractFboRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;

public class SoulFilter extends AbstractFboRectFilter {
    private int mixturePercent;
    private int scalePercent;

    float mix = 0.0f; //透明度，越大越透明
    float scale = 0.0f; //缩放，越大就放的越大

    public SoulFilter() {
        super("attribute vec4 vPositionCoord;\n" + //顶点坐标
                        "attribute vec2 vTextureCoord;\n" + // 纹理坐标
                        "varying   vec2 aTextureCoord;\n" + // 纹理坐标点变换后输出
                        "void main(){\n" +
                        //内置变量： 把坐标点赋值给gl_position 就Ok了。
                        "    gl_Position = vPositionCoord;\n" +
                        "    aTextureCoord = vTextureCoord;\n" +
                        "}",
                "varying highp vec2 aTextureCoord;\n" +
                        "uniform sampler2D vTexture;\n" +
                        "uniform lowp float mixturePercent;\n" +
                        "uniform highp float scalePercent;\n" +
                        "void main() {\n" +
                        "    lowp vec4 textureColor = texture2D(vTexture, aTextureCoord);\n" +
                        "    highp vec2 textureCoordinateToUse = aTextureCoord;\n" +
                        // 纹理中心点
                        "    highp vec2 center = vec2(0.5, 0.5);\n" +
                        // 当前要上颜色的点 与中心点的偏移
                        "    textureCoordinateToUse -= center;\n" +
                        //scalePercent： 放大参数
                        // 如果大于1
                        "    textureCoordinateToUse = textureCoordinateToUse / scalePercent;\n" +
                        "    textureCoordinateToUse += center;\n" +
                        "    lowp vec4 textureColor2 = texture2D(vTexture, textureCoordinateToUse);\n" +
                        "    gl_FragColor = mix(textureColor, textureColor2, mixturePercent);\n" +
                        "}");
    }

    @Override
    public void onCreated() {
        super.onCreated();
        if (program != -1) {
            mixturePercent = GLES30.glGetUniformLocation(program, "mixturePercent");
            scalePercent = GLES30.glGetUniformLocation(program, "scalePercent");
        }
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
        GLES30.glUniform1f(mixturePercent, 1.0f - mix);
        GLES30.glUniform1f(scalePercent, scale + 1.0f);

        mix += 0.03f;
        scale += 0.03f;
        if (mix >= 0.8) {
            mix = 0.0f;
        }
        if (scale >= 0.8) {
            scale = 0.0f;
        }
    }

    @Override
    protected void afterDraw(int textureId, FilterChain filterChain) {

    }
}
