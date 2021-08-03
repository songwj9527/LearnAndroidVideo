//
// Created by fgrid on 2/5/21.
//
#include "default_video_render.h"
#include "../../../decoder/video/video_decoder.h"

DefaultVideoRender::DefaultVideoRender() : BaseVideoRender() {
    TAG = "DefaultVideoRender";
}

DefaultVideoRender::~DefaultVideoRender() {
    LOGE(TAG, "%s", "~DefaultVideoRender 0");
    releaseANativeWindow();
    LOGE(TAG, "%s", "~DefaultVideoRender 1");
}

/**
 * 新建其他扩展自定义线程
 */
void DefaultVideoRender::createOtherThread(JNIEnv *env) {}

/**
 * 渲染结束调用方法
 * @param env
 */
void DefaultVideoRender::doneRender() {}

/**
 * 释放视频渲染相关资源
 */
void DefaultVideoRender::releaseANativeWindow() {
    if (m_native_window != NULL) {
        ANativeWindow_release(m_native_window);
        m_native_window = NULL;
    }
    av_free(&m_out_buffer);
}


/**
 * 设置Surface视图窗口
 * @param env
 * @param surface
 */
void DefaultVideoRender::setSurface(JNIEnv *jniEnv, jobject surface) {
    LOGE(TAG, "%s%s%s", "setSurface() ", surface == NULL ? "NULL" : "OK", jniEnv == NULL ? ", NULL" : ", OK");
    m_surface_ref = surface;
    pthread_mutex_lock(&m_state_mutex);
    if (surface != NULL && jniEnv != NULL) {
        // 初始化渲染窗口
        initANativeWindow(jniEnv);
        // 初始化渲染数据缓存
        initReaderBuffer();
        // 初始化格式转换工具
        initSws();
    } else {
        releaseANativeWindow();
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
void DefaultVideoRender::render() {
    if (isRunning() && m_native_window != NULL && m_rgb_frame != NULL) {
        //锁定窗口
        ANativeWindow_lock(m_native_window, &m_out_buffer, NULL);
        uint8_t *dst = (uint8_t *) m_out_buffer.bits;
        // 获取stride：一行可以保存的内存像素数量*4（即：rgba的位数）
        int dstStride = m_out_buffer.stride * 4;
        int srcStride = m_rgb_frame->linesize[0];

        // 由于window的stride和帧的stride不同，因此需要逐行复制
        for (int h = 0; h < m_dst_h; h++) {
            memcpy(dst + h * dstStride, m_rgb_frame->data[0] + h * srcStride, srcStride);
        }
        //释放窗口
        ANativeWindow_unlockAndPost(m_native_window);
    }
}

void DefaultVideoRender::onStartRun() {}
void DefaultVideoRender::onPauseRun() {}
void DefaultVideoRender::onResumeRun() {}
void DefaultVideoRender::onStopRun() {}