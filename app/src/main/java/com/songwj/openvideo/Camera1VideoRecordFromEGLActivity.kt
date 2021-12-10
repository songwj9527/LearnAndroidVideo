package com.songwj.openvideo

import android.os.Bundle
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
    private var isPaused = false
    private var isRecord = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_video_record_from_egl)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
        if (Camera1Manager.getInstance().openCamera()) {
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

        btn_start.setOnClickListener {
            if (!isRecord) {
//                isRecord = camera1_preview_gl_surface_view.startRecord()
            }
        }
        btn_stop.setOnClickListener {
            if (isRecord) {
//                camera1_preview_gl_surface_view.stopRecord()
                isRecord = false
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
        if (isRecord) {
//            camera1_preview_gl_surface_view.stopRecord()
            isRecord = false
        }
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }
}