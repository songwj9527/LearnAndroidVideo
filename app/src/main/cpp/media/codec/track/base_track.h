//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_BASE_TRACK_H
#define OPENVIDEO_CODEC_BASE_TRACK_H

#include <thread>

extern "C" {
#include <libavutil/time.h>
}

#include "i_track.h"
#include "i_track_callback.h"
#include "../../player/player_state.h"
#include "../extractor/base_extractor.h"
#include "i_sync_clock_receiver.h"

class BaseTrack : public ITrack {
private:
    /**
     * 创建线程
     * @param env
     */
    void CreateTrackThread(JNIEnv *env);

    /**
     * 线程调用的解码方法
     * @param that 当前解码器
     */
    static void RunTrackThread(std::shared_ptr<BaseTrack> that);

    /**
     * 初始化Track资源
     */
    bool Init(JNIEnv *env);

    /**
     * 初始化解码器
     * @param env
     * @return
     */
    bool InitCodec(JNIEnv *env);

    /**
     * 线程循环执行方法
     * @param env
     */
    void LoopTrack(JNIEnv *env);

    /**
     * 线程结束调用方法
     * @param env
     */
    void LoopDone(JNIEnv *env);

    /**
     * 见数据压入解码器输入缓冲
     * @return true，已到达视频结尾；false，则没有
     */
    bool DequeueInputBuffer();

    /**
     * 将解码好的数据从缓冲区拉取出来
     * @return 缓存index索引
     */
    int DequeueOutputBuffer();

    /**
     * 音视频同步
     */
    void SyncClock();


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
    AMediaCodecBufferInfo m_buffer_info;
    // 解码是否已到达结尾
    bool m_is_end_of_stream = false;

    // 总时长（单位：毫秒）
    jlong               m_duration = 0L;
    ISyncClockReceiver *m_i_sync_clock_receiver = NULL;

    // 播放状态
    volatile State  m_state = IDLE;
    volatile State  m_state_prev = IDLE;

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;
    // 播放状态线程等待锁变量
    pthread_mutex_t m_state_mutex;
    pthread_cond_t  m_state_cond;
    pthread_mutex_t m_source_mutex;

    /**
     * 获取轨道类型：音频，MODULE_CODE_AUDIO；视频，MODULE_CODE_VIDEO
     * @return
     */
    virtual int GetTrackType() = 0;

    /**
     * 初始化音视频分离器
     * @param env
     * @return
     */
    virtual bool InitExtractor(JNIEnv *env) = 0;

    /**
     * 初始化渲染器
     * @param env
     * @return
     */
    virtual bool InitRender(JNIEnv *env) = 0;

    /**
     * 渲染
     * @param data
     */
    virtual void Render(uint8_t *buffer_data, AMediaCodecBufferInfo *buffer_info, size_t out_size) = 0;

    /**
     * 渲染一帧数据所需时间（音频为音频时间，视频为一帧画面显示时间），单位：毫秒
     * @return
     */
    virtual int64_t RenderFrameAliveTime() = 0;

    /**
     * 更新同步时钟
     */
    virtual void UpdateClock() = 0;

    /**
     * 判断状态是否为运行状态
     * @return
     */
    bool IsRunning() {
        return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
    }

    /**
     * 子类线程结束调用方法
     * @param env
     */
    virtual void LoopDoneVirtual(JNIEnv *env) = 0;

    /**
     * 状态休眠等待刷新
     */
    void HoldOn();

    /**
     * 状态刷新
     */
    void WakeUp();

    virtual void  StartVirtual(State prev) = 0;
    virtual void  PauseVirtual(State prev) = 0;
    virtual void  ResumeVirtual(State prev) = 0;
    virtual void  StopVirtual(State prev) = 0;
    virtual void  ResetVirtual(State prev) = 0;

    
public:
    BaseTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback);
    ~BaseTrack();

    void  Start() override;
    void  Pause() override;
    void  Resume() override;
    void  Stop() override;
    void  Reset() override;
    jlong GetDuration() override;
    jlong GetCurrentTimestamp() override;
    bool  SeekTo(int64_t timestamp) override;
};

#endif //OPENVIDEO_CODEC_BASE_TRACK_H
