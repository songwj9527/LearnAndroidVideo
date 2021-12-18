//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_BASE_VIDEO_RENDER_H
#define OPENVIDEO_FFMPEG_BASE_VIDEO_RENDER_H

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <libavutil/time.h>
}

#include "../base_render.h"

class BaseFFmpegVideoRender : public BaseFFmpegRender {
private:
//    const char *TAG = "BaseVideoRender";

protected:
    JNIEnv *jniEnv;

    // -------------------视频数据转换相关-----------------------------
    // 视频宽高
    int video_height = 0, video_width = 0;
    // 解码器上下文时间基
    AVRational m_codec_time_base;
    // 解码流的时间基
    AVRational m_stream_time_base;
    // 视频数据编码格式
    AVPixelFormat m_av_pixel_format;

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
     * 初始化
     * @param env
     */
    virtual void Init(JNIEnv *env) = 0;

    /**
     * 初始化渲染窗口
     * @param env
     */
    void InitANativeWindow(JNIEnv *env);

    /**
     * 初始化视频数据转换缓存
     */
    void InitReaderBuffer();

    /**
     * 初始化格式转换工具
     */
    void InitSws();

    /**
     * 将视频帧数据转换成渲染数据
     */
    void ScaleFrame(AVFrame *frame);

    /**
     * 新建读取视频帧、渲染线程等（opengl时需要新增opengl渲染线程，具体可在createOtherThread中自定义处理）
     */
    void PrepareSyncAllThread(JNIEnv *env);

    /**
     * 新建读取视频帧、渲染总线程
     */
    void CreateDefaultThread();

    /**
     * 读取视频帧、渲染总线程调用的方法
     * @param that BaseVideoRender
     */
    static void RunDefaultThread(std::shared_ptr<BaseFFmpegVideoRender> that);

    /**
     * 新建其他扩展自定义线程
     */
    virtual void CreateOtherThread(JNIEnv *env) = 0;

    /**
     * 开始循环渲染
     */
    void LoopRender(JNIEnv *env);

    /**
     * 渲染方法
     */
    virtual void Render() = 0;

    /**
     * 音视频时间校准
     * @param frame
     * @param play
     * @return
     */
    double Synchronize(AVFrame *frame, double render_clock);

    /**
     * 渲染结束调用方法
     * @param env
     */
    virtual void DoneRender() = 0;

    /**
     * 释放视频数据转换相关资源
     */
    void ReleaseReader();

    /**
     * 释放Native屏幕资源
     */
    virtual void ReleaseANativeWindow() = 0;


    /**
     * 用于子类定义start()方法调用时的额外处理
     */
    virtual void OnStartRun() = 0;

    /**
     * 用于子类定义pause()方法调用时的额外处理
     */
    virtual void OnPauseRun() = 0;

    /**
     * 用于子类定义resume()方法调用时的额外处理
     */
    virtual void OnResumeRun() = 0;

    /**
     * 用于子类定义stop()方法调用时的额外处理
     */
    virtual void OnStopRun() = 0;

    void DoUpdateSyncClock(int64_t timestamp) override {
        m_current_timestamp = ((jdouble) (timestamp) / 1000);
        WakeUpState();
    }


public:
    BaseFFmpegVideoRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback,
                    int video_width, int video_height,
                    AVPixelFormat avPixelFormat,
                    AVRational codecTimeBase,
                    AVRational streamTimeBase);
    ~BaseFFmpegVideoRender();

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    virtual void SetSurface(JNIEnv *jniEnv, jobject surface) = 0;

    void Start() override;
    void Pause() override;
    void Resume() override;
    void Stop() override;
    void SeekTo(int64_t timestamp) override;
};

#endif //OPENVIDEO_FFMPEG_BASE_VIDEO_RENDER_H
