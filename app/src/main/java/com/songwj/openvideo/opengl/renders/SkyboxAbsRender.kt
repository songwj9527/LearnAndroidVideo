package com.songwj.openvideo.opengl.renders

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import com.songwj.openvideo.MyApplication
import com.songwj.openvideo.R
import com.songwj.openvideo.opengl.utils.GLDataUtil
import com.songwj.openvideo.opengl.utils.ShaderUtils
import com.songwj.openvideo.opengl.utils.TextureUtils
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

public class SkyboxAbsRender : BaseAbsRender() {
    private var viewWidth:Int = 0
    private var viewHeight:Int = 0
    private var bg = Color.BLACK

//    private var rotationMatrix:FloatArray? = FloatArray(16)
    private var skyRender: SkyRender? = null

    init {
        val list = ArrayList<AbsObjectRender>()
        skyRender = SkyRender()
        list.add(skyRender!!)
        list.add(CubeRender())
        setObjectRenders(list)
//        Matrix.setIdentityM(rotationMatrix, 0)
    }

    override fun onSurfaceCreated(gl10: GL10?, eglConfig: EGLConfig?) {
        super.onSurfaceCreated(gl10, eglConfig)
        // 设置背景色
        GLES20.glClearColor(Color.red(bg) / 255.0f, Color.green(bg) / 255.0f,
            Color.blue(bg) / 255.0f, Color.alpha(bg) / 255.0f)
    }

    override fun onSurfaceChanged(gl10: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        var ratio:Float = ((width+0.0f)/height)
        Matrix.frustumM(projectMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 1000f)
        Matrix.setLookAtM(cameraMatrix, 0, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        super.onSurfaceChanged(gl10, width, height)
    }

    override fun onDrawFrame(gl10: GL10?) {
        // 设置显示范围
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        super.onDrawFrame(gl10)
    }

    fun rotation(rotateMatrix: FloatArray) {
//        rotationMatrix = rotateMatrix
        skyRender?.rotation(rotateMatrix)
    }


    inner class SkyRender : AbsObjectRender() {
        //1------------- 天空盒skybox坐标 -----------------
        //立方体的8个顶点
        private val skyboxVertices = floatArrayOf(
            -1f, 1f, 1f,  // 上左前顶点
            1f, 1f, 1f,  // 上右前顶点
            -1f, 1f, -1f,  // 上左后顶点
            1f, 1f, -1f,  // 上右后顶点
            -1f, -1f, 1f,  // 下左前顶点
            1f, -1f, 1f,  // 下右前顶点
            -1f, -1f, -1f,  // 下左后顶点
            1f, -1f, -1f)

        // 立方体索引
        private val skyboxIndex = shortArrayOf(
            // Front
            1, 3, 0,
            0, 3, 2,  // Back
            4, 6, 5,
            5, 6, 7,  // Left
            0, 2, 4,
            4, 2, 6,  // Right
            5, 7, 1,
            1, 7, 3,  // Top
            5, 1, 4,
            4, 1, 0,  // Bottom
            6, 2, 7,
            7, 2, 3
        )

        private val vertexShaderCode = "uniform mat4 uMVPMatrix;\n" +
                "attribute vec3 aPosition;\n" +
                "varying vec3 TexCoord;\n" +
                "void main() {\n" +
                "    TexCoord = aPosition;\n" +
                "    vec4 pos = uMVPMatrix * vec4(aPosition, 1.0);\n" +
                // 原来的z用w分量代替，保证背景的深度为1，深度最深，不会覆盖任何前景物体
                "    gl_Position = pos.xyww;\n" +
                "}"
        private val fragmentShaderCode = "precision mediump float;\n" +
                // 代表3D纹理坐标的方向向量
                "varying vec3 TexCoord;\n" +
                // 立方体贴图的纹理采样器
                "uniform samplerCube skybox;\n" +
                "void main() {\n" +
                "    gl_FragColor = textureCube(skybox, TexCoord);\n" +
                "}"

        private var skyboxTexture:Int = 0

        private var positionHandle:Int = 0
        private var mvpMatrixHandle:Int =0
        private var skyBoxPosHandle:Int =0

        private val mvpMatrix: FloatArray? = FloatArray(16)
        private val modelMatrix: FloatArray? = FloatArray(16)
        private var rotationMatrix:FloatArray? = FloatArray(16)

        init {
            Matrix.setIdentityM(rotationMatrix, 0)
        }

        override fun initProgram() {
            skyboxTexture = TextureUtils.createTextureCube(
                MyApplication.getInstance(), intArrayOf(
                R.drawable.ic_cube_maps_right, R.drawable.ic_cube_maps_left, R.drawable.ic_cube_maps_top,
                R.drawable.ic_cube_maps_bottom, R.drawable.ic_cube_maps_back, R.drawable.ic_cube_maps_front
            ))

            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
            program = ShaderUtils.linkProgram(vertexShaderId,fragmentShaderId)
            positionHandle = GLES30.glGetAttribLocation(program, "aPosition")
            mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")
            skyBoxPosHandle = GLES30.glGetUniformLocation(program, "skybox")
        }

        override fun initMatrix() {

        }

        override fun onDrawFrame() {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(positionHandle)

            val vertexBuffer: FloatBuffer = GLDataUtil.createFloatBuffer(skyboxVertices)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                false, 3 * 4, vertexBuffer)
            Matrix.setIdentityM(modelMatrix, 0)
            //Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
            Matrix.multiplyMM(mvpMatrix, 0, cameraMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, rotationMatrix, 0, mvpMatrix, 0)
            Matrix.rotateM(mvpMatrix, 0, 90f, 1f, 0f, 0f)
            Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, skyboxTexture)
            GLES30.glUniform1i(skyBoxPosHandle, 0)
            //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, 36,
                GLES30.GL_UNSIGNED_SHORT, GLDataUtil.createShortBuffer(skyboxIndex))

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glUseProgram(0)
        }

        override fun release() {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDeleteTextures(1, intArrayOf(skyboxTexture), 0)
            GLES20.glDeleteProgram(program)
        }

        fun rotation(rotateMatrix: FloatArray) {
            rotationMatrix = rotateMatrix
        }
    }

    inner class CubeRender : AbsObjectRender() {
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

        private val vertexShaderCode =  "uniform mat4 uMVPMatrix;\n" +
                "attribute vec3 aPosition;\n" +
                "attribute vec2 aTexCoords;\n" +
                "varying vec2 TexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
                "    TexCoord = aTexCoords;\n" +
                "}"
        private val fragmentShaderCode =  "precision mediump float;\n" +
                "uniform sampler2D texture;\n" +
                "varying vec2 TexCoord;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(texture, TexCoord);\n" +
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
            cubeTexture = TextureUtils.loadTexture(MyApplication.getInstance(),R.drawable.hzw5)

            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            textCoordsHandle = GLES20.glGetAttribLocation(program, "aTexCoords")
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            texturePosHandle = GLES20.glGetUniformLocation(program, "texture")
        }

        override fun initMatrix() {

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
}