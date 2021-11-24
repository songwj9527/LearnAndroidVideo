package com.songwj.openvideo

import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager
import kotlinx.android.synthetic.main.activity_camera1_preview.*


class Camera1PreviewSurfaceViewActivity : AppCompatActivity(), SurfaceHolder.Callback,
    Camera1Manager.PreviewFrameCallback {
    private var dataBuffer: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_preview)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
        surface_view.holder.addCallback(this)
        Camera1Manager.getInstance().setPreviewCallback(this)
        btn_capture.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                dataBuffer?.let {
                    Camera1Manager.getInstance().pausePreview()
                    val jpegFilePath = Environment.getExternalStorageDirectory().absolutePath + "/capture_" + System.currentTimeMillis() + ".jpg"
                    Camera1Manager.getInstance().capture(dataBuffer, jpegFilePath)
                    Camera1Manager.getInstance().resumePreview()
                }
            }
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Camera1Manager.getInstance().stopPreview()
        Camera1Manager.getInstance().setPreviewHolder(holder)
        Camera1Manager.getInstance().startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {

    }

    override fun onResume() {
        super.onResume()
        Camera1Manager.getInstance().resumePreview()
    }

    override fun onPause() {
        Camera1Manager.getInstance().pausePreview()
        super.onPause()
    }

    override fun onDestroy() {
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        dataBuffer = data
    }
}