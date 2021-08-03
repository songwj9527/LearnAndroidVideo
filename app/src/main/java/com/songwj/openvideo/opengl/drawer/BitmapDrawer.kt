package com.songwj.openvideo.opengl.drawer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10


/**
 * 三角形绘制
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-10-09 09:08
 *
 */
class BitmapDrawer(private val mBitmap: Bitmap): IDrawer {

    // 顶点坐标
    private val mVertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // 纹理坐标
    private val mTextureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private var mTextureId: Int = -1

    //OpenGL程序ID
    private var mProgram: Int = -1

    // 顶点坐标接收者
    private var mVertexPosHandler: Int = -1
    // 纹理坐标接收者
    private var mTexturePosHandler: Int = -1
    // 纹理接收者
    private var mTextureHandler: Int = -1

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureBuffer: FloatBuffer

    init {
        //【步骤1: 初始化顶点坐标】
        initPos()
    }

    private fun initPos() {
        val bb = ByteBuffer.allocateDirect(mVertexCoors.size * 4)
        bb.order(ByteOrder.nativeOrder())
        //将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer.put(mVertexCoors)
        mVertexBuffer.position(0)

        val cc = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
        cc.order(ByteOrder.nativeOrder())
        mTextureBuffer = cc.asFloatBuffer()
        mTextureBuffer.put(mTextureCoors)
        mTextureBuffer.position(0)
    }

    override fun setVideoSize(videoW: Int, videoH: Int) {

    }

    override fun setVideoInfo(videoW: Int, videoH: Int, videoRotation: Int) {

    }

    override fun setWorldSize(worldW: Int, worldH: Int) {

    }

    override fun setAlpha(alpha: Float) {
    }

    override fun setTextureID(id: Int) {
        mTextureId = id
    }

    override fun draw() {
        if (mTextureId != -1) {
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg()
            //【步骤3: 激活并绑定纹理单元】
            activateTexture()
            //【步骤4: 绑定图片到纹理单元】
            bindBitmapToTexture()
            //【步骤5: 开始渲染绘制】
            doDraw()
        }
    }

    private fun createGLPrg() {
        if (mProgram == -1) {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())

            //创建OpenGL ES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
            mProgram = GLES20.glCreateProgram()
            //将顶点着色器加入到程序
            GLES20.glAttachShader(mProgram, vertexShader)
            //将片元着色器加入到程序中
            GLES20.glAttachShader(mProgram, fragmentShader)
            //连接到着色器程序
            GLES20.glLinkProgram(mProgram)

            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition")
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate")
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture")
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram)
    }

    private fun activateTexture() {
        //激活指定纹理单元: 因为OpenGL ES中内置了很多个纹理单元，并且是连续，比如GLES20.GL_TEXTURE0，GLES20.GL_TEXTURE1，GLES20.GL_TEXTURE3...
        //可以选择其中一个，一般默认选第一个GLES20.GL_TEXTURE0，并且OpenGL默认激活的就是第一个纹理单元。
        //另外，纹理单元GLES20.GL_TEXTURE1 = GLES20.GL_TEXTURE0 + 1，以此类推。
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId)
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(mTextureHandler, 0)
        //配置纹理过滤模式: GLES20.GL_TEXTURE_MIN_FILTER（缩）、GLES20.GL_TEXTURE_MAG_FILTER（放）
        //一般使用这两个模式：GL_NEAREST（邻近过滤）、GL_LINEAR（线性过滤）;
        //当设置为GL_NEAREST的时候，OpenGL会选择中心点最接近纹理坐标的那个像素，图像会有明显锯齿形。
        //当设置为GL_LINEAR的时候，它会基于纹理坐标附近的纹理像素，计算出一个插值，近似出这些纹理像素之间的颜色，图像可能会模糊。
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        //配置纹理环绕方式:GLES20.GL_TEXTURE_WRAP_S（水平方向）、GLES20.GL_TEXTURE_WRAP_T（垂直方向）、GLES20.GL_TEXTURE_WRAP_R
        //GL_REPEAT	对纹理的默认行为。重复纹理图像。
        //GL_MIRRORED_REPEAT	和GL_REPEAT一样，但每次重复图片是镜像放置的。
        //GL_CLAMP_TO_EDGE	纹理坐标会被约束在0到1之间，超出的部分会重复纹理坐标的边缘，产生一种边缘被拉伸的效果。
        //GL_CLAMP_TO_BORDER（GLES32）	超出的坐标为用户指定的边缘颜色。
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun bindBitmapToTexture() {
        if (!mBitmap.isRecycled) {
            //绑定图片到被激活的纹理单元
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0)
        }
    }

    private fun doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler)
        GLES20.glEnableVertexAttribArray(mTexturePosHandler)
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer)
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer)
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun release() {
        GLES20.glDisableVertexAttribArray(mVertexPosHandler)
        GLES20.glDisableVertexAttribArray(mTexturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES20.glDeleteProgram(mProgram)
    }

    private fun getVertexShader(): String {
//        //顶点坐标
//        "attribute vec2 aPosition;" +
//                //纹理坐标
//                "attribute vec2 aCoordinate;" +
//                //用于传递纹理坐标给片元着色器，命名和片元着色器中的一致
//                "varying vec2 vCoordinate;" +
//                "void main() {" +
//                "  gl_Position = aPosition;" +
//                "  vCoordinate = aCoordinate;" +
//                "}"
        return "attribute vec4 aPosition;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  gl_Position = aPosition;" +
                "  vCoordinate = aCoordinate;" +
                "}"
    }

    private fun getFragmentShader(): String {
//        //配置float精度，使用了float数据一定要配置：lowp(低)/mediump(中)/highp(高)
//        "precision mediump float;" +
//                //从Java传递进入来的纹理单元
//                "uniform sampler2D uTexture;" +
//                //从顶点着色器传递进来的纹理坐标
//                "varying vec2 vCoordinate;" +
//                "void main() {" +
//                //根据纹理坐标，从纹理单元中取色
//                "  vec4 color = texture2D(uTexture, vCoordinate);" +
//                "  gl_FragColor = color;" +
//                "}"
        return "precision mediump float;" +
                "uniform sampler2D uTexture;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  gl_FragColor = color;" +
                "}"
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        //根据type创建顶点着色器或者片元着色器
        val shader = GLES20.glCreateShader(type)
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        return shader
    }
}