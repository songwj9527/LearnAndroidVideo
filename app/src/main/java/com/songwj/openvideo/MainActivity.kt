package com.songwj.openvideo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_media_codec.setOnClickListener(this)
        btn_simple_opengl_render.setOnClickListener(this)
        btn_simple_opengl_render.setOnLongClickListener(this)
        btn_opengl_player.setOnClickListener(this)
        btn_mult_opengl_player.setOnClickListener(this)
        btn_egl_player.setOnClickListener(this)
        btn_egl_player_texture.setOnClickListener(this)
        btn_fbo_player.setOnClickListener(this)
        btn_media_codec_repack.setOnClickListener(this)
        btn_native_ffmpeg_player.setOnClickListener(this)
        btn_native_ffmpeg_egl_player.setOnClickListener(this)

        var test : ArrayBlockingQueue<String>? = null
        var lock: ReentrantLock? = null
        var hashMap: HashMap<String, Any>? =null
    }

    override fun onClick(v: View?) {
        v?.let {
            var id = v.id
            if (id == R.id.btn_media_codec) {
                startActivity(Intent(this, MediaCodecActivity::class.java))
            }
            else if (id == R.id.btn_simple_opengl_render) {
                var newIntent = Intent(this, SimpleRenderGLSurfaceViewActivity::class.java)
                newIntent.putExtra("type", 0)
                startActivity(newIntent)
            }
            else if (id == R.id.btn_opengl_player) {
                startActivity(Intent(this, OpenGLPlayerActivity::class.java))
            }
            else if (id == R.id.btn_mult_opengl_player) {
                startActivity(Intent(this, MultiOpenGLPlayerActivity::class.java))
            }
            else if (id == R.id.btn_egl_player) {
                startActivity(Intent(this, EGLPlayerSurfaceViewActivity::class.java))
            }
            else if (id == R.id.btn_egl_player_texture) {
                startActivity(Intent(this, EGLPlayerTextureViewActivity::class.java))
            }
            else if (id == R.id.btn_fbo_player) {
                startActivity(Intent(this, FBOSelectActivity::class.java))
            }
            else if (id == R.id.btn_media_codec_repack) {
                startActivity(Intent(this, MediaCodecRepackActivity::class.java))
            }
            else if (id == R.id.btn_native_ffmpeg_player) {
                startActivity(Intent(this, FFmpegPlayerActivity::class.java))
            }
            else if (id == R.id.btn_native_ffmpeg_egl_player) {
                startActivity(Intent(this, FFmpegEGLPlayerActivity::class.java))
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        v?.let {
            var id = v.id
            if (id == R.id.btn_simple_opengl_render) {
                var newIntent = Intent(this, SimpleRenderGLSurfaceViewActivity::class.java)
                newIntent.putExtra("type", 1)
                startActivity(newIntent)
            }
        }
        return true
    }
}
