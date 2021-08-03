package com.songwj.openvideo.mediacodec.decoder

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import com.songwj.openvideo.mediacodec.*
import com.songwj.openvideo.mediacodec.extractor.IExtractor
import java.io.File
import java.nio.ByteBuffer

/**
 * 解码器基类
 */
abstract class BaseDecoder(private val mFilePath: String) : IDecoder {

    private val TAG = "BaseDecoder"

    //-------------线程相关------------------------
    /**
     * 解码器是否在运行
     */
    private var mIsRunning = true

    /**
     * 线程等待锁
     */
    private val mLock = Object()

    /**
     * MediaCodec资源锁
     */
    private val mMediaCodecLock = Object()

    /**
     * 是否可以进入解码
     */
    private var mReadyForDecode = false

    //---------------状态相关-----------------------
    /**
     * 音视频解码器
     */
    protected var mCodec: MediaCodec? = null

    /**
     * 音视频数据读取器
     */
    protected var mExtractor: IExtractor? = null

    /**
     * 解码输入缓存区
     */
    private var mInputBuffers: Array<ByteBuffer>? = null

    /**
     * 解码输出缓存区
     */
    private var mOutputBuffers: Array<ByteBuffer>? = null

    /**
     * 解码数据信息
     */
    private var mBufferInfo = MediaCodec.BufferInfo()

    private var mState = DecodeState.IDLE

    protected var mStateListener: IDecoderStateListener? = null

    /**
     * 流数据是否结束
     */
    private var mIsEOS = false

    private var mDuration: Long = 0

    private var mStartPos: Long = 0

    private var mEndPos: Long = 0

    /**
     * 开始解码时间，用于音视频同步
     */
    private var mStartTimeForSync = -1L

    /**
     * 指定位置时的时间差值
     */
    private var mSeekTimeForSync = 0L

    // 是否需要音视频渲染同步
    private var mSyncRender = true

