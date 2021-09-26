//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_BASE_TRACK_H
#define OPENVIDEO_CODEC_BASE_TRACK_H

#include <thread>

#include "i_track.h"
#include "i_track_callback.h"
#include "../../player/player_state.h"
#include "../extractor/base_extractor.h"

class BaseTrack : public ITrack {
private:
    /**
     * 创建线程
     * @param env
     */
    void CreateTrackThread(JNIEnv *env);

    /**
     * 解码线程调用的解码方法
     * @param that 当前解码器
     */
    static void RunTrackThread(std::shared_ptr<BaseTrack> that);

    /**
     * 初始化Track资源
     */
    void Init(JNIEnv *env);


protected:
    const char *TAG = "BaseTrack";

    // Track的外部回调接口
    ITrackCallback  *m_i_track_callback = NULL;

    // -------------------视频相关-----------------------------
    // 视频源
    const char      *m_source = NULL;

    // 音视频分离器
    BaseExtractor   *m_extractor = NULL;
    // 音视频编解码器
    AMediaCodec     *m_codec = NULL;

    // 总时长（单位：毫秒）
    jlong           m_duration = 0L;
    // 当前解码时间（单位：毫秒）
    jlong           m_current_time = 0L;

    // 播放状态
    volatile State  m_state = IDLE;

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;
    // 播放状态线程等待锁变量
    pthread_mutex_t m_state_mutex;
    pthread_cond_t  m_state_cond;

    /**
     * 初始化音视频分离器
     * @param env
     * @return
     */
    virtual bool InitExtractor(JNIEnv *env) = 0;

    
public:
    BaseTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback);
    ~BaseTrack();

    void  Start() override;
    void  Pause() override;
    void  Resume() override;
    void  Stop() override;
    void  Release() override;
    jlong GetDuration() override;
    jlong GetCurrentTimestamp() override;
    bool  SeekTo(int64_t timestamp) override;
};

#endif //OPENVIDEO_CODEC_BASE_TRACK_H
