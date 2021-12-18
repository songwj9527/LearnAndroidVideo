//
// Created by fgrid on 2021/12/18.
//

#include <unistd.h>
#include "opengl_video_render.h"
#include "../../../opengl/drawer/proxy/def_drawer_proxy_impl.h"
#include "../../../media_codes.h"

FFmpegOpenGLVideoRender::FFmpegOpenGLVideoRender(
        JNIEnv *jniEnv,
        IFFmpegRenderCallback *iRenderCallback,
        int video_width,
        int video_height,
        AVPixelFormat avPixelFormat,
        AVRational codecTimeBase,
        AVRational streamTimeBase)
        : BaseFFmpegVideoRender(jniEnv,
                                iRenderCallback,
                                video_width,
                                video_height,
                                avPixelFormat,
                                codecTimeBase,
                                streamTimeBase) {
    TAG = "FFmpegOpenGLVideoRender";
    // 初始化播放状态线程锁变量
    pthread_mutex_init(&m_egl_mutex, NULL);
    pthread_cond_init(&m_egl_cond, NULL);

    m_drawer_proxy = new DefDrawerProxyImpl();
    
    Init(jniEnv);
}

FFmpegOpenGLVideoRender::~FFmpegOpenGLVideoRender() {
    ReleaseGLSurface();
    ReleaseGLDrawers();
    ReleaseANativeWindow();
    pthread_cond_destroy(&m_egl_cond);
    pthread_mutex_destroy(&m_egl_mutex);
    LOGE(TAG, "~FFmpegOpenGLVideoRender");
}

void FFmpegOpenGLVideoRender::Init(JNIEnv *env) {
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建读取视频帧、渲染线程
    PrepareSyncAllThread(env);
}

void FFmpegOpenGLVideoRender::PrivatePrepared(JNIEnv *env) {
    if (m_state != STOPPED && m_i_render_callback != NULL) {
        m_i_render_callback->OnRenderPrepared(env, MODULE_CODE_OPENGL);
    }
}

void FFmpegOpenGLVideoRender::PrivateComplete(JNIEnv *env) {
    if (m_state != STOPPED && m_i_render_callback != NULL) {
        m_i_render_callback->OnRenderCompleted(env, MODULE_CODE_OPENGL);
    }
}

void FFmpegOpenGLVideoRender::PrivateError(JNIEnv *env, int code, const char *msg) {
    if (m_state != ERROR && m_state != STOPPED && m_i_render_callback != NULL) {
        m_i_render_callback->OnRenderError(env, MODULE_CODE_OPENGL, code, msg);
    }
}

/**
 * 添加OpenGL渲染器
 * @param drawer
 */
void FFmpegOpenGLVideoRender::AddDrawer(Drawer *drawer) {
    if (m_state != ERROR && m_state != STOPPED && m_drawer_proxy != NULL) {
        m_drawer_proxy->AddDrawer(drawer);
    }
}

/**
 * 新建读取视频帧、渲染线程
 */
void FFmpegOpenGLVideoRender::CreateOtherThread(JNIEnv *env) {
//    // 使用智能指针，线程结束时，自动删除本类指针
//    std::shared_ptr<FFmpegOpenGLVideoRender> egl_that(this);
//    std::thread egl_thread(runOpenGLThread, egl_that);
//    egl_thread.detach();

    // 获取JVM虚拟机，为创建线程作准备
    jniEnvForOpengl = env;
    jniEnvForOpengl->GetJavaVM(&m_jvm_for_opengl_thread);
    std::thread egl_thread(RunOpenGLThread, this);
    egl_thread.detach();
}

/**
 * 读取视频帧、渲染线程调用的方法
 * @param that DefaultVideoRender
 */
//void FFmpegOpenGLVideoRender::runOpenGLThread(std::shared_ptr<FFmpegOpenGLVideoRender> that) {
void FFmpegOpenGLVideoRender::RunOpenGLThread(FFmpegOpenGLVideoRender *that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_opengl_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->PrivateError(NULL, VIDEO_OPENGL_RENDER_UNPREPARED, "Fail to init opengl render thread");
        return;
    }
    that->jniEnvForOpengl = env;
    if(!(that->isInitEGL = (that->InitEGL()))) {
        //解除线程和jvm关联
        that->m_jvm_for_opengl_thread->DetachCurrentThread();
        that->PrivateError(NULL, VIDEO_OPENGL_RENDER_UNPREPARED, "Fail to init opengl render thread");
        return;
    }

    LOGE(that->TAG, "%s", "RunOpenGLThread()");
    if (that->m_surface_ref != NULL) {
        if (JNI_TRUE != env->IsSameObject(that->m_surface_ref, NULL)) {
            that->SetSurface(env, that->m_surface_ref);
        }
    }

    that->LoopOpenGL(env);

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
    LOGE(that->TAG, "runOpenGLThread %s", "thread done.");
}

