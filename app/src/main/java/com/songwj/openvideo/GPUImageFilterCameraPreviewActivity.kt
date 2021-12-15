package com.songwj.openvideo

import android.graphics.Rect
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Size
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.*
import com.songwj.openvideo.gupimage.GPUImageFilterUtils
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.util.Rotation
import kotlinx.android.synthetic.main.activity_gupimage_filter_camera_preview.*

class GPUImageFilterCameraPreviewActivity : AppCompatActivity(),
    Camera2Operator.RequestCallback,
    Camera2Operator.OnImageAvailableOutputListener,
    SeekBar.OnSeekBarChangeListener{
    var focus_view: FocusView? = null
    var focusManager: FocusManager? = null
    var isOpened = false
    var isPaused = false

    @Volatile
    private var attachGPUImageView = false
    private val noImageFilter = GPUImageFilter()
    private var currentImageFilter = noImageFilter
    private var filterAdjuster: GPUImageFilterUtils.FilterAdjuster? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gupimage_filter_camera_preview)

        Camera2Manager.getInstance().releaseCamera()
        Camera2Manager.getInstance().switchMode(Camera2Manager.Mode.OUTPUT_YUV420)
        Camera2Manager.getInstance().setRequestCallback(this)
        Camera2Manager.getInstance().setOnImageAvailableOutputListener(this)
        isOpened = Camera2Manager.getInstance().openCamera()

        switch_camera_iv.setOnClickListener {
            if (isOpened) {
                isOpened = false

                attachGPUImageView = false
                tone_seekbar.setOnSeekBarChangeListener(null)
                tone_seekbar.progress = 0
                gpu_image_view.gpuImage.deleteImage()

                focusManager?.setListener(null)
                focusManager?.removeDelayMessage()
                focusManager = null
                isOpened = Camera2Manager.getInstance().swichCamera()
            }
        }

        filter_name_tv.setOnClickListener {
            GPUImageFilterUtils.showDialog(this, onGpuImageFilterChosenListener)
        }
        tone_seekbar.setOnSeekBarChangeListener(this)
        close_iv.setOnClickListener {
            finish()
        }
        save_iv.setOnClickListener {
            if (isOpened && attachGPUImageView) {
                gpu_image_view.saveToPictures("", "capture_" + System.currentTimeMillis() + ".jpg", object : GPUImageView.OnPictureSavedListener {
                    override fun onPictureSaved(p0: Uri?) {

                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        if (ViewCompat.isLaidOut(gpu_image_view) && !gpu_image_view.isLayoutRequested()) {
//            Camera2Manager.getInstance().startPreview()
//        } else {
//            gpu_image_view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
//                override fun onLayoutChange(
//                    v: View,
//                    left: Int,
//                    top: Int,
//                    right: Int,
//                    bottom: Int,
//                    oldLeft: Int,
//                    oldTop: Int,
//                    oldRight: Int,
//                    oldBottom: Int
//                ) {
//                    gpu_image_view.removeOnLayoutChangeListener(this)
//                    Camera2Manager.getInstance().startPreview()
//                }
//            })
//        }
        if (isPaused) {
            isPaused = false
            Camera2Manager.getInstance().startPreview()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        Camera2Manager.getInstance().stopPreview()
//        gpu_image_view.onPause()
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
            runOnUiThread {
                attachGPUImageView(width, height, cameraId, sensorOrientation, sensorRect)
            }
        }
    }

    private fun attachGPUImageView(width: Int,
                                  height: Int,
                                  cameraId: Int,
                                  orientation: Int,
                                  cameraRect: Rect) {
        focus_view?.let {
            camera_container.removeView(focus_view)
        }
        gpu_image_view.filter = currentImageFilter
        gpu_image_view.setRatio(height.toFloat() / width.toFloat())
        setGPUImageRotate(cameraId, orientation)
        gpu_image_view.setRenderMode(GPUImageView.RENDERMODE_CONTINUOUSLY)
        attachGPUImageView = true

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
        gpu_image_view.setOnGestureListener(object : GestureGPUImageView.OnGestureListener {
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

    private fun setGPUImageRotate(cameraId: Int, sensorOrientation: Int) {
        val rotation = getRotation(sensorOrientation)
        var flipHorizontal = false
        var flipVertical = false
        if (Camera2Manager.getInstance().isFrontCamera) { // 前置摄像头需要镜像
            if (rotation == Rotation.NORMAL || rotation == Rotation.ROTATION_180) {
                flipHorizontal = true
            } else {
                flipVertical = true
            }
        }
        gpu_image_view.gpuImage.setRotation(rotation, flipHorizontal, flipVertical)
    }

    private fun getRotation(orientation: Int): Rotation {
        return when (orientation) {
            90 -> Rotation.ROTATION_90
            180 -> Rotation.ROTATION_180
            270 -> Rotation.ROTATION_270
            else -> Rotation.NORMAL
        }
    }

    override fun onAFStateChanged(state: Int) {
        if (!isOpened) {
            return
        }
        when (state) {
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN -> {
                if (isOpened) {
                    runOnUiThread {
                        focusManager?.startFocus()
                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> {
                if (isOpened) {
                    runOnUiThread {
                        focusManager?.focusSuccess()
                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> {
                if (isOpened) {
                    runOnUiThread {
                        focusManager?.focusFailed()
                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> {
                if (isOpened) {
                    runOnUiThread{
                        focusManager?.focusSuccess()
                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> {
                if (isOpened) {
//                    runOnUiThread {
//                        focusManager?.autoFocus()
//                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> {
                if (isOpened) {
                    runOnUiThread {
                        focusManager?.focusFailed()
                    }
                }
            }
            CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
                if (isOpened) {
                    runOnUiThread {
                        focusManager?.hideFocusUI()
                    }
                }
            }
        }
    }

    override fun onImageAvailable(image: ByteArray?, size: Size?) {
        image?.let {
            if (isOpened && attachGPUImageView) {
                var nv21 = ByteArray(size!!.width * size.height * 3 / 2)
                CameraFrameUtils.YUV420ToNV21(image, nv21, size!!.width, size.height)
                gpu_image_view.updatePreviewFrame(nv21, size!!.width, size.height)
            }
        }
    }

    private val onGpuImageFilterChosenListener = object : GPUImageFilterUtils.OnGpuImageFilterChosenListener {
        override fun onGpuImageFilterChosenListener(filter: GPUImageFilter?, filterName: String?) {
            if (isOpened && attachGPUImageView) {
                switchFilterTo(filter!!)
                filter_name_tv.setText(filterName)
            }
        }
    }

    private fun switchFilterTo(filter: GPUImageFilter) {
        if (currentImageFilter == null
            || filter != null && currentImageFilter.javaClass != filter.javaClass
        ) {
            currentImageFilter = filter
            gpu_image_view.filter = currentImageFilter
            filterAdjuster = GPUImageFilterUtils.FilterAdjuster(currentImageFilter)
            tone_seekbar.setVisibility(if (filterAdjuster!!.canAdjust()) View.VISIBLE else View.GONE)
        } else {
            tone_seekbar.setVisibility(View.GONE)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (isOpened && attachGPUImageView) {
            filterAdjuster?.adjust(progress)
            gpu_image_view.requestRender()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }
}