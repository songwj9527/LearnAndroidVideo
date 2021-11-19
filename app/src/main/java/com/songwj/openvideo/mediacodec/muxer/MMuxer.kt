package com.songwj.openvideo.mediacodec.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.abs


/**
 * 音视频封装器
 */
class MMuxer(filePath: String) {

    private val TAG = "MMuxer"

    private var mPath = ""

    private var mMediaMuxer: MediaMuxer? = null

    private var mVideoTrackIndex = -1
    private var mAudioTrackIndex = -1

    private var mIsAudioTrackAdd = false
    private var mIsVideoTrackAdd = false

    private var mIsAudioEnd = false
    private var mIsVideoEnd = false

    private var mVideoTrackTimeUs = 0L
    private var mAudioTrackTimeUs = 0L

    private var mIsStart = false

    private var mStateListener: IMuxerStateListener?  = null

    init {
        mPath = if (TextUtils.isEmpty(filePath)) {
            Environment.getExternalStorageDirectory().absolutePath.toString() + "/" +
                    "LVideo_Test" + System.currentTimeMillis() +".mp4"
        } else {
            filePath
        }
        mMediaMuxer = MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun addVideoTrack(mediaFormat: MediaFormat) {
        synchronized(this) {
            if (mIsVideoTrackAdd) return
            mMediaMuxer?.let {
                try {
                    mVideoTrackIndex = it.addTrack(mediaFormat)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
                mIsVideoTrackAdd = true
                Log.i(TAG, "添加视频轨道")
                startMuxer()
            }
        }
    }

    fun addAudioTrack(mediaFormat: MediaFormat) {
        synchronized(this) {
            if (mIsAudioTrackAdd) return
            mMediaMuxer?.let {
                try {
                    mAudioTrackIndex = it.addTrack(mediaFormat)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
                mIsAudioTrackAdd = true
                Log.i(TAG, "添加音频轨道")
                startMuxer()
            }
        }
    }

    fun setNoAudio() {
        synchronized(this) {
            if (mIsAudioTrackAdd) return
            mIsAudioTrackAdd = true
            mIsAudioEnd = true
            startMuxer()
        }
    }

    fun setNoVideo() {
        synchronized(this) {
            if (mIsVideoTrackAdd) return
            mIsVideoTrackAdd = true
            mIsVideoEnd = true
            startMuxer()
        }
    }

    fun writeVideoData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(this) {
            if (mIsStart && !mIsVideoEnd) {
//                if (mIsPrepareStop) {
//                    mLastVideoDataList.add(byteBuffer)
//                    val info = MediaCodec.BufferInfo()
//                    info.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
//                    mLastVideoInfoList.add(info)
//                    return
//                }
                mMediaMuxer?.writeSampleData(mVideoTrackIndex, byteBuffer, bufferInfo)
                mVideoTrackTimeUs = bufferInfo.presentationTimeUs
                Log.i(TAG, "writeVideoData() ${(bufferInfo.presentationTimeUs / 1000000.0f)}")
            }
        }
    }

    fun writeAudioData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(this) {
            if (mIsStart && !mIsAudioEnd) {
//                if (mIsPrepareStop) {
//                    mLastAudioDataList.add(byteBuffer)
//                    val info = MediaCodec.BufferInfo()
//                    info.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
//                    mLastAudioInfoList.add(info)
//                    return
//                }
                mMediaMuxer?.writeSampleData(mAudioTrackIndex, byteBuffer, bufferInfo)
                mAudioTrackTimeUs = bufferInfo.presentationTimeUs
                Log.i(TAG, "writeAudioData() ${(bufferInfo.presentationTimeUs / 1000000.0f)}")
            }
        }
    }

    private fun startMuxer() {
        if (mIsAudioTrackAdd && mIsVideoTrackAdd) {
            if (!mIsStart) {
                mIsStart = true
                mMediaMuxer?.start()
                mStateListener?.onMuxerStart()
            }
            Log.i(TAG, "启动封装器")
        }
    }

    fun isStarted(): Boolean {
        return mIsStart
    }

    private var mIsPrepareStop = false
    private val mLastAudioDataList = ArrayList<ByteBuffer>()
    private val mLastAudioInfoList = ArrayList<MediaCodec.BufferInfo>()
    private val mLastVideoDataList = ArrayList<ByteBuffer>()
    private val mLastVideoInfoList = ArrayList<MediaCodec.BufferInfo>()
    fun stopMuxerPrepare() {
        synchronized(this) {
            mIsPrepareStop = true
        }
    }

    fun stopMuxer() {
        synchronized(this) {
            mIsVideoEnd = true
            mIsAudioEnd = true
            release()
        }
    }

    fun releaseVideoTrack() {
        synchronized(this) {
            if (!mIsVideoEnd) {
                mIsVideoEnd = true
                release()
            }
        }
    }

    fun releaseAudioTrack() {
        synchronized(this) {
            if (!mIsAudioEnd) {
                mIsAudioEnd = true
                release()
            }
        }
    }

    private fun release() {
        if (mIsAudioEnd && mIsVideoEnd) {
            mIsAudioTrackAdd = false
            mIsVideoTrackAdd = false
            if (mIsStart) {
                Log.d(TAG, "lastAudioTimestamp: $mAudioTrackTimeUs; lastVideoTimestamp: $mVideoTrackTimeUs")
                var lastVideoTimestamp = mVideoTrackTimeUs
                var lastAudioTimestamp = mAudioTrackTimeUs
//                if (mLastVideoDataList.size > 0) {
//                    lastVideoTimestamp = mLastVideoInfoList[mLastVideoInfoList.size - 1].presentationTimeUs
//                }
//                if (mLastAudioDataList.size > 0) {
//                    lastAudioTimestamp = mLastAudioInfoList[mLastAudioInfoList.size - 1].presentationTimeUs
//                }
//                while(abs(lastVideoTimestamp - lastAudioTimestamp) > 10000L) {
//                    if (lastVideoTimestamp > lastAudioTimestamp) {
//                        if (mLastVideoDataList.size > 0) {
//                            mLastVideoDataList.removeAt(mLastVideoDataList.size - 1)
//                            mLastVideoInfoList.removeAt(mLastVideoInfoList.size - 1)
//                        }
//                        if (mLastVideoDataList.size > 0) {
//                            lastVideoTimestamp = mLastVideoInfoList[mLastVideoInfoList.size - 1].presentationTimeUs
//                        } else {
//                            lastVideoTimestamp = mVideoTrackTimeUs
//                        }
//                    } else {
//                        if (mLastAudioDataList.size > 0) {
//                            mLastAudioDataList.removeAt(mLastAudioDataList.size - 1)
//                            mLastAudioInfoList.removeAt(mLastAudioInfoList.size - 1)
//                        }
//                        if (mLastAudioDataList.size > 0) {
//                            lastAudioTimestamp = mLastAudioInfoList[mLastAudioInfoList.size - 1].presentationTimeUs
//                        } else {
//                            lastAudioTimestamp = mAudioTrackTimeUs
//                        }
//                    }
//                    if (mLastVideoDataList.size == 0 && mLastAudioDataList.size == 0) {
//                        break
//                    }
//                }
//                if (mLastAudioDataList.size > 0) {
//                    for (index in mLastAudioDataList.indices) {
//                        mMediaMuxer?.writeSampleData(mAudioTrackIndex, mLastAudioDataList[index], mLastAudioInfoList[index])
//                    }
//                }
//                if (mLastVideoDataList.size > 0) {
//                    for (index in mLastVideoDataList.indices) {
//                        mMediaMuxer?.writeSampleData(mVideoTrackIndex, mLastVideoDataList[index], mLastVideoInfoList[index])
//                    }
//                }
//                Log.d(TAG, "lastAudioTimestamp: $lastAudioTimestamp; lastVideoTimestamp: $lastVideoTimestamp")

                mIsStart = false
                try {
                    mMediaMuxer?.stop()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                mStateListener?.onMuxerFinish()
            }
            try {
                mMediaMuxer?.release()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
//                val file = File(mPath)
//                if (file.exists()) {
//                    file.delete()
//                }
            }
            mMediaMuxer = null
            Log.i(TAG, "退出封装器")
        }

        mIsPrepareStop = false
        mLastAudioDataList.clear()
        mLastAudioInfoList.clear()
        mLastVideoDataList.clear()
        mLastVideoInfoList.clear()
    }

    fun setStateListener(l: IMuxerStateListener?) {
        this.mStateListener = l
    }

    interface IMuxerStateListener {
        fun onMuxerStart() {}
        fun onMuxerFinish() {}
    }
}