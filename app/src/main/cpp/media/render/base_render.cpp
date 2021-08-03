//
// Created by fgrid on 2/19/21.
//

#include "./base_render.h"
#include "../decoder/base_decoder.h"
#include "../player/player.h"
#include "../../utils/logger.h"

BaseRender::BaseRender() {
    // 初始化解码后的帧缓存数据队列
    this->m_decode_frame_queue = std::queue<AVFrame *>();
    // 初始化解码后的帧缓存数据队列锁变量
    pthread_mutex_init(&m_decode_frame_mutex, NULL);
    pthread_cond_init(&m_decode_frame_cond, NULL);
    for (int i = 0; i < m_max_cache_queue_size; i++) {
        // 2）初始化AVFrame，存放解码后的数据
        AVFrame *frame = av_frame_alloc();
        if (frame != NULL) {
            m_decode_frame_queue.push(frame);
        }
    }

    // 初始化待渲染的帧缓存数据队列
    this->m_render_frame_queue = std::queue<CacheFrame *>();
    // 初始化待渲染的帧缓存数据队列锁变量
    pthread_mutex_init(&m_render_frame_mutex, NULL);
    pthread_cond_init(&m_render_frame_cond, NULL);

    // 初始化播放状态线程锁变量
    pthread_mutex_init(&m_state_mutex, NULL);
    pthread_cond_init(&m_state_cond, NULL);
}

BaseRender::~BaseRender() {
    LOGE(TAG, "%s", "~BaseRender 0");
    mediaPlayer = NULL;
    decoder = NULL;

    LOGE(TAG, "%s", "~BaseRender 1");
    /**
     * 释放缓存资源
     */
    renderFrameClear();
    LOGE(TAG, "%s", "~BaseRender 2");
    decodeFrameClear();
    LOGE(TAG, "%s", "~BaseRender 3");

    /**
    * 释放锁
    */
    pthread_cond_destroy(&m_render_frame_cond);
    pthread_mutex_destroy(&m_render_frame_mutex);
    pthread_cond_destroy(&m_decode_frame_cond);
    pthread_mutex_destroy(&m_decode_frame_mutex);
    pthread_cond_destroy(&m_state_cond);
    pthread_mutex_destroy(&m_state_mutex);

    LOGE(TAG, "%s", "~BaseRender 4");
}

/**
 * 获取待解包队列头元素
 * @param cacheFrame
 */
AVFrame * BaseRender::decodeFrameFont() {
    LOGE(TAG, "%s", "decodeFrameFont");
    AVFrame *frame = NULL;
    pthread_mutex_lock(&m_decode_frame_mutex);
    if (!m_decode_frame_queue.empty()) {
        frame = m_decode_frame_queue.front();
    }
    pthread_mutex_unlock(&m_decode_frame_mutex);
    return frame;
}

/**
 * 获取一个待解包赋值的AVFrame，给解码器解包赋值
 * @param cacheFrame
 */
AVFrame * BaseRender::decodeFramePop() {
    LOGE(TAG, "%s", "decodeFramePop");
    AVFrame *frame = NULL;
    pthread_mutex_lock(&m_decode_frame_mutex);
    if (!m_decode_frame_queue.empty()) {
        frame = m_decode_frame_queue.front();
        m_decode_frame_queue.pop();
    }
    pthread_mutex_unlock(&m_decode_frame_mutex);
    return frame;
}

/**
 * 将一个待解包赋值的AVFrame入列，等待解码器给其赋值
 * @param cacheFrame
 */
void BaseRender::decodeFramePush(AVFrame *avFrame) {
    if (avFrame == NULL) {
        return;
    }
    LOGE(TAG, "%s", "decodeFramePush");
    pthread_mutex_lock(&m_decode_frame_mutex);
    m_decode_frame_queue.push(avFrame);
    pthread_mutex_unlock(&m_decode_frame_mutex);
}

/**
 * 发送唤醒待解包赋值的帧缓存的休眠状态
 */
void BaseRender::sendDecodeFrameSignal() {
    pthread_mutex_lock(&m_decode_frame_mutex);
    pthread_cond_signal(&m_decode_frame_cond);
    pthread_mutex_unlock(&m_decode_frame_mutex);
}

/**
 * 清空待解包赋值的帧缓存数据队列
 */
void BaseRender::decodeFrameClear() {
    AVFrame *frame = NULL;
    pthread_mutex_lock(&m_decode_frame_mutex);
    while (!m_decode_frame_queue.empty()) {
        LOGE(TAG, "decode frame queue %d.", m_decode_frame_queue.size());
        frame = m_decode_frame_queue.front();
        m_decode_frame_queue.pop();
        if (frame != NULL) {
            LOGE(TAG, "decodeFrameClear.");
            av_frame_free(&frame);
            frame = NULL;
        }
    }
    pthread_mutex_unlock(&m_decode_frame_mutex);
}

/**
 * 是否待解包赋值的帧缓存为空
 * @return
 */
bool BaseRender::isDecodeFrameQueueEmpty() {
    return m_decode_frame_queue.empty();
}

/**
 * 是否待解包赋值的帧缓存达到最大限制数
 * @return
 */
