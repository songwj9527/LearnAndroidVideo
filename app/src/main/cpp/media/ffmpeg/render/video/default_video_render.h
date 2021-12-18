//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_DEFAULT_VIDEO_RENDER_H
#define OPENVIDEO_FFMPEG_DEFAULT_VIDEO_RENDER_H

#include "base_video_render.h"

class FFmpegDefaultVideoRender : public BaseFFmpegVideoRender {
private:
//    const char *TAG = "DefaultVideoRender";

    // 存放输出到屏幕的缓存数据
    ANativeWindow_Buffer m_out_buffer;


protected:
    /**
     * 初始化
     * @param env
     */
    void Init(JNIEnv *env) override;

    /**
     * 新建其他扩展自定义线程
     */
    void CreateOtherThread(JNIEnv *env) override;

    /**
     * 渲染视图窗口
     */
    void Render() override;

    /**
     * 渲染结束调用方法
     * @param env
     */
    void DoneRender() override;

    /**
     * 释放Native屏幕资源
     */
    void ReleaseANativeWindow() override;

    /**
     * 用于子类定义start()方法调用时的额外处理
     */
    void OnStartRun() override;

    /**
     * 用于子类定义pause()方法调用时的额外处理
     */
    void OnPauseRun() override;

    /**
     * 用于子类定义resume()方法调用时的额外处理
     */
    void OnResumeRun() override;

    /**
     * 用于子类定义stop()方法调用时的额外处理
     */
    void OnStopRun() override;

public:
    FFmpegDefaultVideoRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback,
                       int video_width, int video_height,
                       AVPixelFormat avPixelFormat,
                       AVRational codecTimeBase,
                       AVRational streamTimeBase);
    ~FFmpegDefaultVideoRender();

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    void SetSurface(JNIEnv *jniEnv, jobject surface) override;
};

#endif //OPENVIDEO_FFMPEG_DEFAULT_VIDEO_RENDER_H
