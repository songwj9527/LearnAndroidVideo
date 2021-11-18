package com.songwj.openvideo.opengl.filter;

import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;

public class CameraFilter extends AbstractRectFilter {

    public CameraFilter() {

        super("uniform mat4 vMatrix;\n" +
                        "attribute vec4 vPositionCoord;\n" + //NDK坐标点
                        "attribute vec2 vTextureCoord;\n" +
                        "varying   vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                        " void main() {\n" +
//                                "     gl_Position = (vMatrix * vPositionCoord).xyww;\n" +
                        "     gl_Position = vMatrix * vPositionCoord;\n" +
                        "     aTextureCoord = vTextureCoord;\n" +
                        " }",
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "uniform samplerExternalOES vTexture;\n" +
                        "varying vec2 aTextureCoord;\n" +
                        "void main() {\n" +
                        "    vec4 tc = texture2D(vTexture, aTextureCoord);\n" +
                        "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                        //    gl_FragColor = vec4(color,color,color,1.0);\n
                        "    gl_FragColor = tc;\n" +
                        "}");
    }


    @Override
    protected void beforeDraw() {

    }

    @Override
    public int onDrawFrame(int textureId, FilterChain filterChain) {
        return super.onDrawFrame(textureId, filterChain);
    }

    @Override
    protected void afterDraw() {

    }
}
