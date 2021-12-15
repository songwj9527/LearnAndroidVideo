package com.songwj.openvideo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import com.songwj.openvideo.camera.CameraXManager
import kotlinx.android.synthetic.main.activity_camera_x_preview.*

class CameraXPreviewActivity : AppCompatActivity(),
    CameraXManager.OnPreparedListener,
    CameraXManager.OnImageAnalysisListener,
    CameraXManager.OnErrorListener {
    private var camerax_preview: PreviewView? = null
    private var isOpened = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x_preview)
        CameraXManager.getInstance().releaseCamera()

        CameraXManager.getInstance().setOnPreparedListener(this)
        CameraXManager.getInstance().setOnImageAnalysisListener(this)
        CameraXManager.getInstance().setOnErrorListener(this)

        CameraXManager.getInstance().openCamera(this)
        btn_switch_camera.setOnClickListener {
            if (isOpened) {
                CameraXManager.getInstance().switchCamera(this)
            }
        }
        btn_take_capture.setOnClickListener {
            if (isOpened) {
                val jpegFilePath = Environment.getExternalStorageDirectory().absolutePath + "/capture_" + System.currentTimeMillis() + ".jpg"
                CameraXManager.getInstance().takeCapture(jpegFilePath)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            isPaused = false
            CameraXManager.getInstance().setPreviewView(camerax_preview)
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        CameraXManager.getInstance().setPreviewView(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraXManager.getInstance().releaseCamera()
    }

    override fun onPrepared(cameraId: Int, sensorRotationDegrees: Int) {
        isOpened = true
        camerax_container.removeAllViews()
        val widthMax = camerax_container.width
        val heightMax = camerax_container.height
        camerax_preview = PreviewView(this@CameraXPreviewActivity)
        val ratioView = (widthMax.toFloat() / heightMax.toFloat())
        val ratioPreview = if (sensorRotationDegrees == 90 || sensorRotationDegrees == 270) {
            (1280f / 1920f)
        } else {
            (1920f / 1280f)
        }
        val lp = if (ratioView > ratioPreview) {
            FrameLayout.LayoutParams(
                (heightMax * ratioPreview).toInt(),
                heightMax
            )
        } else {
            FrameLayout.LayoutParams(
                widthMax,
                (widthMax / ratioPreview).toInt()
            )
        }

        lp.gravity = Gravity.CENTER
        camerax_container.addView(camerax_preview, lp)
        camerax_container.requestLayout()
        CameraXManager.getInstance().setPreviewView(camerax_preview)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onImageAnalyze(image: ImageProxy?) {
        image?.let {
//            Log.e("CameraXPreviewActivity", "onImageAnalyze(): ${it.format} >> ${it.width}, ${it.height}")
            it.image?.let {

            }
        }
    }

    override fun onError(errorCode: Int, errorMessage: String?) {
        isOpened = false
        Log.e("CameraXPreviewActivity", "onError(): $errorCode, $errorMessage")
    }
}