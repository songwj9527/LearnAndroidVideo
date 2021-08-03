package com.songwj.openvideo.mediacodec.decoder


/**
 * 解码状态
 */
enum class DecodeState {
    /** 初始状态（未进行任何操作）**/
    IDLE,
    /** 准备完成状态（初始化数据提取器、参数、渲染器等）**/
    PREPARED,
    /** 解码中 **/
    STARTED,
    /** 解码暂停 **/
    PAUSED,
    /** 解码器停止 **/
    STOPED,
    /** 正在快进 **/
    SEEKING,
    /** 快进更新缓存 **/
    SEEKING_FLUSH,
    /** 解码完成 **/
    COMPLETED
}
