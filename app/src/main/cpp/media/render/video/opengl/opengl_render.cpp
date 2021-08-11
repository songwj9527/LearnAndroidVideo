//
// Created by fgrid on 2021/7/20.
//

#include <unistd.h>
#include "opengl_render.h"
#include "../../../player/player.h"
#include "../../../decoder/video/video_decoder.h"
#include "../../../opengl/drawer/proxy/def_drawer_proxy_impl.h"

OpenGLRender::OpenGLRender(bool for_synthesizer) : BaseVideoRender(for_synthesizer) {
    TAG = "OpenGLRender";
    // 初始化播放状态线程锁变量
    pthread_mutex_init(&m_egl_mutex, NULL);
    pthread_cond_init(&m_egl_cond, NULL);

    m_drawer_proxy = new DefDrawerProxyImpl();
}

OpenGLRender::~OpenGLRender() {
    LOGE(TAG, "%s", "~OpenGLRender 0");
    releaseGLSurface();
    LOGE(TAG, "%s", "~OpenGLRender 1");
    releaseGLDrawers();
    LOGE(TAG, "%s", "~OpenGLRender 2");
    releaseANativeWindow();
    LOGE(TAG, "%s", "~OpenGLRender 3");
    pthread_cond_destroy(&m_egl_cond);
    pthread_mutex_destroy(&m_egl_mutex);
    LOGE(TAG, "%s", "~OpenGLRender 4");
}

void OpenGLRender::privatePrepared(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderPrepared(env, MODULE_CODE_OPENGL);
    }
}

void OpenGLRender::privateComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderCompleted(env, MODULE_CODE_OPENGL);
    }
}

void OpenGLRender::privateError(JNIEnv *env, int code, const char *msg) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderError(env, MODULE_CODE_OPENGL, code, msg);
    }
}

/**
 * 添加OpenGL渲染器
 * @param drawer
 */
void OpenGLRender::addDrawer(Drawer *drawer) {
    if (m_drawer_proxy != NULL) {
        m_drawer_proxy->AddDrawer(drawer);
    }
}

/**
 * 新建读取视频帧、渲染线程
 */
void OpenGLRender::createOtherThread(JNIEnv *env) {
//    // 使用智能指针，线程结束时，自动删除本类指针
//    std::shared_ptr<OpenGLRender> egl_that(this);
//    std::thread egl_thread(runOpenGLThread, egl_that);
//    egl_thread.detach();

    // 获取JVM虚拟机，为创建线程作准备
    jniEnvForOpengl = env;
    jniEnvForOpengl->GetJavaVM(&m_jvm_for_opengl_thread);
    std::thread egl_thread(runOpenGLThread, this);
    egl_thread.detach();
}

/**
 * 读取视频帧、渲染线程调用的方法
 * @param that DefaultVideoRender
 */
//void OpenGLRender::runOpenGLThread(std::shared_ptr<OpenGLRender> that) {
void OpenGLRender::runOpenGLThread(OpenGLRender *that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_opengl_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->privateError(NULL, VIDEO_OPENGL_RENDER_UNPREPARED, "Fail to init opengl render thread");
        return;
    }
    that->jniEnvForOpengl = env;
    if(!(that->isInitEGL = (that->initEGL()))) {
        //解除线程和jvm关联
        that->m_jvm_for_opengl_thread->DetachCurrentThread();
        that->privateError(NULL, VIDEO_OPENGL_RENDER_UNPREPARED, "Fail to init opengl render thread");
        return;
    }

    LOGE(that->TAG, "%s", "runOpenGLThread()");
    if (that->m_surface_ref != NULL) {
        if (JNI_TRUE != env->IsSameObject(that->m_surface_ref, NULL)) {
            that->setSurface(env, that->m_surface_ref);
        }
    }

    that->loopOpenGL(env);

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
    LOGE(that->TAG, "runOpenGLThread %s", "thread done.");
}

/**
 * 初始化EGL渲染
 */
bool OpenGLRender::initEGL() {
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
void OpenGLRender::loopOpenGL(JNIEnv *env) {
    pthread_mutex_lock(&m_egl_mutex);
    if (m_state == STOPPED || glRenderState == STOP) {
        LOGI(TAG, "loopOpenGL %d, %d", m_state, glRenderState)
        isInitEGL = false;
        return;
    }
    pthread_mutex_unlock(&m_egl_mutex);
    privatePrepared(env);
    while (true) {
        switch (glRenderState) {
            case NO_SURFACE:
                waitGL(0);
                break;
            case FRESH_SURFACE:
                LOGI(TAG, "loopOpenGL FRESH_SURFACE")
                destroyGLSurface();
                initANativeWindow(env);
                initReaderBuffer();
                initSws();
                createGLSurface();
                setOpenGLState(RENDERING);
                break;
            case SURFACE_CHANGE:
                glViewport(0, 0, m_dst_w, m_dst_h);
                m_drawer_proxy->SetDrawerSize(m_dst_w, m_dst_h);
                setOpenGLState(RENDERING);
                break;
            case RENDERING:
                renderOpenGL();
                break;
            case SURFACE_DESTROY:
                destroyGLSurface();
                setOpenGLState(NO_SURFACE);
            case STOP:
                destroyGLSurface();
                isInitEGL = false;
                return;
            default:
                break;
        }
        if (glRenderMode == RENDER_CONTINUOUSLY) {
            usleep(20000);
        }
    }
}

void OpenGLRender::renderOnFrame() {
    if (isRunning() && RENDERING == glRenderState) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT);
        m_drawer_proxy->Draw(frame_date);
        m_egl_surface->SwapBuffers();

        if (m_need_output_pixels && m_pixel_receiver != NULL) {//输出画面rgba
            m_need_output_pixels = false;
            renderOnFrame(); //再次渲染最新的画面

            size_t size = m_dst_w * m_dst_h * 4 * sizeof(uint8_t);

            uint8_t *rgb = (uint8_t *) malloc(size);
            if (rgb == NULL) {
                realloc(rgb, size);
                LOGE(TAG, "内存分配失败： %d", rgb)
            }
            glReadPixels(0, 0, m_dst_w, m_dst_h, GL_RGBA, GL_UNSIGNED_BYTE, rgb);
            m_pixel_receiver->ReceivePixel(rgb);
        }
    }
}

