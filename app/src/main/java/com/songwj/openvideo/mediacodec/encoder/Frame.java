package com.songwj.openvideo.mediacodec.encoder;

public class Frame {
    byte[] buffer;
    long presentationTimeUs;

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public long getPresentationTimeUs() {
        return presentationTimeUs;
    }

    public void setPresentationTimeUs(long presentationTimeUs) {
        this.presentationTimeUs = presentationTimeUs;
    }
}
