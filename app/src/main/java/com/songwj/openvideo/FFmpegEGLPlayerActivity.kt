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
import kotlinx.android.synthetic.main.activity_ffmpeg_eglplayer.*
import java.math.BigDecimal

class FFmpegEGLPlayerActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
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
                            var currentDuration = nativePlayer?.currentTimestamp
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
        setContentView(R.layout.activity_ffmpeg_eglplayer)

        filePath = intent.getStringExtra("file_path")

        nativePlayer = NativePlayer(NativePlayer.PlayerType.OPENGL_PLAYER)
        Log.e(
            "FFmpegActivity",
            "onCreate()"
        )
//        texture_view.surfaceTextureListener = this
        nativePlayer?.setDataSource( if(TextUtils.isEmpty(filePath)) {
            Environment.getExternalStorageDirectory().absolutePath + "/video2.mp4"
        } else {
            filePath
        })
        start.setOnClickListener {
            if (!isStartButtonPress) {
                isStartButtonPress = true
                isPaused = false
                if (!isPrepared) {
                    nativePlayer?.prepareSync()
                }
            } else if(isPrepared && !isStarted) {
                nativePlayer?.start()
                isStarted = true
                isPaused = false
                handler?.sendEmptyMessage(1010)
            } else if(isStarted) {
                if (isCompleted) {
                    isCompleted = false
                    isPaused = false
                    isSeeking = true
                    nativePlayer?.seekTo(0)
//                    handler?.sendEmptyMessage(1010)
                }
                else if (isPaused) {
                    nativePlayer?.resume()
                    isPaused = false
                    handler?.sendEmptyMessage(1010)
                } else if (isSeeking) {

                }
            }
        }
        pause.setOnClickListener {
            if (isStarted && !isSeeking) {
                nativePlayer?.pause()
                isPaused = true
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
                texture_view = TextureRenderView(this@FFmpegEGLPlayerActivity)
                texture_view?.setVideoFrame(videoWidth, videoHeight)
                texture_view?.setVideoRotationDegree(videoRotation)
                texture_view?.rotation = (videoRotation.toFloat())
                texture_view?.setSurfaceTextureListener(this@FFmpegEGLPlayerActivity)

                val lp = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.gravity = Gravity.CENTER
                texture_container.addView(texture_view, lp)
                texture_container.requestLayout()
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
                if (isPrepared) {
                    nativePlayer?.pause()
                }
                seekBar?.let {
                    startPosition = it.progress
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isSeeking) {
                    progress_seek_bar.progress = startPosition
                    return
                }
                var progress = startPosition
                seekBar?.let {
                    progress = it.progress
                }
                if (progress == startPosition) {
                    if (isStarted && !isPaused && !isCompleted) {
                        nativePlayer?.resume()
                    }
                    return
                }

                if (!isPrepared) {
                    progress_seek_bar.progress = 0
                    return
                }
                progress_bar.visibility = View.VISIBLE
                isSeeking = true
                var timestamp = (1.0 * progress * duration / 100)
                Log.e(
                    "FFmpegActivity",
                    "seekTo() ${BigDecimal("$timestamp").toLong()} / $duration"
                )
                if (isCompleted) {
                    isCompleted = false
                    isPaused = true
                }
                nativePlayer?.seekTo(BigDecimal("$timestamp").toLong())
                var seconds = (progress * duration / 100) / 1000
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
        if (!isPaused && isPrepared && isCompleted) {
            nativePlayer?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isPaused && isPrepared && isCompleted) {
            nativePlayer?.pause()
        }
    }

//    override fun onStop() {
//        super.onStop()
//        nativePlayer?.stop()
//    }

    override fun onDestroy() {
        nativePlayer?.release()
        nativePlayer = null
        handler?.removeMessages(1010)
        super.onDestroy()
        handler = null
    }
}