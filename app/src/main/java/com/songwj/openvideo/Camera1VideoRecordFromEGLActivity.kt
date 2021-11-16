package com.songwj.openvideo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager

class Camera1VideoRecordFromEGLActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_video_record_from_egl)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
    }
}