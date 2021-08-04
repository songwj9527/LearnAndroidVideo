package com.songwj.openvideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import com.songwj.openvideo.ffmpeg.NativeRepack
import kotlinx.android.synthetic.main.activity_ffmpeg_repack.*

class FFmpegRepackActivity : AppCompatActivity(), View.OnClickListener {
    private var ffRepack: NativeRepack? = null
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg_repack)

        ffRepack = NativeRepack()
        ffRepack?.CreateRepack(Environment.getExternalStorageDirectory().absolutePath + "/video.mp4",
            Environment.getExternalStorageDirectory().absolutePath + "/video_repack.mp4")
        start.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (!started) {
            started = true
            Thread(object : Runnable {

                override fun run() {
                    ffRepack?.startRepack()
                }

            }).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ffRepack?.releaseRepack()
    }
}