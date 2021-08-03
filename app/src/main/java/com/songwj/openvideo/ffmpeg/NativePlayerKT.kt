package com.songwj.openvideo.ffmpeg

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import java.lang.ref.WeakReference
import java.math.BigInteger

class NativePlayerKT {

    external fun ffmpegInfo(): String?
    external fun videoTypeTransform(srcFilePath: String?, destFilePath: String?)
    private external fun nativeCreatePlayer(): Int
    private external fun nativeSetDataSource(nativePlayer: Int, url: String?)
    private external fun nativePrepareSync(nativePlayer: Int)
    private external fun nativeSetSurface(nativePlayer: Int, surface: Surface?)
    private external fun nativeStart(nativePlayer: Int)
    private external fun nativePause(nativePlayer: Int)
    private external fun nativeResume(nativePlayer: Int)
    private external fun nativeStop(nativePlayer: Int)
    private external fun nativeReset(nativePlayer: Int)
    private external fun nativeRelease(nativePlayer: Int)
    private external fun nativeGetDuration(nativePlayer: Int): Long
    private external fun nativeGetCurrentPosition(nativePlayer: Int): Long
    private external fun nativeSeekTo(nativePlayer: Int, position: Long)
    private external fun nativeGetMaxVolumeLevel(nativePlayer: Int): Int
    private external fun nativeGetVolumeLevel(nativePlayer: Int): Int
    private external fun nativeSetVolumeLevel(nativePlayer: Int, volume: Int)

    companion object {
        /**
         * native层调用此方法返回相应状态
         */
        private fun postEventFromNative(weakThis: Any?, what: Int, arg1: Int, arg2: Int, obj: Any?) {
            Log.e("NativePlayer", "postEventFromNative(): $what")
            if (weakThis != null) {
                var nativePlayer: NativePlayerKT? = null
                if (weakThis is WeakReference<*>) {
                    nativePlayer = (weakThis as WeakReference<NativePlayerKT?>).get()
                } else if (weakThis is NativePlayerKT) {
                    nativePlayer = weakThis
                }
                if (nativePlayer != null && nativePlayer.eventHandler != null) {
                    nativePlayer.eventHandler!!.sendMessage(
                        nativePlayer.eventHandler!!.obtainMessage(
                            what,
                            arg1,
                            arg2,
                            obj
                        )
                    )
                }
            }
        }

        /**
         * 处理native返回的事件
         */
        private const val NATIVE_CALLBACK_EVENT_NONE = 0
        private const val NATIVE_CALLBACK_EVENT_ERROR = 1
        private const val NATIVE_CALLBACK_EVENT_PREPARED = 2
        private const val NATIVE_CALLBACK_EVENT_INFO = 3
        private const val NATIVE_CALLBACK_EVENT_BUFFERING_UPDATE = 4
        private const val NATIVE_CALLBACK_EVENT_POSITION_UPDATE = 5
        private const val NATIVE_CALLBACK_EVENT_COMPLETED = 6
        private const val NATIVE_CALLBACK_EVENT_SEEK_COMPLETED = 7

        /*******************************************************
         * native部分
         */
        init {
            System.loadLibrary("native-lib")
        }
    }

    private var nativePlayer: Int?
    private var eventHandler: EventHandler? = null

    /**
     * 获取播放状态
     * @return
     */
    var state = State.IDLE
        private set
    private val statePrev = State.IDLE
    private var onPreparedListener: OnPreparedListener? = null
    private var onInfoListener: OnInfoListener? = null
    private var onSeekCompletedListener: OnSeekCompletedListener? = null
    private var onBufferingUpdateListener: OnBufferingUpdateListener? = null
    private var onPositionUpdateListener: OnPositionUpdateListener? = null
    private var onCompletedListener: OnCompletedListener? = null
    private var onErrorListener: OnErrorListener? = null

    /**
     * 设置视频源
     * @param url
     */
    fun setDataSource(url: String) {
        if (nativePlayer == null) {
            synchronized(NativePlayerKT::class.java) {
                if (nativePlayer == null) {
//                    nativePlayer = nativeCreatePlayer(WeakReference<NativePlayer>(this))
                    nativePlayer = nativeCreatePlayer()
                }
            }
        }
        if (nativePlayer == null) {
            if (onErrorListener != null) {
                onErrorListener!!.onError(this, -1, -1, "Create native player failed.")
            }
            return
        }
        if (TextUtils.isEmpty(url)) {
            if (onErrorListener != null) {
                onErrorListener!!.onError(this, -1, -1, "URL is empty.")
            }
            return
        }
        nativeSetDataSource(nativePlayer!!, url)
        state = State.IDLE
    }

