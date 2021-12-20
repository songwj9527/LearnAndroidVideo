package com.songwj.openvideo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_rtmp_live_push.*

class RTMPLivePushActivity : AppCompatActivity() {
    private val RTMP_LIVE_URL = "rtmp://202.197.224.44:1935/songwj9527/live"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtmp_live_push)
        btn_switch_camera.setOnClickListener {
            av_stream_push.switchCamera()
        }
    }

    override fun onDestroy() {
        av_stream_push.release()
        super.onDestroy()
    }
}