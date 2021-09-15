//
// Created by fgrid on 2/19/21.
//

#ifndef OPENVIDEO_OPENSL_RENDER_H
#define OPENVIDEO_OPENSL_RENDER_H

#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "./audio_render.h"

extern "C" {
#include <libavutil/mem.h>
}

class OpenSLRender : public AudioRender {
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

    // 状态线程锁变量
    pthread_mutex_t m_command_mutex;
    pthread_cond_t m_command_cond;
    volatile int m_command = 0; // 0, 默认状态；1，重置播放；2，停止播放

    void waitCommand() {
        pthread_mutex_lock(&m_command_mutex);
        pthread_cond_wait(&m_command_cond, &m_command_mutex);
        pthread_mutex_unlock(&m_decode_frame_mutex);
    }

    void sendCommand(int command) {
        pthread_mutex_lock(&m_command_mutex);
        if (command < 0 || command > 2) {
            command = 0;
        }
        m_command = command;
        pthread_cond_signal(&m_command_cond);
        pthread_mutex_unlock(&m_decode_frame_mutex);
    }
    
    /**
     * 初始化线程调用方法
     * @param that
     */
    void static RunPrepare(std::shared_ptr<OpenSLRender> that);

    /**
     * 初始化线程调用方法
     * @param that
     */
    void static runPrepare(OpenSLRender* that);

    /**
     * 更新JNIEnv
     */
    void updateJNIEnv();

    /**
     * 初始化方法
     */
    void prepare(JNIEnv *env);

    /**
     * 创建OpenSL ES引擎
     * @return
     */
    bool createEngine(JNIEnv *env);

    /**
     * 创建OpenSL ES音频输出混音器
     * @return
     */
    bool createOutputMixer(JNIEnv *env);

    /**
     * 配置OpenSL ES音频播放器
     * @return
     */
    bool configPlayer(JNIEnv *env);


    /**
     * 用于判断OpenSL ES相关错误
     * @param result
     * @param hint
     * @return
     */
    bool checkError(SLresult result, std::string hint);

    /**
     * 设置OpenSL ES开始播放
     */
    void startOpenSL(JNIEnv *env);
    void restartOpenSL(JNIEnv *env);

    /**
     * OpenSL ES加载缓存回调方法
     * @param bufferQueueItf
     * @param context
     */
    void static sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context);

    /**
     * 具体播放音频数据方法
     */
    void render();

    /**
     * 判断渲染线程是否可以继续渲染
     * @return
     */
    bool isRunning();

    /**
     * 释放OpenSL ES相关资源
     */
    void releaseOpenSL();

protected:
    /**
     * 渲染准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderPrepared方法）
     */
    void onPrepared(JNIEnv *env) override;

    /**
     * 渲染线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onRenderError方法）
     */
    void onError(JNIEnv *env, int code, const char *msg) override;

    /**
     * 渲染已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderComplete方法）
     */
    void onComplete(JNIEnv *env) override;

public:
    OpenSLRender(bool for_synthesizer);
    ~OpenSLRender();

    /**
     * 初始化Render
     * @param env
     * @param decoder
     */
    void prepareSync(JNIEnv *env, FFmpegPlayer *mediaPlayer, BaseDecoder *decoder) override;

    /**
     * 开始渲染
     */
    void start() override;

    /**
     * 暂停渲染
     */
    void pause() override;

    /**
     * 继续渲染
     */
    void resume() override;

    /**
     * 停止渲染
     */
    void stop() override;

    /**
     * 释放渲染相关资源
     */
    void release() override;

    /**
     * 获取最大音量
     * @return
     */
    jint getMaxVolumeLevel();

    /**
     * 获取当前音量
     * @return
     */
    jint getVolumeLevel();

    /**
     * 设置音量
     * @param volume
     */
    void setVolumeLevel(jint volume);
};
#endif //OPENVIDEO_OPENSL_RENDER_H
