package com.songwj.openvideo.mediacodec.encoder


/**
 * 编码状态回调接口
 *
 */
interface IEncodeStateListener {
    fun encodeStart(encoder: BaseEncoder)
    fun encoderFinish(encoder: BaseEncoder)
}