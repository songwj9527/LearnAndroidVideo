package com.songwj.openvideo.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.GLES11
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.songwj.openvideo.opengl.OpenGLTools
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random


/**
 * 灵魂出窍视频渲染器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-10-26 15:45
 *
 */
class SoulBurrVideoDrawer : IDrawer {
    /**上下颠倒的顶点矩阵*/
    private val mReserveVertexCoors = floatArrayOf(
        -1f, 1f,
        1f, 1f,
        -1f, -1f,
        1f, -1f
    )

    private val mDefVertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // 顶点坐标
    private var mVertexCoors = mDefVertexCoors

    // 纹理坐标
    private val mTextureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private var mWorldWidth: Int = -1
    private var mWorldHeight: Int = -1
    private var mVideoWidth: Int = -1
    private var mVideoHeight: Int = -1
    private var mVideoRotation: Int = 0

    private var mTextureId: Int = -1

    private var mSurfaceTexture: SurfaceTexture? = null

    private var mSftCb: ((SurfaceTexture) -> Unit)? = null

    //OpenGL程序ID
    private var mProgram: Int = -1

    //矩阵变换接收者
    private var mVertexMatrixHandler: Int = -1
    // 顶点坐标接收者
    private var mVertexPosHandler: Int = -1
    // 纹理坐标接收者
    private var mTexturePosHandler: Int = -1
    // 纹理接收者
    private var mTextureHandler: Int = -1
    // 半透值接收者
    private var mAlphaHandler: Int = -1

//-------------灵魂出窍相关的变量--------------
    // 灵魂帧缓冲
    private var mSoulFrameBuffer: Int = -1

    // 灵魂纹理ID
    private var mSoulTextureId: Int = -1

    // 灵魂纹理接收者
    private var mSoulTextureHandler: Int = -1

    // 灵魂缩放进度接收者
    private var mProgressHandler: Int = -1

    // 是否更新FBO纹理
    private var mDrawFbo: Int = 1

    // 更新FBO标记接收者
    private var mDrawFobHandler: Int = -1

    // 更新FBO标记接收者
    private var mTimeFobHandler: Int = -1

    // 一帧灵魂的时间
    private var mModifyTime: Long = -1
//--------------------------------------

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureBuffer: FloatBuffer

    private var mMatrix: FloatArray? = null

    private var mAlpha = 1f

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

    private var mWidthRatio = 1f
    private var mHeightRatio = 1f
    private fun initDefMatrix() {
        if (mMatrix != null) return
        if (mVideoWidth != -1 && mVideoHeight != -1 &&
            mWorldWidth != -1 && mWorldHeight != -1) {
            Log.e("SoulVideoDrawer", "width: $mWorldWidth, height: $mWorldHeight," +
                    " videoWidth: $mVideoWidth, videoHeight: $mVideoHeight, videoRotation: $mVideoRotation")
            mMatrix = FloatArray(16)
            var prjMatrix = FloatArray(16)
            var originRatio = mVideoWidth / mVideoHeight.toFloat()
            if (mVideoRotation == 90 || mVideoRotation % 180 > 0) {
                originRatio = mVideoHeight / mVideoWidth.toFloat()
            }
            val worldRatio = mWorldWidth / mWorldHeight.toFloat()
            if (mWorldWidth > mWorldHeight) {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio
                }
            } else {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    mWidthRatio = worldRatio / originRatio

                }
            }

            Matrix.orthoM(
                prjMatrix, 0,
                -mWidthRatio, mWidthRatio,
                -mHeightRatio, mHeightRatio,
                3f, 5f
            )

