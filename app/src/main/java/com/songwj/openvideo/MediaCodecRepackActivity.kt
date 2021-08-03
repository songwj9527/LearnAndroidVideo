package com.songwj.openvideo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import com.songwj.openvideo.mediacodec.muxer.MP4Repack
import kotlinx.android.synthetic.main.activity_media_codec_repack.*

class MediaCodecRepackActivity : AppCompatActivity() {
    var repack: MP4Repack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec_repack)

        button.setOnClickListener {
            if (repack != null) {
                return@setOnClickListener
            }
            repack = MP4Repack(Environment.getExternalStorageDirectory().absolutePath + "/video.mp4")
            repack?.start()
        }

        button2.setOnClickListener{
            val fileName = "LVideo_Test" + /*SimpleDateFormat("yyyyMM_dd-HHmmss").format(Date()) +*/ ".mp4"
            val filePath = Environment.getExternalStorageDirectory().absolutePath.toString() + "/"
            var newIntent = Intent(this, FFmpegPlayerActivity::class.java)
            newIntent.putExtra("file_path", filePath + fileName)
            startActivity(newIntent)
        }
    }
}