void OpenGLRender::renderOpenGL() {
    LOGE(TAG, "renderOpenGL")
    renderOnFrame();
    waitGL(0);
}

/**
 * 渲染结束调用方法
 * @param env
 */
void OpenGLRender::doneRender() {
    while (isInitEGL) {
        usleep(50000);
    }
}

/**
 * 设置渲染状态
 * @param state
 */
void OpenGLRender::setOpenGLState(OpenGLRenderState state) {
    LOGE(TAG, "setOpenGLState(): %d", state)
    pthread_mutex_lock(&m_egl_mutex);
    if (glRenderState != state) {
        glRenderState = state;
        pthread_cond_signal(&m_egl_cond);
    }
    pthread_mutex_unlock(&m_egl_mutex);
}

/**
 * 休眠OpenGL线程: 时间单位为毫秒（ms）
 */
void OpenGLRender::waitGL(int64_t ms) {
    pthread_mutex_lock(&m_egl_mutex);
    if (glRenderState != RENDERING) {
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
void OpenGLRender::wakeUpGL() {
    pthread_mutex_lock(&m_egl_mutex);
    pthread_cond_signal(&m_egl_cond);
    pthread_mutex_unlock(&m_egl_mutex);
}

/**
 * 设置Surface视图窗口
 * @param env
 * @param surface
 */
void OpenGLRender::setSurface(JNIEnv *jniEnv, jobject surface) {
    LOGE(TAG, "%s%s%s", "setSurface() ", surface == NULL ? "NULL" : "OK", jniEnv == NULL ? ", NULL" : ", OK");
    if (!isInitEGL) {
        return;
    }
    m_surface_ref = surface;
    if (surface != NULL && jniEnv != NULL) {
        setOpenGLState(FRESH_SURFACE);
    } else {
        setOpenGLState(SURFACE_DESTROY);
    }
}

/**
 * 创建GL窗口
 */
void OpenGLRender::createGLSurface() {
    if (m_egl_surface == NULL) {
        m_egl_surface = new EglSurface();
    }
    if (decoder != NULL && m_native_window != NULL) {
        if (m_dst_w == -1) {
            m_dst_w = ((VideoDecoder *) decoder)->getVideoWidth();
        }
        if (m_dst_h == -1) {
            m_dst_w = ((VideoDecoder *) decoder)->getVideoHeight();
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
void OpenGLRender::destroyGLSurface() {
    if (m_egl_surface != NULL) {
        m_egl_surface->DestroyEglSurface();
    }
}

/**
 * 释放GL视频渲染器资源
 */
void OpenGLRender::releaseGLDrawers() {
    if (m_drawer_proxy != NULL) {
        m_drawer_proxy->Release();
        delete m_drawer_proxy;
        m_drawer_proxy = NULL;
    }
}

/**
 * 释放GL窗口资源
 */
void OpenGLRender::releaseGLSurface() {
    if (m_egl_surface != NULL) {
        m_egl_surface->Release();
        delete m_egl_surface;
        m_egl_surface = NULL;
    }
}

/**
 * 释放视频渲染相关资源
 */
void OpenGLRender::releaseANativeWindow() {
    if (m_native_window != NULL) {
        ANativeWindow_release(m_native_window);
        m_native_window = NULL;
    }
}

/**
 * 渲染视图窗口
 */
void OpenGLRender::render() {
    LOGE(TAG, "%s", "render()");
    frame_date = m_rgb_frame->data[0];
}

void OpenGLRender::onStartRun() {
    if (isInitEGL) {
        wakeUpGL();
    }
}
void OpenGLRender::onPauseRun() {

}
void OpenGLRender::onResumeRun() {
    if (isInitEGL) {
        wakeUpGL();
    }
}
void OpenGLRender::onStopRun() {
    if (isInitEGL) {
        setOpenGLState(STOP);
    }
}

void OpenGLRender::SetPixelReceiver(OpenGLPixelReceiver *receiver) {
    m_pixel_receiver = receiver;
}

void OpenGLRender::RequestRgbaData() {
    m_need_output_pixels = true;
}