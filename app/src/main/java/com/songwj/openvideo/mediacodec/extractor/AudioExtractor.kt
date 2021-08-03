package com.songwj.openvideo.mediacodec.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer


/**
 * 音频数据提取器
 *
 */
class AudioExtractor(path: String): IExtractor {

    private val mMediaExtractor = MMExtractor(path)

    override fun getMediaExtractor(): MediaExtractor? {
        return mMediaExtractor.getMediaExtractor()
    }

    override fun getTrack(): Int {
        return mMediaExtractor.getAudioTrack()
    }

    override fun getFormat(): MediaFormat? {
        return mMediaExtractor.getAudioFormat()
    }

    override fun readBuffer(byteBuffer: ByteBuffer): Int {
        return mMediaExtractor.readBuffer(byteBuffer)
    }

    override fun getCurrentTimestamp(): Long {
        return mMediaExtractor.getCurrentTimestamp()
    }

    override fun getSampleFlag(): Int {
        return mMediaExtractor.getSampleFlag()
    }

    override fun setStartPosition(position: Long) {
        return mMediaExtractor.setStartPosition(position)
    }

    override fun seekTo(position: Long, mode: Int): Long {
        return mMediaExtractor.seekTo(position, mode)
    }

    override fun getValidSampleTime(time: Long, maxRange: Int): Long {
        return time
    }

    override fun stop() {
        mMediaExtractor.stop()
    }
}