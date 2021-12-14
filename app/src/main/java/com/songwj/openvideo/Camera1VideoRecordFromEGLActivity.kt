package com.songwj.openvideo

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager
import com.songwj.openvideo.opengl.AspectRatioGLSurfaceView
import com.songwj.openvideo.opengl.Camera1RecordGLSurfaceView
import kotlinx.android.synthetic.main.activity_camera1_video_record_from_egl.*

class Camera1VideoRecordFromEGLActivity : AppCompatActivity() {
    private var glSurfaceView: Camera1RecordGLSurfaceView? = null
    private var isOpened = false
    private var isPaused = false
    private var isRecording = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_video_record_from_egl)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
        isOpened = Camera1Manager.getInstance().openCamera()
        if (isOpened) {
            glSurfaceView = Camera1RecordGLSurfaceView(this)
            glSurfaceView?.setRatioMode(AspectRatioGLSurfaceView.AspectRatioMode.EQUAL_PROPORTION_FIT_PARENT)
            val cameraOrientation = Camera1Manager.getInstance().cameraOrientation
            if (cameraOrientation == 90 || cameraOrientation == 270) {
                glSurfaceView?.setVideoFrame(Camera1Manager.getInstance().cameraSize.height, Camera1Manager.getInstance().cameraSize.width)
            } else {
                glSurfaceView?.setVideoFrame(Camera1Manager.getInstance().cameraSize.width, Camera1Manager.getInstance().cameraSize.height)
            }
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            gl_container_view.addView(glSurfaceView, lp)
        }

        btn_switch_camera.setOnClickListener {
            if (isOpened && !isRecording) {
                Camera1Manager.getInstance().switchCamera()
            }
        }
        btn_take_capture.setOnClickListener {
            if (isOpened) {
                val filePath = Environment.getExternalStorageDirectory().absolutePath + "/picture_" + System.currentTimeMillis() + ".jpeg"
                glSurfaceView?.takeCapture(filePath)
            }
        }
        btn_start.setOnClickListener {
            if (isOpened) {
                val filePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
                isRecording = glSurfaceView?.startRecord(filePath) == true
            }
        }
        btn_stop.setOnClickListener {
            if (isOpened && isRecording) {
                glSurfaceView?.stopRecord()
                isRecording = false
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
        glSurfaceView?.stopRecord()
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }
}