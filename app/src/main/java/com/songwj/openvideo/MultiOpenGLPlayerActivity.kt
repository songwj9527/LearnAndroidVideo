package com.songwj.openvideo

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.mediacodec.MediaCodecPlayer
import com.songwj.openvideo.opengl.SimpleRender
import com.songwj.openvideo.opengl.drawer.IDrawer
import com.songwj.openvideo.opengl.drawer.VideoDrawer
import kotlinx.android.synthetic.main.activity_opengl_player.*

class MultiOpenGLPlayerActivity : AppCompatActivity() {
    private val videoName1 = "video.mp4"
    private var videoPath1 = ""
    private val videoName2 = "video2.mp4"
    private var videoPath2 = ""
    private var render: SimpleRender? = null
    private lateinit var mediaCodecPlayer1: MediaCodecPlayer
    private lateinit var drawer1: IDrawer
    private lateinit var mediaCodecPlayer2: MediaCodecPlayer
    private lateinit var drawer2: IDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_player)

        videoPath1 = Environment.getExternalStorageDirectory().absolutePath + "/" + videoName1
        videoPath2 = Environment.getExternalStorageDirectory().absolutePath + "/" + videoName2
        mediaCodecPlayer1 = MediaCodecPlayer()
        mediaCodecPlayer1.setOnInfoListener(object : MediaCodecPlayer.OnInfoListener {
            override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
                info?.let {
                    drawer1.setVideoInfo(info.width, info.height, info.rotation)
//            mediaCodecPlayer?.setDisplay(Surface((drawer as VideoDrawer).getSurfaceTexture()))
                }
            }

        })
        mediaCodecPlayer2 = MediaCodecPlayer()
        mediaCodecPlayer2.setOnInfoListener(object : MediaCodecPlayer.OnInfoListener {
            override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
                info?.let {
                    drawer2.setVideoInfo(info.width, info.height, info.rotation)
//            mediaCodecPlayer?.setDisplay(Surface((drawer as VideoDrawer).getSurfaceTexture()))
                }
            }

        })
        render = SimpleRender()
        initFirstRender()
        initSecondRender()
        move_surface_view.setEGLContextClientVersion(2)
        move_surface_view.setRenderer(render)

        btn_play.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                mediaCodecPlayer1?.start()
                mediaCodecPlayer2?.start()
            }

        })
    }

    private fun initFirstRender() {
        drawer1 = VideoDrawer()
        //设置视频宽高
//        drawer.setVideoSize(1920, 1080)
        drawer1.setAlpha(1f)
        drawer1.getSurfaceTexture {
            //使用SurfaceTexture初始化一个Surface，并传递给MediaCodec使用
//            mediaCodecPlayer?.setDisplay(Surface(it))
            it?.let {
                initPlayer1(Surface(it))
            }
        }
        render?.addDrawer(drawer1)
    }

    private fun initSecondRender() {
        drawer2 = VideoDrawer()
        //设置视频宽高
//        drawer.setVideoSize(1920, 1080)
        drawer2.setAlpha(0.7f)
        drawer2.getSurfaceTexture {
            //使用SurfaceTexture初始化一个Surface，并传递给MediaCodec使用
//            mediaCodecPlayer?.setDisplay(Surface(it))
            it?.let {
                initPlayer2(Surface(it))
            }
        }
        render?.addDrawer(drawer2)
        // 设置缩放系数
        Handler().postDelayed({
            drawer2.let {
                (it as VideoDrawer)?.scale(0.5f, 0.5f, 1.0f)
            }
        }, 1000)
        move_surface_view.addDrawer(drawer2 as VideoDrawer)
    }

    private fun initPlayer1(sf: Surface) {
        mediaCodecPlayer1?.setDisplay(sf)
        mediaCodecPlayer1.setDataSource(videoPath1)
    }

    private fun initPlayer2(sf: Surface) {
        mediaCodecPlayer2?.setDisplay(sf)
        mediaCodecPlayer2.setDataSource(videoPath2)
    }

//    override fun onResume() {
//        super.onResume()
//        mediaCodecPlayer?.resume()
//    }

    override fun onPause() {
        super.onPause()
        mediaCodecPlayer1?.pause()
        mediaCodecPlayer2?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaCodecPlayer1?.stop()
        mediaCodecPlayer2?.stop()
        mediaCodecPlayer1?.release()
        mediaCodecPlayer2?.release()
        drawer1?.release()
        drawer2?.release()
    }
}