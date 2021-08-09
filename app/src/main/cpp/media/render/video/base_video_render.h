//
// Created by fgrid on 2021/7/20.
//

#ifndef OPENVIDEO_BASE_VIDEO_RENDER_H
#define OPENVIDEO_BASE_VIDEO_RENDER_H

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

#include "../base_render.h"

class VideoDecoder;
class BaseVideoRender : public BaseRender {
private:
//    const char *TAG = "BaseVideoRender";

protected:
//    const char *TAG = "BaseVideoRender";

    JNIEnv *jniEnv;

    // -------------------视频数据转换相关-----------------------------
    //视频数据目标格式
    const AVPixelFormat DST_FORMAT = AV_PIX_FMT_RGBA;

    //存放YUV转换为RGB后的数据
    AVFrame *m_rgb_frame = NULL;

    // 存放YUV转换为RGB后的数据
    uint8_t *m_buf_for_rgb_frame = NULL;

    //视频格式转换器
    SwsContext *m_sws_ctx = NULL;

    // -------------------视图渲染相关-----------------------------
    // Surface引用，必须使用引用，否则无法在线程中操作
    jobject m_surface_ref = NULL;

    // 本地窗口
    ANativeWindow *m_native_window = NULL;

    //显示的目标宽
    int m_dst_w;
    //显示的目标高
    int m_dst_h;


    /**
     * 渲染准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderPrepared方法）
     */
    void onPrepared(JNIEnv *env) override;

    /**
     * 渲染线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onRenderError方法）
     */
    void onError(JNIEnv *env,int code, const char *msg) override;

    /**
     * 渲染已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderComplete方法）
     */
    void onComplete(JNIEnv *env) override;


    /**
     * 初始化渲染窗口
     * @param env
     */
    void initANativeWindow(JNIEnv *env);

    /**
     * 初始化视频数据转换缓存
     */
    void initReaderBuffer();

    /**
     * 初始化格式转换工具
     */
    void initSws();

    /**
     * 将视频帧数据转换成渲染数据
     */
    void scaleFrame(AVFrame *frame);

    /**
     * 新建读取视频帧、渲染线程等（opengl时需要新增opengl渲染线程，具体可在createOtherThread中自定义处理）
     */
    void prepareSyncAllThread(JNIEnv *env);

    /**
     * 新建读取视频帧、渲染总线程
     */
    void createDefaultThread();

    /**
     * 读取视频帧、渲染总线程调用的方法
     * @param that BaseVideoRender
     */
    static void runDefaultThread(std::shared_ptr<BaseVideoRender> that);

    /**
     * 新建其他扩展自定义线程
     */
    virtual void createOtherThread(JNIEnv *env) = 0;

    /**
     * 判断渲染线程是否可以继续渲染
     * @return
     */
    bool isRunning();

    /**
     * 开始循环渲染
     */
    void loopRender(JNIEnv *env);

    /**
     * 渲染方法
     */
    virtual void render() = 0;

    /**
     * 音视频时间校准
     * @param frame
     * @param play
     * @return
     */
    double synchronize(AVFrame *frame, double render_clock);

    /**
     * 渲染结束调用方法
     * @param env
     */
    virtual void doneRender() = 0;

    /**
     * 释放视频数据转换相关资源
     */
    void releaseReader();

    /**
     * 释放Native屏幕资源
     */
    virtual void releaseANativeWindow() = 0;


    /**
     * 用于子类定义start()方法调用时的额外处理
     */
    virtual void onStartRun() = 0;

    /**
     * 用于子类定义pause()方法调用时的额外处理
     */
    virtual void onPauseRun() = 0;

    /**
     * 用于子类定义resume()方法调用时的额外处理
     */
    virtual void onResumeRun() = 0;

    /**
     * 用于子类定义stop()方法调用时的额外处理
     */
    virtual void onStopRun() = 0;


public:
    BaseVideoRender(bool for_synthesizer);
    ~BaseVideoRender();

    /**
     * 初始化Render
     * @param env
     * @param decoder
     */
    void prepareSync(JNIEnv *env, Player *mediaPlayer, BaseDecoder *decoder) override;

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    virtual void setSurface(JNIEnv *jniEnv, jobject surface) = 0;

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
};

#endif //OPENVIDEO_BASE_VIDEO_RENDER_H
