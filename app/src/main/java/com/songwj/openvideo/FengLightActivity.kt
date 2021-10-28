package com.songwj.openvideo

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.opengl.renders.FengLightRender
import kotlinx.android.synthetic.main.activity_feng_light.*

class FengLightActivity : AppCompatActivity() {
    private var fengLightRender: FengLightRender? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feng_light)

        gl_surface_view.setEGLContextClientVersion(3)
        fengLightRender = FengLightRender()
        gl_surface_view.setRenderer(fengLightRender)
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onDestroy() {
        super.onDestroy()
        fengLightRender?.release()
    }
}