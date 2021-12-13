package com.songwj.openvideo.opengl.filter;

import android.opengl.GLES20;
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

/**
 * 因为fbo的原因，cube无法完全显示出来
 */
public class CubeFilter extends AbstractFboRectFilter {

    public CubeFilter() {
        super("uniform mat4 vMatrix;\n" +
                        "attribute vec3 vPositionCoord;\n" + //NDK坐标点
                        "attribute vec2 vTextureCoord;\n" +
                        "varying   vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                        " void main() {\n" +
                        "    gl_Position = vMatrix * vec4(vPositionCoord, 1.0);\n" +
                        "    aTextureCoord = vTextureCoord;\n" +
                        " }",
                "precision mediump float;\n" +
                        "uniform sampler2D vTexture;\n" +
                        "varying vec2 aTextureCoord;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(vTexture, aTextureCoord);\n" +
                        "}");
    }

//    private final float[] cubePositionCoods = new float[]{
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, 0.5f, -0.5f, 1.0f,
//            0.5f, 0.5f, -0.5f, 1.0f,
//            -0.5f, 0.5f, -0.5f, 1.0f,
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            -0.5f, -0.5f, 0.5f, 1.0f,
//            0.5f, -0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, -0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, -0.5f, 1.0f,
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            -0.5f, -0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, -0.5f, 1.0f,
//            0.5f, -0.5f, 0.5f, 1.0f,
//            0.5f, -0.5f, 0.5f, 1.0f,
//            -0.5f, -0.5f, 0.5f, 1.0f,
//            -0.5f, -0.5f, -0.5f, 1.0f,
//            -0.5f, 0.5f, -0.5f, 1.0f,
//            0.5f, 0.5f, -0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, 0.5f, 1.0f,
//            -0.5f, 0.5f, -0.5f, 1.0f,
//    };
private final float[] cubePositionCoods = new float[]{
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        -0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f,
        -0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, -0.5f,
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
        FilterContext context = filterChain.getContext();
        Matrix.setIdentityM(modelMatrix, 0);
        GLES30.glUniformMatrix4fv(vMatrixHandler, 1, false, modelMatrix, 0);
    }

    @Override
    protected void afterDraw(int textureId, FilterChain filterChain) {
        onDrawCube(textureId, filterChain);
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

        GLES30.glUseProgram(program);
        GLES30.glViewport(0, 0, filterChain.getContext().width, filterChain.getContext().height);

//        GLES30.glVertexAttribPointer(vPositionCoordHandler, 4, GLES30.GL_FLOAT,
//                false, 0, cubeVertexBuffer);
        GLES30.glVertexAttribPointer(vPositionCoordHandler, 3, GLES30.GL_FLOAT,
                false, 0, cubeVertexBuffer);
        GLES30.glEnableVertexAttribArray(vPositionCoordHandler);

        GLES30.glVertexAttribPointer(vTextureCoordHandler, 2, GLES30.GL_FLOAT,
                false, 0, cubeTextureBuffer);
        GLES30.glEnableVertexAttribArray(vTextureCoordHandler);

//        FloatBuffer vertexBuffer = GLDataUtil.createFloatBuffer(cubeVertices);
//        GLES30.glVertexAttribPointer(vPositionCoordHandler, 3, GLES30.GL_FLOAT,
//                false, 5 * 4, vertexBuffer);
//        GLES30.glEnableVertexAttribArray(vPositionCoordHandler);
//        vertexBuffer.position(3);
//        GLES30.glVertexAttribPointer(vTextureCoordHandler, 2, GLES30.GL_FLOAT,
//                false, 5 * 4, vertexBuffer);
//        GLES30.glEnableVertexAttribArray(vTextureCoordHandler);

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
        GLES30.glDisableVertexAttribArray(vPositionCoordHandler);
        GLES30.glDisableVertexAttribArray(vTextureCoordHandler);
        GLES30.glUseProgram(0);

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
//        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glDeleteTextures(1, cubeTextureIds, 0);
    }
}
