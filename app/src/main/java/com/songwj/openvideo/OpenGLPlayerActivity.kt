package com.songwj.openvideo

import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.mediacodec.MediaCodecPlayer
import com.songwj.openvideo.opengl.SimpleRender
import com.songwj.openvideo.opengl.drawer.IDrawer
import com.songwj.openvideo.opengl.drawer.VideoDrawer
import kotlinx.android.synthetic.main.activity_opengl_player.*

class OpenGLPlayerActivity : AppCompatActivity(), MediaCodecPlayer.OnPreparedListener,MediaCodecPlayer.OnInfoListener {
    private val videoName = "video2.mp4"
    private var videoPath = ""
    private lateinit var mediaCodecPlayer: MediaCodecPlayer
    private lateinit var drawer: IDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_player)

        videoPath = Environment.getExternalStorageDirectory().absolutePath + "/" + videoName
        mediaCodecPlayer = MediaCodecPlayer()
        mediaCodecPlayer.setOnInfoListener(this)
        initRender()
        btn_play.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                mediaCodecPlayer?.start()
            }

        })
    }

    private fun initRender() {
        drawer = VideoDrawer()
        //设置视频宽高
//        drawer.setVideoSize(1920, 1080)
        drawer.setAlpha(1f)
        drawer.getSurfaceTexture {
            //使用SurfaceTexture初始化一个Surface，并传递给MediaCodec使用
//            mediaCodecPlayer?.setDisplay(Surface(it))
            it?.let {
                initPlayer(Surface(it))
            }
        }
        var render = SimpleRender()
        render.addDrawer(drawer)
        move_surface_view.setEGLContextClientVersion(2)
        move_surface_view.setRenderer(render)
    }

    private fun initPlayer(sf: Surface) {
        mediaCodecPlayer?.setDisplay(sf)
        mediaCodecPlayer.setDataSource(videoPath)
    }

    override fun onPrepared(mediaCodecPlayer: MediaCodecPlayer?) {
//        mediaCodecPlayer?.prepare()
    }

    override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
        info?.let {
            drawer.setVideoInfo(info.width, info.height, info.rotation)
//            mediaCodecPlayer?.setDisplay(Surface((drawer as VideoDrawer).getSurfaceTexture()))
        }
    }

//    override fun onResume() {
//        super.onResume()
//        mediaCodecPlayer?.resume()
//    }

    override fun onPause() {
        super.onPause()
        mediaCodecPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaCodecPlayer?.stop()
        mediaCodecPlayer?.release()
        drawer?.release()
    }
}