    override fun run() {
        // 初始化，并启动解码器
        if (!init()) {
            return
        }
        mState = DecodeState.PREPARED
        mStateListener?.decoderPrepared(this)

        try {
            while (!Thread.interrupted() && mIsRunning) {
                synchronized(mLock) {
                    if (isSeeking() || (!isStarted() && !isSeekingFlush())) {
                        mLock.wait()

                        // ---------【同步时间矫正】-------------
                        //恢复同步的起始时间，即去除等待流失的时间
                        Log.e(this.javaClass.simpleName, "mStartTimeForSync: $mStartTimeForSync" +
                                ", currentTimeMillis(): ${System.currentTimeMillis()}" +
                                ", currentTimestamp: ${getCurrentTimestamp()}" +
                                ", mSeekTimeForSync: $mSeekTimeForSync")
                        mStartTimeForSync = System.currentTimeMillis() - if (mSeekTimeForSync != 0L) {
                            mSeekTimeForSync
                        } else {
                            getCurrentTimestamp()
                        }
                        mSeekTimeForSync = 0L
                    }
                }

                if (!mIsRunning ||
                    isStoped()) {
                    mIsRunning = false
                    break
                }

                if (mStartTimeForSync == -1L) {
                    mStartTimeForSync = System.currentTimeMillis()
                }

                Log.e(this.javaClass.simpleName, "state: $mState")
                val isSeekingFlush = isSeekingFlush()
                if (isSeeking() || (!isStarted() && !isSeekingFlush)) {
                    continue
                }

                /**
                 * 如果seekTo指定位置播放时，需要刷新帧缓存，即isSeekingFlush 为true。此时无需渲染
                 * （这里主要时获取最近的帧的时间，因为seekTo()后getCurrentTimestamp()方法获取的时间还是seekTo之前的时间）
                 * 如果没有还走正常流程.
                 */
                synchronized(mMediaCodecLock) {
                    //如果数据没有解码完毕，将数据推入解码器解码
                    if (!mIsEOS) {
                        //【解码步骤：2. 见数据压入解码器输入缓冲】
                        mIsEOS = pushBufferToDecoder()
                    }

                    //【解码步骤：3. 将解码好的数据从缓冲区拉取出来】
                    val index = pullBufferFromDecoder()
                    Log.e(this.javaClass.simpleName, "pullBufferFromDecoder: $index")
                    if (index >= 0) {
                        /**
                         * 如果seekTo指定位置播放时，需要刷新帧缓存，即isSeekingFlush 为true。此时无需渲染
                         */
                        if (!isSeekingFlush) {
                            // ---------【音视频同步】-------------
                            if (mSyncRender && mState == DecodeState.STARTED) {
                                sleepRender()
                            }

                            //【解码步骤：4. 渲染】
                            if (mSyncRender) {// 如果只是用于编码合成新视频，无需渲染
                                var outputBuffer: ByteBuffer
                                outputBuffer = if (Build.VERSION.SDK_INT >= 21) {
                                    mCodec?.getOutputBuffer(index)!!
                                } else {
                                    mOutputBuffers!![index]
                                }

                                render(outputBuffer, mBufferInfo)
                            }

                            //将解码数据传递出去
                            val frame = Frame()
                            frame.buffer = mOutputBuffers!![index]
                            frame.setBufferInfo(mBufferInfo)
                            mStateListener?.decodeOneFrame(this, frame)
                        }

                        if (mIsRunning && !isStoped()) {
                            //【解码步骤：5. 释放输出缓冲】
                            mCodec!!.releaseOutputBuffer(index, true)
                        }

                        if (mState == DecodeState.PREPARED) {
                            mState = DecodeState.PAUSED
                        }

                        /**
                         * 如果seekTo指定位置播放时，需要刷新帧缓存，即isSeekingFlush 为true。此时无需渲染
                         * （这里主要时获取最近的帧的时间，因为seekTo()后getCurrentTimestamp()方法获取的时间还是seekTo之前的时间）
                         */
                        if (isSeekingFlush) {
                            var nowSampleTime = 0L
                            nowSampleTime = mExtractor?.getMediaExtractor()?.sampleTime!!
//                    mSeekTimeForSync = (nowSampleTime - preSampleTime) / 1000
                            mSeekTimeForSync = nowSampleTime / 1000
                            mStartTimeForSync = System.currentTimeMillis() - mSeekTimeForSync
                            Log.e(this.javaClass.simpleName, "nowSampleTime: $nowSampleTime" +
                                    ", presentTimestamp: ${getCurrentTimestamp()}" +
                                    ", mSeekTimeForSync: $mSeekTimeForSync" +
                                    ", mStartTimeForSync: $mStartTimeForSync")
                        }
                    }

                    //【解码步骤：6. 判断解码是否完成】
                    if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        mCodec?.flush()
                        if (index >= 0 && isSeekingFlush) {
                            if (mState == DecodeState.SEEKING_FLUSH)  {
                                mState = DecodeState.SEEKING
                            }
                            if (mIsRunning && !isStoped()) {
                                mStateListener?.decoderSeekCompleted(this)
                            }
                        }
                        Log.i(TAG, "解码结束")
                        mState = DecodeState.COMPLETED
                        mStateListener?.decoderCompleted(this)
                    } else if (index >= 0 && isSeekingFlush) {
                        mCodec?.flush()
                        if (mState == DecodeState.SEEKING_FLUSH)  {
                            mState = DecodeState.SEEKING
                        }
                        if (mIsRunning && !isStoped()) {
                            mStateListener?.decoderSeekCompleted(this)
                        } else {

                        }
                    } else {

                    }
                }

//                mStateListener?.decoderProgress(this, (100 * getCurrentTimestamp() / getDuration()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            release()
        }
    }

    /**
     * 初始化
     */
    private fun init(): Boolean {
        if (mFilePath.isEmpty() || !File(mFilePath).exists()) {
            mStateListener?.decoderError(this, "文件路径为空")
            return false
        }

        if (!check()) {
            mStateListener?.decoderError(this, "检查子类参数异常")
            return false
        }

        //初始化数据提取器
        mExtractor = initExtractor(mFilePath)
        if (mExtractor == null || mExtractor!!.getFormat() == null) {
            mExtractor?.stop()
            mExtractor = null
            mStateListener?.decoderError(this, "无法解析文件")
            return false
        }

        // 初始化参数
        if (!initParams()) {
            mExtractor?.stop()
            mExtractor = null
            mStateListener?.decoderError(this, "初始化参数失败")
            return false
        }

        // 初始化渲染器
        if (!initRender()) {
            mExtractor?.stop()
            mExtractor = null
            mStateListener?.decoderError(this, "初始化渲染器失败")
            return false
        }

        // 初始化解码器
        if (!initCodec()) {
            mExtractor?.stop()
            mExtractor = null
            if (mStateListener != null) {
                mStateListener?.decoderError(this, "初始化解码器失败")
            }
            return false
        }
        return true
    }

