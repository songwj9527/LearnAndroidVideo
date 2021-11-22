package com.songwj.openvideo.opengl.renders

import android.content.Context
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.*
import android.os.Environment
import com.songwj.openvideo.camera.Camera1Manager
import com.songwj.openvideo.mediarecord.MediaRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Camera1PreviewRender(context: Context?, onFrameAvailableListener: OnFrameAvailableListener?) : BaseAbsRender() {
    private val TAG = "Camera1PreviewRender"
    private var context: Context? = null
    private var onFrameAvailableListener: OnFrameAvailableListener? = null
    private var viewWidth = -1
    private var viewHeight = -1
    private val texture = IntArray(1)
    private var cameraTexture: SurfaceTexture? = null

    private var mediaRecorder: MediaRecorder? = null
    private var eglContext: EGLContext? = null

    init {
        this.context = context
        this.onFrameAvailableListener = onFrameAvailableListener
        Matrix.setIdentityM(projectMatrix, 0)
        Matrix.setIdentityM(cameraMatrix, 0)
        addObjectRender(CameraRender())
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 调用父类，完成另外添加进来的图形的初始化
        eglContext = EGL14.eglGetCurrentContext()
        super.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        super.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glEnable(GLES20.GL_DEPTH_TEST)
        GLES30.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        // 调用父类，完成另外添加进来的图形的绘制
        super.onDrawFrame(gl)
//        // 1.glReadPixels返回的是大端的RGBA Byte组数，我们使用小端Buffer接收得到ABGR Byte组数
//        val frameBuffer: ByteBuffer = ByteBuffer.allocateDirect(viewWidth * viewHeight * 4).order(ByteOrder.LITTLE_ENDIAN)
//        gl?.glReadPixels(0, 0, viewWidth, viewHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, frameBuffer)
//        frameBuffer.rewind()//reset position
//        val pixelCount = viewWidth * viewHeight
//        val colors = IntArray(pixelCount)
//        frameBuffer.asIntBuffer().get(colors)
//        for (i in 0 until pixelCount) {
//            val c = colors[i]   //2.每个int类型的c是接收到的ABGR，但bitmap需要ARGB格式，所以需要交换B和R的位置
//            colors[i] = c and -0xff0100 or (c and 0x00ff0000 shr 16) or (c and 0x000000ff shl 16) //交换B和R，得到ARGB
//        }
        mediaRecorder?.onDrawFrame(texture[0], cameraTexture!!.timestamp)
    }

    fun startRecord(): Boolean {
        var ret = true
        if (mediaRecorder == null) {
            val cameraSize = Camera1Manager.getInstance().cameraSize
            val filePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
            if (Camera1Manager.getInstance().cameraDisplayOrientation == 90 || Camera1Manager.getInstance().cameraDisplayOrientation == 270) {
                mediaRecorder = MediaRecorder(filePath, cameraSize.height, cameraSize.width, false)
            } else {
                mediaRecorder = MediaRecorder(filePath, cameraSize.width, cameraSize.height, false)
            }
            mediaRecorder?.setEGLContext(eglContext)
            ret = mediaRecorder!!.start()
            if (!ret) {
                mediaRecorder?.stop()
                mediaRecorder = null
            }
        }
        return ret
    }

    fun stopRecord() {
        mediaRecorder?.stop()
        mediaRecorder = null
    }

    inner class CameraRender : AbsObjectRender() {
        private //编译顶点着色程序
        val vertexShaderStr = "uniform mat4 vMatrix;\n" +
                "attribute vec3 vPositionCoord;\n" + //NDK坐标点
                "attribute vec2 vTextureCoord;\n" +
                "varying   vec2 aTextureCoord;\n" + //纹理坐标点变换后输出
                " void main() {\n" +
                "//     gl_Position = vPositionCoord;\n" +
                "     vec4 pos = vec4(vPositionCoord, 1.0);\n" +
                "     gl_Position = (vMatrix * pos).xyww;\n" +
                "     aTextureCoord = vTextureCoord;\n" +
                " }"
        //编译片段着色程序
        val fragmentShaderStr = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES vTexture;\n" +
                "varying vec2 aTextureCoord;\n" +
                "void main() {\n" +
                "    vec4 tc = texture2D(vTexture, aTextureCoord);\n" +
//                "    gl_FragColor = tc;\n" +
                // 滤镜效果
                "    gl_FragColor = vec4(tc.r * 0.875, tc.g * 0.62, tc.b * 0.863, tc.a);\n" +
                "}"

        private val posCoordinate = floatArrayOf(
            -1f, -1f, 1f,
            -1f, 1f, 1f,
            1f, -1f, 1f,
            1f, 1f, 1f)
        private val texCoordinate = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

        private var mvpMatrixHandle = 0
        private var uPosHandle = 0
        private var aTexHandle = 0
        private var uTextureHandle = 0

        //透视矩阵、相机矩阵定义放在基类中，方便传给其他绘制对象
        private val mvpMatrix = FloatArray(16)
        private val tempMatrix = FloatArray(16)

        private var posBuffer: FloatBuffer? = null
        private var texBuffer: FloatBuffer? = null

        init {
            Matrix.setIdentityM(mvpMatrix, 0)
            Matrix.setIdentityM(tempMatrix, 0)
            posBuffer = GLDataUtil.createFloatBuffer(posCoordinate)
            texBuffer = GLDataUtil.createFloatBuffer(texCoordinate)
        }

        override fun initProgram() {
            val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
            val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr)
            //连接程序
            program = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
            createAndBindCameraTexture()
        }

        override fun initMatrix() {
            Matrix.setIdentityM(projectMatrix, 0)
            Matrix.setIdentityM(cameraMatrix, 0)
            var widthRatio = 1f
            var heightRatio = 1f
            var cameraSize = Camera1Manager.getInstance().cameraSize
            val viewRatio = viewWidth / viewHeight.toFloat()
            val cameraRatio = cameraSize.height / cameraSize.width.toFloat()
            if (viewWidth > viewHeight) {
                if (cameraRatio > viewRatio) {
                    heightRatio = cameraRatio / viewRatio
                } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = viewRatio / cameraRatio
                }
            } else {
                if (cameraRatio > viewRatio) {
                    heightRatio = cameraRatio / viewRatio
                } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                    widthRatio = viewRatio / cameraRatio
                }
            }
            Matrix.orthoM(
                projectMatrix, 0,
                -widthRatio, widthRatio,
                -heightRatio, heightRatio,
                3f, 5f
            )
            Matrix.setLookAtM(
                cameraMatrix, 0,
                0f, 0f, 5.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
            )
            //计算变换矩阵
            Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, cameraMatrix, 0)
        }

        override fun onDrawFrame() {
            //开启深度测试
            /********** 绘制摄像头画面   */
            //在OpenGLES环境中使用程序
            GLES30.glUseProgram(program)
            mvpMatrixHandle = GLES30.glGetUniformLocation(program, "vMatrix")
            uPosHandle = GLES30.glGetAttribLocation(program, "vPositionCoord")
            aTexHandle = GLES30.glGetAttribLocation(program, "vTextureCoord")
            uTextureHandle = GLES30.glGetAttribLocation(program, "vTexture")

            //激活指定纹理单元
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            //绑定纹理ID到纹理单元
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
            //将激活的纹理单元传递到着色器里面
            GLES30.glUniform1i(uTextureHandle, 0)
            //配置边缘过渡参数
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR.toFloat())
            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            cameraTexture?.updateTexImage() //通过此方法更新接收到的预览数据

            // 将前面计算得到的mMVPMatrix(frustumM setLookAtM 通过multiplyMM 相乘得到的矩阵) 传入vMatrix中，与顶点矩阵进行相乘
            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES30.glVertexAttribPointer(uPosHandle, 3, GLES30.GL_FLOAT, false, 0, posBuffer)
            GLES30.glVertexAttribPointer(aTexHandle, 2, GLES30.GL_FLOAT, false, 0, texBuffer)
            GLES30.glEnableVertexAttribArray(uPosHandle)
            GLES30.glEnableVertexAttribArray(aTexHandle)
            //顶点个数是4个 mPosCoordinate.length/2每个定点x、y2个坐标，所以得到顶点个数。
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, posCoordinate.size / 2)
            GLES30.glDisableVertexAttribArray(uPosHandle)
            GLES30.glDisableVertexAttribArray(aTexHandle)
            GLES30.glUseProgram(0)
            /********* 开始绘制  */
        }

        override fun release() {
            Camera1Manager.getInstance().stopPreview()
            cameraTexture?.setOnFrameAvailableListener(null)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glDeleteTextures(1, texture, 0)
            GLES30.glDeleteProgram(program)
            Camera1Manager.getInstance().release()
        }

        private fun createAndBindCameraTexture() {
            GLES30.glGenTextures(1, texture, 0) //生成一个OpenGl纹理
            cameraTexture = SurfaceTexture(texture[0]) //以上面OpenGl生成的纹理函数参数创建SurfaceTexture,SurfaceTexture接收的数据将传入该纹理
            cameraTexture?.setOnFrameAvailableListener(onFrameAvailableListener) //设置SurfaceTexture的回调，通过摄像头预览数据已更新
            Camera1Manager.getInstance().startPreview()
            Camera1Manager.getInstance().updatePreviewTexture(cameraTexture)
        }
    }
}