bool BaseRender::isDecodeFrameQueueMax() {
    return (m_decode_frame_queue.size() >= m_max_cache_queue_size);
}

/**
 * 将一个待渲染的AVFrame入列，等待渲染器渲染
 * @param cacheFrame
 */
void BaseRender::renderFramePush(CacheFrame * cacheFrame) {
    LOGE(TAG, "%s", "renderFramePush");
    if (cacheFrame == NULL) {
        return;
    }
    pthread_mutex_lock(&m_render_frame_mutex);
    m_render_frame_queue.push(cacheFrame);
    pthread_cond_signal(&m_render_frame_cond);
    pthread_mutex_unlock(&m_render_frame_mutex);
}

/**
 * 获取一个待渲染的AVFrame，给渲染器渲染
 * @param cacheFrame
 */
CacheFrame * BaseRender::renderFramePop() {
    CacheFrame *frame = NULL;
    while (m_state == RUNNING) {
        pthread_mutex_lock(&m_render_frame_mutex);
        if (!m_render_frame_queue.empty()) {
            // 取成功了  弹出队列  销毁frame
            frame = m_render_frame_queue.front();
            if (frame != NULL) {
                m_render_frame_queue.pop();
                pthread_mutex_unlock(&m_render_frame_mutex);
                break;
            } else {
                pthread_mutex_unlock(&m_render_frame_mutex);
            }
        } else {
            // 如果队列里面没有数据的话  一直等待阻塞
            pthread_cond_wait(&m_render_frame_cond, &m_render_frame_mutex);
            pthread_mutex_unlock(&m_render_frame_mutex);
        }
    }
    return frame;
}

/**
 * 获取待渲染队列头元素
 */
CacheFrame * BaseRender::renderFrameFont() {
    CacheFrame *frame = NULL;
    if (m_state == RUNNING) {
        pthread_mutex_lock(&m_render_frame_mutex);
        if (!m_render_frame_queue.empty()) {
            // 取成功了  弹出队列  销毁frame
            frame = m_render_frame_queue.front();
        }
        pthread_mutex_unlock(&m_render_frame_mutex);
    }
    return frame;
}

/**
 * 进入等待
 * @param second
 */
void BaseRender::waitRenderFrame(long second) {
    pthread_mutex_lock(&m_state_mutex);
    if (second > 0) {
        timeval now;
        timespec outTime;
        gettimeofday(&now, NULL);
        outTime.tv_sec = now.tv_sec + second;
        outTime.tv_nsec = now.tv_usec * 1000;
        pthread_cond_timedwait(&m_state_cond, &m_state_mutex, &outTime);
    } else {
        pthread_cond_wait(&m_state_cond, &m_state_mutex);
    }
    pthread_mutex_unlock(&m_state_mutex);
}

/**
 * 发送唤醒待解包赋值的帧缓存的休眠状态
 */
void BaseRender::sendRenderFrameSignal() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_signal(&m_state_cond);
    pthread_mutex_unlock(&m_state_mutex);
}

/**
 * 清空待渲染的帧缓存数据队列
 */
void BaseRender::renderFrameClear() {
    pthread_mutex_lock(&m_render_frame_mutex);
    pthread_cond_signal(&m_render_frame_cond);
    pthread_mutex_unlock(&m_render_frame_mutex);
    CacheFrame *frame = NULL;
    pthread_mutex_lock(&m_render_frame_mutex);
    while (!m_render_frame_queue.empty()) {
        LOGE(TAG, "render frame queue %d.", m_render_frame_queue.size());
        frame = m_render_frame_queue.front();
        m_render_frame_queue.pop();
        if (frame != NULL) {
            if (frame->m_frame != NULL) {
                decodeFramePush(frame->m_frame);
            }
            delete frame;
        }
    }
    pthread_mutex_unlock(&m_render_frame_mutex);
}

/**
 * 是否待渲染的帧缓存达到最大限制数
 * @return
 */
bool BaseRender::isRenderFrameQueueMax() {
    return (m_render_frame_queue.size() >= m_max_cache_queue_size);
}

/**
 * 获取当前播放时间
 * @return
 */
jlong BaseRender::getCurrentPosition() {
    return (jlong) (current_render_clock * 1000);
//    return (jlong) current_render_clock;
}

/**
 * 正在指定位置播放
 */
void BaseRender::setSeeking(bool enable) {
    LOGE(TAG, "seekTo() %s.", enable ? "true" : "false");
    if (enable) {
        if (m_state != STOPPED && m_state != SEEKING) {
            m_state_prev = m_state;
            m_state = SEEKING;
        }
    } else if(m_state == SEEKING) {
        if (m_state_prev == COMPLETED) {
            // 是否从播放完成到可以播放状态（用于声音播放，如果播放完成后需要重新执行播放方法）
            isCompletedToReset = true;
            m_state = PAUSED;
        } else {
            m_state = m_state_prev;
        }
    } else {
        m_state_prev = m_state;
    }
    sendRenderFrameSignal();
}

/**
 * 设置指定的播放位置
 * @param position
 */
void BaseRender::seekTo(jlong position) {
    current_render_clock = ((double) position) / 1000;
    if (mediaPlayer != NULL) {
        mediaPlayer->setSyncClock(current_render_clock);
    }
}