/**
 * 初始化EGL渲染
 */
bool FFmpegOpenGLVideoRender::InitEGL() {
    if (m_egl_surface == NULL) {
        m_egl_surface = new EglSurface();
        bool result = m_egl_surface->Init();;

        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        //------开启混合，即半透明---------
        // 开启很混合模式
        glEnable(GL_BLEND);
        // 配置混合算法
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        m_drawer_proxy->UpdateTextureIds();

        return result;
    }
    return false;
}

/**
 * 开始OpenGL渲染业务
 */
void FFmpegOpenGLVideoRender::LoopOpenGL(JNIEnv *env) {
    pthread_mutex_lock(&m_egl_mutex);
    if (m_state == ERROR || m_state == STOPPED  || eglRenderState == STOP) {
        LOGI(TAG, "LoopOpenGL %d, %d", m_state, eglRenderState)
        isInitEGL = false;
        return;
    }
    pthread_mutex_unlock(&m_egl_mutex);
    PrivatePrepared(env);
    LOGE(TAG, "LoopOpenGL(): Loop.");
    while (true) {
        LOGE(TAG, "LoopOpenGL(): state %d", eglRenderState);
        switch (eglRenderState) {
            case NO_SURFACE:
                WaitGL(0);
                break;
            case FRESH_SURFACE:
                LOGI(TAG, "loopOpenGL FRESH_SURFACE")
                DestroyGLSurface();
                InitANativeWindow(env);
                InitReaderBuffer();
                InitSws();
                CreateGLSurface();
                SetEGLRenderState(RENDERING);
                break;
            case SURFACE_CHANGE:
                glViewport(0, 0, m_dst_w, m_dst_h);
                m_drawer_proxy->SetDrawerSize(m_dst_w, m_dst_h);
                SetEGLRenderState(RENDERING);
                break;
            case RENDERING:
                RenderOpenGL();
                break;
            case SURFACE_DESTROY:
                DestroyGLSurface();
                SetEGLRenderState(NO_SURFACE);
                break;
            case STOP:
                DestroyGLSurface();
                isInitEGL = false;
                return;
            default:
                break;
        }
        if (eglRenderMode == RENDER_CONTINUOUSLY) {
            usleep(20000);
        }
    }
}

void FFmpegOpenGLVideoRender::RenderOnFrame() {
    if (IsRunning() && RENDERING == eglRenderState) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);
        m_drawer_proxy->Draw(frame_date);
        m_egl_surface->SwapBuffers();

//        if (m_need_output_pixels && m_pixel_receiver != NULL) {//输出画面rgba
//            m_need_output_pixels = false;
//            RenderOnFrame(); //再次渲染最新的画面
//
//            size_t size = m_dst_w * m_dst_h * 4 * sizeof(uint8_t);
//
//            uint8_t *rgb = (uint8_t *) malloc(size);
//            if (rgb == NULL) {
//                realloc(rgb, size);
//                LOGE(TAG, "内存分配失败： %d", rgb)
//            }
//            glReadPixels(0, 0, m_dst_w, m_dst_h, GL_RGBA, GL_UNSIGNED_BYTE, rgb);
//            m_pixel_receiver->ReceivePixel(rgb);
//        }
    }
}

void FFmpegOpenGLVideoRender::RenderOpenGL() {
    LOGE(TAG, "RenderOpenGL")
    RenderOnFrame();
    WaitGL(0);
}

/**
 * 渲染结束调用方法
 * @param env
 */
void FFmpegOpenGLVideoRender::DoneRender() {
    while (isInitEGL) {
        usleep(50000);
    }
}

/**
 * 设置渲染状态
 * @param state
 */
void FFmpegOpenGLVideoRender::SetEGLRenderState(EGLRenderState state) {
    LOGD(TAG, "SetEGLRenderState(): %d", state)
    pthread_mutex_lock(&m_egl_mutex);
    if (eglRenderState != state) {
        eglRenderState = state;
        pthread_cond_signal(&m_egl_cond);
    }
    pthread_mutex_unlock(&m_egl_mutex);
}

/**
 * 休眠OpenGL线程: 时间单位为毫秒（ms）
 */
