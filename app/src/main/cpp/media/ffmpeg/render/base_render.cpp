//
// Created by fgrid on 2021/12/16.
//

#include "base_render.h"
#include "../../../utils/logger.h"

BaseFFmpegRender::BaseFFmpegRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback) {
    this->m_i_render_callback = iRenderCallback;

    // 初始化条件锁
    pthread_mutex_init(&m_frame_mutex, NULL);
    pthread_cond_init(&m_frame_cond, NULL);
    // 初始化条件锁
    pthread_mutex_init(&m_state_mutex, NULL);
    pthread_cond_init(&m_state_cond, NULL);
}

BaseFFmpegRender::~BaseFFmpegRender() {
    // 清空帧缓存
    ClearFrameQueue();

    // 释放锁
    pthread_cond_destroy(&m_frame_cond);
    pthread_mutex_destroy(&m_frame_mutex);
    // 释放锁
    pthread_cond_destroy(&m_state_cond);
    pthread_mutex_destroy(&m_state_mutex);
    LOGE(TAG, "%s", "~BaseFFmpegRender");
}

AVFrame * BaseFFmpegRender::PopFrame() {
    AVFrame *avFrame = NULL;
    while (IsRunning()) {
        avFrame = NULL;
        pthread_mutex_lock(&m_frame_mutex);
        LOGE(TAG, "PopFrame(): queue size %d", m_frame_queue.size())
        if (m_frame_queue.empty()) {
            pthread_cond_wait(&m_frame_cond, &m_frame_mutex);
        } else {
            avFrame = m_frame_queue.front();
            m_frame_queue.pop();
        }
        pthread_mutex_unlock(&m_frame_mutex);
        if (avFrame != NULL) {
            break;
        }
    }
    return avFrame;
}

void BaseFFmpegRender::ClearFrameQueue() {
    // 清空帧缓存
    pthread_mutex_lock(&m_frame_mutex);
    while (!m_frame_queue.empty()) {
        AVFrame *avFrame = m_frame_queue.front();
        m_frame_queue.pop();
        if (avFrame != NULL) {
            av_frame_free(&avFrame);
            avFrame = NULL;
        }
    }
    pthread_mutex_unlock(&m_frame_mutex);
}

void BaseFFmpegRender::PushFrame(AVFrame *avFrame) {
    if (IsRunning()) {
        pthread_mutex_lock(&m_frame_mutex);
        LOGE(TAG, "PushFrame(): queue size %d", m_frame_queue.size())
        if (avFrame != NULL) {
            m_frame_queue.push(avFrame);
            pthread_cond_signal(&m_frame_cond);
        }
        pthread_mutex_unlock(&m_frame_mutex);
    }
}

void BaseFFmpegRender::PushSeekFrame(AVFrame *avFrame, int64_t timestamp) {
    if (IsRunning()) {
        pthread_mutex_lock(&m_frame_mutex);
        if (avFrame != NULL) {
            while (!m_frame_queue.empty()) {
                AVFrame *old = m_frame_queue.front();
                m_frame_queue.pop();
                if (old != NULL) {
                    av_frame_free(&old);
                }
            }
            m_frame_queue.push(avFrame);
            pthread_cond_signal(&m_frame_cond);
        }
        pthread_mutex_unlock(&m_frame_mutex);
        DoUpdateSyncClock(timestamp);
    }
}

void BaseFFmpegRender::PushSeekFrameNull(int64_t timestamp) {
    if (IsRunning()) {
        pthread_mutex_lock(&m_frame_mutex);
        while (!m_frame_queue.empty()) {
            AVFrame *old = m_frame_queue.front();
            m_frame_queue.pop();
            if (old != NULL) {
                av_frame_free(&old);
            }
        }
        pthread_cond_signal(&m_frame_cond);
        pthread_mutex_unlock(&m_frame_mutex);
        DoUpdateSyncClock(timestamp);
    }
}

void BaseFFmpegRender::WakeUpFrameQueue() {
    pthread_mutex_lock(&m_frame_mutex);
    pthread_cond_signal(&m_frame_cond);
    pthread_mutex_unlock(&m_frame_mutex);
}

bool BaseFFmpegRender::IsRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}

void BaseFFmpegRender::WaitState() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_wait(&m_state_cond, &m_state_mutex);
    pthread_mutex_unlock(&m_state_mutex);
}

void BaseFFmpegRender::WakeUpState() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_signal(&m_state_cond);
    pthread_mutex_unlock(&m_state_mutex);
}

jdouble BaseFFmpegRender::GetCurrentTimestamp() {
    return m_current_timestamp;
}