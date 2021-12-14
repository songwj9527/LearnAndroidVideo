package com.songwj.openvideo

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.*
import com.songwj.openvideo.mediarecord.Camera2Recorder
import kotlinx.android.synthetic.main.activity_camera2_output.*

class Camera2OutputActivity : AppCompatActivity(), Camera2Operator.RequestCallback, TextureView.SurfaceTextureListener, Camera2Operator.OnImageAvailableOutputListener {
    var texture_view: GestureTextureRenderView? = null
    var focus_view: FocusView? = null
    var focusManager: FocusManager? = null
    var isOpened = false
    var isPaused = false

    var handler = Handler()
    @Volatile
    var takePicture = false
    var pictureFilePath = ""

    var camera2Recorder: Camera2Recorder? = null
    @Volatile
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_output)
        Camera2Manager.getInstance().releaseCamera()

        Camera2Manager.getInstance().switchMode(Camera2Manager.Mode.OUTPUT_YUV420)
        Camera2Manager.getInstance().setRequestCallback(this)
        Camera2Manager.getInstance().setOnImageAvailableOutputListener(this)
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

        btn_take_capture.setOnClickListener {
            synchronized(this@Camera2OutputActivity) {
                if (takePicture) {
                    return@setOnClickListener
                }
                takePicture = true
                pictureFilePath = Environment.getExternalStorageDirectory().absolutePath + "/capture_" + System.currentTimeMillis() + ".jpg"
            }
        }
        btn_start.setOnClickListener {
            synchronized(this@Camera2OutputActivity) {
                if (isRecording) {
                    return@setOnClickListener
                }
                isRecording = true
                val videoFilePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
                val previewSize = Camera2Manager.getInstance().previewSize
                val cameraOrientation = Camera2Manager.getInstance().cameraOrientation
                camera2Recorder = if (cameraOrientation == 90 || cameraOrientation == 270) {
                    Camera2Recorder(videoFilePath, previewSize.height, previewSize.width)
                } else {
                    Camera2Recorder(videoFilePath, previewSize.width, previewSize.height)
                }
                camera2Recorder?.start()
            }
        }
        btn_stop.setOnClickListener {
            synchronized(this@Camera2OutputActivity) {
                if (isRecording) {
                    camera2Recorder?.stop()
                    camera2Recorder = null
                    isRecording = false;
                }
            }
        }
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

    var nv21: ByteArray? = null
    var dest: ByteArray? = null
    var nv12: ByteArray? = null
    override fun onImageAvailable(image: ByteArray?, size: Size?) {
        if (image == null || size == null) {
            return
        }
        if (takePicture) {
            Camera2Manager.getInstance().takePictureYUV420(
                handler,
                Camera2Manager.CaptureYUV420Runnable(
                    pictureFilePath,
                    image,
                    size,
                    Camera2Manager.getInstance().cameraId,
                    Camera2Manager.getInstance().cameraOrientation
                )
            )
            takePicture = false
        }
        if (isRecording) {
            val cameraOrientation = Camera2Manager.getInstance().cameraOrientation
            var width = size.width
            var height = size.height

            if (dest == null || dest!!.size != (width * height * 3 / 2)) {
                dest = ByteArray(width * height * 3 / 2)
            }
            if (nv21 == null || nv21!!.size != (width * height * 3 / 2)) {
                nv21 = ByteArray(width * height * 3 / 2)
            }
            if (nv12 == null || nv12!!.size != (width * height * 3 / 2)) {
                nv12 = ByteArray(width * height * 3 / 2)
            }

            // 将YUV420转换成NV21
            CameraFrameUtils.yuv420ToNv21(image, nv21, width, height)
            // 默认摄像头图像传感器的坐标系（图像）有旋转角度的，所以想要旋转相应角度，才是屏幕正常显示的坐标（图像）
            CameraFrameUtils.nv21Rotate(nv21, dest, width, height, cameraOrientation)
            // nv21转换成nv12的数据
            CameraFrameUtils.nv21toNV12(dest, nv12)
            camera2Recorder?.let {
                it.onVideoFrameUpdate(nv12)
            }
        }
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