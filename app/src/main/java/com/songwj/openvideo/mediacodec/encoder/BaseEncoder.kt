package com.songwj.openvideo.mediacodec.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.songwj.openvideo.mediacodec.muxer.MMuxer
import java.nio.ByteBuffer
import java.util.*


/**
 * 基础编码器
 */
abstract class BaseEncoder : Thread {
    protected var TAG = "BaseEncoder"

    // 目标视频宽，只有视频编码的时候才有效
    protected var mWidth = 0
    // 目标视频高，只有视频编码的时候才有效
    protected var mHeight = 0

    // 是否手动编码
    protected var isEncodeManually = true

    // 待编码帧缓存
    private var mFrames = LinkedList<Frame>()
    // 待编码帧缓存最大帧个数（因为编码较慢，mFrames会不断堆积，容易造成内存溢出；这里如果待编码帧缓存个数超过最大个数，待编码帧缓存就出列一帧，并将该帧压入到codec中编码）
    protected var mFramesMaxSize = 150
    // 待编码帧缓存的操作锁
    private var mFramesLock = Object()

    // Mp4合成器
    protected var mMuxer: MMuxer? = null
    // 是否已经添加了MuxerTrack
    protected var mAddedMuxerTrack = false

    // 编码器
    protected lateinit var mCodec: MediaCodec

    // 当前编码帧信息
    private val mBufferInfo = MediaCodec.BufferInfo()
    // 状态锁
    private var mLock = Object()

    // 是否编码结束
    private var mIsEOS = false

    // 是否主动停止编码
    private var mStop = false

    // 编码状态监听器
    protected var mStateListener: IEncodeStateListener? = null

    constructor(muxer: MMuxer) {
        this.mMuxer = muxer
        initCodec()
    }

    constructor(muxer: MMuxer, width: Int, height: Int, isEncodeManually: Boolean) {
        this.mMuxer = muxer
        this.mWidth = width
        this.mHeight = height
        this.isEncodeManually = isEncodeManually
        initCodec()
    }

    /**
     * 初始化编码器
     */
    private fun initCodec() {
        mCodec = createCodec()
        configEncoder(mCodec)
        mCodec.start()
        Log.i(TAG, "编码器初始化完成")
    }

    abstract fun createCodec() : MediaCodec

    override fun run() {
        super.run()
        loopEncode()
        doneEncode()
    }

    fun dequeueFrame(frame: Frame?) {
        var first: Frame? = null
        frame?.let {
            synchronized(mFramesLock) {
                if (!mStop) {
                    mFrames.add(it)
                    // （因为编码较慢，mFrames会不断堆积，容易造成内存溢出；这里如果待编码帧缓存个数超过最大个数，待编码帧缓存就出列一帧，并将该帧压入到codec中编码）
                    if (mFramesMaxSize != 0 && mFrames.size > mFramesMaxSize) {
                        first =  mFrames.removeFirst()
                    }
                }
            }
        }
        first?.let {
            dequeueInputBuffer(it.buffer, it.presentationTimeUs)
        }
    }

    /**
     * 循环编码
     */
    private fun loopEncode() {
        Log.i(TAG, "开始编码")
        mStateListener?.encodeStart(this)
        mCodec.flush()
        while (!mIsEOS) {
            drain()
            if (!mMuxer!!.isStarted()) {
                notifyWait(500)
            } else if (encodeManually()) {
                var frame = synchronized(mFramesLock) {
                    if (mFrames.isEmpty()) {
                        null
                    } else {
                        mFrames.removeFirst()
                    }
                }
                frame?.let {
                    dequeueInputBuffer(it.buffer, it.presentationTimeUs)
                }
                if (frame == null) {
                    notifyWait(500)
                }
            }
        }
    }

