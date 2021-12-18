//
// Created by fgrid on 2021/12/18.
//

#ifndef OPENVIDEO_FFMPEG_OPENGL_VIDEO_RENDER_H
#define OPENVIDEO_FFMPEG_OPENGL_VIDEO_RENDER_H

#include "base_video_render.h"
#include "../../../opengl/egl/egl_surface.h"
#include "../../../opengl/drawer/proxy/drawer_proxy.h"

/**
 * 渲染状态
 */
enum EGLRenderState {
    NO_SURFACE, //没有有效的surface
    FRESH_SURFACE, //持有一个未初始化的新的surface
    SURFACE_CHANGE, //surface尺寸变化
    RENDERING, //初始化完毕，可以开始渲染
    SURFACE_DESTROY, //surface销毁
    STOP //停止绘制
};
/**
 * 渲染模式
 */
enum EGLRenderMode {
    RENDER_CONTINUOUSLY,
    RENDER_WHEN_DIRTY
};

class FFmpegOpenGLVideoRender : public BaseFFmpegVideoRender {
private:
//    const char *TAG = "OpenGLRender";

    // EGL显示表面
    EglSurface *m_egl_surface = NULL;
    // 绘制代理器
    DrawerProxy *m_drawer_proxy = NULL;
    //是否已经新建过EGL上下文，用于判断是否需要生产新的纹理ID
    bool isNeverCreateEglContext = true;

    // -------------------定义线程相关-----------------------------
    JNIEnv *jniEnvForOpengl;
    // 线程依附的JVM环境
    JavaVM *m_jvm_for_opengl_thread = NULL;
    // 是否EGL已经初始化成功
    bool isInitEGL = false;

    // opengl状态线程锁变量
    pthread_mutex_t m_egl_mutex;
    pthread_cond_t m_egl_cond;
    // opengl渲染状态
    volatile EGLRenderState eglRenderState = NO_SURFACE;
    // opengl渲染模式
    EGLRenderMode eglRenderMode = RENDER_WHEN_DIRTY;

    // 渲染帧数据
    uint8_t *frame_date = NULL;


    /**
     * 初始化EGL渲染
     */
    bool InitEGL();

    /**
     * 创建GL窗口
     */
    void CreateGLSurface();
    /**
     * 销毁GL窗口
     */
    void DestroyGLSurface();

//    /**
//     * OpenGL渲染线程调用的方法
//     * @param that FFmpegOpenGLVideoRender
//     */
//    static void RunOpenGLThread(std::shared_ptr<FFmpegOpenGLVideoRender> that);
    /**
     * OpenGL渲染线程调用的方法
     * @param that DefaultVideoRender
     */
    static void RunOpenGLThread(FFmpegOpenGLVideoRender *that);

    /**
     * 开始OpenGL渲染业务
     */
    void LoopOpenGL(JNIEnv *env);

    /**
     * 渲染OpenGL画面
     */
    void RenderOnFrame();

    /**
     * 渲染OpenGL画面
     */
    void RenderOpenGL();

    /**
     * 设置渲染状态
     * @param state
     */
    void SetEGLRenderState(EGLRenderState state);

    /**
     * 休眠OpenGL线程: 时间单位为毫秒（ms）
     */
    void WaitGL(int64_t ms);

    /**
     * 唤醒OpenGL线程
     */
    void WakeUpGL();

    /**
     * 释放GL视频渲染器资源
     */
    void ReleaseGLDrawers();

    /**
     * 释放GL窗口资源
     */
    void ReleaseGLSurface();

    void PrivatePrepared(JNIEnv *env);
    void PrivateError(JNIEnv *env, int code, const char *msg);
    void PrivateComplete(JNIEnv *env);

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
    FFmpegOpenGLVideoRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback,
                            int video_width, int video_height,
                            AVPixelFormat avPixelFormat,
                            AVRational codecTimeBase,
                            AVRational streamTimeBase);
    ~FFmpegOpenGLVideoRender();

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    void SetSurface(JNIEnv *jniEnv, jobject surface) override;

    /**
     * 添加OpenGL渲染器
     * @param drawer
     */
    void AddDrawer(Drawer *drawer);
};

#endif //OPENVIDEO_FFMPEG_OPENGL_VIDEO_RENDER_H
