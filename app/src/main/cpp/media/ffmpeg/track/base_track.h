//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_BASE_TRACK_H
#define OPENVIDEO_FFMPEG_BASE_TRACK_H

#include <thread>

#include "../../player/player_state.h"
#include "i_track_callback.h"
#include "../render/base_render.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
}

/*****************************************************************
 * 自定义音视频轨道：ITracker
 ****************************************************************/
class IFFmpegTracker {
public:
    virtual State GetState() = 0;
    virtual void Start() = 0;
    virtual void Pause() = 0;
    virtual void Resume() = 0;
    virtual void Stop() = 0;
    virtual bool SeekTo(int64_t timestamp) = 0;
    virtual jlong GetDuration() = 0;
    virtual jlong GetCurrentTimestamp() = 0;
};

/*****************************************************************
 * 自定义音视频轨道基类：BaseTrack
 ****************************************************************/
class BaseFFmpegTrack : IFFmpegTracker, public IFFmpegRenderCallback {
private:
    void CreateDecoder(JNIEnv *env);
    static void RunDecoderThread(std::shared_ptr<BaseFFmpegTrack> that);
    bool InitDecoder(JNIEnv * env);
    void LoopDecoder(JNIEnv * env);
    void DoneDecoder(JNIEnv * env);
    void DecodeFrame(JNIEnv * env, bool isSeeking);
    int64_t ObtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame);

    void CreateRender(JNIEnv * env);

protected:
    const char *TAG = "BaseTrack";

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;

    //-------------定义解码相关------------------------------
    // 视频源
    const char *m_url = NULL;

    // 解码信息上下文
    AVFormatContext *m_format_ctx = NULL;
    // 解码器上下文
    AVCodecContext *m_codec_ctx = NULL;
    // 数据流索引
    int m_stream_index = -1;
    // 解码器
    AVCodec *m_codec = NULL;
    // 待解码包
    AVPacket *m_packet = NULL;
    // 最终解码数据
    AVFrame *m_frame = NULL;

    // 总时长（单位：毫秒）
    volatile jlong m_duration = 0;
    // 当前播放时间（单位：毫秒）
    volatile jdouble m_current = 0;
    // seek时间（单位：毫秒）
    jlong m_seek_timestamp = 0;

    // 是否解码器已经全部解码
    volatile bool isFrameEOS= false;

    // 播放状态
    volatile State m_state = IDLE;
    // 线程等待锁变量
    pthread_mutex_t m_state_mutex;
    pthread_cond_t m_state_cond;

    // 渲染器
    BaseFFmpegRender *m_render = NULL;

    // 外部回调
    IFFmpegTrackCallback *m_i_track_callback = NULL;


    virtual AVMediaType GetMediaType() = 0;

    virtual void CreateTargetRender(JNIEnv *env) = 0;

    virtual void OnPrepared(JNIEnv *env) = 0;
    virtual void OnError(JNIEnv *env, int code, const char *msg) = 0;
    virtual void OnComplete(JNIEnv *env) = 0;
    virtual void OnSeekComplete(JNIEnv *env) = 0;

    void WaitState();
    void WakeUpState();
    bool IsRunning();

public:
    BaseFFmpegTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback);
    ~BaseFFmpegTrack();

    void OnRenderPrepared(JNIEnv *jniEnv, int render) override;
    void OnRenderCompleted(JNIEnv *jniEnv, int render) override;
    void OnRenderError(JNIEnv *jniEnv, int render, int code, const char *msg) override;
    void UpdateSyncClock(jdouble syncClock) override;
    jdouble GetSyncClock() override;

    State GetState() override;
    void  Start() override;
    void  Pause() override;
    void  Resume() override;
    void  Stop() override;
    bool  SeekTo(int64_t timestamp) override;
    jlong GetDuration() override;
    jlong GetCurrentTimestamp() override;
};

#endif //OPENVIDEO_FFMPEG_BASE_TRACK_H
