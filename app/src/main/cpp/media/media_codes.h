//
// Created by fgrid on 1/29/21.
//

#ifndef OPENVIDEO_MEDIA_CODES_H
#define OPENVIDEO_MEDIA_CODES_H

/**
 * 以下是Native传给Java层的业务码
 */
#define CALLBACK_NONE               0
#define CALLBACK_ERROR              1
#define CALLBACK_PREPARED           2
#define CALLBACK_INFO               3
#define CALLBACK_VIDEO_SIZE_CHANGED 4
#define CALLBACK_BUFFERING_UPDATE   5
#define CALLBACK_COMPLETED          6
#define CALLBACK_SEEK_COMPLETED     7


/**
 * 以下为区分播放流程的错误码
 */
#define PLAYER_NONE                 0
#define PLAYER_SOURCE               1
#define PLAYER_PREPARED             2
#define PLAYER_PLAYING              3
#define PLAYER_PAUSE                4
#define PLAYER_STOP                 5
#define PLAYER_COMPLETION           6

// 默认模块标识
#define MODULE_CODE_NONE                        0
// 音频模块标识
#define MODULE_CODE_AUDIO                       1
// 视频模块标识
#define MODULE_CODE_VIDEO                       2
// 字幕模块标识
#define MODULE_CODE_SUBTITLE                    3
// OPENGL模块标识
#define MODULE_CODE_OPENGL                      4

// 所有成功的返回码
#define SUCCESS_CODE                            0

// 视频地址java的String转c字符串失败
#define JAVA_PATH_2_C_CHARS_FAILED              -1000
// 非法视频地址
#define MEDIA_SOURCE_URL_ERROR                  -1001
// 创建解码线程失败
#define OPEN_DECODER_THREAD_FAILED              -1002
// 创建播放线程失败
#define OPEN_PLAY_THREAD_FAILED                 -1003
// 获取封装上下文失败
#define OPEN_FFMPEG_AV_CONTEXT_FAILED           -1004
// 打开音视频源失败
#define OPEN_FFMPEG_AV_SOURCE_FAILED            -1005
// 未找到解码器
#define NO_FOUND_CODEC                          -1006
// 无法打开解码器上下文
#define OPEN_CODEC_CONTEXT_FAILED               -1007
// 无法打开解码器
#define OPEN_CODEC_FAILED                       -1008
// 创建packet异常
#define CREATE_PACKET_ERROR                     -1009
// 创建frame异常
#define CREATE_FRAME_ERROR                      -1010

// 播放未准备完成
#define VIDEO_RENDER_UNPREPARED                 -2000
// 播放未准备完成
#define VIDEO_OPENGL_RENDER_UNPREPARED          -2001

// 播放未准备完成
#define AUDIO_RENDER_UNPREPARED                 -3000
// 初始化音频转换上下文失败
#define CREATE_AUDIO_SWR_CONTEXT_FAILED         -3001
// 初始化音频转换缓存数组失败
#define CREATE_AUDIO_BUFFER_FAILED              -3002
// 创建OpenSLES引擎engineObject失败
#define CREATE_ENGINE_OBJECT_FAILED             -3003
// 实现OpenSLES引擎engineObject失败
#define REALIZE_ENGINE_OBJECT_FAILED            -3004
// 获取OpenSLES引擎接口engineEngine失败
#define GET_ENGINE_OBJECT_INTERFACE_FAILED      -3005
// 创建OpenSLES混音器outputMixObject失败
#define CREATE_ENGINE_OUTPUT_MIX_FAILED         -3006
// 实现OpenSLES混音器outputMixObject失败
#define REALIZE_ENGINE_OUTPUT_MIX_FAILED        -3007
// 获取OpenSLES混音器接口outputMixEnv失败
#define GET_ENGINE_ENGINE_OUTPUT_MIX_INTERFACE_FAILED      -3008
// 创建OpenSLES播放器失败
#define CREATE_OPENSL_ES_PLAYER_FAILED          -3009
// 初始化OpenSLES播放器失败
#define INIT_OPENSL_ES_PLAYER_FAILED            -3010
// 获取OpenSLES播放器接口失败
#define GET_OPENSL_ES_PLAYER_INTERFACE_FAILED   -3011
// 获取OpenSLES播放器缓存接口失败
#define GET_OPENSL_ES_PLAYER_BUFFER_INTERFACE_FAILED   -3012
// 组册OpenSLES播放器缓存回调失败
#define REGISTER_OPENSL_ES_PLAYER_BUFFER_CALLBACK_FAILED   -3013
// 获取OpenSLES播放器音量接口失败
#define GET_OPENSL_ES_PLAYER_VOLUME_INTERFACE_FAILED   -3014

// 无法初始化NDK的多媒轨道体线程
#define OPEN_MEDIA_TRACK_THREAD_FAILED      -4000
// 无法初始化NDK的多媒体分离器
#define INIT_MEDIA_EXTRACTOR_FAILED         -4001
// 无法初始化NDK的多媒体信息
#define INIT_MEDIA_PARAMS_FAILED            -4002
// 无法初始化NDK的多媒体渲染器
#define INIT_MEDIA_RENDER_FAILED            -4003
// 无法初始化NDK的多媒体解码器
#define INIT_MEDIA_CODEC_FAILED             -4004

#endif //OPENVIDEO_MEDIA_CODES_H