    /**
     * 将数据放置到编码输入缓存区编码
     */
    private fun dequeueInputBuffer(buffer: ByteArray?, presentationTimeUs: Long) {
        val index = mCodec.dequeueInputBuffer(5000)
        /*向编码器输入数据*/
        if (index >= 0) {
            val inputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mCodec.getInputBuffer(index);
            } else {
                mCodec.inputBuffers[index]
            }
            inputBuffer?.clear()
            if (buffer != null && buffer.isNotEmpty()) {
                inputBuffer?.put(buffer)
                mCodec.queueInputBuffer(index, 0, buffer.size,
                    presentationTimeUs, 0)
            } else { // 音频结束符标记
                mCodec.queueInputBuffer(index, 0, 0,
                    presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }
    }

    /**
     * 榨干编码输出数据
     */
    var TRY_AGAIN_LATER_MAX_COUNT = 100
    var TRY_AGAIN_LATER_COUNT = 0
    private fun drain() {
        var index = mCodec.dequeueOutputBuffer(mBufferInfo, 5000)
        Log.i(TAG, "dequeueOutputBuffer(): $index")
        if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == index) {
            Log.i(TAG, "drain(): 添加轨道")
            mMuxer?.let {
                addTrack(it, mCodec.outputFormat)
            }
            mAddedMuxerTrack = true
            TRY_AGAIN_LATER_COUNT = 0
        } else if (MediaCodec.INFO_TRY_AGAIN_LATER == index) {
            if (TRY_AGAIN_LATER_COUNT > TRY_AGAIN_LATER_MAX_COUNT) {
                mIsEOS = true
                mBufferInfo.set(0, 0, 0, mBufferInfo.flags)
                return
            }
            TRY_AGAIN_LATER_COUNT++
        } else if (MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED == index) {
            TRY_AGAIN_LATER_COUNT = 0
        } else {
            TRY_AGAIN_LATER_COUNT = 0
            while (index >= 0) {
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.e(TAG, "drain(): 编码结束")
                    mIsEOS = true
                    mBufferInfo.set(0, 0, 0, mBufferInfo.flags)
                    break
                }
//                if (((mBufferInfo.flags) and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    // SPS or PPS, which should be passed by MediaFormat.
                    var outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mCodec.getOutputBuffer(index)!!
                    } else {
                        mCodec.outputBuffers[index]
                    }
                    outputBuffer.get()
                    mCodec.releaseOutputBuffer(index, false)
                    index = mCodec.dequeueOutputBuffer(mBufferInfo, 5000)
                    continue
                }

                if (!mIsEOS) {
                    if (!mMuxer!!.isStarted()) {
                        mCodec.releaseOutputBuffer(index, false)
                        SystemClock.sleep(10)
                        if (mStop) {
                            break
                        }
                        index = mCodec.dequeueOutputBuffer(mBufferInfo, 5000)
                        continue
                    }
                    var outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mCodec.getOutputBuffer(index)!!
                    } else {
                        mCodec.outputBuffers[index]
                    }
                    mMuxer?.let {
                        writeData(it, outputBuffer, mBufferInfo)
                    }
                    mCodec.releaseOutputBuffer(index, false)
                    if (mStop) {
                        break
                    }
                    index = mCodec.dequeueOutputBuffer(mBufferInfo, 5000)
                }
            }
        }
    }

    /**
     * 编码结束，释放资源
     */
    private fun doneEncode() {
        Log.i(TAG, "doneEncode(): $mIsEOS, $mStop")
        mStop = true
        try {
            release(mMuxer!!)
            mMuxer = null
            mCodec.stop()
            mCodec.release()
            mStateListener?.encoderFinish(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 编码进入等待
     */
    private fun notifyWait(timeout: Long) {
        try {
            synchronized(mLock) {
                if (timeout > 0) {
                    mLock.wait(timeout)
                } else {
                    mLock.wait()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通知继续编码
     */
    private fun notifyGo() {
        try {
            synchronized(mLock) {
                mLock.notify()
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 开始编码
     */
    fun startEncode() {
        Log.e(TAG, "startEncode()(: 开始编码")
//        try {
//            synchronized(mLock) {
//                mLock.notify()
//            }
//        } catch (e : Exception) {
//            e.printStackTrace()
//        }
    }

    /**
     * 通知结束编码
     */
    fun stopEncode() {
        Log.e(TAG, "stopEncode(): 结束编码")
        synchronized(mFramesLock) {
            if (!mStop) {
                mStop = true
                if (!encodeManually()) {
                    mCodec.signalEndOfInputStream()
                } else {
                    val frame = Frame()
                    frame.buffer = null
                    frame.presentationTimeUs = 0L
                    mFrames.add(frame)
                }
            }
        }
//        notifyGo()
    }

    /**
     * 设置状态监听器
     */
    fun setStateListener(l: IEncodeStateListener) {
        this.mStateListener = l
    }

    /**
     * 编码类型
     */
    abstract fun encodeType(): String

    /**
     * 子类配置编码器
     */
    abstract fun configEncoder(codec: MediaCodec)

    /**
     * 配置mp4音视频轨道
     */
    abstract fun addTrack(muxer: MMuxer, mediaFormat: MediaFormat)

    /**
     * 往mp4写入音视频数据
     */
    abstract fun writeData(muxer: MMuxer, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    /**
     * 释放子类资源
     */
    abstract fun release(muxer: MMuxer)

    /**
     * 每一帧排队等待时间
     */
    open fun frameWaitTimeMs() = 20L

    /**
     * 是否手动编码
     * 音频：true,  音频数据需要用户自己压入编码缓冲区，完成编码
     * 视频：false, 即设置outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)，视频编码通过Surface，MediaCodec自动完成编码
     *      true,  即设置outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)，视频编码需要用户自己压入编码缓冲区，完成编码
     */
    open fun encodeManually() = true
}