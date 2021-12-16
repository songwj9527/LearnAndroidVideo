package com.songwj.openvideo

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.mediacodec.MediaCodecPlayer
import kotlinx.android.synthetic.main.activity_media_codec.*


class MediaCodecActivity : AppCompatActivity(),
    View.OnClickListener,
    SeekBar.OnSeekBarChangeListener,
    TextureView.SurfaceTextureListener,
    MediaCodecPlayer.OnPreparedListener,
    MediaCodecPlayer.OnErrorListener,
    MediaCodecPlayer.OnInfoListener,
    MediaCodecPlayer.OnProgressListener,
    MediaCodecPlayer.OnSeekCompleteListener,
    MediaCodecPlayer.OnCompleteListener {

    private val videoName = "h265.mp4"
    private var videoPath: String = ""

    private var textureView: TextureRenderView? = null
    private lateinit var mediaCodecPlayer: MediaCodecPlayer
    private var isRunning = false

    private var handler: Handler? = object : Handler() {
        override fun dispatchMessage(msg: Message?) {
            super.dispatchMessage(msg)
            msg?.let {
                if (it.what == 1010) {
                    Log.e(">>>>>>", "what: ${it.what}")
                    var duration = mediaCodecPlayer?.duration
                    Log.e(">>>>>>", "duration: $duration; position: ${mediaCodecPlayer?.currentTimestamp}")
                    if (duration != null && duration > 0) {
                        var progress = -1
                        var seconds = 0L
                        if (mediaCodecPlayer.state == MediaCodecPlayer.State.COMPLETED) {
                            seconds = duration
                            progress = 100
                        } else {
                            var currentDuration = mediaCodecPlayer?.currentTimestamp
                            currentDuration?.let {
                                progress = (it * 100 / duration).toInt()
                                seconds = it
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
                            progress_seekbar.progress = progress
                        }
                    }
                    mediaCodecPlayer?.let {
                        if (it.state == MediaCodecPlayer.State.STARTED) {
                            this.sendEmptyMessageDelayed(1010, 1000)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)

        // https://fg-im.oss-cn-hangzhou.aliyuncs.com/develop/2021/09/18/16319561101667686608883e84d8f24620c26c1c48d8ae600a243a8.mp4
        // https://fg-im.oss-cn-hangzhou.aliyuncs.com/develop/2020/04/08/15863342135663238314273demo.mov
        // https://fg-im.oss-cn-hangzhou.aliyuncs.com/develop/2020/04/08/15863400286359235045891VID_20200327_155101.mp4
        videoPath = "https://fg-im.oss-cn-hangzhou.aliyuncs.com/develop/2020/04/08/15863342135663238314273demo.mov"
//        var inPath = intent.getStringExtra("file_path")
//        videoPath = if(TextUtils.isEmpty(inPath)) {
//            Environment.getExternalStorageDirectory().absolutePath + "/" + videoName
//        } else {
//            inPath
//        }

        progress_seekbar.setOnSeekBarChangeListener(this)
        start.setOnClickListener(this)
        pause.setOnClickListener(this)

        mediaCodecPlayer = MediaCodecPlayer()
        mediaCodecPlayer.setOnPreparedListener(this)
        mediaCodecPlayer.setOnErrorListener(this)
        mediaCodecPlayer.setOnInfoListener(this)
        mediaCodecPlayer.setOnProgressListener(this)
        mediaCodecPlayer.setOnSeekCompleteListener(this)
        mediaCodecPlayer.setOnCompleteListener(this)
    }

    override fun onClick(v: View?) {
        v?.let {
            var id = v.id
            if (id == R.id.start) {
                if (isRunning) {
                    if (mediaCodecPlayer.state == MediaCodecPlayer.State.PAUSED) {
                        mediaCodecPlayer.resume()
                        handler?.sendEmptyMessage(1010)
                    }
                    else if (mediaCodecPlayer.state == MediaCodecPlayer.State.COMPLETED) {
                        progress_seekbar.progress = 0
                        tv_time.text = "00:00:00"
//                        mediaCodecPlayer.reset()
//                        mediaCodecPlayer.start()
                        mediaCodecPlayer.restart()
                    }
                    return
                }
                isRunning = true
                tv_time.text = "00:00:00"
                progress_seekbar.progress = 0
                textureView?.let {
                    it.surfaceTextureListener = null
                    texture_container.removeView(it)
                    textureView = null
                }
                textureView = TextureRenderView(this)
                textureView!!.surfaceTextureListener = this

                val lp = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.gravity = Gravity.CENTER
                texture_container.addView(textureView, lp)

                mediaCodecPlayer?.setDataSource(videoPath)
                Toast.makeText(this, "start.", Toast.LENGTH_SHORT).show()
            }
            if (id == R.id.pause) {
                if (!isRunning) {
                    return
                }
                mediaCodecPlayer?.pause()
                Toast.makeText(this, "pause.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mediaCodecPlayer?.resume()
        handler?.sendEmptyMessage(1010)
    }

    override fun onPause() {
        super.onPause()
        mediaCodecPlayer?.pause()
        handler?.removeMessages(1010)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaCodecPlayer?.stop()
        mediaCodecPlayer?.release()
        handler?.removeMessages(1010)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        Log.e("MediaCodecActivity", "onProgressChanged(): " + progress)
    }

    var startPosition = 0
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        Log.e("MediaCodecActivity", "onStartTrackingTouch(): " + seekBar?.progress)
        if (mediaCodecPlayer?.state != MediaCodecPlayer.State.IDLE && mediaCodecPlayer?.state != MediaCodecPlayer.State.SEEKING) {
            mediaCodecPlayer?.pause()
        }
        seekBar?.let {
            startPosition = it.progress
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        Log.e("MediaCodecActivity", "onStopTrackingTouch(): " + seekBar?.progress)
        seekBar?.let {
            if (mediaCodecPlayer?.state == MediaCodecPlayer.State.IDLE || mediaCodecPlayer.state == MediaCodecPlayer.State.SEEKING) {
                it.progress = startPosition
                Toast.makeText(this, "seeking.", Toast.LENGTH_SHORT).show()
            } else {
                progress_bar.visibility = View.VISIBLE
//                if (it.progress < startPosition) {
//                    mediaCodecPlayer.returnToPosition(it.progress)
//                    return
//                }
                mediaCodecPlayer.seekTo(it.progress)
            }
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.e("MediaCodecActivity", "onSurfaceTextureAvailable(): $width, $height")
        mediaCodecPlayer?.setDisplay(Surface(surface))
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.e("MediaCodecActivity", "onSurfaceTextureSizeChanged(): $width, $height")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        Log.e("MediaCodecActivity", "onSurfaceTextureUpdated()")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        Log.e("MediaCodecActivity", "onSurfaceTextureDestroyed()")
        mediaCodecPlayer?.setDisplay(null)
        surface?.release()
        return true
    }


    override fun onPrepared(mediaCodecPlayer: MediaCodecPlayer?) {
        Log.e("MediaCodecActivity", "onPrepared()")
        runOnUiThread {
            tv_time.text = "00:00:00"
        }
        var duration = mediaCodecPlayer?.duration
        if (duration != null) {
            if (duration > 3600L) {
                var hour = duration / 3600
                var minute = 0L
                var second = duration % 3600
                if (second > 60) {
                    minute = second / 60
                    second = second % 60
                }
                var text = if (hour > 9) {
                    ("" + hour)
                } else {
                    ("0" + hour)
                }

                text += ":"
                text += if (minute > 9) {
                    minute
                } else {
                    ("0" + minute)
                }

                text += ":"
                text += if (second > 9) {
                    second
                } else {
                    ("0" + second)
                }
                runOnUiThread {
                    tv_duration.text = text
                }
            } else if (duration > 60) {
                var minute = duration / 60
                var second = duration % 60

                var text = "00:"
                text += if (minute > 9) {
                    minute
                } else {
                    ("0" + minute)
                }

                text += ":"
                text += if (second > 9) {
                    second
                } else {
                    ("0" + second)
                }
                runOnUiThread {
                    tv_duration.text = text
                }
            } else {
                var text = "00:00:"
                text += if (duration > 9) {
                    duration
                } else {
                    ("0" + duration)
                }
                runOnUiThread {
                    tv_duration.text = text
                }
            }
        }

        mediaCodecPlayer?.start()
        handler?.sendEmptyMessage(1010)
    }

    override fun onProgress(mediaCodecPlayer: MediaCodecPlayer?, progress: Long) {

    }

    override fun onSeekComplete(mediaCodecPlayer: MediaCodecPlayer?) {
        Log.e("MediaCodecActivity", "onSeekComplete()")
        handler?.sendEmptyMessage(1010)
        runOnUiThread {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onComplete(mediaCodecPlayer: MediaCodecPlayer?) {
        Log.e("MediaCodecActivity", "onComplete()")
        handler?.sendEmptyMessage(1010)
    }

    override fun onInfo(mediaCodecPlayer: MediaCodecPlayer?, info: MediaCodecPlayer.Info?) {
        info?.let {
            Log.e("MediaCodecActivity", "onInfo(): " + info.width + ", " + info.height + info.rotation )
            textureView?.let {
                if (info.rotation == 90 || info.rotation == 270) {
                    it.setVideoFrame(info.height, info.width)
                } else {
                    it.setVideoFrame(info.width, info.height)
                }
//                it.setVideoRotationDegree(info.rotation)
//                if (info.rotation > 0) {
//                    it.setRotation(1.0f*info.rotation)
//                }
                runOnUiThread {
                    it.requestLayout()
                }
            }

        }
    }

    override fun onError(mediaCodecPlayer: MediaCodecPlayer?, what: Int, msg: String?) {
        Log.e("MediaCodecActivity", "onError(): " + what + ", " + msg)
    }
}
