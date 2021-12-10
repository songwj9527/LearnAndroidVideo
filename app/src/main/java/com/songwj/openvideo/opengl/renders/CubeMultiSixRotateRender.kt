package com.songwj.openvideo.opengl.renders

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.songwj.openvideo.MyApplication.Companion.getInstance
import com.songwj.openvideo.R
import com.songwj.openvideo.opengl.utils.ShaderUtils
import com.songwj.openvideo.opengl.utils.TextureUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CubeMultiSixRotateRender : GLSurfaceView.Renderer {
    companion object {
        // 顶点坐标
        //每个面6个点，3个点组成一个三角形
        private val vertexCoordinate = floatArrayOf( //正面
            -1.0f, 1.0f, 1.0f,  //正面左上0
            1.0f, 1.0f, 1.0f,  //正面右上3
            1.0f, -1.0f, 1.0f,  //正面右下2
            -1.0f, 1.0f, 1.0f,  //正面左上0
            1.0f, -1.0f, 1.0f,  //正面右下2
            -1.0f, -1.0f, 1.0f,  //正面左下1
            //左面
            -1.0f, 1.0f, 1.0f,  //正面左上0
            -1.0f, -1.0f, 1.0f,  //正面左下1
            -1.0f, -1.0f, -1.0f,  //反面左下5
            -1.0f, 1.0f, 1.0f,  //正面左上0
            -1.0f, -1.0f, -1.0f,  //反面左下5
            -1.0f, 1.0f, -1.0f,  //反面左上4
            //上面
            -1.0f, 1.0f, 1.0f,  //正面左上0
            1.0f, 1.0f, 1.0f,  //正面右上3
            1.0f, 1.0f, -1.0f,  //反面右上7
            -1.0f, 1.0f, 1.0f,  //正面左上0
            1.0f, 1.0f, -1.0f,  //反面右上7
            -1.0f, 1.0f, -1.0f,  //反面左上4
            //后面
            1.0f, -1.0f, -1.0f,  //反面右下6
            1.0f, 1.0f, -1.0f,  //反面右上7
            -1.0f, 1.0f, -1.0f,  //反面左上4
            1.0f, -1.0f, -1.0f,  //反面右下6
            -1.0f, 1.0f, -1.0f,  //反面左上4
            -1.0f, -1.0f, -1.0f,  //反面左下5
            //右面
            1.0f, -1.0f, -1.0f,  //反面右下6
            1.0f, 1.0f, -1.0f,  //反面右上7
            1.0f, 1.0f, 1.0f,  //正面右上3
            1.0f, -1.0f, -1.0f,  //反面右下6
            1.0f, 1.0f, 1.0f,  //正面右上3
            1.0f, -1.0f, 1.0f,  //正面右下2
            //下面
            1.0f, -1.0f, -1.0f,  //反面右下6
            -1.0f, -1.0f, -1.0f,  //反面左下5
            -1.0f, -1.0f, 1.0f,  //正面左下1
            1.0f, -1.0f, -1.0f,  //反面右下6
            -1.0f, -1.0f, 1.0f,  //正面左下1
            1.0f, -1.0f, 1.0f
        )
    }

    // 纹理坐标
    //每个面6个点坐标,先按每个面都用同一个纹理处理
    /**  纹理坐标个数要与定点坐标个数一致，顶点数组是36个，纹理坐标个数也必须一一对应是36个
     * 且对应的坐标转换也要一致，下方每个面的纹理坐标都一样，所以对应的前面顶点坐标组成三角形
     * 时，坐标也要跟转换后的纹理坐标一一对应。
     */
    var textureCoordinate = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    )
    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer

    //渲染程序
    private var mProgram = 0

    //纹理id列表
    private lateinit var textureIds: IntArray

    //相机矩阵
    private val mViewMatrix = FloatArray(16)

    //投影矩阵
    private val mProjectMatrix = FloatArray(16)

    //最终变换矩阵
    private val mMVPMatrix = FloatArray(16)

    //旋转矩阵  [20200623]  进行物体旋转 要与其他矩阵相乘，最终保存到mMVPMatrix中
    private val rotationMatrix = FloatArray(16)
    private val scaleMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val tempScaleMatrix = FloatArray(16)

    //累计旋转过的角度
    private var angle = 0f

    //缩放比例
    @Volatile
    private var scale = 0.3f

    //是否处于持续放大状态
    @Volatile
    private var isOnScaleSmall = false

    //上次执行缩放比例变化的时间
    private var lastZoomTime = 0L

    init {
        lastZoomTime = System.currentTimeMillis()
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoordinate.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        //要把所有6个面的数据都塞进去
        vertexBuffer.put(vertexCoordinate)
        vertexBuffer.position(0)
        textureBuffer = ByteBuffer.allocateDirect(textureCoordinate.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        textureBuffer.put(textureCoordinate)
        textureBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.e("CubeRender", "onSurfaceCreated()")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        //开启深度测试
        GLES30.glEnable(GLES20.GL_DEPTH_TEST)
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)

        //编译顶点着色程序
        val vertexShaderStr = "#version 300 es\n" +
                "layout (location = 0) in vec4 vPosition;\n" +
                "layout (location = 1) in vec2 aTextureCoord;\n" +
                "uniform mat4 vMatrix;\n" +
                "out vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position  = vMatrix * vPosition;\n" +
                "    gl_PointSize = 10.0;\n" +
                "    vTexCoord = aTextureCoord;\n" +
                "}" //ResReadUtils.readResource(R.raw.vertex_cubic_texture_shader);
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        //编译片段着色程序
        val fragmentShaderStr = "#version 300 es\n" +
                "precision mediump float;\n" +
                "uniform sampler2D uTextureUnit;\n" +
                "in vec2 vTexCoord;\n" +
                "out vec4 vFragColor;\n" +
                "void main() {\n" +
                "    vFragColor = texture(uTextureUnit,vTexCoord);\n" +
                "}" //ResReadUtils.readResource(R.raw.fragment_cubic_texture_shader);
        val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr)
        //连接程序
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        //在OpenGLES环境中使用程序
        GLES30.glUseProgram(mProgram)
        //加载纹理
        val resList = intArrayOf(
            R.drawable.hzw1,
            R.drawable.hzw2,
            R.drawable.hzw3,
            R.drawable.hzw4,
            R.drawable.hzw5,
            R.drawable.hzw6
        )
        textureIds = TextureUtils.loadTextures(getInstance(), resList)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        Log.e("CubeRender", "onSurfaceChanged()")
        GLES30.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        //设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
        //设置相机位置
        Matrix.setLookAtM(
            mViewMatrix, 0, 5f, 5f, 10.0f,  //摄像机坐标
            0f, 0f, 0f,  //目标物的中心坐标
            0f, 1.0f, 0.0f
        ) //相机方向
        //接着是摄像机顶部的方向了，如下图，很显然相机旋转，up的方向就会改变，这样就会会影响到绘制图像的角度。
        //例如设置up方向为y轴正方向，upx = 0,upy = 1,upz = 0。这是相机正对着目标图像
        //计算变换矩阵
        Matrix.multiplyMM(tempMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)
        //先初始化为单位正交矩阵
        Matrix.setIdentityM(rotationMatrix, 0)
        //旋转角度
        Matrix.rotateM(rotationMatrix, 0, 0f, 0f, 0f, 1f)
        //透视矩阵*相机矩阵*旋转矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, tempMatrix, 0, rotationMatrix, 0)
        //进行初始旋转操作
        Matrix.setIdentityM(tempMatrix, 0)
        Matrix.translateM(tempMatrix, 0, 0.25f, 0f, 0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, tempMatrix, 0)
        //                Matrix.rotateM(mViewMatrix,0,45,0,0,1);
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        //先初始化为单位正交矩阵
        Matrix.setIdentityM(rotationMatrix, 0)
        //旋转角度
        Matrix.rotateM(rotationMatrix, 0, angle, 0f, 1f, 0f) //angle每次绘制完进行自增
        //透视矩阵*相机矩阵*旋转矩阵  native层代码，估计数据放进去时做了拷贝，所以输出矩阵也可以使用mMVPMatrix保存运算后结果
