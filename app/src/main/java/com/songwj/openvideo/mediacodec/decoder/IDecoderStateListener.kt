package com.songwj.openvideo.mediacodec.decoder

import com.songwj.openvideo.mediacodec.Frame


/**
 * 解码状态回调接口
 */
interface IDecoderStateListener {
    fun decoderPrepared(decodeJob: BaseDecoder?)
    fun decoderRunning(decodeJob: BaseDecoder?)
    fun decoderProgress(decodeJob: BaseDecoder?, progress: Long)
    fun decoderPause(decodeJob: BaseDecoder?)
    fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame)
    fun decoderSeekCompleted(decodeJob: BaseDecoder?)
    fun decoderCompleted(decodeJob: BaseDecoder?)
    fun decoderDestroy(decodeJob: BaseDecoder?)
    fun decoderError(decodeJob: BaseDecoder?, msg: String)
}