void FFmpegOpenGLVideoRender::WaitGL(int64_t ms) {
    pthread_mutex_lock(&m_egl_mutex);
    if (eglRenderState != RENDERING) {
        if (ms > 0) {
            struct timeval now;
            //在now基础上，增加ms毫秒
            struct timespec out_time;

            // 获取当前时间戳
            gettimeofday(&now, NULL);
            out_time.tv_sec = static_cast<__kernel_time_t>(now.tv_sec + ms / 1000);
            // us的值有可能超过1秒
            uint64_t us = now.tv_usec + 1000 * (ms % 1000);
            out_time.tv_sec += static_cast<__kernel_time_t>(us / 1000000);
            // 设置纳秒
            us = us % 1000000;
            out_time.tv_nsec = static_cast<long>(us * 1000);

            pthread_cond_timedwait(&m_egl_cond, &m_egl_mutex, &out_time);
        } else {
            pthread_cond_wait(&m_egl_cond, &m_egl_mutex);
        }
    }
    pthread_mutex_unlock(&m_egl_mutex);
}

/**
 * 唤醒OpenGL线程
 */
void FFmpegOpenGLVideoRender::WakeUpGL() {
    pthread_mutex_lock(&m_egl_mutex);
    pthread_cond_signal(&m_egl_cond);
    pthread_mutex_unlock(&m_egl_mutex);
}

/**
 * 设置Surface视图窗口
 * @param env
 * @param surface
 */
void FFmpegOpenGLVideoRender::SetSurface(JNIEnv *jniEnv, jobject surface) {
    LOGE(TAG, "setSurface() %s%s", surface == NULL ? "NULL" : "OK", jniEnv == NULL ? ", NULL" : ", OK");
    if (!isInitEGL) {
        return;
    }
    m_surface_ref = surface;
    if (surface != NULL && jniEnv != NULL) {
        SetEGLRenderState(FRESH_SURFACE);
    } else {
        SetEGLRenderState(SURFACE_DESTROY);
    }
}

/**
 * 创建GL窗口
 */
void FFmpegOpenGLVideoRender::CreateGLSurface() {
    if (m_egl_surface == NULL) {
        m_egl_surface = new EglSurface();
    }
    if (IsRunning() && m_native_window != NULL) {
        if (m_dst_w == -1) {
            m_dst_w = video_width;
        }
        if (m_dst_h == -1) {
            m_dst_w = video_height;
        }
        // 绘制区域的宽高
        int windowWidth = ANativeWindow_getWidth(m_native_window);
        int windowHeight = ANativeWindow_getHeight(m_native_window);

        m_egl_surface->CreateEglSurface(m_native_window, windowWidth, windowHeight);

        glViewport(0, 0, windowWidth, windowHeight);
        m_drawer_proxy->SetDrawerSize(windowWidth, windowHeight);
    }
}

/**
 * 销毁GL窗口
 */
void FFmpegOpenGLVideoRender::DestroyGLSurface() {
    if (m_egl_surface != NULL) {
        m_egl_surface->DestroyEglSurface();
    }
}

/**
 * 释放GL视频渲染器资源
 */
void FFmpegOpenGLVideoRender::ReleaseGLDrawers() {
    if (m_drawer_proxy != NULL) {
        m_drawer_proxy->Release();
        delete m_drawer_proxy;
        m_drawer_proxy = NULL;
    }
}

/**
 * 释放GL窗口资源
 */
void FFmpegOpenGLVideoRender::ReleaseGLSurface() {
    if (m_egl_surface != NULL) {
        m_egl_surface->Release();
        delete m_egl_surface;
        m_egl_surface = NULL;
    }
}

/**
 * 释放视频渲染相关资源
 */
void FFmpegOpenGLVideoRender::ReleaseANativeWindow() {
    if (m_native_window != NULL) {
        ANativeWindow_release(m_native_window);
        m_native_window = NULL;
    }
}

/**
 * 渲染视图窗口
 */
void FFmpegOpenGLVideoRender::Render() {
    LOGD(TAG, "Set EGL Render Frame");
    frame_date = m_rgb_frame->data[0];
}

void FFmpegOpenGLVideoRender::OnStartRun() {
    LOGD(TAG, "OnStartRun()");
    if (isInitEGL) {
        WakeUpGL();
    }
}
void FFmpegOpenGLVideoRender::OnPauseRun() {

}
void FFmpegOpenGLVideoRender::OnResumeRun() {
    LOGD(TAG, "OnResumeRun()");
    if (isInitEGL) {
        WakeUpGL();
    }
}
void FFmpegOpenGLVideoRender::OnStopRun() {
    LOGD(TAG, "OnStopRun()");
    if (isInitEGL) {
        SetEGLRenderState(STOP);
    }
}