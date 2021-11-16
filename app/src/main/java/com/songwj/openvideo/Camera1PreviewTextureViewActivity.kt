package com.songwj.openvideo

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager
import kotlinx.android.synthetic.main.activity_camera1_preview_texture_view.*

class Camera1PreviewTextureViewActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, Camera1Manager.PreviewFrameCallback {
    private var texture_view: TextureRenderView? = null
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_preview_texture_view)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
        if (Camera1Manager.getInstance().openCamera()) {
            texture_view = TextureRenderView(this)
            texture_view?.setVideoFrame(Camera1Manager.getInstance().cameraSize.height, Camera1Manager.getInstance().cameraSize.width)
            texture_view?.surfaceTextureListener = this
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            root_view.addView(texture_view, lp)
            texture_view?.requestLayout()
            Camera1Manager.getInstance().setPreviewCallback(this)
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
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Camera1Manager.getInstance().updatePreviewTexture(surface)
        Camera1Manager.getInstance().resumePreview()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        Camera1Manager.getInstance().stopPreview()
        Camera1Manager.getInstance().setPreviewTexture(null)
        surface?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        Camera1Manager.getInstance().setPreviewTexture(surface)
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

    }
}