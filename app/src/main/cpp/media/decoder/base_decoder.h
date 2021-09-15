//
// Created by fgrid on 1/25/21.
//

#ifndef OPENVIDEO_BASE_DECODER_H
#define OPENVIDEO_BASE_DECODER_H

#include <jni.h>
#include <string>
#include <thread>
#include <queue>

#include "../player/player_state.h"
#include "../render/render_frame.h"
#include "../cache_frame.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
}

/*****************************************************************
 * 自定义解码器模版：IDecoder
 ****************************************************************/
class IDecoder {
public:
    virtual State getState() = 0;
    virtual void start() = 0;
    virtual void pause() = 0;
    virtual void resume() = 0;
    virtual void stop() = 0;
    virtual jlong getDuration() = 0;
    virtual jlong getCurrentPosition() = 0;
    virtual bool seekTo(int64_t timestamp) = 0;
};

class FFmpegPlayer;
class BaseRender;
/*****************************************************************
 * 自定义解码器基类：BaseDecoder
 ****************************************************************/
class BaseDecoder : IDecoder {
private:
//    const char *TAG = "BaseDecoder";

    /**
     * 初始化
     * @param env
     */
    void init(JNIEnv *env);

    /**
     * 新建解码线程
     */
    void createDecodeThread();

    /**
     * 解码线程调用的解码方法
     * @param that 当前解码器
     */
    static void runDecode(std::shared_ptr<BaseDecoder> that);

    /**
     * 初始化解码器
     * @param env
     */
    bool initDecoder(JNIEnv * env);

    /**
     * 开始循环解码
     */
    void loopDecode(JNIEnv * env);

    /**
     * 判断解码线程是否可以继续解码
     * @return
     */
    bool isRunning();

    /**
     * 进入等待解码
     * @param second
     */
    void wait(long second);

    /**
     * 恢复解码
     */
    void sendSignal();


    /**
     * 解码一帧数据
     * @return
     */
    void decodeFrame(JNIEnv * env, bool isSeeking);

    /**
     * 获取当前帧时间戳
     */
    int64_t obtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame);

    /**
     * 解码结束调用方法
     * @param env
     */
    void doneDecode();

    // seek时间（单位：毫秒）
    jlong m_seek_timestamp = 0;

protected:
    const char *TAG = "BaseDecoder";
    // 播放器
    FFmpegPlayer *mediaPlayer = NULL;
    // 渲染器
    BaseRender *render = NULL;

    //-------------定义解码相关------------------------------
    // 经过视频源
    const char *m_url = NULL;

    // 解码信息上下文
    AVFormatContext *m_format_ctx = NULL;

    // 解码器上下文
    AVCodecContext *m_codec_ctx = NULL;

    // 解码器
    AVCodec *m_codec = NULL;

    // 待解码包
    AVPacket *m_packet = NULL;

    // 最终解码数据
    AVFrame *m_frame = NULL;

    // 当前解码时间（单位：秒）
    jlong m_cur_t_s = 0;

    // 总时长（单位：毫秒）
    jlong m_duration = 0;

    // 解码状态
    volatile State m_state = IDLE;

    // 数据流索引
    int m_stream_index = -1;

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;

    // 线程等待锁变量
    pthread_mutex_t m_mutex;
    pthread_cond_t m_cond;

    // 为合成器提供解码
    bool m_for_synthesizer = false;


    /**
     * 音视频索引
     */
    virtual AVMediaType getMediaType() = 0;

    /**
     * 解码准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderPrepared方法）
     */
    virtual void onPrepared(JNIEnv *env) = 0;

    /**
     * 解码线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderError方法）
     */
    virtual void onError(JNIEnv *env, int code, const char *msg) = 0;

    /**
     * 解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderComplete方法）
     */
    virtual void onComplete(JNIEnv *env) = 0;

    /**
     * 指定位置解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderSeekComplete方法）
     */
    virtual void onSeekComplete(JNIEnv *env) = 0;

public:
    BaseDecoder(JNIEnv *env, FFmpegPlayer *mediaPlayer, const char *url, BaseRender *render, bool for_synthesizer);
    virtual ~BaseDecoder();

    /**
    * 解码器上下文
    * @return
    */
    AVCodecContext *getCodecContext() {
        return m_codec_ctx;
    }

    /**
    * 数据编码格式
    * @return
    */
    AVPixelFormat getPixelFormat() {
        return m_codec_ctx->pix_fmt;
    }

    /**
    * 获取解码器上下文时间基
    */
    AVRational getCodecTimeBase() {
        return m_codec_ctx->time_base;
    }

    /**
     * 获取解码流时间基
     */
    AVRational getStreamTimeBase() {
        return m_format_ctx->streams[m_stream_index]->time_base;
    }

    /**
     * 给外部调用唤醒解码线程
     */
    void wake();

    /**
     * 获取当前解码器状态
     * @return
     */
    State getState() override;

    /**
     * 开始解码
     */
    void start() override;

    /**
     * 暂停解码
     */
    void pause() override;

    /**
     * 继续解码
     */
    void resume() override;

    /**
     * 停止解码
     */
    void stop() override;

    /**
     * 音视频总时长
     * @return
     */
    jlong getDuration() override;

    /**
     * 当前解码位置
     * @return
     */
    jlong getCurrentPosition() override;

    /**
     * 指定位置解码
     * @param timestamp 单位毫秒
     */
    bool seekTo(int64_t timestamp) override;

    /**
     * 是否已解码结束
     * @return
     */
    bool isCompleted();
};

#endif //OPENVIDEO_BASE_DECODER_H
