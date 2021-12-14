package com.songwj.openvideo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.ffmpeg.NativeLoader
import kotlinx.android.synthetic.main.activity_main.*

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
        btn_feng_light.setOnClickListener(this)
        btn_texture_light.setOnClickListener(this)
        btn_camera1_preview_surface_view.setOnClickListener(this)
        btn_camera1_preview_texture_view.setOnClickListener(this)
        btn_camera1_system_media_record.setOnClickListener(this)
        btn_camera1_video_record_from_nv21.setOnClickListener(this)
        btn_camera1_preview_gl_surface_view.setOnClickListener(this)
        btn_camera1_preview_filter_gl_surface_view.setOnClickListener(this)
        btn_camera1_gl_surface_view_record.setOnClickListener(this)
        btn_camera2_preview.setOnClickListener(this)
        btn_camera2_take_picture.setOnClickListener(this)
        btn_camera2_video_record.setOnClickListener(this)
        btn_camera2_output.setOnClickListener(this)
        btn_camera2_preview_filter_gl_surface_view.setOnClickListener(this)
        btn_camera2_gl_surface_view_record.setOnClickListener(this)

        handlerPermission()
    }

    private val REQUEST_PERMISSION = 101
    private fun handlerPermission() {
        requestPermissions(arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAPTURE_AUDIO_OUTPUT

        ), REQUEST_PERMISSION)
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
            else if (id == R.id.btn_feng_light) {
                startActivity(Intent(this, FengLightActivity::class.java))
            }
            else if (id == R.id.btn_texture_light) {
                startActivity(Intent(this, TextureLightActivity::class.java))
            }
            else if (id == R.id.btn_camera1_preview_surface_view) {
                startActivity(Intent(this, Camera1PreviewSurfaceViewActivity::class.java))
            }
            else if (id == R.id.btn_camera1_preview_texture_view) {
                startActivity(Intent(this, Camera1PreviewTextureViewActivity::class.java))
            }
            else if (id == R.id.btn_camera1_system_media_record) {
                startActivity(Intent(this, Camera1SystemMediaRecordActivity::class.java))
            }
            else if (id == R.id.btn_camera1_video_record_from_nv21) {
                startActivity(Intent(this, Camera1VideoRecordFromNV21Activity::class.java))
            }
            else if (id == R.id.btn_camera1_preview_gl_surface_view) {
                startActivity(Intent(this, Camera1PreviewGLSurfaceActivity::class.java))
            }
            else if (id == R.id.btn_camera1_preview_filter_gl_surface_view) {
                startActivity(Intent(this, Camera1PreviewGLSurfaceFilterActivity::class.java))
            }
            else if (id == R.id.btn_camera1_gl_surface_view_record) {
                startActivity(Intent(this, Camera1VideoRecordFromEGLActivity::class.java))
            }
            else if (id == R.id.btn_camera2_preview) {
                startActivity(Intent(this, Camera2PreviewActivity::class.java))
            }
            else if (id == R.id.btn_camera2_take_picture) {
                startActivity(Intent(this, Camera2TakePictureActivity::class.java))
            }
            else if (id == R.id.btn_camera2_video_record) {
                startActivity(Intent(this, Camera2VideoRecordActivity::class.java))
            }
            else if (id == R.id.btn_camera2_output) {
                startActivity(Intent(this, Camera2OutputActivity::class.java))
            }
            else if (id == R.id.btn_camera2_preview_filter_gl_surface_view) {
                startActivity(Intent(this, Camera2PreviewGLSurfaceFilterActivity::class.java))
            }
            else if (id == R.id.btn_camera2_gl_surface_view_record) {
                startActivity(Intent(this, Camera2RecordGLSurfaceFIlterActivity::class.java))
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
