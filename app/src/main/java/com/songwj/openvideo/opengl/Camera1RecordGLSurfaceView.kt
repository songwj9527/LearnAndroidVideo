package com.songwj.openvideo.opengl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.songwj.openvideo.camera.Camera1Manager
import com.songwj.openvideo.mediarecord.Camera1EglRecorder
import com.songwj.openvideo.opengl.filter.CameraFilter
import com.songwj.openvideo.opengl.filter.CubeFilter
import com.songwj.openvideo.opengl.filter.DuskColorFilter
import com.songwj.openvideo.opengl.filter.ScreenFilter
import com.songwj.openvideo.opengl.renders.Camera1FilterRender
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Camera1RecordGLSurfaceView : Camera1FilterGLSurfaceView {
    constructor(context: Context?) : super(context) {
        setEGLContextClientVersion(3)
        render = Camera1FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setEGLContextClientVersion(3)
        render = Camera1FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        handler.removeCallbacksAndMessages(null)
        synchronized(this) { take_capture = false }
        handler.removeCallbacksAndMessages(null)
        stopRecord()
        super.surfaceDestroyed(holder)
    }

    override fun onDrawFrame(textureId: Int, timestamp: Long) {
        super.onDrawFrame(textureId, timestamp)
        synchronized(this) {
            if (take_capture && !TextUtils.isEmpty(pictureFilePath)) {
                take_capture = false
                // 1.glReadPixels返回的是大端的RGBA Byte组数，我们使用小端Buffer接收得到ABGR Byte组数
                val frameBuffer = ByteBuffer.allocateDirect(width * height * 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                GLES30.glReadPixels(
                    0,
                    0,
                    width,
                    height,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    frameBuffer
                )
                frameBuffer.rewind() //reset position
                val pixelCount = width * height
                val colors = IntArray(pixelCount)
                frameBuffer.asIntBuffer()[colors]
                for (i in 0 until pixelCount) {
                    val c = colors[i] //2.每个int类型的c是接收到的ABGR，但bitmap需要ARGB格式，所以需要交换B和R的位置
                    colors[i] = c and -0xff0100 or (c and 0x00ff0000 shr 16) or (c and 0x000000ff shl 16) //交换B和R，得到ARGB
                }
                //上下翻转
//                var matrix = android.graphics.Matrix()
//                // 缩放 当sy为-1时向上翻转 当sx为-1时向左翻转 sx、sy都为-1时相当于旋转180°
//                matrix.postScale(1f, -1f);
//                // 因为向上翻转了所以y要向下平移一个bitmap的高度
//                matrix.postTranslate(0f, bitmap.height.toFloat())
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                //上下翻转
                for (y in 0 until height / 2) {
                    for (x in 0 until width) {
                        val temp: Int = colors[(height - y - 1) * width + x]
                        colors[(height - y - 1) * width + x] = colors[y * width + x]
                        colors[y * width + x] = temp
                    }
                }
                handler.post(TakePictrue(pictureFilePath, width, height, colors))
                pictureFilePath = null
            }
            if (isRecording && recorder != null) {
                recorder!!.onDrawFrame(textureId, timestamp)
            }
        }
    }

    private class TakePictrue(filePath: String?, width: Int, height: Int, colors: IntArray) :
        Runnable {
        private val filePath: String
        private val width: Int
        private val height: Int
        private val colors: IntArray
        override fun run() {
            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(colors, 0, width, 0, 0, width, height)
            val saveFile = File(filePath)
            try {
                saveFile.createNewFile()
                val fileOutputStream = FileOutputStream(saveFile)
                if (fileOutputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                }
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            this.filePath = "$filePath"
            this.width = width
            this.height = height
            this.colors = colors
        }
    }

    @Volatile
    private var take_capture = false
    private var pictureFilePath: String? = null
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    fun takeCapture(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            return
        }
        handler.removeCallbacksAndMessages(null)
        synchronized(this) {
            pictureFilePath = filePath
            take_capture = true
        }
    }

    private var recorder: Camera1EglRecorder? = null

    @Volatile
    private var isRecording = false
    fun startRecord(videoFilePath: String?) {
        if (TextUtils.isEmpty(videoFilePath)) {
            return
        }
        synchronized(this) {
            if (isRecording) {
                return
            }
            if (recorder == null) {
                val cameraOrientation = Camera1Manager.getInstance().cameraOrientation
                val cameraSize = Camera1Manager.getInstance().cameraSize
                val width =
                    if (cameraOrientation == 90 || cameraOrientation == 270) cameraSize.height else cameraSize.width
                val height =
                    if (cameraOrientation == 90 || cameraOrientation == 270) cameraSize.width else cameraSize.height
                recorder = Camera1EglRecorder(videoFilePath, width, height, eglContext)
            } else {
                recorder!!.setDataSource(videoFilePath)
            }
            isRecording = recorder!!.start()
        }
    }

    fun stopRecord() {
        synchronized(this) {
            if (recorder != null) {
                recorder!!.stop()
            }
            isRecording = false
        }
    }
}