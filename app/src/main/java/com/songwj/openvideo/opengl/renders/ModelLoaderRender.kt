package com.songwj.openvideo.opengl.renders

import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.text.TextUtils
import android.util.Log
import com.songwj.openvideo.MyApplication.Companion.getInstance
import com.songwj.openvideo.R
import com.songwj.openvideo.opengl.model.LoadObjectUtil
import com.songwj.openvideo.opengl.model.bean.ObjectBean
import java.io.IOException
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelLoaderRender : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "ModelLoaderRender"
        const val planetDir = "planet"
        const val rockDir = "rock"
    }

    var mWidth = 0
    var mHeight = 0
    private val list: List<ObjectBean>?

    //渲染程序
    private var mProgram = 0

    //相机矩阵
    private val mViewMatrix = FloatArray(16)

    //投影矩阵
    private val mProjectMatrix = FloatArray(16)

    //最终变换矩阵
    private val mMVPMatrix = FloatArray(16)

    //---------------- 2 绘制平面背景
    //    private float[] planeVertices = {
    //            // positions          // texture Coords (note we set these higher than 1 (together with GL_REPEAT as texture wrapping mode). this will cause the floor texture to repeat)
    //            5.0f, -0.5f, 5.0f, 2.0f, 0.0f,
    //            -5.0f, -0.5f, 5.0f, 0.0f, 0.0f,
    //            -5.0f, -0.5f, -5.0f, 0.0f, 2.0f,
    //
    //            5.0f, -0.5f, 5.0f, 2.0f, 0.0f,
    //            -5.0f, -0.5f, -5.0f, 0.0f, 2.0f,
    //            5.0f, -0.5f, -5.0f, 2.0f, 2.0f
    //    };
    //中间一条
    //    private float[] planeVertices = {
    //            // positions          // texture Coords (note we set these higher than 1 (together with GL_REPEAT as texture wrapping mode). this will cause the floor texture to repeat)
    //            4.0f, 1f, 0f, 2.0f, 0.0f,
    //            -4.5f, 1f, 0f, 0.0f, 0.0f,
    //            -4.5f, -1f, 0f, 0.0f, 2.0f,
    //
    //            4.0f, 1f, 0f, 2.0f, 0.0f,
    //            -4.5f, -1f, 0f, 0.0f, 2.0f,
    //            4.0f, -1f, 0f, 2.0f, 2.0f
    //    };
    // 实际显示的大小和顶点坐标以及观察的距离有关
    // 或者背景不使用透视投影？？！！
    private val planeVertices =
        floatArrayOf( // positions          // texture Coords (note we set these higher than 1 (together with GL_REPEAT as texture wrapping mode). this will cause the floor texture to repeat)
            3f, 5f, 0f, 2.0f, 0.0f,
            -3f, 5f, 0f, 0.0f, 0.0f,
            -3f, -5f, 0f, 0.0f, 2.0f,
            3f, 5f, 0f, 2.0f, 0.0f,
            -3f, -5f, 0f, 0.0f, 2.0f,
            3f, -5f, 0f, 2.0f, 2.0f
        )

    //    private float[] planeVertices = {
    //            // positions          // texture Coords (note we set these higher than 1 (together with GL_REPEAT as texture wrapping mode). this will cause the floor texture to repeat)
    //            1.0f, -0.5f, 1.0f, 2.0f, 0.0f,
    //            -1.0f, -0.5f, 1.0f, 0.0f, 0.0f,
    //            -1.0f, -0.5f, -1.0f, 0.0f, 2.0f,
    //
    //            1.0f, -0.5f, 1.0f, 2.0f, 0.0f,
    //            -1.0f, -0.5f, -1.0f, 0.0f, 2.0f,
    //            1.0f, -0.5f, -.0f, 2.0f, 2.0f
    //    };
    //    private float[] planeVertices = {
    //            // positions          // texture Coords (note we set these higher than 1 (together with GL_REPEAT as texture wrapping mode). this will cause the floor texture to repeat)
    //            -1f, -0.5f, -1f, 1.0f, 0.0f,
    //            -1f, -0.5f, -1f, 0.0f, 0.0f,
    //            -1f, -0.5f, -1f, 0.0f, 1.0f,
    //
    //            1f, -0.5f, -1f, 1.0f, 0.0f,
    //            -1f, -0.5f, -1f, 0.0f, 1.0f,
    //            1f, -0.5f, -1f, 1.0f, 1.0f
    //    };
    private var mProgramBg = 0
    private var planVertexBuffer: FloatBuffer? = null
    private var textureFloor = 0

    //最终变换矩阵
    private val mMVPMatrixFloor = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    init {
        list = LoadObjectUtil.loadObject(
            rockDir + "/rock.obj",
            getInstance()!!.resources, rockDir
        )
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
        mProgram = generateProgram()
        //在OpenGLES环境中使用程序
        GLES30.glUseProgram(mProgram)
        mProgramBg = generateProgram()
        GLES30.glUseProgram(mProgramBg)
        // 传入顶点坐标
        planVertexBuffer = GLDataUtil.createFloatBuffer(planeVertices)
        textureFloor =
            TextureUtils.loadTextureNormal(getInstance(), R.drawable.ic_depth_testing_metal)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        mWidth = width
        mHeight = height
        val ratio = width.toFloat() / height
        //设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
        //设置相机位置
        Matrix.setLookAtM(
            mViewMatrix, 0, 0f, 0f, 5f,  //摄像机坐标
            0f, 0f, 0f,  //目标物的中心坐标
            0f, 1.0f, 0.0f
        ) //相机方向
        //接着是摄像机顶部的方向了，如下图，很显然相机旋转，up的方向就会改变，这样就会会影响到绘制图像的角度。
        //例如设置up方向为y轴正方向，upx = 0,upy = 1,upz = 0。这是相机正对着目标图像
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10) {
        // 设置显示范围
        GLES30.glViewport(0, 0, mWidth, mHeight)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        //开启深度测试
        drawModel()
        drawBackFloor()
    }

    fun generateProgram(): Int {
        val vertexShaderStr = "attribute vec4 vPosition;" +  // 顶点坐标
                "attribute vec2 vTextureCoord;" +  //纹理坐标
                "varying vec2 aCoord;" +  // mvp矩阵
                "uniform mat4 vMatrix;" +
                "void main(){" +  //内置变量： 把坐标点赋值给gl_position 就Ok了。
                "    gl_Position =vMatrix * vPosition;" +
                "    aCoord = vTextureCoord;" +
                "}"
        // 编译顶点着色器
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        val fragmentShaderStr = "precision mediump float;" +  // 数据精度
                "varying vec2 aCoord;" +
                "uniform sampler2D  vTexture;" +  // samplerExternalOES: 图片， 采样器
                "void main(){" +  //  texture2D: vTexture采样器，采样  aCoord 这个像素点的RGBA值
                "    vec4 rgba = texture2D(vTexture, aCoord);" +
                "    gl_FragColor = rgba;" +
                "}"
        //编译片段着色程序
        val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr)
        //连接程序
        return ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
    }

    // 参数顶点坐标handle位置，纹理坐标handle位置，纹理位置
    fun drawModel() {
        //左乘矩阵
        val uMaxtrixLocation = GLES30.glGetUniformLocation(mProgram, "vMatrix")
        // 将前面计算得到的mMVPMatrix(frustumM setLookAtM 通过multiplyMM 相乘得到的矩阵) 传入vMatrix中，与顶点矩阵进行相乘
        GLES30.glUniformMatrix4fv(uMaxtrixLocation, 1, false, mMVPMatrix, 0)
        val vertexPosLoc = GLES30.glGetAttribLocation(mProgram, "vPosition")
        GLES30.glEnableVertexAttribArray(vertexPosLoc)
        val textPosLoc = GLES20.glGetAttribLocation(mProgram, "vTextureCoord")
        val textureLoc = GLES20.glGetUniformLocation(mProgram, "vTexture")
        //启用顶点颜色句柄
        GLES30.glEnableVertexAttribArray(textPosLoc)
        //绘制模型
        if (list != null && !list.isEmpty()) {
            for (item in list) {
                if (item != null) {
                    /// 数据如何排列？？？！！！
                    GLES30.glVertexAttribPointer(
                        vertexPosLoc, 3, GLES20.GL_FLOAT,
                        false, 3 * 4, GLDataUtil.createFloatBuffer(item.aVertices)
                    )
                    GLES30.glVertexAttribPointer(
                        textPosLoc, 2, GLES20.GL_FLOAT,
                        false, 2 * 4, GLDataUtil.createFloatBuffer(item.aTexCoords)
                    )
                    if (item.mtl != null) {
                        if (!TextUtils.isEmpty(item.mtl.Kd_Texture)) {
                            if (item.diffuse < 0) {
                                try {
                                    val bitmap = BitmapFactory.decodeStream(
                                        getInstance()!!.assets.open(
                                            rockDir + "/" + item.mtl.Kd_Texture
                                        )
                                    )
                                    item.diffuse = TextureUtils.createTextureWithBitmap(bitmap)
                                    bitmap.recycle()
                                } catch (e: IOException) {
                                    Log.e(TAG, "onDrawFrame: $e")
                                }
                            }
                        } else {
                            if (item.diffuse < 0) {
                                item.diffuse = TextureUtils.loadTexture(
                                    getInstance(),
                                    R.drawable.ic_launcher_background
                                )
                            }
                        }
                        GLES30.glActiveTexture(GLES20.GL_TEXTURE0)
                        GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, item.diffuse)
                        GLES30.glUniform1i(textureLoc, 0)
                    }

                    // 绘制顶点
                    GLES30.glDrawArrays(GLES20.GL_TRIANGLES, 0, item.aVertices.size / 3)
                }
            }
        }
        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(vertexPosLoc)
        GLES30.glDisableVertexAttribArray(textPosLoc)
    }

    // 参数顶点坐标handle位置，纹理坐标handle位置，纹理位置
    fun drawBackFloor() {
        val textureId = textureFloor
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrixFloor, 0, mViewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrixFloor, 0, mProjectMatrix, 0, mMVPMatrixFloor, 0)

        //是否需要2个program?
        val vertexPosLoc = GLES30.glGetAttribLocation(mProgramBg, "vPosition")
        GLES30.glEnableVertexAttribArray(vertexPosLoc)
        val textPosLoc = GLES30.glGetAttribLocation(mProgramBg, "vTextureCoord")
        GLES30.glEnableVertexAttribArray(textPosLoc)
        val vTextureFilterLoc = GLES30.glGetUniformLocation(mProgramBg, "vTexture")


        // 传入顶点坐标
