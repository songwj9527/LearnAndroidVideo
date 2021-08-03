package com.songwj.openvideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.View
import com.songwj.openvideo.mediacodec.decoder.BaseDecoder
import com.songwj.openvideo.mediacodec.Frame
import com.songwj.openvideo.mediacodec.MediaCodecPlayer
import com.songwj.openvideo.opengl.drawer.IDrawer
import com.songwj.openvideo.opengl.drawer.VideoDrawer
import com.songwj.openvideo.opengl.egl.CustomerGLSurfaceRender
import kotlinx.android.synthetic.main.activity_egl_player_surface_view.*

class EGLPlayerSurfaceViewActivity : AppCompatActivity() {
    private val videoName1 = "video.mp4"
    private var videoPath1 = ""
    private val videoName2 = "video2.mp4"
    private var videoPath2 = ""
    private lateinit var mediaCodecPlayer1: MediaCodecPlayer
    private lateinit var drawer1: IDrawer
    private lateinit var mediaCodecPlayer2: MediaCodecPlayer
    private lateinit var drawer2: IDrawer
    private var renderer = CustomerGLSurfaceRender()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egl_player_surface_view)

        videoPath1 = Environment.getExternalStorageDirectory().absolutePath + "/" + videoName1
        videoPath2 = Environment.getExternalStorageDirectory().absolutePath + "/" + videoName2
        mediaCodecPlayer1 = MediaCodecPlayer()
        mediaCodecPlayer1.setOnInfoListener(object : MediaCodecPlayer.OnInfoListener {
            override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
                Log.e(">>>>>", "onInfo: video 1")
                info?.let {
                    drawer1.setVideoInfo(info.width, info.height, info.rotation)
//                    mediaCodecPlayer?.setDisplay(Surface((drawer1 as VideoDrawer).getSurfaceTexture()))
                }
            }

        })
        mediaCodecPlayer1.setOnFrameListener(object : MediaCodecPlayer.OnFrameListener{
            override fun onFrame(
                mediaCodecPlayer: MediaCodecPlayer?,
                baseDecoder: BaseDecoder?,
                frame: Frame?
            ) {
                frame?.let {
                    Log.e(">>>>>", "onFrame: video 1")
                    renderer.notifySwap(frame.bufferInfo.presentationTimeUs)
                }

            }

        })
        mediaCodecPlayer1.setOnPreparedListener(object : MediaCodecPlayer.OnPreparedListener {
            override fun onPrepared(mediaCodecPlayer: MediaCodecPlayer?) {
                Log.e(">>>>>", "onPrepared: video 1")
                mediaCodecPlayer?.start()
            }

        })
        mediaCodecPlayer2 = MediaCodecPlayer()
        mediaCodecPlayer2.setOnInfoListener(object : MediaCodecPlayer.OnInfoListener {
            override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
                Log.e(">>>>>", "onInfo: video 2")
                info?.let {
                    drawer2.setVideoInfo(info.width, info.height, info.rotation)
//                    mediaCodecPlayer?.setDisplay(Surface((drawer2 as VideoDrawer).getSurfaceTexture()))
                }
            }

        })
        var isScaled = false;
        mediaCodecPlayer2.setOnFrameListener(object : MediaCodecPlayer.OnFrameListener{
            override fun onFrame(
                mediaCodecPlayer: MediaCodecPlayer?,
                baseDecoder: BaseDecoder?,
                frame: Frame?
            ) {
                frame?.let {
                    Log.e(">>>>>", "onFrame: video 2")
                    renderer.notifySwap(frame.bufferInfo.presentationTimeUs)
                }
                if (!isScaled) {
                    isScaled = true
                    // 设置缩放系数
//                    surface_view.postDelayed({
//                        drawer2.let {
//                            (it as VideoDrawer)?.scale(0.5f, 0.5f, 1.0f)
//                        }
//                    }, 100)
                    drawer2.let {
                        (it as VideoDrawer)?.scale(0.5f, 0.5f, 1.0f)
                    }
                }
            }

        })
        mediaCodecPlayer2.setOnPreparedListener(object : MediaCodecPlayer.OnPreparedListener {
            override fun onPrepared(mediaCodecPlayer: MediaCodecPlayer?) {
                Log.e(">>>>>", "onPrepared: video 2")
                mediaCodecPlayer?.start()
            }

        })

        initFirstRender()
        initSecondRender()
        renderer.setSurface(surface_view)
        btn_play.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                mediaCodecPlayer1.setDataSource(videoPath1)
//                mediaCodecPlayer1?.prepare()
                mediaCodecPlayer2.setDataSource(videoPath2)
//                mediaCodecPlayer2?.prepare()
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
        renderer?.addDrawer(drawer1)
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
        renderer?.addDrawer(drawer2)
    }

    private fun initPlayer1(sf: Surface) {
        mediaCodecPlayer1?.setDisplay(sf)
//        mediaCodecPlayer1.setDataSource(videoPath1)

    }

    private fun initPlayer2(sf: Surface) {
        mediaCodecPlayer2?.setDisplay(sf)
//        mediaCodecPlayer2.setDataSource(videoPath2)
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
    }
}