            if (mVideoRotation == 90 || mVideoRotation % 180 > 0) {
                Matrix.rotateM(prjMatrix,0, mVideoRotation.toFloat(), 0f, 0f, -1f)
            }
            //设置相机位置
            val viewMatrix = FloatArray(16)
            Matrix.setLookAtM(
                viewMatrix, 0,
                0f, 0f, 5.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
            )
            //计算变换矩阵
            Matrix.multiplyMM(mMatrix, 0, prjMatrix, 0, viewMatrix, 0)
        }
    }

    override fun setVideoSize(videoW: Int, videoH: Int) {
        mVideoWidth = videoW
        mVideoHeight = videoH
    }

    override fun setVideoInfo(videoW: Int, videoH: Int, videoR: Int) {
        mVideoWidth = videoW
        mVideoHeight = videoH
        mVideoRotation = videoR
    }

    override fun setWorldSize(worldW: Int, worldH: Int) {
        mWorldWidth = worldW
        mWorldHeight = worldH
    }

    override fun setAlpha(alpha: Float) {
        mAlpha = alpha
    }

    override fun setTextureID(id: Int) {
        mTextureId = id
        mSurfaceTexture = SurfaceTexture(id)
        mSftCb?.invoke(mSurfaceTexture!!)
    }

    override fun getSurfaceTexture(cb: (st: SurfaceTexture) -> Unit) {
        mSftCb = cb
    }

    override fun draw() {
        if (mTextureId != -1) {
            initDefMatrix()
            //【步骤2: 创建、编译并启动OpenGL着色器】
            createGLPrg()
            // 【更新FBO】
            updateFBO()
            // 【激活灵魂纹理单元】
            activateSoulTexture()
            //【步骤3: 激活并绑定纹理单元】
            activateDefTexture()
            //【步骤4: 绑定图片到纹理单元】
            updateTexture()
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

            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix")
            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition")
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture")
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate")
            mAlphaHandler = GLES20.glGetAttribLocation(mProgram, "alpha")

            mSoulTextureHandler = GLES20.glGetUniformLocation(mProgram, "uSoulTexture")
            mProgressHandler = GLES20.glGetUniformLocation(mProgram, "progress")
            mDrawFobHandler = GLES20.glGetUniformLocation(mProgram, "drawFbo")
            mTimeFobHandler = GLES20.glGetUniformLocation(mProgram, "timestamp")
        }
        //使用OpenGL程序
        GLES20.glUseProgram(mProgram)
    }

    private fun updateFBO() {
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            if (mSoulTextureId == -1) {
                // 创建FBO纹理
                mSoulTextureId = OpenGLTools.createFBOTexture(mVideoWidth, mVideoHeight)
            }
            if (mSoulFrameBuffer == -1) {
                mSoulFrameBuffer = OpenGLTools.createFrameBuffer()
            }
            if (System.currentTimeMillis() - mModifyTime > 500) {
                mModifyTime = System.currentTimeMillis()
                // 绑定FBO
                OpenGLTools.bindFBO(mSoulFrameBuffer, mSoulTextureId)
                // 配置FBO窗口
                configFboViewport()
                // 激活默认的纹理
                activateDefTexture()
                // 更新纹理
                updateTexture()
                // 绘制到FBO
                doDraw()
                // 解绑FBO
                OpenGLTools.unbindFBO()
                // 恢复默认绘制窗口
                configDefViewport()
            }
        }
    }

    /**
     * 配置FBO窗口
     */
    private fun configFboViewport() {
        mDrawFbo = 1
        if (mMatrix != null) {
            // 将变换矩阵回复为单位矩阵（将画面拉升到整个窗口大小，设置窗口宽高和FBO纹理宽高一致，画面刚好可以正常绘制到FBO纹理上）
            Matrix.setIdentityM(mMatrix, 0)
        }
        // 设置颠倒的顶点坐标
        mVertexCoors = mReserveVertexCoors
        //重新初始化顶点坐标
        initPos()
        if (mMatrix != null) {
            GLES20.glViewport(0, 0, mVideoWidth, mVideoHeight)
        } else {
            GLES20.glViewport(0, 0, mWorldWidth, mWorldHeight)
        }
        //设置一个颜色状态
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        //使能颜色状态的值来清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    /**
     * 配置默认显示的窗口
     */
    private fun configDefViewport() {
        mDrawFbo = 0
        mMatrix = null
        // 恢复顶点坐标
        mVertexCoors = mDefVertexCoors
        initPos()
        initDefMatrix()
        // 恢复窗口
        GLES20.glViewport(0, 0, mWorldWidth, mWorldHeight)
    }

    private fun activateDefTexture() {
        activateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId, 0, mTextureHandler)
    }

    private fun activateSoulTexture() {
        if (mSoulTextureId != -1) {
            activateTexture(GLES11.GL_TEXTURE_2D, mSoulTextureId, 1, mSoulTextureHandler)
        }
    }

    private fun activateTexture(type: Int, textureId: Int, index: Int, textureHandler: Int) {
        //激活指定纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + index)
        //绑定纹理ID到纹理单元
        GLES20.glBindTexture(type, textureId)
        //将激活的纹理单元传递到着色器里面
        GLES20.glUniform1i(textureHandler, index)
        //配置边缘过渡参数
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(type, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(type, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(type, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun updateTexture() {
        mSurfaceTexture?.updateTexImage()
    }

    private fun doDraw() {
        //启用顶点的句柄
        GLES20.glEnableVertexAttribArray(mVertexPosHandler)
        GLES20.glEnableVertexAttribArray(mTexturePosHandler)
        if (mMatrix != null) {
            GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0)
        }
        GLES20.glUniform1f(mProgressHandler, (System.currentTimeMillis() - mModifyTime)/500f)
        GLES20.glUniform1i(mDrawFobHandler, mDrawFbo)
        var value = (System.currentTimeMillis() % 0.6f)
        GLES20.glUniform1f(mTimeFobHandler, value)
        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer)
        GLES20.glVertexAttribPointer(mTexturePosHandler, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer)
        GLES20.glVertexAttrib1f(mAlphaHandler, mAlpha)
        //开始绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun release() {
        releaseFBO()
        GLES20.glDisableVertexAttribArray(mVertexPosHandler)
        GLES20.glDisableVertexAttribArray(mTexturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES20.glDeleteProgram(mProgram)
    }

    private fun releaseFBO() {
        if (mSoulTextureId != -1) {
            val fbs = IntArray(1)
            fbs[0] = mSoulFrameBuffer
            val texts = IntArray(1)
            texts[0] = mSoulTextureId

            OpenGLTools.deleteFBO(fbs, texts)
        }
    }

    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "precision mediump float;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "attribute float alpha;" +
                "varying float inAlpha;" +
                "void main() {" +
                "    gl_Position = uMatrix*aPosition;" +
                "    vCoordinate = aCoordinate;" +
                "    inAlpha = alpha;" +
                "}"
    }

    private fun getFragmentShader(): String {
        //一定要加换行"\n"，否则会和下一行的precision混在一起，导致编译出错
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "varying float inAlpha;" +
                "uniform samplerExternalOES uTexture;" +
                "uniform float progress;" +
                "uniform float timestamp;" +
                "uniform int drawFbo;" +
                "uniform sampler2D uSoulTexture;" +
                "void main() {" +
                    // 最大抖动
                    "float maxJitter = 0.06;" +
                    // 一次毛刺滤镜的时长
                    "float duration = 0.3;" +
                    // 红色颜色偏移量
                    "float colorROffset = 0.01;" +
                    // 绿色颜色偏移量
                    "float colorBOffset = -0.025;" +
                    // 时间周期[0.0,0.6];
//                    "float time = timestamp;" +
                    "float time = 0.6 * progress;" +
                    // 振幅:[0,1];
                    "float amplitude = max(sin(time * (3.1415926 / duration)), 0.0);" +
//                    "float amplitude = progress;" +
                    // 像素随机偏移[-1,1]
                    "float jitter = fract(sin(vCoordinate.y) * 43758.5453123) * 2.0 - 1.0;" +
                    // 是否要做偏移
                    "bool needOffset = abs(jitter) < maxJitter * amplitude;" +

                    // 获取纹理X值.根据needOffset，来计算它X撕裂.
                    // needOffset = YES，撕裂较大;
                    // needOffset = NO，撕裂较小.
                    "float textureX = vCoordinate.x + (needOffset ? jitter : (jitter * amplitude * 0.006));" +
                    // 撕裂后的纹理坐标x,y
                    "vec2 textureCoords = vec2(textureX, vCoordinate.y);" +
                    // 颜色偏移3组颜色
                    // 根据撕裂后获取的纹理颜色值
                    "vec4 burrMask = texture2D(uSoulTexture, textureCoords);" +
                    // 撕裂后的纹理颜色偏移
                    "vec4 maskR = texture2D(uSoulTexture, textureCoords + vec2(colorROffset * amplitude, 0.0));" +
                    // 撕裂后的纹理颜色偏移
                    "vec4 maskB = texture2D(uSoulTexture, textureCoords + vec2(colorBOffset * amplitude, 0.0));" +
                    // 颜色mask
                    "vec4 mask = texture2D(uTexture, vCoordinate);" +

                    "if (drawFbo == 0) {" +
                        // 颜色混合 默认颜色混合方程式 = mask * (1.0-alpha) + weakMask * alpha
                    "    gl_FragColor = vec4(maskR.r, burrMask.g, maskB.b, burrMask.a);" +
                    "} else {" +
                    "   gl_FragColor = vec4(mask.r, mask.g, mask.b, inAlpha);" +
                    "}" +
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

    fun translate(dx: Float, dy: Float) {
        mMatrix?.let {
            Matrix.translateM(it, 0, dx*mWidthRatio*2, -dy*mHeightRatio*2, 0f)
        }
    }

    fun scale(sx: Float, sy: Float) {
        mMatrix?.let {
            Matrix.scaleM(it, 0, sx, sy, 1f)
        }
        mWidthRatio /= sx
        mHeightRatio /= sy
    }
}