package com.songwj.openvideo

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.camera.Camera1Manager
import com.songwj.openvideo.camera.CameraUtils
import com.songwj.openvideo.mediarecord.MediaRecorder
import kotlinx.android.synthetic.main.activity_camera1_video_record_from_nv21.*

class Camera1VideoRecordFromNV21Activity : AppCompatActivity(), TextureView.SurfaceTextureListener, Camera1Manager.PreviewFrameCallback {
    private var isPaused = false
    private var mediaRecorder: MediaRecorder? = null
    private var isRecord = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1_video_record_from_nv21)
        Camera1Manager.getInstance().release()
        Camera1Manager.getInstance().bindActivity(this)
        if (Camera1Manager.getInstance().openCamera()) {
            texture_view?.setVideoFrame(Camera1Manager.getInstance().cameraSize.height, Camera1Manager.getInstance().cameraSize.width)
            texture_view?.surfaceTextureListener = this
            texture_view?.requestLayout()
            btn_command.setOnClickListener {
                if (isRecord) {
                    isRecord = false
                    mediaRecorder?.stop()
                    mediaRecorder = null
                    btn_command.text = "开始"
                } else {
                    val cameraSize = Camera1Manager.getInstance().cameraSize
                    val filePath = Environment.getExternalStorageDirectory().absolutePath + "/video_" + System.currentTimeMillis() + ".mp4"
                    if (Camera1Manager.getInstance().cameraDisplayOrientation == 90 || Camera1Manager.getInstance().cameraDisplayOrientation == 270) {
                        mediaRecorder = MediaRecorder(filePath, cameraSize.height, cameraSize.width, true)
                    } else {
                        mediaRecorder = MediaRecorder(filePath, cameraSize.width, cameraSize.height, true)
                    }
                    if (mediaRecorder!!.start()) {
                        isRecord = true
                        btn_command.text = "结束"
                    } else {
                        mediaRecorder?.stop()
                        mediaRecorder = null
                        isRecord = false
                    }
                }
            }
        }
        btn_switch_camera.setOnClickListener {
            Camera1Manager.getInstance().switchCamera()
            texture_view?.setVideoFrame(Camera1Manager.getInstance().cameraSize.height, Camera1Manager.getInstance().cameraSize.width)
            texture_view?.surfaceTextureListener = this
            texture_view?.requestLayout()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) {
            isPaused = false
            Camera1Manager.getInstance().resumePreview()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        Camera1Manager.getInstance().pausePreview()
    }

    override fun onDestroy() {
        Camera1Manager.getInstance().release()
        super.onDestroy()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Camera1Manager.getInstance().updatePreviewTexture(surface)
        Camera1Manager.getInstance().resumePreview()
        Camera1Manager.getInstance().setPreviewCallback(this)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        Camera1Manager.getInstance().stopPreview()
        Camera1Manager.getInstance().setPreviewTexture(null)
        Camera1Manager.getInstance().setPreviewCallback(null)
        surface?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        Camera1Manager.getInstance().setPreviewTexture(surface)
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (isRecord) {
            data?.let {
                val cameraSize = Camera1Manager.getInstance().cameraSize
                var dest = ByteArray(cameraSize.width * cameraSize.height * 3 / 2)
                // 默认摄像头图像传感器的坐标系（图像）旋转相应角度，才是屏幕正常显示的坐标（图像）
                if (Camera1Manager.getInstance().cameraOrientation == 270) {
                    CameraUtils.nv21RotateTo270(it, dest, cameraSize.width, cameraSize.height)
                } else if (Camera1Manager.getInstance().cameraOrientation == 90) {
                    CameraUtils.nv21RotateTo90(it, dest, cameraSize.width, cameraSize.height)
                } else {
                    dest = it
                }
                // nv21转换成nv12的数据
                var nv12 = CameraUtils.nv21toNV12(dest)
                mediaRecorder?.onVideoFrameUpdate(nv12)
            }
        }
    }
}