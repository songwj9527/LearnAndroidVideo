package com.songwj.openvideo.mediacodec.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer


/**
 * 音视频分离器定义
 */
interface IExtractor {

    /**
     * 音视频分离器
     */
    fun getMediaExtractor(): MediaExtractor?

    /**
     * 获取音视频信道
     */
    fun getTrack(): Int

    /**
     * 获取音视频对应的格式参数
     */
    fun getFormat(): MediaFormat?

    /**
     * 读取音视频数据
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int

    /**
     * 获取当前帧时间
     */
    fun getCurrentTimestamp(): Long

    /**
     * 获取当前帧标志
     */
    fun getSampleFlag(): Int

    /**
     * 设置开始位置
     */
    fun setStartPosition(position: Long)

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     */
    fun seekTo(position: Long, mode: Int): Long

    /**
     * 查找这个时间点对应的最接近的一帧。
     * 这一帧的时间点如果和目标时间相差不到 一帧间隔 就算相近
     * maxRange:查找范围
     */
    fun getValidSampleTime(time: Long, maxRange: Int = 5): Long

    /**
     * 停止读取数据
     */
    fun stop()
}