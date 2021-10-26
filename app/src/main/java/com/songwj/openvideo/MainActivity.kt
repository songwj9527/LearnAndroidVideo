package com.songwj.openvideo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.ffmpeg.NativeLoader
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock

class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NativeLoader.getInstance().loadLibrary()

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
        btn_native_ffmpeg_repack.setOnClickListener(this)
        btn_native_codec_player.setOnClickListener(this)
        btn_model_loader.setOnClickListener(this)
        btn_cube_scale_rotate.setOnClickListener(this)
        btn_skybox.setOnClickListener(this)

        var test : ArrayBlockingQueue<String>? = null
        var lock: ReentrantLock? = null
        var hashMap: HashMap<String, Any>? =null

        handlerPermission()
    }

    private val REQUEST_PERMISSION = 101
    private fun handlerPermission() {
        requestPermissions(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION) {

        } else {
            super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            var id = v.id
            if (id == R.id.btn_media_codec) {
                startActivity(Intent(this, MediaCodecActivity::class.java))
            }
            else if (id == R.id.btn_simple_opengl_render) {
                var newIntent = Intent(this, SimpleRenderGLSurfaceViewActivity::class.java)
                newIntent.putExtra("type", 1)
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
            else if (id == R.id.btn_native_ffmpeg_repack) {
                startActivity(Intent(this, FFmpegRepackActivity::class.java))
            } else if (id == R.id.btn_native_codec_player) {
                startActivity(Intent(this, NativeCodecPlayerActivity::class.java))
            } else if (id == R.id.btn_model_loader) {
                startActivity(Intent(this, ModelLoaderActivity::class.java))
            }
            else if (id == R.id.btn_cube_scale_rotate) {
                startActivity(Intent(this, CubeScaleRotateActivity::class.java))
            }
            else if (id == R.id.btn_skybox) {
                startActivity(Intent(this, SkyboxActivity::class.java))
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
