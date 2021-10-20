package com.songwj.openvideo

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.opengl.renders.CubeMultiSixRotateRender
import kotlinx.android.synthetic.main.activity_cube_scale_rotate.*

class CubeScaleRotateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cube_scale_rotate)
        gl_surface_view.setEGLContextClientVersion(3)
        gl_surface_view.setRenderer(CubeMultiSixRotateRender())
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}