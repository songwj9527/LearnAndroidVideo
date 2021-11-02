package com.songwj.openvideo

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.opengl.renders.TextureLightRender
import kotlinx.android.synthetic.main.activity_texture_light.*

class TextureLightActivity : AppCompatActivity() {
    private var textureLightRender: TextureLightRender? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_texture_light)

        gl_surface_view.setEGLContextClientVersion(3)
        textureLightRender = TextureLightRender()
        gl_surface_view.setRenderer(textureLightRender)
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onDestroy() {
        super.onDestroy()
        textureLightRender?.release()
    }


}