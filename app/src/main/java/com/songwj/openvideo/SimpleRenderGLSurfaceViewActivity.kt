package com.songwj.openvideo

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.songwj.openvideo.opengl.renders.SimpleRender
import com.songwj.openvideo.opengl.drawer.BitmapDrawer
import com.songwj.openvideo.opengl.drawer.IDrawer
import com.songwj.openvideo.opengl.drawer.TriangleDrawer
import kotlinx.android.synthetic.main.activity_simple_render_glsurface_view.*

class SimpleRenderGLSurfaceViewActivity : AppCompatActivity() {

    var type = 0
    lateinit var drawer: IDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_render_glsurface_view)

        type = intent.getIntExtra("type", 0)
        if (type == 0) {
            drawer = TriangleDrawer()
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.cover)
            drawer = BitmapDrawer(bitmap)
        }
        gl_surface_view.setEGLContextClientVersion(2)
        val simpleRender = SimpleRender()
        simpleRender.addDrawer(drawer)
        gl_surface_view.setRenderer(simpleRender)
    }

    override fun onDestroy() {
        drawer.release()
        super.onDestroy()
    }
}
