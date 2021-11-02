package com.songwj.openvideo.opengl.renders

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.SystemClock
import android.renderscript.Matrix4f
import com.songwj.openvideo.MyApplication
import com.songwj.openvideo.R
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class TextureLightRender : BaseAbsRender() {
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

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        // 设置背景色
        GLES20.glClearColor(Color.red(bg) / 255.0f, Color.green(bg) / 255.0f,
            Color.blue(bg) / 255.0f, Color.alpha(bg) / 255.0f)
        super.onSurfaceCreated(gl10, eglConfig)
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        var ratio:Float =(width+0.0f)/height
        // 设置透视投影矩阵，近点是2，远点是8
        Matrix.frustumM(projectMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 8f)
        Matrix.setLookAtM(cameraMatrix, 0, viewPos[0], viewPos[1], viewPos[2],
            0f, 0f, 0f,
            0f, 1.0f, 0.0f)
        GLES20.glViewport(0, 0, width, height)
        super.onSurfaceChanged(gl10, width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        // 设置显示范围
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        // 设置显示范围
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        angleInDegrees = 360.0f / 10000.0f * (SystemClock.uptimeMillis() % 10000L)
        Matrix.setIdentityM(lightModelMatrix, 0)
        Matrix.rotateM(lightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);

        //Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, 1.0f);
        Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0)
        Matrix.multiplyMV(lightPosInEyeSpace, 0, cameraMatrix, 0, lightPosInWorldSpace, 0)
        super.onDrawFrame(gl10)
    }

    inner class CubeRender : AbsObjectRender() {
        //2-------------- 立方体物体顶点纹理坐标 ----------------------
        private var cubeVertices: FloatArray? = floatArrayOf(
            // 顶点               // 法向量          // 纹理坐标
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, -0.5f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f, 0.5f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, -0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            0.5f, -0.5f, -0.5f, 0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            0.5f, -0.5f, 0.5f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            0.5f, -0.5f, 0.5f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, 0.5f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            -0.5f, -0.5f, -0.5f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            0.5f, 0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            0.5f, 0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            -0.5f, 0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f
        )

        // 物体的shadeCode
        private val vertexShaderCode = "uniform mat4 uMVMatrix;\n" +
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 normalMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                // 法向量
                "attribute vec3 aNormal;\n" +
                "attribute vec2 aTexCoords;\n" +
                "varying vec3 fragPos;\n" +
                "varying vec3 norm;\n" +
                "varying vec2 TexCoord;\n" +
                "void main() {\n" +
                "    fragPos = vec3(uMVMatrix * aPosition);\n" +
                //    norm = normalize(mat3(transpose(inverse(uMVMatrix))) * aNormal);\n" +
                // mat3作用：把被处理过的矩阵强制转换为3×3矩阵，来保证它失去了位移属性以及能够乘以vec3的法向量 +
                // norm = normalize(vec3(uMVMatrix * vec4(aNormal, 0.0)));与下面的效果相同
                "    norm = normalize(mat3(normalMatrix) * aNormal);\n" +
                "    TexCoord = aTexCoords;\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "}"
        private val fragmentShaderCode ="precision mediump float;\n" +
                "varying vec2 TexCoord;\n" +
                "varying vec3 fragPos;\n" +
                "varying vec3 norm;\n" +
                // 定义材质结构体
                "struct Material {\n" +
                "    sampler2D ambient;\n" +
                "    sampler2D diffuse;\n" +
                "    sampler2D specular;\n" +
                "    float shininess;\n" +
                "};\n" +
                "uniform Material material;\n" +
                // 定义光源结构体
                "struct Light {\n" +
                "    vec3 position;\n" +
                "    vec3 ambient;\n" +
                "    vec3 diffuse;\n" +
                "    vec3 specular;\n" +
                "};\n" +
                "uniform Light light;\n" +
                "void main() {\n" +
                //1-- 环境光照
                "    vec3 ambient = light.ambient * texture2D(material.ambient, TexCoord).rgb;\n" +
                //2-- 漫反射光照
                // 归一化光源线
                "    vec3 lightDir = normalize(light.position - fragPos);\n" +
                "    float diff = max(dot(norm, lightDir), 0.0);\n" +
                "    vec3 diffuse = light.diffuse * diff * texture2D(material.diffuse, TexCoord).rgb;\n" +
                //3-- 镜面光照
                "    vec3 viewDir = normalize(-fragPos);\n" +
                "    vec3 reflectDir = reflect(-lightDir, norm);\n" +
                "    float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);\n" +
                "    vec3 specular = (spec * light.specular) * texture2D(material.specular, TexCoord).rgb;\n" +
                // 结果
                "    vec3 result = ambient + diffuse + specular;\n" +
                "    gl_FragColor = vec4(result, 1.0);\n" +
                "}"

        private var ambientTexture:Int = 0
        private var diffuseTexture:Int = 0
        private var specularTexture:Int = 0

        private var positionHandle:Int = 0
        private var textureCoordsHandle:Int = 0
        private var mvMatrixHandle:Int = 0
        private var mvpMatrixHandle:Int = 0
        private var normalHandle:Int = 0
        private var normalPosHandle:Int = 0
        //1--立方体纹理材质
        private var materialAmbientPosHandle:Int = 0
        private var materialDiffusePosHandle:Int = 0
        private var materialSpecularPosHandle:Int = 0
        private var materialShininessPosHandle:Int = 0
        // 2-- 光照
        private var lightAmbientPosHandle:Int = 0
        private var lightDiffusePosHandle:Int = 0
        private var lightSpecularPosHandle:Int = 0
        private var lightPosHandle:Int = 0

        // 顶点数据buffer
        private lateinit var vertexBuffer: FloatBuffer

        private var mvpMatrix:FloatArray?= FloatArray(16)
        private var modelMatrix:FloatArray?=FloatArray(16)

        override fun initProgram() {
            //纹理的创建也需要放到opengl线程
            ambientTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.ic_light_maps_image1)
            diffuseTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.ic_light_maps_image2)
            specularTexture = TextureUtils.loadTexture(MyApplication.getInstance(), R.drawable.ic_light_maps_image3)
            vertexBuffer = GLDataUtil.createFloatBuffer(cubeVertices)
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
                normalPosHandle = GLES20.glGetUniformLocation(program, "normalMatrix")

                //1--立方体纹理材质
                materialAmbientPosHandle = GLES20.glGetUniformLocation(program, "material.ambient")
                materialDiffusePosHandle = GLES20.glGetUniformLocation(program, "material.diffuse")
                materialSpecularPosHandle = GLES20.glGetUniformLocation(program, "material.specular")
                materialShininessPosHandle = GLES20.glGetUniformLocation(program, "material.shininess")

                // 2-- 光照
                lightAmbientPosHandle = GLES20.glGetUniformLocation(program, "light.ambient")
                lightDiffusePosHandle = GLES20.glGetUniformLocation(program, "light.diffuse")
                lightSpecularPosHandle = GLES20.glGetUniformLocation(program, "light.specular")
                lightPosHandle = GLES20.glGetUniformLocation(program, "light.position")

