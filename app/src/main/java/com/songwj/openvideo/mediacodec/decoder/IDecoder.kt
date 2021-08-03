package com.songwj.openvideo.mediacodec.decoder

import android.media.MediaFormat

/**
 * 解码器定义
 */
interface IDecoder : Runnable {

    /**
     * 开始播放
     */
    fun start()

    /**
     * 暂停解码
     */
    fun pause()

    /**
     * 继续解码
     */
    fun resume()

    /**
     * 停止解码
     */
    fun stop()

    /**
     * 重置解码
     */
    fun reset()

    /**
     * 释放资源
     */
    fun release()

    /**
     * 指定位置播放
     */
    fun seekTo(positon: Int)

    /**
     * 是否未初始化
     */
    fun isIDLE(): Boolean

    /**
     * 是否已经初始化完成
     */
    fun isPrepared(): Boolean

    /**
     * 是否正在解码
     */
    fun isStarted(): Boolean

    /**
     * 是否正在快进
     */
    fun isSeeking(): Boolean

    /**
     * 是否正在快进刷新缓存
     */
    fun isSeekingFlush(): Boolean

    /**
     * 是否暂停
     */
    fun isPaused(): Boolean

    /**
     * 是否停止解码
     */
    fun isStoped(): Boolean

    /**
     * 是否解码全部完成
     */
    fun isCompleted(): Boolean

    /**
     * 设置状态监听器
     */
    fun setStateListener(listener: IDecoderStateListener?)

    /**
     * 获取解码的文件路径
     */
    fun getFilePath(): String

    /**
     * 获取音视频对应的格式参数
     */
    fun getMediaFormat(): MediaFormat?

    /**
     * 获取音视频对应的媒体轨道
     */
    fun getTrack(): Int

    /**
     * 获取时长
     */
    fun getDuration(): Long

    /**
     * 当前帧时间，单位：ms
     */
    fun getCurrentTimestamp(): Long

    /**
     * 无需音视频同步
     */
    fun withoutSync(): IDecoder
}