    /**
     * 准备播放器
     */
    fun prepareSync() {
        if (nativePlayer == null) {
            return
        }
        nativePrepareSync(nativePlayer!!)
    }

    fun setSurface(surface: Surface?) {
        if (nativePlayer == null) {
            return
        }
        nativeSetSurface(nativePlayer!!, surface)
    }

    /**
     * 开始播放
     */
    fun start() {
        if (nativePlayer == null) {
            return
        }
        if (state != State.RUNNING && state != State.STOPED) {
            state = State.RUNNING
            nativeStart(nativePlayer!!)
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        if (nativePlayer == null) {
            return
        }
        if (state == State.RUNNING) {
            state = State.PAUSED
            nativePause(nativePlayer!!)
        }
    }

    /**
     * 继续播放
     */
    fun resume() {
        if (nativePlayer == null) {
            return
        }
        if (state == State.PAUSED) {
            state = State.RUNNING
            nativeResume(nativePlayer!!)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        if (nativePlayer == null) {
            return
        }
        if (state != State.IDLE && state != State.STOPED) {
            state = State.STOPED
            nativeStop(nativePlayer!!)
        }
    }

    /**
     * 重置播放器
     */
    fun reset() {
        if (nativePlayer == null) {
            return
        }
        if (state != State.IDLE) {
            state = State.IDLE
            nativeReset(nativePlayer!!)
            //            resetListener();
        }
    }

    private fun resetListener() {
        onPreparedListener = null
        onInfoListener = null
        onSeekCompletedListener = null
        onBufferingUpdateListener = null
        onPositionUpdateListener = null
        onCompletedListener = null
        onErrorListener = null
    }

    /**
     * 释放播放器
     */
    fun release() {
        if (nativePlayer == null) {
            return
        }
        state = State.IDLE
        nativeRelease(nativePlayer!!)
        resetListener()
        nativePlayer = null
    }

    /**
     * 获取视频时长
     */
    val duration: Long
        get() = if (nativePlayer == null) {
            0L
        } else nativeGetDuration(nativePlayer!!)

    /**
     * 获取视频当前播放位置
     */
    val currentDuration: Long
        get() = if (nativePlayer == null) {
            0L
        } else nativeGetCurrentPosition(nativePlayer!!)

    /**
     * 指定位置播放
     * @param position
     */
    fun seekTo(position: Long) {
        if (nativePlayer == null) {
            return
        }
        nativeSeekTo(nativePlayer!!, position)
    }

    /**
     * 获取最大音量
     * @return
     */
    val maxVolumeLevel: Int
        get() = if (nativePlayer == null) {
            0
        } else nativeGetMaxVolumeLevel(nativePlayer!!)
    /**
     * 获取最大音量
     * @return
     */
    /**
     * 设置音量
     * @param volume
     */
    var volumeLevel: Int
        get() = if (nativePlayer == null) {
            0
        } else nativeGetVolumeLevel(nativePlayer!!)
        set(volume) {
            if (nativePlayer == null) {
                return
            }
            nativeSetVolumeLevel(nativePlayer!!, volume)
        }

    private inner class EventHandler(nativePlayer: NativePlayerKT?, looper: Looper?) :
        Handler(looper) {
        private val weakNativePlayer: WeakReference<NativePlayerKT?>
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            if (msg != null) {
                val code = msg.what
                Log.e("NativePlayer", "handleMessage(): $code")
                when (code) {
                    NATIVE_CALLBACK_EVENT_PREPARED -> {
                        val player1 = weakNativePlayer.get()
                        player1!!.state = State.PREPARED
                        if (player1 != null && player1.onPreparedListener != null) {
                            player1.onPreparedListener!!.onPrepared(player1)
                        }
                    }
                    NATIVE_CALLBACK_EVENT_INFO -> {
                        val player2 = weakNativePlayer.get()
                        if (player2 != null && player2.onInfoListener != null) {
                            player2.onInfoListener!!.onInfo(
                                player2,
                                msg.arg1,
                                msg.arg2,
                                BigInteger(msg.obj as String).toInt()
                            )
                        }
                    }
                    NATIVE_CALLBACK_EVENT_BUFFERING_UPDATE -> {
                        val player4 = weakNativePlayer.get()
                        if (player4 != null && player4.onBufferingUpdateListener != null) {
                            player4.onBufferingUpdateListener!!.onBufferingUpdate(player4, msg.arg1)
                        }
                    }
                    NATIVE_CALLBACK_EVENT_POSITION_UPDATE -> {
                        val player5 = weakNativePlayer.get()
                        if (player5 != null && player5.onPositionUpdateListener != null) {
                            player5.onPositionUpdateListener!!.onPositionUpdate(
                                player5,
                                msg.arg1.toLong()
                            )
                        }
                    }
                    NATIVE_CALLBACK_EVENT_COMPLETED -> {
                        val player6 = weakNativePlayer.get()
                        player6!!.state = State.COMPLETED
                        if (player6 != null && player6.onCompletedListener != null) {
                            player6.onCompletedListener!!.onCompleted(player6)
                        }
                    }
                    NATIVE_CALLBACK_EVENT_SEEK_COMPLETED -> {
                        val player7 = weakNativePlayer.get()
                        state = statePrev
                        if (player7 != null && player7.onSeekCompletedListener != null) {
                            player7.onSeekCompletedListener!!.onSeekCompleted(player7)
                        }
                    }
                    NATIVE_CALLBACK_EVENT_ERROR -> {
                        val player8 = weakNativePlayer.get()
                        if (player8 != null && player8.onErrorListener != null) {
                            var errorMsg = ""
                            if (msg.obj != null) {
                                errorMsg = if (msg.obj is String) {
                                    msg.obj as String
                                } else {
                                    msg.obj.toString()
                                }
                            }
                            player8.onErrorListener!!.onError(player8, msg.arg1, msg.arg2, errorMsg)
                        }
                    }
                    else -> {
                    }
                }
            }
        }

        init {
            weakNativePlayer = WeakReference<NativePlayerKT?>(nativePlayer)
        }
    }

    fun setOnPreparedListener(onPreparedListener: OnPreparedListener?) {
        this.onPreparedListener = onPreparedListener
    }

    fun setOnInfoListener(onInfoListener: OnInfoListener?) {
        this.onInfoListener = onInfoListener
    }

    fun setOnSeekCompletedListener(onSeekCompletedListener: OnSeekCompletedListener?) {
        this.onSeekCompletedListener = onSeekCompletedListener
    }

    fun setOnBufferingUpdateListener(onBufferingUpdateListener: OnBufferingUpdateListener?) {
        this.onBufferingUpdateListener = onBufferingUpdateListener
    }

    fun setOnPositionUpdateListener(onPositionUpdateListener: OnPositionUpdateListener?) {
        this.onPositionUpdateListener = onPositionUpdateListener
    }

    fun setOnCompletedListener(onCompletedListener: OnCompletedListener?) {
        this.onCompletedListener = onCompletedListener
    }

    fun setOnErrorListener(onErrorListener: OnErrorListener?) {
        this.onErrorListener = onErrorListener
    }

    /**
     * 播放状态
     */
    enum class State {
        /** 初始状态（未进行任何操作）  */
        IDLE,

        /** 准备完成状态（初始化数据提取器、参数、渲染器等）  */
        PREPARED,

        /** 播放中   */
        RUNNING,

        /** 播放暂停   */
        PAUSED,

        /** 播放停止   */
        STOPED,

        /** 正在快进   */
        SEEKING,

        /** 播放结束   */
        COMPLETED
    }

    interface OnPreparedListener {
        fun onPrepared(player: NativePlayerKT?)
    }

    interface OnInfoListener {
        fun onInfo(player: NativePlayerKT?, videoWidth: Int, videoHeight: Int, videoRotation: Int)
    }

    interface OnSeekCompletedListener {
        fun onSeekCompleted(player: NativePlayerKT?)
    }

    interface OnBufferingUpdateListener {
        fun onBufferingUpdate(player: NativePlayerKT?, progress: Int)
    }

    interface OnPositionUpdateListener {
        fun onPositionUpdate(player: NativePlayerKT?, progress: Long)
    }

    interface OnCompletedListener {
        fun onCompleted(player: NativePlayerKT?)
    }

    interface OnErrorListener {
        fun onError(player: NativePlayerKT?, what: Int, extra: Int, msg: String?)
    }

    /*******************************************************
     * java部分
     */
    init {
        nativePlayer = nativeCreatePlayer()
        var looper: Looper?
        if (Looper.myLooper().also { looper = it } != null) {
            eventHandler = EventHandler(this, looper)
        } else if (Looper.getMainLooper().also { looper = it } != null) {
            eventHandler = EventHandler(this, looper)
        } else {
            eventHandler = null
        }
    }
}