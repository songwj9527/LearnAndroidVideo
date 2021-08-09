//
// Created by fgrid on 2/5/21.
//

#ifndef OPENVIDEO_DEFAULT_VIDEO_RENDER_H
#define OPENVIDEO_DEFAULT_VIDEO_RENDER_H

#include "../../base_render.h"
#include "../base_video_render.h"

class DefaultVideoRender : public BaseVideoRender {
private:
//    const char *TAG = "DefaultVideoRender";

    // 存放输出到屏幕的缓存数据
    ANativeWindow_Buffer m_out_buffer;


protected:

    /**
     * 新建其他扩展自定义线程
     */
    void createOtherThread(JNIEnv *env) override;

    /**
     * 渲染视图窗口
     */
    void render() override;

    /**
     * 渲染结束调用方法
     * @param env
     */
    void doneRender() override;

    /**
     * 释放Native屏幕资源
     */
    void releaseANativeWindow() override;

    /**
     * 用于子类定义start()方法调用时的额外处理
     */
    void onStartRun() override;

    /**
     * 用于子类定义pause()方法调用时的额外处理
     */
    void onPauseRun() override;

    /**
     * 用于子类定义resume()方法调用时的额外处理
     */
    void onResumeRun() override;

    /**
     * 用于子类定义stop()方法调用时的额外处理
     */
    void onStopRun() override;

public:
    DefaultVideoRender(bool for_synthesizer);
    ~DefaultVideoRender();

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    void setSurface(JNIEnv *jniEnv, jobject surface) override;
};
#endif //OPENVIDEO_DEFAULT_VIDEO_RENDER_H
