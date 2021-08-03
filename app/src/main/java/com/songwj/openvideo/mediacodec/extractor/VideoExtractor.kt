package com.songwj.openvideo.mediacodec.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer


/**
 * 视频数据提取器
 */
class VideoExtractor(path: String): IExtractor {

    private val mMediaExtractor = MMExtractor(path)

    /**每一帧持续时间，微秒*/
    private var mPreTimestamp: Long = 0

    override fun getMediaExtractor(): MediaExtractor? {
        return mMediaExtractor.getMediaExtractor()
    }

    override fun getTrack(): Int {
        return mMediaExtractor.getVideoTrack()
    }

    override fun getFormat(): MediaFormat? {
        return mMediaExtractor.getVideoFormat()
    }

    fun getVideoWidth(): Int {
        return mMediaExtractor.getVideoFormat()?.getInteger(MediaFormat.KEY_WIDTH)!!
    }

    fun getVideoHeight(): Int {
        return mMediaExtractor.getVideoFormat()?.getInteger(MediaFormat.KEY_HEIGHT)!!
    }

    fun getVideoRotation(): Int {
        return mMediaExtractor.getVideoFormat()?.getInteger(MediaFormat.KEY_ROTATION)!!
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

    fun getPreTimestamp(): Long {
        mMediaExtractor.getVideoFormat()?.let {
            mPreTimestamp = 1000000L / it.fps
            return mPreTimestamp
        }
        return 0L
    }

    override fun setStartPosition(position: Long) {
        return mMediaExtractor.setStartPosition(position)
    }

    override fun seekTo(position: Long, mode: Int): Long {
        return mMediaExtractor.seekTo(position, mode)
    }

    /**
     * 查找这个时间点对应的最接近的一帧。
     * seekTo指定位置播放不准（主要因为指定的位置可能不是I帧，所以最终效果可能是指定位置后，开始播放的位置在指定位置之前）
     * 该方法通过SEEK_TO_PREVIOUS_SYNC参数seek到指定位置的前一个I帧，然后向后推进，比较帧时间：
     * 这一帧的时间点如果和目标时间相差不到 一帧间隔 就算相近
     * maxRange:查找范围
     **/
    override fun getValidSampleTime(time: Long, maxRange: Int): Long {
        getMediaExtractor()?.let {
            val perFrameTime = getPreTimestamp()
            it.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var count = 0
            var sampleTime = it.sampleTime
            while (count < maxRange) {
                it.advance()
                val s = it.sampleTime
                if (s != -1L) {
                    count++
                    // 选取和目标时间差值最小的那个
                    sampleTime = if(sampleTime > s) {
                        s
                    } else {
                        sampleTime
                    }
                    if (Math.abs(sampleTime - time) <= perFrameTime) {
                        //如果这个差值在 一帧间隔 内，即为成功
                        return sampleTime
                    }
                } else {
                    count = maxRange
                }
            }
            return sampleTime
        }
        return time
    }

    override fun stop() {
        return mMediaExtractor.stop()
    }

    val MediaFormat.fps: Int
        get() = try {
            getInteger(MediaFormat.KEY_FRAME_RATE)
        } catch (e: Exception) {
            0
        }
}