package com.songwj.openvideo

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager
import kotlinx.android.synthetic.main.activity_camera1_preview.*
import kotlinx.android.synthetic.main.activity_camera1_system_media_record.*
import kotlinx.android.synthetic.main.activity_camera1_system_media_record.surface_view
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException

class Camera1SystemMediaRecordActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var isPaused = false
    private var isCameraOk = false
    private var mediaRecorder: MediaRecorder? = null
    private var isRecord = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_system_media_record)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)

        surface_view.holder.addCallback(this)
        btn_command.setOnClickListener {
            if (!isCameraOk) {
                return@setOnClickListener
            }
            if (isRecord) {
                stopRecord()
                isRecord = false
                btn_command.text = "开始"
            } else {
                isRecord = startRecord()
                if (isRecord) {
                    btn_command.text = "停止"
                }
            }
        }
        btn_switch_camera.setOnClickListener {
            if (isCameraOk) {
                Camera1Manager.getInstance().camera.apply {
                    // 如果没有lock，switchCamera()会报 RuntimeException
                    lock()
                }
                Camera1Manager.getInstance().stopPreview()
                Camera1Manager.getInstance().switchCamera()
                Camera1Manager.getInstance().camera.apply {
//                    startPreview()
                    cancelAutoFocus()
                    // 否则会报 MediaRecorder(13280): start failed: -19
                    unlock()
                }
            }
        }
    }

    private var filePath = ""
    private fun startRecord(): Boolean {
        var ret = false
        val cameraSize = Camera1Manager.getInstance().cameraSize
        filePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
        try {
            mediaRecorder = MediaRecorder().apply {
                reset()
                setCamera(Camera1Manager.getInstance().camera)
                // 设置音频源与视频源 这两项需要放在setOutputFormat之前
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                //设置输出格式
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                //这两项需要放在setOutputFormat之后 IOS必须使用ACC
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)  //音频编码格式
                //使用MPEG_4_SP格式在华为P20 pro上停止录制时会出现
                //MediaRecorder: stop failed: -1007
                //java.lang.RuntimeException: stop failed.
                // at android.media.MediaRecorder.stop(Native Method)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)  //视频编码格式
                //设置最终出片分辨率
                setVideoSize(cameraSize.width, cameraSize.height)
                val cameraOrientation = Camera1Manager.getInstance().cameraOrientation
//                if (cameraOrientation == 90 || cameraOrientation == 270) {
//                    setVideoSize(cameraSize.height, cameraSize.width)
//                } else {
//                    setVideoSize(cameraSize.width, cameraSize.height)
//                }
                setOrientationHint(cameraOrientation)
//                setVideoSize(640, 480)
//                setOrientationHint(90)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(3 * cameraSize.height * cameraSize.width)
//                //设置记录会话的最大持续时间（毫秒）
//                setMaxDuration(60 * 1000)
                setOutputFile(filePath)
                prepare()
                start()
                ret = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder?.release()
            mediaRecorder = null
        }
        return ret
    }

    private fun stopRecord() {
        if (isRecord && mediaRecorder != null) {
            // 捕捉异常改为拍照
            try {
                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                }
            } catch (e: java.lang.RuntimeException) {
                //当catch到RE时，说明是录制时间过短
                Log.e("拍摄时间过短", e.message)
                mediaRecorder?.apply {
                    reset()
                    release()
                }
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            isPaused = false
            Camera1Manager.getInstance().resumePreview()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        Camera1Manager.getInstance().pausePreview()
    }

    override fun onDestroy() {
        Camera1Manager.getInstance().camera.apply {
            lock()
        }
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.e("MediaRecordActivity", "surfaceCreated")
        Camera1Manager.getInstance().setPreviewHolder(holder)
        isCameraOk = Camera1Manager.getInstance().openCamera()

    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.e("MediaRecordActivity", "surfaceChanged")
        if (isCameraOk) {
            Camera1Manager.getInstance().camera.apply {
                startPreview()
                cancelAutoFocus()
                // 否则会报 MediaRecorder(13280): start failed: -19
                unlock()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.e("MediaRecordActivity", "surfaceDestroyed")
    }
}