package com.songwj.openvideo.opengl.filter;

import android.opengl.GLES30;
import android.opengl.Matrix;

import com.songwj.openvideo.MyApplication;
import com.songwj.openvideo.R;
import com.songwj.openvideo.opengl.filter.base.AbstractChainRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractFboRectFilter;
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;
import com.songwj.openvideo.opengl.filter.base.FilterContext;
import com.songwj.openvideo.opengl.utils.GLDataUtil;
import com.songwj.openvideo.opengl.utils.TextureUtils;

import java.nio.FloatBuffer;

public class CubeFilter extends AbstractChainRectFilter {

    public CubeFilter() {
        super("uniform mat4 vMatrix;\n" +
                        "attribute vec4 vPositionCoord;\n" + //NDK坐标点
                        "attribute vec2 vTextureCoord;\n" +
                        "varying   vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                        " void main() {\n" +
                        "    gl_Position = vMatrix * vPositionCoord;\n" +
                        "    aTextureCoord = vTextureCoord;\n" +
                        " }",
                "precision mediump float;\n" +
                        "uniform sampler2D vTexture;\n" +
                        "varying vec2 aTextureCoord;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(vTexture, aTextureCoord);\n" +
                        "}");
    }

    private final float[] cubePositionCoods = new float[]{
            -0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f,
            -0.5f, 0.5f, -0.5f, 1.0f,
            -0.5f, -0.5f, -0.5f, 1.0f,
            -0.5f, -0.5f, 0.5f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, -0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, -0.5f, 1.0f,
            -0.5f, -0.5f, -0.5f, 1.0f,
            -0.5f, -0.5f, -0.5f, 1.0f,
            -0.5f, -0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f,
            -0.5f, -0.5f, 0.5f, 1.0f,
            -0.5f, -0.5f, -0.5f, 1.0f,
            -0.5f, 0.5f, -0.5f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, 0.5f, 1.0f,
            -0.5f, 0.5f, -0.5f, 1.0f,
    };
    //-------------- 立方体物体纹理坐标 ----------------------
    private final float[] cubeTextureCoords = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f
    };

    private float[] cubeVertices = new float[] {
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
    };

    // 顶点坐标buffer
    private FloatBuffer cubeVertexBuffer;
    // 纹理坐标buffer
    private FloatBuffer cubeTextureBuffer;

    private int[] cubeTextureIds = new int[1];
    private int cubeTextureId = 0;
    private int vMatrixHandler = -1;

    private float[] projectMatrix= new float[16];
    private float[] cameraMatrix= new float[16];
    private float[] mvpMatrix= new float[16];
    private float[] modelMatrix= new float[16];

    //累计旋转过的角度
    private float angle = 0f;

    @Override
    public void onCreated() {
        super.onCreated();
        cubeTextureId = TextureUtils.loadTexture(MyApplication.Companion.getInstance(), R.drawable.hzw5);
        cubeTextureIds[0] = cubeTextureId;
        if (program != -1) {
            vMatrixHandler = GLES30.glGetUniformLocation(program, "vMatrix");
        }

        // 顶点着色器坐标保存
        cubeVertexBuffer = GLDataUtil.createFloatBuffer(cubePositionCoods);
        // 片元着色器坐标保存
        cubeTextureBuffer = GLDataUtil.createFloatBuffer(cubeTextureCoords);
    }

    @Override
    public int proceed(int textureId, FilterChain filterChain) {
//        createFboFrame(filterChain.getContext().width, filterChain.getContext().height);
//        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);
//        super.onDrawFrame(textureId, filterChain);
//        onDrawCube(textureId, filterChain);
//        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, GLES30.GL_NONE);
//        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
//        //!! 返回的是FBO创建的纹理frameTextures[0]
//        return filterChain.proceed(frameTextures[0]);
        return filterChain.proceed(onDrawCube(textureId, filterChain));
    }

    @Override
    protected void activeTexture(int textureId) {

    }

    @Override
    protected void beforeDraw(int textureId, FilterChain filterChain) {

    }

    @Override
    protected void afterDraw(int textureId, FilterChain filterChain) {
//        onDrawCube(textureId, filterChain);
    }

    @Override
    public void onDestroy() {
        releaseCube();
        super.onDestroy();
    }

    private int onDrawCube(int textureId, FilterChain filterChain) {
        if (program == -1) {
            return textureId;
        }
        initMatrix(filterChain.getContext());

        // 开启混合模式
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glUseProgram(program);

        GLES30.glVertexAttribPointer(vPositionCoordHandler, 4, GLES30.GL_FLOAT,
                false, 0, cubeVertexBuffer);
        GLES30.glEnableVertexAttribArray(vPositionCoordHandler);

        GLES30.glVertexAttribPointer(vTextureCoordHandler, 2, GLES30.GL_FLOAT,
                false, 0, cubeTextureBuffer);
        GLES30.glEnableVertexAttribArray(vTextureCoordHandler);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -2f);
        Matrix.scaleM(modelMatrix, 0, 0.5f, 0.5f, 0.5f);
        Matrix.rotateM(modelMatrix, 0, angle, 1.0f, 1.0f, 1.0f);
        Matrix.multiplyMM(mvpMatrix, 0, cameraMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(vMatrixHandler, 1, false, mvpMatrix, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cubeTextureId);
        GLES30.glUniform1i(vTextureHandler, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
//        GLES30.glUseProgram(0);

        // 关闭混合模式
        GLES30.glDisable(GLES30.GL_BLEND);

        angle += 1;
        if(angle >= 360){
            angle = 0f;
        }
        return textureId;
    }

    private boolean isInitMatrix = false;
    private void initMatrix(FilterContext context) {
        if (isInitMatrix) {
            return;
        }
        isInitMatrix = true;
        Matrix.setIdentityM(projectMatrix, 0);
        Matrix.setIdentityM(cameraMatrix, 0);
        float ratio = ((context.width + 0.0f) / context.height);
        Matrix.frustumM(projectMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f);
        Matrix.setLookAtM(cameraMatrix, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f);
    }

    private void releaseCube() {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glDeleteTextures(1, cubeTextureIds, 0);
    }
}
