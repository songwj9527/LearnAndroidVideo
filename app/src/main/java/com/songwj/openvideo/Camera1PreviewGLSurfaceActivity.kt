package com.songwj.openvideo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager

class Camera1PreviewGLSurfaceActivity : AppCompatActivity() {
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_preview_glsurface)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
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
}