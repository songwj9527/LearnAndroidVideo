package com.songwj.openvideo.opengl.renders

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import com.songwj.openvideo.MyApplication
import com.songwj.openvideo.R
import java.nio.FloatBuffer

class CubeRender : AbsObjectRender() {
    //2-------------- 立方体物体顶点纹理坐标 ----------------------
    private var cubeVertices: FloatArray? = floatArrayOf( // positions          // texture Coords
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
    )

    private val vertexShaderCode =  "uniform mat4 vMatrix;\n" +
            "attribute vec3 vPositionCoord;\n" +
            "attribute vec2 vTextureCoord;\n" +
            "varying vec2 aTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vMatrix * vec4(vPositionCoord, 1.0);\n" +
            "    aTextureCoord = vTextureCoord;\n" +
            "}"
    private val fragmentShaderCode =  "precision mediump float;\n" +
            "uniform sampler2D vTexture;\n" +
            "varying vec2 aTextureCoord;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(vTexture, aTextureCoord);\n" +
            "}"

    private var cubeTexture:Int = 0

    private var positionHandle:Int = 0
    private var textCoordsHandle:Int = 0
    private var mvpMatrixHandle:Int =0
    private var texturePosHandle:Int =0

    private val mvpMatrix: FloatArray? = FloatArray(16)
    private val modelMatrix: FloatArray? = FloatArray(16)

    //累计旋转过的角度
    private var angle = 0f

    override fun initProgram() {
        cubeTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.hzw5)

        var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
        var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
        program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        positionHandle = GLES20.glGetAttribLocation(program, "vPositionCoord")
        textCoordsHandle = GLES20.glGetAttribLocation(program, "vTextureCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "vMatrix")
        texturePosHandle = GLES20.glGetUniformLocation(program, "vTexture")
    }

    override fun initMatrix() {
        Matrix.setIdentityM(projectMatrix, 0)
        Matrix.setIdentityM(cameraMatrix, 0)
        var ratio:Float = ((width+0.0f)/height)
        Matrix.frustumM(projectMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
        Matrix.setLookAtM(cameraMatrix, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
    }

    override fun onDrawFrame() {
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texturePosHandle)

        val vertexBuffer: FloatBuffer = GLDataUtil.createFloatBuffer(cubeVertices)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
            false, 5 * 4, vertexBuffer)
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(textCoordsHandle, 2, GLES20.GL_FLOAT,
            false, 5 * 4, vertexBuffer)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -2f)
        Matrix.scaleM(modelMatrix, 0, 0.5f, 0.5f, 0.5f)
//        Matrix.rotateM(modelMatrix, 0, 45f, 1.0f, 1.0f, 0f)
        Matrix.rotateM(modelMatrix, 0, angle, 1.0f, 1.0f, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, cameraMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES30.GL_TEXTURE_2D, cubeTexture)
        GLES20.glUniform1i(texturePosHandle, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texturePosHandle)
        GLES20.glUseProgram(0)

        angle += 1
        if(angle >= 360){
            angle = 0F
        }
    }

    override fun release() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(cubeTexture), 0)
        GLES20.glDeleteProgram(program)
    }

}