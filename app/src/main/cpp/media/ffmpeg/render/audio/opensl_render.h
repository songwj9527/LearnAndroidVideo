//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_OPENSL_RENDER_H
#define OPENVIDEO_FFMPEG_OPENSL_RENDER_H

#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "./audio_render.h"

extern "C" {
#include <libavutil/time.h>
#include <libavutil/mem.h>
}

class FFmpegOpenSLRender : public FFmpegAudioRender {
private:
//    const char *TAG = "OpenSLRender";

    JNIEnv *jniEnv;

    // -------------------OpenSL ES相关-----------------------------
    const SLuint32 SL_QUEUE_BUFFER_COUNT = 2;

    // 引擎接口
    SLObjectItf m_engine_obj = NULL;
    SLEngineItf m_engine = NULL;

    //混音器
    SLObjectItf m_output_mix_obj = NULL;
    SLEnvironmentalReverbItf m_output_mix_evn_reverb = NULL;
    SLEnvironmentalReverbSettings m_reverb_settings = SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;

    // pcm播放器
    SLObjectItf m_pcm_player_obj = NULL;
    SLPlayItf m_pcm_player = NULL;
    SLVolumeItf m_pcm_player_volume = NULL;

    //缓冲器队列接口
    SLAndroidSimpleBufferQueueItf m_pcm_buffer;

    volatile bool isCompletedToReset = false;

    // 状态线程锁变量
    pthread_mutex_t m_command_mutex;
    pthread_cond_t m_command_cond;
    volatile int m_command = 0; // 0, 默认状态；1，重置播放；2，停止播放

    void WaitCommand() {
        pthread_mutex_lock(&m_command_mutex);
        pthread_cond_wait(&m_command_cond, &m_command_mutex);
        pthread_mutex_unlock(&m_command_mutex);
    }

    void SendCommand(int command) {
        pthread_mutex_lock(&m_command_mutex);
        if (command < 0 || command > 2) {
            command = 0;
        }
        m_command = command;
        pthread_cond_signal(&m_command_cond);
        pthread_mutex_unlock(&m_command_mutex);
    }

    /**
     * 初始化
     * @param env
     */
    void Init(JNIEnv *env);

    /**
     * 初始化线程调用方法
     * @param that
     */
    void static RunPrepareThread(std::shared_ptr<FFmpegOpenSLRender> that);

    /**
     * 初始化方法
     */
    bool PrepareOpenSL(JNIEnv *env);

    /**
     * 创建OpenSL ES引擎
     * @return
     */
    bool CreateEngine(JNIEnv *env);

    /**
     * 创建OpenSL ES音频输出混音器
     * @return
     */
    bool CreateOutputMixer(JNIEnv *env);

    /**
     * 配置OpenSL ES音频播放器
     * @return
     */
    bool ConfigPlayer(JNIEnv *env);


    /**
     * 用于判断OpenSL ES相关错误
     * @param result
     * @param hint
     * @return
     */
    bool CheckError(SLresult result, std::string hint);

    /**
     * 设置OpenSL ES开始播放
     */
    void StartOpenSL(JNIEnv *env);
    void RestartOpenSL(JNIEnv *env);

    /**
     * OpenSL ES加载缓存回调方法
     * @param bufferQueueItf
     * @param context
     */
    void static sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context);

    /**
     * 更新JNIEnv
     */
    void UpdateJNIEnv();

    /**
     * 具体播放音频数据方法
     */
    void Render();

    /**
     * 释放OpenSL ES相关资源
     */
    void ReleaseOpenSL();

protected:
    void DoUpdateSyncClock(int64_t timestamp) override {
        m_current_timestamp = ((jdouble) (timestamp) / 1000);
        if (m_i_render_callback != NULL) {
            m_i_render_callback->UpdateSyncClock(((jdouble) (timestamp) / 1000));
        }
    };

public:
    FFmpegOpenSLRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback, AVCodecContext *codecContext, AVRational streamTimeBase);
    ~FFmpegOpenSLRender();

    jint GetMaxVolumeLevel();
    jint GetVolumeLevel();
    void SetVolumeLevel(jint volume);

    void Start() override;
    void Pause() override;
    void Resume() override;
    void Stop() override;
    void SeekTo(int64_t timestamp) override;
};

#endif //OPENVIDEO_FFMPEG_OPENSL_RENDER_H
