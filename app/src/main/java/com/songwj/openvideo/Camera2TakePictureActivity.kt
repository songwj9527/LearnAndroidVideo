package com.songwj.openvideo

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera2Manager
import kotlinx.android.synthetic.main.activity_camera2_take_picture.*

class Camera2TakePictureActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    var texture_view: TextureRenderView? = null
    var isOpened = false
    var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_take_picture)
        Camera2Manager.getInstance().releaseCamera()
        
        var cameraOrientation = Camera2Manager.getInstance().cameraOrientation
        var previewSize = Camera2Manager.getInstance().previewSize


        isOpened = Camera2Manager.getInstance().openCamera()
        if (isOpened) {
            texture_view = TextureRenderView(this)
            if (cameraOrientation == 90 || cameraOrientation == 270) {
                texture_view?.setVideoFrame(previewSize.height, previewSize.width)
            } else {
                texture_view?.setVideoFrame(previewSize.width, previewSize.height)
            }
            texture_view?.surfaceTextureListener = this
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            camera_container.addView(texture_view, lp)
        }

        btn_switch_camera.setOnClickListener {
            if (isOpened) {
                Camera2Manager.getInstance().releaseCamera()
                isOpened = Camera2Manager.getInstance().swichCamera()
                if (isOpened) {
                    var cameraOrientation = Camera2Manager.getInstance().cameraOrientation
                    var previewSize = Camera2Manager.getInstance().previewSize

                    camera_container.removeAllViews()
                    texture_view = TextureRenderView(this)
                    if (cameraOrientation == 90 || cameraOrientation == 270) {
                        texture_view?.setVideoFrame(previewSize.height, previewSize.width)
                    } else {
                        texture_view?.setVideoFrame(previewSize.width, previewSize.height)
                    }
                    texture_view?.surfaceTextureListener = this
                    val lp = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.gravity = Gravity.CENTER
                    camera_container.addView(texture_view, lp)
                    camera_container.requestLayout()
                }
            }
        }

        btn_take_capture.setOnClickListener {
            val jpegFilePath = Environment.getExternalStorageDirectory().absolutePath + "/capture_" + System.currentTimeMillis() + ".jpg"
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