//        Matrix.multiplyMM(mMVPMatrix,0,mMVPMatrix,0,rotationMatrix,0);    //解法错误
        //正解---如果要进行自动旋转、平移、缩放等操作。则每次mMVPMatrix矩阵要重新走一遍流程进行计算，而不是在上一次mMVPMatrix的基础上进行矩阵相乘
        Matrix.multiplyMM(tempMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)
        Matrix.setIdentityM(scaleMatrix, 0)
        //设置缩放比例
        Matrix.scaleM(scaleMatrix, 0, scale, scale, scale)
        //执行缩放
        Matrix.multiplyMM(tempScaleMatrix, 0, tempMatrix, 0, scaleMatrix, 0)
        //执行旋转
        Matrix.multiplyMM(mMVPMatrix, 0, tempScaleMatrix, 0, rotationMatrix, 0)


        //左乘矩阵
        val uMaxtrixLocation = GLES30.glGetUniformLocation(mProgram, "vMatrix")
        // 将前面计算得到的mMVPMatrix(frustumM setLookAtM 通过multiplyMM 相乘得到的矩阵) 传入vMatrix中，与顶点矩阵进行相乘
        GLES30.glUniformMatrix4fv(uMaxtrixLocation, 1, false, mMVPMatrix, 0)
        val aPositionLocation = GLES30.glGetAttribLocation(mProgram, "vPosition")
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        //x y z 所以数据size 是3
        GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        val aTextureLocation = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
        //纹理坐标数据 x、y，所以数据size是 2
        GLES30.glVertexAttribPointer(aTextureLocation, 2, GLES30.GL_FLOAT, false, 0, textureBuffer)
        //启用纹理颜色句柄
        GLES30.glEnableVertexAttribArray(aTextureLocation)
        //启用纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        //每个面6个顶点数据，使用不同的纹理贴图
        for (i in textureIds.indices) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[i])
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, i * 6, 6)
        }

        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
        GLES30.glDisableVertexAttribArray(aTextureLocation)
        //每次绘制旋转1度
        if (angle == 360f) {
            angle = 0f
        } else {
            angle += 1f
        }

        //每隔500ms进行一次缩放比例变化
        val timeNow = System.currentTimeMillis()
        lastZoomTime = if (timeNow - lastZoomTime < 500) {
            return
        } else {
            timeNow
        }
        if (!isOnScaleSmall) {
            scale += 0.1f
            if (scale >= 0.9) {
                isOnScaleSmall = true
            }
        } else {
            scale -= 0.1f
            if (scale <= 0.3) {
                isOnScaleSmall = false
            }
        }
    }

    fun release() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(textureIds.size, textureIds, 0)
        GLES20.glDeleteProgram(mProgram)
    }
}