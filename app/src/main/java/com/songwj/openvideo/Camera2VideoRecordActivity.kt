package com.songwj.openvideo

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.*
import kotlinx.android.synthetic.main.activity_camera2_video_record.*

class Camera2VideoRecordActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    var texture_view: GestureTextureRenderView? = null
    var focus_view: FocusView? = null
    var focusManager: FocusManager? = null
    var isOpened = false
    var isPaused = false
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_video_record)
        Camera2Manager.getInstance().releaseCamera()

        Camera2Manager.getInstance().switchMode(Camera2Manager.Mode.VIDEO_RECORD)
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
                        attachTextureView(width, height, cameraId, sensorOrientation, sensorRect)
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
                texture_view = null
                Camera2Manager.getInstance().setSurfaceTexture(null)
                isOpened = Camera2Manager.getInstance().swichCamera()
            }
        }

        btn_start.setOnClickListener {
            if (isRecording) {
                return@setOnClickListener
            }
            isRecording = true
            val videoFilePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
            Camera2Manager.getInstance().startRecord(videoFilePath, object : Camera2Operator.RecordVideoCallback {
                override fun onStarted() {

                }

                override fun onError() {
                    isRecording = false
                }

                override fun onStoped() {
                    isRecording = false
                }

            })
        }

        btn_stop.setOnClickListener {

        }
    }

    private fun attachTextureView(width: Int,
                                  height: Int,
                                  cameraId: Int,
                                  orientation: Int,
                                  cameraRect: Rect
    ) {
        camera_container.removeAllViews()
        texture_view = GestureTextureRenderView(this)
        if (orientation == 90 || orientation == 270) {
            texture_view?.setVideoFrame(height, width)
        } else {
            texture_view?.setVideoFrame(width, height)
        }
        texture_view?.surfaceTextureListener = this
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        camera_container.addView(texture_view, lp)
        focus_view = FocusView(this)
        camera_container.addView(focus_view)
        camera_container.requestLayout()
        if (orientation == 90 || orientation == 270) {
            focus_view?.initFocusArea(camera_container.height, camera_container.width)
        } else {
            focus_view?.initFocusArea(camera_container.width, camera_container.height)
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
        texture_view?.setOnGestureListener(object : GestureTextureRenderView.OnGestureListener {
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
        Camera2Manager.getInstance().releaseCamera()
        focusManager?.setListener(null)
        focusManager?.removeDelayMessage()
        focusManager = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Camera2Manager.getInstance().setSurfaceTexture(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        surface?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }
}