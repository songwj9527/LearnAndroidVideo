//
// Created by fgrid on 2021/9/28.
//

#ifndef OPENVIDEO_CODEC_AUDIO_TRACK_H
#define OPENVIDEO_CODEC_AUDIO_TRACK_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "base_track.h"
#include "../../media_codes.h"

class AudioTrack : public BaseTrack {
private:
    // -------------------OpenSL ES相关-----------------------------
    SLuint32 m_sample_rate = 0;
    SLuint32 m_channel_count = 2;
    int m_bit = 2;

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

    // 输出缓冲
    uint8_t *m_out_buffer[2] = {NULL, NULL};

    // 播放状态线程等待锁变量
    pthread_mutex_t m_play_mutex;
    pthread_cond_t  m_play_cond;

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
     * 根据extractor获取的rate转换成OpenSLES的rate
     * @param sample_rate
     * @return
     */
    SLuint32 GetSampleRateForOpenSLES(int sample_rate);

    /**
     * 用于判断OpenSL ES相关错误
     * @param result
     * @param hint
     * @return
     */
    bool CheckError(SLresult result, std::string hint);

    /**
     * OpenSL ES加载缓存回调方法
     * @param bufferQueueItf
     * @param context
     */
    void static sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context);

    /**
     * 播放
     */
    void Play();
    void HoldOnPlay();
    void WakeUpPlay();

    /**
     * 释放OpenSL ES相关资源
     */
    void ReleaseOpenSL();


protected:
    int GetTrackType() override {
        return MODULE_CODE_AUDIO;
    }

    bool InitExtractor(JNIEnv *env) override;
    bool InitRender(JNIEnv *env) override;
    void Render(uint8_t *buffer_data, AMediaCodecBufferInfo *buffer_info, size_t out_size) override;
    int64_t RenderFrameAliveTime() override;
    void UpdateClock() override;
    void LoopDoneVirtual(JNIEnv *env) override;

    void  StartVirtual(State prev) override;
    void  PauseVirtual(State prev) override;
    void  ResumeVirtual(State prev) override;
    void  StopVirtual(State prev) override;
    void  ResetVirtual(State prev) override;


public:
    AudioTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback);
    ~AudioTrack();

    /**
     * 获取最大音量
     * @return
     */
    jint GetMaxVolumeLevel();

    /**
     * 获取当前音量
     * @return
     */
    jint GetVolumeLevel();

    /**
     * 设置音量
     * @param volume
     */
    void SetVolumeLevel(jint volume);
};

#endif //OPENVIDEO_CODEC_AUDIO_TRACK_H
