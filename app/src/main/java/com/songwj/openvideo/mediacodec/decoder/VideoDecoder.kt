package com.songwj.openvideo.mediacodec.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import com.songwj.openvideo.mediacodec.extractor.IExtractor
import com.songwj.openvideo.mediacodec.extractor.VideoExtractor
import java.nio.ByteBuffer

class VideoDecoder(filePath: String, surface: Surface?) : BaseDecoder(filePath) {

    private var mSurface = surface
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mVideoRotation = 0

    override fun check(): Boolean {
        return true
    }

    override fun initExtractor(path: String): IExtractor {
        var extractor = VideoExtractor(path)
        mVideoWidth = extractor.getVideoWidth()
        mVideoHeight = extractor.getVideoHeight()
        mVideoRotation = extractor.getVideoRotation()
        return extractor
    }

    override fun initSpecParams(format: MediaFormat) {

    }

    override fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean {
        if (mSurface != null) {
            codec.configure(format, mSurface , null, 0)
            notifyDecode()
        }
        return true
    }

    override fun initRender(): Boolean {
        return true
    }

    override fun render(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {

    }

    override fun doneDecode() {

    }

    fun getVideoWidth(): Int {
        return mVideoWidth
    }

    fun getVideoHeight() : Int {
        return mVideoHeight
    }

    fun getVideoRotation() : Int {
        return mVideoRotation
    }

    fun setDisplay(surface: Surface?) {
        mSurface = surface
        mCodec?.let {
            it.configure(mExtractor!!.getFormat(), mSurface , null, 0)
            notifyDecode()
        }
    }
}