//                vertexBuffer = GLDataUtil.createFloatBuffer(cubeVertices)
                // 传入顶点坐标
                GLES20.glEnableVertexAttribArray(positionHandle)
                vertexBuffer.position(0)
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)
                // 法向量
                vertexBuffer.position(3)
                GLES20.glEnableVertexAttribArray(normalHandle)
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)
                // 纹理坐标
                vertexBuffer.position(6)
                GLES20.glEnableVertexAttribArray(textureCoordsHandle)
                GLES20.glVertexAttribPointer(textureCoordsHandle, 2, GLES20.GL_FLOAT,
                    false, 8 * 4, vertexBuffer)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, cameraMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvpMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

                // 生成法线矩阵，移除对法向量错误缩放的影响
                // Normal = mat3(transpose(inverse(model))) * aNormal; model对应于model乘以view矩阵
                // 逆矩阵计算比较耗时，所以不放着色器中进行，改为放到cpu中计算
                val normalMatrix = Matrix4f()
                normalMatrix.loadMultiply(Matrix4f(cameraMatrix), Matrix4f(modelMatrix))
                normalMatrix.inverse()
                normalMatrix.transpose()
                GLES20.glUniformMatrix4fv(normalPosHandle, 1, false, normalMatrix.array, 0)

                //1--立方体纹理材质
                //启用纹理
                TextureUtils.bindTexture(materialAmbientPosHandle, ambientTexture, 0)
                //--- situation1  金属匡作为specular，则光照时，木纹中间无强光，黑色的部分没有反光，越是接近白色(金属框)反光越大
//        TextureUtils.bindTexture(materialDiffusePosHandle, ambient, 1)
//        TextureUtils.bindTexture(materialSpecularPosHandle, diffuse, 2)
                //---- situation2 使用绿色贴图作为specular高亮
                TextureUtils.bindTexture(materialDiffusePosHandle, diffuseTexture, 1)
                TextureUtils.bindTexture(materialSpecularPosHandle, specularTexture, 2)

                GLES20.glUniform1f(materialShininessPosHandle, 256.0f)

                // 2-- 光照
                GLES20.glUniform3f(lightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2])

                GLES20.glUniform3f(lightAmbientPosHandle, 0.3f, 0.3f, 0.3f)
                GLES20.glUniform3f(lightDiffusePosHandle, 0.5f, 0.5f, 0.5f)
                GLES20.glUniform3f(lightSpecularPosHandle, 1.0f, 1.0f, 1.0f)

                // 绘制顶点
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, cubeVertices!!.size/(3+3+2))

                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glDisableVertexAttribArray(normalHandle)
                GLES20.glDisableVertexAttribArray(textureCoordsHandle)
            }
        }

        override fun release() {
            val textures = IntArray(3)
            textures[0] = ambientTexture
            textures[1] = diffuseTexture
            textures[2] = specularTexture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDeleteTextures(3, textures, 0)
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