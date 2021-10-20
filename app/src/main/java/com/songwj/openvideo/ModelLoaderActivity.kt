package com.songwj.openvideo

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.songwj.openvideo.opengl.renders.ModelLoaderRender
import kotlinx.android.synthetic.main.activity_model_loader.*

class ModelLoaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_loader)
        gl_surface_view.setEGLContextClientVersion(3)
        gl_surface_view.setRenderer(ModelLoaderRender())
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}