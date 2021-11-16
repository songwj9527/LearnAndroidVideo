package com.songwj.openvideo.opengl.renders

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.SystemClock
import com.songwj.openvideo.MyApplication
import com.songwj.openvideo.R
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FengLightRender : BaseAbsRender() {

    private var viewWidth:Int = 0
    private var viewHeight:Int = 0
    private var bg = Color.BLACK

    // 视点
    private val viewPos = floatArrayOf(1.5f, -1f, 4f, 1f)
    private var angleInDegrees = 0f
    // 光源
    private var lightPosInModelSpace: FloatArray? = floatArrayOf(0f, 0.4f, 1f, 1f)
    private val lightPosInWorldSpace:FloatArray? = FloatArray(4)
    private var lightPosInEyeSpace = FloatArray(4)
    private var lightModelMatrix:FloatArray?=FloatArray(16)

    init {
        val list = ArrayList<AbsObjectRender>()
        list.add(LightRender())
        list.add(CubeRender())
        setObjectRenders(list)
    }

    override fun onSurfaceCreated(gl10: GL10?, eglConfig: EGLConfig?) {
        // 设置背景色
        GLES20.glClearColor(Color.red(bg) / 255.0f, Color.green(bg) / 255.0f,
            Color.blue(bg) / 255.0f, Color.alpha(bg) / 255.0f)
        super.onSurfaceCreated(gl10, eglConfig)
    }

    override fun onSurfaceChanged(gl10: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        var ratio:Float =(width+0.0f)/height
        // 设置透视投影矩阵，近点是3，远点是8
        Matrix.frustumM(projectMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 8f)
        Matrix.setLookAtM(cameraMatrix, 0, viewPos[0], viewPos[1], viewPos[2],
            0f, 0f, 0f,
            0f, 1.0f, 0.0f)
        GLES20.glViewport(0, 0, width, height)
        super.onSurfaceChanged(gl10, width, height)
    }

    override fun onDrawFrame(gl10: GL10?) {
        // 设置显示范围
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        // 设置显示范围
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        angleInDegrees = 360.0f / 10000.0f * (SystemClock.uptimeMillis() % 10000L)
        Matrix.setIdentityM(lightModelMatrix, 0)
        Matrix.rotateM(lightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f)

        //Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, 1.0f);
        Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0)
        Matrix.multiplyMV(lightPosInEyeSpace, 0, cameraMatrix, 0, lightPosInWorldSpace, 0)

        super.onDrawFrame(gl10)
    }


    inner class CubeRender : AbsObjectRender() {
        //2-------------- 立方体物体顶点纹理坐标 ----------------------
        private var cubeVertices: FloatArray? = floatArrayOf(
            // positions（3位）          // texture Coords(2位)  //normal(3位)
            //后
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,0.0f,0.0f,-1.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 0.0f,0.0f,0.0f,-1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,0.0f,0.0f,-1.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,0.0f,0.0f,-1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,0.0f,0.0f,-1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,0.0f,0.0f,-1.0f,
            //前
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,0.0f, 0.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,0.0f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,0.0f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 1.0f,0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,0.0f, 0.0f, 1.0f,
            //左
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,-1.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,-1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,-1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,-1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,-1.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,-1.0f, 0.0f, 0.0f,
            //右
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,1.0f, 0.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 1.0f,1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 0.0f, 0.0f,1.0f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,1.0f, 0.0f, 0.0f,
            //下
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,0.0f, -1.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 1.0f,0.0f, -1.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,0.0f, -1.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f,0.0f, -1.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,0.0f, -1.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,0.0f, -1.0f, 0.0f,
            //上
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 1.0f,0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f,0.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,0.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,0.0f, 1.0f, 0.0f
        )

        // 物体的shadeCode
        private val vertexShaderCode = "uniform mat4 uMVMatrix;\n" +
                "uniform mat4 uMVPMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                // 法向量
                "attribute vec3 aNormal;\n" +
                "attribute vec3 objectColor;\n" +
                "varying vec3 fragPos;\n" +
                "varying vec3 norm;\n" +
                // 纹理坐标
                "attribute vec2 aTexCoords;\n" +
                "varying vec2 TexCoord;\n" +
                "void main() {\n" +
                // 乘以model view矩阵(uMVMatrix)，转换到观察空间
                "    fragPos = vec3(uMVMatrix * aPosition);\n" +
                "    norm = normalize(vec3(uMVMatrix * vec4(aNormal, 0.0)));\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                // 纹理坐标
                "    TexCoord = aTexCoords;\n" +
                "}"
        private val fragmentShaderCode ="precision mediump float;\n" +
                "varying vec2 TexCoord;\n" +
                "varying vec3 fragPos;\n" +
                "varying vec3 norm;\n" +
                "uniform vec3 aLightPos;\n" +
                "uniform sampler2D texture;\n" +
                "void main() {\n" +
                //1--- 环境光照
                "    float ambientStrength = 0.3;\n" +
                "    vec3 lightColor = vec3(1.0, 1.0, 1.0);\n" +
                "    vec3 ambient = ambientStrength * lightColor;\n" +
                //2-- 漫反射光照
                // 材质漫反射系数
                "    float diffuseStrength = 0.5;\n" +
                // 归一化光源线
                "    vec3 lightDir = normalize(aLightPos - fragPos);\n" +
                "    float diff = max(dot(norm, lightDir), 0.0);\n" +
                "    vec3 diffuse = diffuseStrength*diff * lightColor;\n" +
                //3-- 镜面光照
                "    float specularStrength = 2.5;\n" +
                "    vec3 viewDir = normalize(-fragPos);\n" +//在观察空间计算的好处是，观察者的位置总是(0, 0, 0)，
                // lightDir向量进行了取反。reflect函数要求第一个向量是从光源指向片段位置的向量
                "    vec3 reflectDir = reflect(-lightDir, norm);\n" +
                "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);\n" +
                "    vec3 specular = spec * specularStrength * lightColor;\n" +
                "    vec4 textColor = texture2D(texture,TexCoord);\n" +
                // 结果
                "    vec3 result = (ambient + diffuse + specular) * vec3(textColor);\n" +//-- 1颜色
                "    gl_FragColor = vec4(result, 1.0);\n" +
                "}"

        private var cubeTexture:Int = 0

        private var positionHandle:Int = 0
        private var textureCoordsHandle:Int = 0
        private var mvMatrixHandle:Int = 0
        private var mvpMatrixHandle:Int = 0
        private var normalHandle:Int = 0
        private var lightPosHandle:Int = 0
        private var texturePosHandle:Int = 0

        // 顶点数据buffer
        private lateinit var vertexBuffer: FloatBuffer

        private var mvpMatrix:FloatArray?= FloatArray(16)
        private var modelMatrix:FloatArray?=FloatArray(16)

        override fun initProgram() {
            //纹理的创建也需要放到opengl线程
            cubeTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.hzw5)
            vertexBuffer = GLDataUtil.createFloatBuffer(cubeVertices)
//            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
//            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
//            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
//            GLES20.glUseProgram(program)
//            if (program != 0) {
//                // 顶点坐标
//                positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
//                // 纹理坐标
//                textureCoordsHandle = GLES20.glGetAttribLocation(program, "aTexCoords")
//                mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix")
//                mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
//                // 法向量
//                normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
//                lightPosHandle = GLES20.glGetUniformLocation(program, "aLightPos")
//                texturePosHandle = GLES20.glGetUniformLocation(program, "texture")
//
//                //纹理的创建也需要放到opengl线程
//                cubeTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.hzw5)
//                vertexBuffer = GLDataUtil.createFloatBuffer(cubeVertices)
//            }
        }

        override fun initMatrix() {

        }

        override fun onDrawFrame() {
            /**
             * 每次渲染都要重新获取一下渲染程序，如果在initProgram()中进行的话，两个render会出现后一个覆盖前一个的情况
             */
            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
            GLES20.glUseProgram(program)
            if (program != 0) {
                // 顶点坐标
                positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
                // 纹理坐标
                textureCoordsHandle = GLES20.glGetAttribLocation(program, "aTexCoords")
                mvMatrixHandle = GLES20.glGetUniformLocation(program, "uMVMatrix")
                mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
                // 法向量
                normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
                lightPosHandle = GLES20.glGetUniformLocation(program, "aLightPos")
                texturePosHandle = GLES20.glGetUniformLocation(program, "texture")

                Matrix.setIdentityM(modelMatrix, 0)

                // 传入顶点坐标
                GLES20.glEnableVertexAttribArray(positionHandle)
                vertexBuffer.position(0)
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)
                vertexBuffer.position(3)
                // 纹理坐标
                GLES20.glEnableVertexAttribArray(textureCoordsHandle)
                GLES20.glVertexAttribPointer(textureCoordsHandle, 2, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)
                // 法向量
                GLES20.glEnableVertexAttribArray(normalHandle)
                vertexBuffer.position(5)
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)

                Matrix.multiplyMM(mvpMatrix, 0, cameraMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvpMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
                // 光源位置 乘以model和view矩阵，转换到观察空间
                GLES20.glUniform3f(lightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2])

                //启用纹理
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES30.GL_TEXTURE_2D, cubeTexture)
                GLES20.glUniform1i(texturePosHandle, 0)

                // 绘制顶点
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, cubeVertices!!.size/(3+2+3))


                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glDisableVertexAttribArray(normalHandle)
                GLES20.glDisableVertexAttribArray(textureCoordsHandle)
            }
        }

        override fun release() {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDeleteTextures(1, intArrayOf(cubeTexture), 0)
            GLES20.glDeleteProgram(program)
        }
    }

    inner class LightRender : AbsObjectRender() {

        // 物体的shadeCode
        private val vertexShaderCode = "//#version 300 es\n" +
                "uniform mat4 uMVPMatrix;\n" +
                "attribute vec3 aPosition;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
                //点大小
                "    gl_PointSize = 25.0;\n" +
                "}"
        private val fragmentShaderCode ="precision mediump float;\n" +
                "void main() {\n" +
                " gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "}"

        private var positionHandle:Int = 0
        private var mvpMatrixHandle:Int = 0

        private var lightMVPMatrix:FloatArray?=FloatArray(16)

        override fun initProgram() {
//            // ---------- 绘制光源 ---------------
//            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
//            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
//            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
//            GLES20.glUseProgram(program)
//            if (program != 0) {
//                // 顶点坐标
//                positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
//                mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
//            }
        }

        override fun initMatrix() {

        }

        override fun onDrawFrame() {
            /**
             * 每次渲染都要重新获取一下渲染程序，如果在initProgram()中进行的话，两个render会出现后一个覆盖前一个的情况
             */
            // ---------- 绘制光源 ---------------
            var vertexShaderId:Int = ShaderUtils.compileVertexShader(vertexShaderCode)
            var fragmentShaderId:Int = ShaderUtils.compileFragmentShader(fragmentShaderCode)
            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
            GLES20.glUseProgram(program)
            if (program != 0) {
                // 顶点坐标
                positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
                mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")

                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                    false, 3 * 4, GLDataUtil.createFloatBuffer(lightPosInModelSpace))

                Matrix.multiplyMM(lightMVPMatrix, 0, cameraMatrix, 0, lightModelMatrix, 0)
                Matrix.multiplyMM(lightMVPMatrix, 0, projectMatrix, 0, lightMVPMatrix, 0)


                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, lightMVPMatrix, 0)

                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)

                GLES20.glDisableVertexAttribArray(positionHandle)
            }
        }

        override fun release() {
            GLES20.glDeleteProgram(program)
        }

    }
}