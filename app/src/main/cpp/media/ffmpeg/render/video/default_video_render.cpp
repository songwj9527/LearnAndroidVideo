//
// Created by fgrid on 2021/12/17.
//

#include "default_video_render.h"
#include "../../../../utils/logger.h"

FFmpegDefaultVideoRender::FFmpegDefaultVideoRender(
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
    TAG = "FFmpegDefaultVideoRender";
    Init(jniEnv);
}

FFmpegDefaultVideoRender::~FFmpegDefaultVideoRender() {
    ReleaseANativeWindow();
    LOGE(TAG, "%s", "~FFmpegDefaultVideoRender 1");
}

void FFmpegDefaultVideoRender::Init(JNIEnv *env) {
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建读取视频帧、渲染线程
    PrepareSyncAllThread(env);
}

void FFmpegDefaultVideoRender::CreateOtherThread(JNIEnv *env) {}

/**
 * 渲染结束调用方法
 * @param env
 */
void FFmpegDefaultVideoRender::DoneRender() {}

/**
 * 释放视频渲染相关资源
 */
void FFmpegDefaultVideoRender::ReleaseANativeWindow() {
    LOGE(TAG, "%s", "ReleaseANativeWindow() 0");
    if (m_native_window != NULL) {
        ANativeWindow_release(m_native_window);
        m_native_window = NULL;
    }
    av_free(&m_out_buffer);
    LOGE(TAG, "%s", "ReleaseANativeWindow() 1");
}


/**
 * 设置Surface视图窗口
 * @param env
 * @param surface
 */
void FFmpegDefaultVideoRender::SetSurface(JNIEnv *jniEnv, jobject surface) {
    LOGE(TAG, "%s%s%s", "setSurface() ", surface == NULL ? "NULL" : "OK", jniEnv == NULL ? ", NULL" : ", OK");
    m_surface_ref = surface;
    pthread_mutex_lock(&m_state_mutex);
    if (surface != NULL && jniEnv != NULL && IsRunning()) {
        // 初始化渲染窗口
        InitANativeWindow(jniEnv);
        // 初始化渲染数据缓存
        InitReaderBuffer();
        // 初始化格式转换工具
        InitSws();
    } else {
        ReleaseANativeWindow();
        if (m_sws_ctx != NULL) {
            sws_freeContext(m_sws_ctx);
            m_sws_ctx = NULL;
        }
        if (m_buf_for_rgb_frame != NULL) {
            free(m_buf_for_rgb_frame);
            m_buf_for_rgb_frame = NULL;
        }
    }
    pthread_mutex_unlock(&m_state_mutex);
}

/**
 * 渲染视图窗口
 */
void FFmpegDefaultVideoRender::Render() {
    if (IsRunning() && m_native_window != NULL && m_rgb_frame != NULL) {
        // 获取stride：一行可以保存的内存像素数量*4（即：rgba的位数）
        int dstStride = m_out_buffer.stride * 4;
        int srcStride = m_rgb_frame->linesize[0];
        //锁定窗口
        int lock = ANativeWindow_lock(m_native_window, &m_out_buffer, NULL);
        LOGD(TAG, "Render(): ANativeWindow_lock %d", lock)
        if (lock) {
            ReleaseANativeWindow();
            InitANativeWindow(jniEnv);
            return;
        }
        uint8_t *dst = (uint8_t *) m_out_buffer.bits;
        // 由于window的stride和帧的stride不同，因此需要逐行复制
        for (int h = 0; h < m_dst_h; h++) {
            memcpy(dst + h * dstStride, m_rgb_frame->data[0] + h * srcStride, srcStride);
        }
        //释放窗口
        ANativeWindow_unlockAndPost(m_native_window);
    }
}

void FFmpegDefaultVideoRender::OnStartRun() {}
void FFmpegDefaultVideoRender::OnPauseRun() {}
void FFmpegDefaultVideoRender::OnResumeRun() {}
void FFmpegDefaultVideoRender::OnStopRun() {}