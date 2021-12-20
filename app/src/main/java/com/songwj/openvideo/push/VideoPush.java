package com.songwj.openvideo.push;

import android.util.Size;

import com.songwj.openvideo.camera.Camera2Manager;
import com.songwj.openvideo.camera.Camera2Operator;

public class VideoPush implements BasePush, Camera2Operator.OnImageAvailableOutputListener {
    private volatile boolean isPushing = false;

    @Override
    public void startPush() {
        isPushing = true;
        Camera2Manager.getInstance().setOnImageAvailableOutputListener(this);
    }

    @Override
    public void stopPush() {
        isPushing = false;
        Camera2Manager.getInstance().setOnImageAvailableOutputListener(null);
    }

    @Override
    public void release() {
        stopPush();
    }

    @Override
    public void onImageAvailable(byte[] image, Size size) {
        if (image != null && isPushing) {
            // 视频编码
        }
    }
}
