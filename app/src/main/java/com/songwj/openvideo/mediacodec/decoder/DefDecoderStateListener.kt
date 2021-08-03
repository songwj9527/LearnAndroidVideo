package com.songwj.openvideo.mediacodec.decoder

import com.songwj.openvideo.mediacodec.Frame


/**
 * 默认实现的解码监听器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2020-05-28 20:18
 *
 */
interface DefDecoderStateListener: IDecoderStateListener {
    override fun decoderPrepared(decodeJob: BaseDecoder?) {}

    override fun decoderRunning(decodeJob: BaseDecoder?) {}

    override fun decoderProgress(decodeJob: BaseDecoder?, progress: Long) {}

    override fun decoderPause(decodeJob: BaseDecoder?) {}

    override fun decodeOneFrame(decodeJob: BaseDecoder?, frame: Frame) {}

    override fun decoderCompleted(decodeJob: BaseDecoder?) {}

    override fun decoderSeekCompleted(decodeJob: BaseDecoder?) {}

    override fun decoderDestroy(decodeJob: BaseDecoder?) {}

    override fun decoderError(decodeJob: BaseDecoder?, msg: String) {}
}