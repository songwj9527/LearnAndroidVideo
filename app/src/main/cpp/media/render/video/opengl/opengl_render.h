//
// Created by fgrid on 2021/7/20.
//

#ifndef OPENVIDEO_OPENGL_RENDER_H
#define OPENVIDEO_OPENGL_RENDER_H

#include "../base_video_render.h"
#include "opengl_pixel_receiver.h"
#include "../../../opengl/drawer/proxy/drawer_proxy.h"
#include "../../../opengl/egl/egl_surface.h"

/**
 * 渲染状态
 */
enum OpenGLRenderState {
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
enum OpenGLRenderMode {
    RENDER_CONTINUOUSLY,
    RENDER_WHEN_DIRTY
};

class OpenGLRender : public BaseVideoRender {
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
    volatile bool isInitEGL = false;

    // opengl状态线程锁变量
    pthread_mutex_t m_egl_mutex;
    pthread_cond_t m_egl_cond;
    // opengl渲染状态
    volatile OpenGLRenderState glRenderState = NO_SURFACE;
    // opengl渲染模式
    OpenGLRenderMode glRenderMode = RENDER_WHEN_DIRTY;

    // 渲染帧数据
    uint8_t *frame_date = NULL;

    // 输出屏幕数据
    volatile bool m_need_output_pixels = false;
    OpenGLPixelReceiver * m_pixel_receiver = NULL;

    /**
     * 初始化EGL渲染
     */
    bool initEGL();

    /**
     * 创建GL窗口
     */
    void createGLSurface();
    /**
     * 销毁GL窗口
     */
    void destroyGLSurface();

//    /**
//     * OpenGL渲染线程调用的方法
//     * @param that OpenGLRender
//     */
//    static void runOpenGLThread(std::shared_ptr<OpenGLRender> that);
    /**
     * OpenGL渲染线程调用的方法
     * @param that DefaultVideoRender
     */
    static void runOpenGLThread(OpenGLRender *that);

    /**
     * 开始OpenGL渲染业务
     */
    void loopOpenGL(JNIEnv *env);

    /**
     * 渲染OpenGL画面
     */
    void renderOnFrame();

    /**
     * 渲染OpenGL画面
     */
    void renderOpenGL();

    /**
     * 设置渲染状态
     * @param state
     */
    void setOpenGLState(OpenGLRenderState state);

    /**
     * 休眠OpenGL线程: 时间单位为毫秒（ms）
     */
    void waitGL(int64_t ms);

    /**
     * 唤醒OpenGL线程
     */
    void wakeUpGL();

    /**
     * 释放GL视频渲染器资源
     */
    void releaseGLDrawers();

    /**
     * 释放GL窗口资源
     */
    void releaseGLSurface();

    void privatePrepared(JNIEnv *env);
    void privateError(JNIEnv *env, int code, const char *msg);
    void privateComplete(JNIEnv *env);

protected:
    /**
     * 在该扩展方法中添加OpenGL独立线程
     */
    void createOtherThread(JNIEnv *env) override;

    /**
     * 渲染结束调用方法
     * @param env
     */
    void doneRender() override;
    /**
     * 渲染视图窗口
     */
    void render() override;

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
    OpenGLRender(bool for_synthesizer);
    ~OpenGLRender();

    /**
     * 添加OpenGL渲染器
     * @param drawer
     */
    void addDrawer(Drawer *drawer);

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    void setSurface(JNIEnv *jniEnv, jobject surface) override;

    /**
     * 设置屏幕rgba数据接收者
     * @param receiver
     */
    void SetPixelReceiver(OpenGLPixelReceiver *receiver);

    /**
     * 请求获取屏幕rgba数据
     */
    void RequestRgbaData();
};

#endif //OPENVIDEO_OPENGL_RENDER_H
