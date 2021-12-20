package com.songwj.openvideo.push;

import android.media.AudioRecord;

import com.songwj.openvideo.camera.Camera2Manager;

public class AudioPush implements BasePush {
    private AudioRecord audioRecord = null;
    private volatile boolean isPushing = false;

    @Override
    public void startPush() {
        isPushing = true;
        new Thread(new AudioRecordTask()).start();
    }

    @Override
    public void stopPush() {
        isPushing = false;
        audioRecord.stop();
    }

    @Override
    public void release() {
        stopPush();
//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    class AudioRecordTask implements Runnable {

        @Override
        public void run() {
            audioRecord.startRecording();
            while (isPushing) {
//                byte[] bubffer = new byte[];
//                int length = audioRecord.read(bubffer, 0, bubffer.length);
//                if (length > 0) {
//                    // 进行音频编码
//                }
            }
        }
    }
}
