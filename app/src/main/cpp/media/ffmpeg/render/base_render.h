//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_BASE_RENDER_H
#define OPENVIDEO_FFMPEG_BASE_RENDER_H

#include <thread>
#include <queue>

#include "i_render_callback.h"
#include "../../player/player_state.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
}

/*****************************************************************
 * 自定义渲染器：BaseRender
 ****************************************************************/
class BaseFFmpegRender {
private:
//    const char *TAG = "BaseRender";

protected:
    const char *TAG = "BaseRender";

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;
    // 播放状态锁变量
    pthread_mutex_t m_state_mutex;
    pthread_cond_t m_state_cond;

    // 播放状态
    volatile State m_state = IDLE;
    // 当前播放时间（单位：毫秒）
    volatile jdouble m_current_timestamp = 0;

    // 帧缓存数据队列
    std::queue<AVFrame *> m_frame_queue;
    // 帧缓存数据队列锁变量
    pthread_mutex_t m_frame_mutex;
    pthread_cond_t m_frame_cond;

    // 是否解码器已经全部解码
    volatile bool isFrameEOS= false;

    IFFmpegRenderCallback *m_i_render_callback = NULL;

    AVFrame * PopFrame();
    void ClearFrameQueue();
    void WakeUpFrameQueue();

    virtual void DoUpdateSyncClock(int64_t timestamp) = 0;

    bool IsRunning();
    void WaitState();
    void WakeUpState();


public:
    BaseFFmpegRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback);
    ~BaseFFmpegRender();

    void PushFrame(AVFrame *avFrame);
    void PushSeekFrame(AVFrame *avFrame, int64_t timestamp);
    void PushSeekFrameNull(int64_t timestamp);

    void EnableFrameEOS() {
        isFrameEOS = true;
    }

    virtual void Start() = 0;
    virtual void Pause() = 0;
    virtual void Resume() = 0;
    virtual void Stop() = 0;
    virtual void SeekTo(int64_t timestamp) = 0;
    jdouble GetCurrentTimestamp();
};

#endif //OPENVIDEO_FFMPEG_BASE_RENDER_H