    private fun initParams(): Boolean {
        try {
            val format = mExtractor!!.getFormat()!!
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000
            if (mEndPos == 0L) mEndPos = mDuration

            initSpecParams(mExtractor!!.getFormat()!!)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    val MediaFormat.fps: Int
        get() = try {
            getInteger(MediaFormat.KEY_FRAME_RATE)
        } catch (e: Exception) {
            0
        }


    private fun initCodec(): Boolean {
        try {
            if (mStartPos > 0 && mStartPos < mEndPos) {
                mExtractor?.seekTo(mStartPos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }
            val type = mExtractor!!.getFormat()!!.getString(MediaFormat.KEY_MIME)
            mCodec = MediaCodec.createDecoderByType(type)
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                mCodec?.stop()
                mCodec?.release()
                mExtractor?.stop()
                mCodec = null
                mExtractor = null
                return false
            }
            mCodec!!.start()

            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
        } catch (e: Exception) {
            return false
        }
        return true
    }

    /**
     * 检查子类参数
     */
    abstract fun check(): Boolean

    /**
     * 初始化数据提取器
     */
    abstract fun initExtractor(path: String): IExtractor

    /**
     * 初始化子类自己特有的参数
     */
    abstract fun initSpecParams(format: MediaFormat)

    /**
     * 配置解码器
     */
    abstract fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean

    /**
     * 初始化渲染器
     */
    abstract fun initRender(): Boolean

    private fun pushBufferToDecoder(): Boolean {
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(1000)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            var inputBuffer: ByteBuffer? = null
            if (Build.VERSION.SDK_INT >= 21) {
                inputBuffer = mCodec!!.getInputBuffer(inputBufferIndex);
            } else {
                inputBuffer = mInputBuffers!![inputBufferIndex]
            }
            val sampleSize = mExtractor!!.readBuffer(inputBuffer!!)

            if (sampleSize < 0) {
                //如果数据已经取完，压入数据结束标志：MediaCodec.BUFFER_FLAG_END_OF_STREAM
                mCodec!!.queueInputBuffer(inputBufferIndex, 0, 0,
                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isEndOfStream = true
            } else {
                mCodec!!.queueInputBuffer(inputBufferIndex, 0,
                    sampleSize, mExtractor!!.getMediaExtractor()!!.sampleTime/*mExtractor!!.getCurrentTimestamp()*/, 0)
            }
        }
        return isEndOfStream
    }

    private fun pullBufferFromDecoder(): Int {
        // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
        var index = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000)
        when (index) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { }
            MediaCodec.INFO_TRY_AGAIN_LATER -> {}
            // 当Buffer变化时，重新指向新的我Buffer
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                mOutputBuffers = mCodec!!.outputBuffers
            }
            else -> {
                return index
            }
        }
        return -1
    }

    private fun sleepRender() {
        val passTime = System.currentTimeMillis() - mStartTimeForSync
        val curTime = getCurrentTimestamp()
        Log.e(this.javaClass.simpleName, "passTime: $passTime" +
                ", curTime: $curTime")
        if (curTime > passTime) {
            Thread.sleep(curTime - passTime)
        }
    }

    @SuppressLint("WrongConstant")
    override fun seekTo(positon: Int) {
        var preSampleTime = 0L
        preSampleTime = mExtractor?.getMediaExtractor()?.sampleTime!!
        var duration = 1L
        var validTimestamp = 0L
        if (isCompleted()) {
            reset()
        }
        synchronized(mLock) {
            mState = DecodeState.SEEKING
            mLock.notifyAll()
        }

        synchronized(mMediaCodecLock) {
            /**
             * 如果seekTo指定位置播放时，需要刷新帧缓存，即isSeekingFlush 为true。此时无需渲染
             * （这里主要时获取最近的帧的时间，因为seekTo()后getCurrentTimestamp()方法获取的时间还是seekTo之前的时间）
             * 如果没有还走正常流程.
             */
            mExtractor?.let {
                duration = it.getFormat()!!.getLong(MediaFormat.KEY_DURATION)
                /**
                 * seekTo指定位置播放不准（主要因为指定的位置可能不是I帧，所以最终效果可能是指定位置后，开始播放的位置在指定位置之前）
                 * 调用getValidSampleTime()来获取可以达到指定位置效果的位置值validTimestamp，
                 * 再执行seekTo(validTimestamp, SEEK_TO_CLOSEST_SYNC)，来实现指定位置播放。
                 */
                validTimestamp = it.getValidSampleTime(positon * duration / 100, 10)
//                validTimestamp = positon * duration / 100
                it.seekTo(validTimestamp, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
            mCodec?.flush()
        }

        synchronized(mLock) {
            mIsEOS = false
            mState = DecodeState.SEEKING_FLUSH
            mLock.notifyAll()
        }

        if (!mIsRunning && isStoped()) {
            return
        }
    }

    /**
     * 渲染
     */
    abstract fun render(outputBuffer: ByteBuffer,
                        bufferInfo: MediaCodec.BufferInfo)

    override fun start() {
        if (!isStarted() && !isStoped()) {
            mState = DecodeState.STARTED
        }
        notifyDecode()
    }

    override fun pause() {
        if (!isIDLE() && !isStoped() && !isCompleted() && !isPaused() && !isSeekingFlush()) {
            mState = DecodeState.PAUSED
            mStateListener?.decoderPause(this)
        }
    }

    override fun resume() {
        if (isPaused()) {
            mState = DecodeState.STARTED
        }
        notifyDecode()
    }

    override fun stop() {
        synchronized(mLock) {
            mState = DecodeState.STOPED
            mIsRunning = false
        }
        notifyDecode()
    }

    override fun reset() {
        synchronized(mLock) {
            mState = DecodeState.PREPARED
            mStartTimeForSync = -1L
        }

        synchronized(mMediaCodecLock) {
            mBufferInfo = MediaCodec.BufferInfo()
            mCodec?.reset()
////            mExtractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                mStateListener?.decoderError(this, "重置解码器失败")
                return
            }
            mCodec?.start()

            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
            mIsEOS = false
        }
    }

    override fun release() {
        Log.e(TAG, "解码停止，释放解码器")
        synchronized(mLock) {
            doneDecode()
            mState = DecodeState.IDLE
            mIsEOS = false
        }
        synchronized(mMediaCodecLock) {
            try {
                mCodec?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                mCodec?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mCodec = null
            mExtractor?.stop()
            mExtractor = null
        }
        mStateListener?.decoderDestroy(this)
    }

    /**
     * 子类结束解码时可能需要特殊处理
     */
    abstract fun doneDecode()


    override fun isIDLE(): Boolean {
        return mState == DecodeState.IDLE
    }

    override fun isPrepared(): Boolean {
        return mState == DecodeState.PREPARED
    }

    override fun isStarted(): Boolean {
        return mState == DecodeState.STARTED
    }

    override fun isSeeking(): Boolean {
        return mState == DecodeState.SEEKING
    }

    override fun isSeekingFlush(): Boolean {
        return mState == DecodeState.SEEKING_FLUSH
    }

    override fun isPaused(): Boolean {
        return mState == DecodeState.PAUSED
    }

    override fun isStoped(): Boolean {
        return mState == DecodeState.STOPED
    }

    override fun isCompleted(): Boolean {
        return mState == DecodeState.COMPLETED
    }

    override fun setStateListener(listener: IDecoderStateListener?) {
        this.mStateListener = listener
    }

    override fun getFilePath(): String {
        return mFilePath
    }

    override fun getMediaFormat(): MediaFormat? {
        return mExtractor?.getFormat()
    }

    override fun getTrack(): Int {
        mExtractor?.let {
            return it.getTrack()
        }
        return -1
    }

    override fun getDuration(): Long {
        return mDuration
    }

    override fun getCurrentTimestamp(): Long {
        return mBufferInfo.presentationTimeUs / 1000
    }

    override fun withoutSync(): IDecoder {
        mSyncRender = false
        return this
    }

    /**
     * 解码线程进入等待
     */
    private fun waitDecode() {
        try {
            if (mState == DecodeState.PAUSED) {
                mStateListener?.decoderPause(this)
            }
            synchronized(mLock) {
                mLock.wait()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通知解码线程继续运行
     */
    protected fun notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll()
        }
        if (mState == DecodeState.STARTED) {
            mStateListener?.decoderRunning(this)
        }
    }
}