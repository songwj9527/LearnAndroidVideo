//
// Created by fgrid on 2/19/21.
//

#ifndef OPENVIDEO_BASE_RENDER_H
#define OPENVIDEO_BASE_RENDER_H

#include <jni.h>
#include <thread>
#include <queue>

#include "./render_frame.h"
#include "../player/player_state.h"
#include "i_render_state_cb.h"

class Player;
class BaseDecoder;
/*****************************************************************
 * 自定义渲染器：BaseRender
 ****************************************************************/
class BaseRender {
private:
//    const char *TAG = "BaseRender";

protected:
    const char *TAG = "BaseRender";

    // 解码后的帧缓存数据队列
    std::queue<AVFrame *> m_decode_frame_queue;
    // 解码后的帧缓存数据队列锁变量
    pthread_mutex_t m_decode_frame_mutex;
    pthread_cond_t m_decode_frame_cond;

    // 待渲染的帧缓存数据队列
    std::queue<RenderFrame *> m_render_frame_queue;
    // 待渲染的帧缓存数据队列锁变量
    pthread_mutex_t m_render_frame_mutex;
    pthread_cond_t m_render_frame_cond;

    // 最大缓存10帧数据
    const int m_max_cache_queue_size = 6;

    // -------------------定义线程相关-----------------------------
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;

    // 播放状态线程锁变量
    pthread_mutex_t m_state_mutex;
    pthread_cond_t m_state_cond;

    // -------------------视频数据转换相关-----------------------------
    // 播放器
    Player *mediaPlayer = NULL;
    // 解码器
    BaseDecoder *decoder = NULL;
    // 当前渲染时间
    volatile double  current_render_clock = 0;

    // 渲染状态
    volatile State m_state = IDLE;
    // 前一个状态
    State m_state_prev = IDLE;
    // 是否从播放完成到可以播放状态（用于声音播放，如果播放完成后需要重新执行播放方法）
    bool isCompletedToReset = false;

    // 为合成器提供解码
    bool m_for_synthesizer = false;

    // 渲染状态外部回调
    IRenderStateCb *m_i_render_state_cb = NULL;


    /**
     * 渲染准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderPrepared方法）
     */
    virtual void onPrepared(JNIEnv *env) = 0;

    /**
     * 渲染异常时调用（子类实现此方法：最终会调用MediaPlayer的onRenderError方法）
     */
    virtual void onError(JNIEnv *env, int code, const char *msg) = 0;

    /**
     * 渲染已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderComplete方法）
     */
    virtual void onComplete(JNIEnv *env) = 0;

    /**
     * 是否为合成器提供解码
     * @return true 为合成器提供解码 false 解码播放
     */
    bool ForSynthesizer() {
        return m_for_synthesizer;
    }

public:
    BaseRender(bool for_synthesizer);
    virtual ~BaseRender();

    /**
     * 获取待解包队列头元素
     * @param cacheFrame
     */
    AVFrame * decodeFrameFont();

    /**
     * 获取一个待解包赋值的AVFrame，给解码器解包赋值
     * @param cacheFrame
     */
    AVFrame * decodeFramePop();


    /**
     * 将一个待解包赋值的AVFrame入列，等待解码器给其赋值
     * @param cacheFrame
     */
    void decodeFramePush(AVFrame *avFrame);

    /**
     * 发送唤醒待解包赋值的帧缓存的休眠状态
     */
    void sendDecodeFrameSignal();

    /**
     * 清空待解包赋值的帧缓存数据队列
     */
    void decodeFrameClear();

    /**
     * 是否待解包赋值的帧缓存为空
     * @return
     */
    bool isDecodeFrameQueueEmpty();

    /**
     * 是否待解包赋值的帧缓存达到最大限制数
     * @return
     */
    bool isDecodeFrameQueueMax();

    /**
     * 将一个待渲染的AVFrame入列，等待渲染器渲染
     * @param cacheFrame
     */
    void renderFramePush(RenderFrame * cacheFrame);

    /**
     * 获取一个待渲染的AVFrame，给渲染器渲染
     * @param cacheFrame
     */
    RenderFrame * renderFramePop();

    /**
     * 获取待渲染队列头元素
     */
    RenderFrame * renderFrameFont();

    /**
     * 唤醒获取待渲染队列
     */
    void sendRenderFrameQueueSignal();

    /**
     * 进入等待解码
     * @param second
     */
    void waitRenderFrame(long second);

    /**
     * 发送唤醒待渲染的帧缓存的休眠状态
     */
    void sendRenderFrameSignal();

    /**
     * 清空待渲染的帧缓存数据队列
     */
    void renderFrameClear();

    /**
     * 是否待渲染的帧缓存达到最大限制数
     * @return
     */
    bool isRenderFrameQueueMax();

    /**
     * 初始化Render
     * @param env
     * @param decoder
     */
    virtual void prepareSync(JNIEnv *env, Player *mediaPlayer, BaseDecoder *decoder) = 0;

    /**
     * 开始渲染
     */
    virtual void start() = 0;

    /**
     * 暂停渲染
     */
    virtual void pause() = 0;

    /**
     * 继续渲染
     */
    virtual void resume() = 0;

    /**
     * 停止渲染
     */
    virtual void stop() = 0;

    /**
     * 释放渲染相关资源
     */
    virtual void release() = 0;

    /**
     * 正在指定位置播放
     */
    void setSeeking(bool enable);

    /**
     * 设置指定的播放位置
     * @param position
     */
    void seekTo(jlong position);

    /**
     * 获取当前播放时间
     * @return
     */
    jlong getCurrentPosition();

    /**
     * 设置渲染状态外部回调
     * @param renderStateCb
     */
    void setIRenderStateCb(IRenderStateCb *renderStateCb) {
        m_i_render_state_cb = renderStateCb;
    }
};
#endif //OPENVIDEO_BASE_RENDER_H