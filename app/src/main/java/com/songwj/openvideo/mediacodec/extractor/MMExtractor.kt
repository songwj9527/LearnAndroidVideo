package com.songwj.openvideo.mediacodec.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer


/**
 * 音视频分离器
 */

class MMExtractor(path: String?) {

    /**音视频分离器*/
    private var mExtractor: MediaExtractor? = null

    /**音频通道索引*/
    private var mAudioTrack = -1

    /**视频通道索引*/
    private var mVideoTrack = -1

    /**当前帧时间戳*/
    private var mCurSampleTime: Long = 0

    /**当前帧标志*/
    private var mCurSampleFlag: Int = 0

    /**开始解码时间点*/
    private var mStartPosition: Long = 0

    init {
        mExtractor = MediaExtractor()
        try {
            Log.e("MMExtractor", path)
            mExtractor?.setDataSource(path!!) // 设置数据源
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 音视频分离器
     */
    fun getMediaExtractor(): MediaExtractor? {
        return mExtractor
    }

    /**
     * 获取视频格式参数
     */
    fun getVideoFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                mVideoTrack = i
                break
            }
        }
        return if (mVideoTrack >= 0) {
            return mExtractor!!.getTrackFormat(mVideoTrack)
        } else {
            return null
        }
    }

    /**
     * 获取音频格式参数
     */
    fun getAudioFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio/")) {
                mAudioTrack = i
                break
            }
        }
        return if (mAudioTrack >= 0) {
            return mExtractor!!.getTrackFormat(mAudioTrack)
        } else {
            return null
        }
    }

    /**
     * 读取视频数据
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int {
        byteBuffer.clear()
        selectSourceTrack()
        var readSampleCount = mExtractor!!.readSampleData(byteBuffer, 0)
        if (readSampleCount < 0) {
            return -1
        }
        // 记录当前帧的时间戳
        mCurSampleTime = mExtractor!!.sampleTime
        // 记录当前帧的标志
        mCurSampleFlag = mExtractor!!.sampleFlags
        //进入下一帧
        mExtractor!!.advance()
        return readSampleCount
    }

    /**
     * 选择通道
     */
    private fun selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mExtractor!!.selectTrack(mVideoTrack)
        } else if (mAudioTrack >= 0) {
            mExtractor!!.selectTrack(mAudioTrack)
        }
    }

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     */
    fun seekTo(position: Long, mode: Int): Long {
        mExtractor!!.seekTo(position, mode)
        return mExtractor!!.sampleTime
    }

    /**
     * 停止读取数据
     */
    fun stop() {
        mExtractor?.release()
        mExtractor = null
    }

    /**
     * 获取视频通道
     */
    fun getVideoTrack(): Int {
        return mVideoTrack
    }

    /**
     * 获取音频通道
     */
    fun getAudioTrack(): Int {
        return mAudioTrack
    }

    /**
     * 设置开始位置
     */
    fun setStartPosition(position: Long) {
        mStartPosition = position
    }

    /**
     * 获取当前帧时间
     */
    fun getCurrentTimestamp(): Long {
        return mCurSampleTime
    }

    /**
     * 获取当前帧标志
     */
    fun getSampleFlag(): Int {
        return mCurSampleFlag
    }
}