package com.songwj.openvideo

import android.graphics.Rect
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.*
import com.songwj.openvideo.opengl.Camera2RecordGLSurfaceView
import kotlinx.android.synthetic.main.activity_camera1_video_record_from_egl.*

class Camera2RecordGLSurfaceFIlterActivity : AppCompatActivity() {
    private var glSurfaceView: Camera2RecordGLSurfaceView? = null
    var focus_view: FocusView? = null
    var focusManager: FocusManager? = null
    var isOpened = false
    var isPaused = false
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_video_record_from_egl)
        Camera2Manager.getInstance().releaseCamera()

        Camera2Manager.getInstance().switchMode(Camera2Manager.Mode.PREVIEW)
        Camera2Manager.getInstance().setRequestCallback(object : Camera2Operator.RequestCallback {
            override fun onOpened(
                cameraId: Int,
                sensorOrientation: Int,
                sensorRect: Rect,
                width: Int,
                height: Int
            ) {
                if (isOpened) {
                    runOnUiThread({
                        attachPreviewGLSurface(width, height, cameraId, sensorOrientation, sensorRect)
                    })
                }
            }


            override fun onAFStateChanged(state: Int) {
                if (!isOpened) {
                    return
                }
                when (state) {
                    CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.startFocus()
                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.focusSuccess()
                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.focusFailed()
                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.focusSuccess()
                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> {
                        if (isOpened) {
//                            runOnUiThread({
//                                focusManager?.autoFocus()
//                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.focusFailed()
                            })
                        }
                    }
                    CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
                        if (isOpened) {
                            runOnUiThread({
                                focusManager?.hideFocusUI()
                            })
                        }
                    }
                }
            }

        })
        isOpened = Camera2Manager.getInstance().openCamera()

        btn_switch_camera.setOnClickListener {
            if (isOpened && !isRecording) {
                isOpened = false
                focusManager?.setListener(null)
                focusManager?.removeDelayMessage()
                focusManager = null
                glSurfaceView = null
                Camera2Manager.getInstance().setSurfaceTexture(null)
//                glSurfaceView?.onRenderDestroy()
                isOpened = Camera2Manager.getInstance().swichCamera()
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

    private fun attachPreviewGLSurface(width: Int,
                                       height: Int,
                                       cameraId: Int,
                                       orientation: Int,
                                       cameraRect: Rect
    ) {
        gl_container_view.removeAllViews()
        glSurfaceView = Camera2RecordGLSurfaceView(this)
        if (orientation == 90 || orientation == 270) {
            glSurfaceView?.setVideoFrame(height, width)
        } else {
            glSurfaceView?.setVideoFrame(width, height)
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        gl_container_view.addView(glSurfaceView, lp)
        focus_view = FocusView(this)
        gl_container_view.addView(focus_view)
        gl_container_view.requestLayout()
        if (orientation == 90 || orientation == 270) {
            focus_view?.initFocusArea(gl_container_view.height, gl_container_view.width)
        } else {
            focus_view?.initFocusArea(gl_container_view.width, gl_container_view.height)
        }

        focusManager = FocusManager(focus_view, Looper.getMainLooper())
        focusManager?.onPreviewChanged(width, height, cameraId, orientation, cameraRect)
        focusManager?.setListener(object : FocusManager.CameraUiEvent {
            override fun resetTouchToFocus() {
                if (isOpened) {
                    Camera2Manager.getInstance().resetTouchToFocus()
                }
            }
        })
        glSurfaceView?.setOnGestureListener(object : GestureGLSurfaceView.OnGestureListener {
            override fun onClick(x: Float, y: Float) {
                if (isOpened) {
                    focusManager?.let {
                        it.startFocus(x, y)
                        val focusRect: MeteringRectangle = it.getFocusArea(x, y, true)
                        val meterRect: MeteringRectangle = it.getFocusArea(x, y, false)
                        Camera2Manager.getInstance().setTouchFocusRegions(focusRect, meterRect)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            isPaused = false
            Camera2Manager.getInstance().startPreview()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        Camera2Manager.getInstance().stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        glSurfaceView?.stopRecord()
        Camera2Manager.getInstance().releaseCamera()
        focusManager?.setListener(null)
        focusManager?.removeDelayMessage()
        focusManager = null
    }
}