//        FloatBuffer planVertexBuffer = GLDataUtil.createFloatBuffer(planeVertices);
        planVertexBuffer!!.position(0)
        GLES30.glVertexAttribPointer(
            vertexPosLoc, 3, GLES20.GL_FLOAT,
            false, 5 * 4, planVertexBuffer
        )
        // 纹理坐标
        planVertexBuffer!!.position(3)
        GLES30.glVertexAttribPointer(
            textPosLoc, 2, GLES20.GL_FLOAT,
            false, 5 * 4, planVertexBuffer
        )

//        float[] mMVPMatrixIdentity = new float[16];
//        Matrix.setIdentityM(mMVPMatrixIdentity, 0);

        //左乘矩阵
        val uMaxtrixLocation = GLES30.glGetUniformLocation(mProgramBg, "vMatrix")
        // 将前面计算得到的mMVPMatrix(frustumM setLookAtM 通过multiplyMM 相乘得到的矩阵) 传入vMatrix中，与顶点矩阵进行相乘
        GLES30.glUniformMatrix4fv(uMaxtrixLocation, 1, false, mMVPMatrixFloor, 0)

//        GLES30.glUniformMatrix4fv(uMaxtrixLocation,1,false,mMVPMatrixIdentity,0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(vTextureFilterLoc, 0)

        // 绘制顶点
        GLES30.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES30.glDisableVertexAttribArray(vertexPosLoc)
        GLES30.glDisableVertexAttribArray(textPosLoc)
    }

    fun release() {
        GLES20.glDeleteProgram(mProgram)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(textureFloor), 0)
        GLES20.glDeleteProgram(mProgramBg)
    }
}