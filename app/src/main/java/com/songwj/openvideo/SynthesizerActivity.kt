package com.songwj.openvideo

import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.SeekBar
import com.songwj.openvideo.ffmpeg.NativePlayer
import kotlinx.android.synthetic.main.activity_synthesizer.*
import java.math.BigDecimal

class SynthesizerActivity : AppCompatActivity() , TextureView.SurfaceTextureListener {
    var filePath: String? = null
    var nativePlayer: NativePlayer? = null
    var texture_view: TextureRenderView? = null
    var surface: Surface? = null
    var isStartButtonPress = false
    var isPrepared = false
    var isStarted = false
    var isPaused = false
    var isSeeking = false
    var isCompleted = false
    var duration:Long = 0L
    var currentPosition:Long = 0L

    private var handler: Handler? = object : Handler() {
        override fun dispatchMessage(msg: Message?) {
            super.dispatchMessage(msg)
            msg?.let {
                if (it.what == 1010) {
                    if (duration > 0) {
                        var progress = -1
                        var seconds = 0L
                        if (isCompleted) {
                            seconds = duration / 1000
                            progress = 100
                        } else {
                            var currentDuration = nativePlayer?.currentDuration
                            currentDuration?.let {
                                progress = (it * 100 / duration).toInt()
                                seconds = it / 1000
                            }
                        }
                        if (!(progress < 0)) {
                            var secondsStr = "00"
                            var minutes = 0L
                            var minutesStr = "00"
                            var hours = 0L
                            var hoursStr = "00"
                            if (seconds > 3600) {
                                hours = seconds / 3600
                                hoursStr = if (hours > 9) {
                                    "$hours"
                                } else {
                                    "0$hours"
                                }
                                seconds = (seconds % 3600)
                            }
                            if (seconds > 60) {
                                minutes = seconds / 60
                                minutesStr = if (minutes > 9) {
                                    "$minutes"
                                } else {
                                    "0$minutes"
                                }
                                seconds = (seconds % 60)
                            }
                            secondsStr = if (seconds > 9) {
                                "$seconds"
                            } else {
                                "0$seconds"
                            }
                            var text = ("$hoursStr:$minutesStr:$secondsStr")
                            tv_time.text = text
                            progress_seek_bar.progress = progress
                        }
                    }
                    if (isStarted && !isPaused && !isCompleted && !isSeeking) {
                        this.sendEmptyMessageDelayed(1010, 1000)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_synthesizer)

        filePath = intent.getStringExtra("file_path")

        nativePlayer = NativePlayer(NativePlayer.PlayerType.SYNTHESIZER)
        Log.e(
            "FFmpegActivity",
            "onCreate()"
        )
//        texture_view.surfaceTextureListener = this
        nativePlayer?.CreateSynthesizer( if(TextUtils.isEmpty(filePath)) {
            Environment.getExternalStorageDirectory().absolutePath + "/video2.mp4"
        } else {
            filePath
        }, Environment.getExternalStorageDirectory().absolutePath + "/video_synthesizer.mp4")
        start.setOnClickListener {
            if (!isStartButtonPress) {
                isStartButtonPress = true
                isPaused = false
                if (!isPrepared) {
                    nativePlayer?.prepareSync()
                }
            } else if(isPrepared && !isStarted) {
                nativePlayer?.startSynthesizer()
                isStarted = true
                isPaused = false
                handler?.sendEmptyMessage(1010)
            } else if(isStarted) {

            }
        }
        nativePlayer?.setOnErrorListener(object : NativePlayer.OnErrorListener {
            override fun onError(player: NativePlayer?, what: Int, extra: Int, msg: String?) {
                Log.e("FFmpegActivity", "$what, $extra, $msg")
                nativePlayer?.reset()
                isStarted = false
                isPrepared = false
                isSeeking = false
                isStartButtonPress = false
                isCompleted = false
                isPaused = false
            }
        })
        nativePlayer?.setOnPreparedListener(object : NativePlayer.OnPreparedListener {
            override fun onPrepared(player: NativePlayer?) {
                Log.e("FFmpegActivity", "onPrepared()")
                isPrepared = true
//                nativePlayer?.start()
                try {
                    var timestamp = nativePlayer?.duration
                    timestamp?.let {
                        duration = timestamp
                    }
                    var seconds = duration / 1000
                    var secondsStr = "00"
                    var minutes = 0L
                    var minutesStr = "00"
                    var hours = 0L
                    var hoursStr = "00"
                    if (seconds > 3600) {
                        hours = seconds / 3600
                        hoursStr = if (hours < 10) {
                            "0$hours"
                        } else {
                            "$hours"
                        }
                        seconds = (seconds % 3600)
                    }
                    if (seconds > 60) {
                        minutes = seconds / 60
                        minutesStr = if (minutes < 10) {
                            "0$minutes"
                        } else {
                            "$minutes"
                        }
                        seconds = (seconds % 60)
                    }
                    secondsStr = if (seconds < 10) {
                        "0$seconds"
                    } else {
                        "$seconds"
                    }
                    var text = ("$hoursStr:$minutesStr:$secondsStr")
                    tv_duration.text = text
                    Log.e("FFmpegActivity", "video duration: $text")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
//                nativePlayer?.setSurface(surface)
            }

        })
        nativePlayer?.setOnInfoListener(object : NativePlayer.OnInfoListener {
            override fun onInfo(
                player: NativePlayer?,
                videoWidth: Int,
                videoHeight: Int,
                videoRotation: Int
            ) {
                Log.e(
                    "FFmpegActivity",
                    "onInfo(): $videoWidth, $videoHeight, $videoRotation"
                )
                texture_view = TextureRenderView(this@SynthesizerActivity)
                texture_view?.setVideoFrame(videoWidth, videoHeight)
                texture_view?.setVideoRotationDegree(videoRotation)
                texture_view?.rotation = (videoRotation.toFloat())
                texture_view?.setSurfaceTextureListener(this@SynthesizerActivity)

                val lp = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.gravity = Gravity.CENTER
                texture_container.addView(texture_view, lp)
                texture_view?.requestLayout()
            }
        })
        nativePlayer?.setOnSeekCompletedListener(object : NativePlayer.OnSeekCompletedListener {
            override fun onSeekCompleted(player: NativePlayer?) {
                Log.e(
                    "FFmpegActivity",
                    "onSeekCompleted(): $isStarted, $isPaused, $isCompleted"
                )
                isSeeking = false
                if (isStarted && !isPaused && !isCompleted) {
                    nativePlayer?.resume()
                    Log.e(
                        "FFmpegActivity",
                        "onSeekCompleted(): resume()"
                    )
                } else if(isPrepared && !isStarted) {
                    isStarted = true
                    nativePlayer?.start()
                    Log.e(
                        "FFmpegActivity",
                        "onSeekCompleted(): start()"
                    )
                } else {
                    Log.e(
                        "FFmpegActivity",
                        "onSeekCompleted(): else"
                    )
                }
                progress_bar.visibility = View.GONE
                handler?.sendEmptyMessage(1010)
            }

        })
        nativePlayer?.setOnCompletedListener(object : NativePlayer.OnCompletedListener {
            override fun onCompleted(player: NativePlayer?) {
                Log.e(
                    "FFmpegActivity",
                    "onCompleted()"
                )
                isCompleted = true
                isSeeking = false
                var seconds = duration / 1000
                var secondsStr = "00"
                var minutes = 0L
                var minutesStr = "00"
                var hours = 0L
                var hoursStr = "00"
                if (seconds > 3600) {
                    hours = seconds / 3600
                    hoursStr = if (hours < 10) {
                        "0$hours"
                    } else {
                        "$hours"
                    }
                    seconds = (seconds % 3600)
                }
                if (seconds > 60) {
                    minutes = seconds / 60
                    minutesStr = if (minutes < 10) {
                        "0$minutes"
                    } else {
                        "$minutes"
                    }
                    seconds = (seconds % 60)
                }
                secondsStr = if (seconds < 10) {
                    "0$seconds"
                } else {
                    "$seconds"
                }
                var text = ("$hoursStr:$minutesStr:$secondsStr")
                tv_time.text = text
            }

        })

        progress_seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }
    var startPosition = 0

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.e(
            "FFmpegActivity",
            "onSurfaceTextureAvailable(): $width, $height"
        )
        this.surface = Surface(surface)
        nativePlayer?.setSurface(this.surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
//        Log.e(
//            "FFmpegActivity",
//            "onSurfaceTextureSizeChanged(): $width, $height"
//        )
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
//        Log.e(
//            "FFmpegActivity",
//            "onSurfaceTextureUpdated()"
//        )
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
//        Log.e(
//            "FFmpegActivity",
//            "onSurfaceTextureDestroyed()"
//        )
        nativePlayer?.setSurface(null)
        surface?.release()
        return true
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

//    override fun onStop() {
//        super.onStop()
//        nativePlayer?.stop()
//    }

    override fun onDestroy() {
        nativePlayer?.releaseSynthesizer()
        nativePlayer = null
        handler?.removeMessages(1010)
        super.onDestroy()
        handler = null
    }
}