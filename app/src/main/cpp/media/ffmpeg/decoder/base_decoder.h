//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_BASE_DECODER_H
#define OPENVIDEO_FFMPEG_BASE_DECODER_H

#include <thread>

#include "../../player/player_state.h"
#include "i_decoder_callback.h"

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
    virtual State GetState() = 0;
    virtual void Start() = 0;
    virtual void Pause() = 0;
    virtual void Resume() = 0;
    virtual void Stop() = 0;
    virtual bool SeekTo(int64_t timestamp) = 0;
    virtual jlong GetDuration() = 0;
    virtual jlong GetCurrentPosition() = 0;
};

/*****************************************************************
 * 自定义解码器基类：BaseDecoder
 ****************************************************************/
class BaseDecoder : IDecoder{
private:
    // seek时间（单位：毫秒）
    jlong m_seek_timestamp = 0;


    void Init(JNIEnv *env);

    void CreateDecoderThread();
    static void RunDecoder(std::shared_ptr<BaseDecoder> that);

    bool InitDecoder(JNIEnv * env);
    void LoopDecoder(JNIEnv * env);
    void DoneDecoder();

    void DecodeFrame(JNIEnv * env, bool isSeeking);
    int64_t ObtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame);

    bool IsRunning();

    void Wait();
    void WakeUp();


protected:
    const char *TAG = "BaseDecoder";

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
    volatile jlong m_cur_t_s = 0;

    // 总时长（单位：毫秒）
    volatile jlong m_duration = 0;

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

    IDecoderCallback *m_i_decoder_callback = NULL;


    virtual AVMediaType GetMediaType() = 0;
    virtual void OnPrepared(JNIEnv *env) = 0;
    virtual void OnError(JNIEnv *env, int code, const char *msg) = 0;
    virtual void OnComplete(JNIEnv *env) = 0;
    virtual void OnSeekComplete(JNIEnv *env) = 0;


public:
    BaseDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback);
    ~BaseDecoder();


    State   GetState() override;
    void    Start() override;
    void    Pause() override;
    void    Resume() override;
    void    Stop() override;
    bool    SeekTo(int64_t timestamp) override;
    jlong   GetDuration() override;
    jlong   GetCurrentPosition() override;
};

#endif //OPENVIDEO_FFMPEG_BASE_DECODER_H
