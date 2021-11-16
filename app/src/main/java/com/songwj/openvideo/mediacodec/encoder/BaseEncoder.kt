package com.songwj.openvideo.mediacodec.encoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.songwj.openvideo.mediacodec.muxer.MMuxer
import java.nio.ByteBuffer


/**
 * 基础编码器
 */
abstract class BaseEncoder : Thread {
    protected var TAG = "BaseEncoder"

    // 目标视频宽，只有视频编码的时候才有效
    protected var mWidth = 0
    // 目标视频高，只有视频编码的时候才有效
    protected var mHeight = 0

    // 视频是否手动编码，false，颜色空间 从编码视图的surface窗口获得；true，颜色空间 从外部获得，需要外部传入数据放到编码队列中去编码
    protected var isVideoEncodeManually = true

    // 状态锁
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
        this.isVideoEncodeManually = isEncodeManually
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
        frame?.let {
//            synchronized(mFramesLock) {
//                if (!mStop) {
//                    mFrames.add(it)
//                    dequeueInputBuffer(frame.buffer, frame.presentationTimeUs)
//                }
//            }
//            SystemClock.sleep(frameWaitTimeMs())
            synchronized(mFramesLock) {
                if (!mStop) {
                    dequeueInputBuffer(frame.buffer, frame.presentationTimeUs)
                }
            }
        }
    }

    /**
     * 循环编码
     */
    private fun loopEncode() {
        Log.i(TAG, "开始编码")
        mStateListener?.encodeStart(this)
        firstAddTrack()
        mCodec.flush()
        while (!mIsEOS) {
            drain()
            if (mStop) {
//                drain()
                break
            }
        }
    }

    /**
     * 因为视频编码 var index = mCodec.dequeueOutputBuffer(mBufferInfo, 100000)，
     * 得到的index一直等于MediaCodec.INFO_OUTPUT_FORMAT_CHANGED，
     * 也就无法调用muxer.addTrack()方法来添加轨道
     * 所以由次方法来
     */
    abstract protected fun firstAddTrack()

    /**
     * 榨干编码输出数据
     */
    private fun drain() {
        var index = mCodec.dequeueOutputBuffer(mBufferInfo, 100000)
        Log.i(TAG, "dequeueOutputBuffer(): $index")
        if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == index) {
            Log.i(TAG, "drain(): 添加轨道")
            mMuxer?.let {
                addTrack(it, mCodec.outputFormat)
            }
            mAddedMuxerTrack = true
        } else if (MediaCodec.INFO_TRY_AGAIN_LATER == index) {

        } else if (MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED == index) {

        } else {
            while (index >= 0) {
//                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
//                    Log.e(TAG, "drain(): 编码结束")
//                    mIsEOS = true
//                    mBufferInfo.set(0, 0, 0, mBufferInfo.flags)
//                    break
//                }
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
                    index = mCodec.dequeueOutputBuffer(mBufferInfo, 100000)
                    continue
                }

//                if (!mIsEOS) {
//                    var outputBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        mCodec.getOutputBuffer(index)!!
//                    } else {
//                        mCodec.outputBuffers[index]
//                    }
//                    mMuxer?.let {
//                        Log.e(TAG, "writeData(): ${mBufferInfo.presentationTimeUs}")
//                        writeData(it, outputBuffer, mBufferInfo)
//                    }
//                }
                if (!mMuxer!!.isStarted()) {
                    mCodec.releaseOutputBuffer(index, false)
                    SystemClock.sleep(10)
                    if (mStop) {
                        break
                    }
                    index = mCodec.dequeueOutputBuffer(mBufferInfo, 100000)
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
                index = mCodec.dequeueOutputBuffer(mBufferInfo, 100000)
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
                    mLock.wait(1000)
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
        try {
            synchronized(mLock) {
                mLock.notify()
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通知结束编码
     */
    fun stopEncode() {
        Log.e(TAG, "stopEncode()(: 结束编码")
        synchronized(mFramesLock) {
            if (!mStop) {
                mStop = true
//                if (!encodeManually()) {
//                    mCodec.signalEndOfInputStream()
//                }
            }
        }
        notifyGo()
    }

    /**
     * 将数据放置到编码输入缓存区编码
     */
    private fun dequeueInputBuffer(buffer: ByteArray?, presentationTimeUs: Long) {
        val index = mCodec.dequeueInputBuffer(50000)
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