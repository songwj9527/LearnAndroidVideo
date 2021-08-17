//
// Created by fgrid on 2/1/21.
//
#include "base_decoder.h"
#include "../player/player.h"
#include "../render/base_render.h"

/*****************************************************************
 * 自定义解码器基类：BaseDecoder
 ****************************************************************/
BaseDecoder::BaseDecoder(JNIEnv *env, Player *mediaPlayer, const char *url, BaseRender *render, bool for_synthesizer) {
    this->mediaPlayer = mediaPlayer;
    this->render = render;
    this->m_for_synthesizer = for_synthesizer;
    this->m_url = url;
    init(env);
}

BaseDecoder::~BaseDecoder() {
    LOGE(TAG, "%s", "~BaseDecoder 0");
    mediaPlayer = NULL;
    render = NULL;

    LOGE(TAG, "%s", "~BaseDecoder 1");
    /**
     * 释放ffmpeg相关资源
     */
    doneDecode();
    LOGE(TAG, "%s", "~BaseDecoder 2");
    /**
     * 释放锁
     */
    pthread_cond_destroy(&m_cond);
    pthread_mutex_destroy(&m_mutex);
    LOGE(TAG, "%s", "~BaseDecoder 3");
}

/**
 * 初始化
 * @param env
 */
void BaseDecoder::init(JNIEnv *env) {
    // 初始化条件锁
    pthread_mutex_init(&m_mutex, NULL);
    pthread_cond_init(&m_cond, NULL);
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建解码线程
    createDecodeThread();
    LOGE(TAG, "%s", "init()");
}

/**
 * 新建解码线程
 */
void BaseDecoder::createDecodeThread() {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseDecoder> that(this);
    std::thread th(runDecode, that);
    th.detach();
}

/**
 * 解码线程调用的解码方法
 * @param that
 */
void BaseDecoder::runDecode(std::shared_ptr<BaseDecoder> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->onError(NULL, OPEN_DECODER_THREAD_FAILED, "Fail to Init decode thread");
        return;
    }

    // 初始化解码器
    if (!that->initDecoder(env)) {
        //解除线程和jvm关联
        that->m_jvm_for_thread->DetachCurrentThread();
        return;
    }
    LOGE(that->TAG, "%s", "runDecode()");

    // 开始解码
    that->loopDecode(env);

    // 结束解码
    that->doneDecode();

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();

    LOGE(that->TAG, "%s", "runDecode() done.");
}

/**
 * 初始化解码器
 * @param env
 * @return
 */
bool BaseDecoder::initDecoder(JNIEnv *env) {
    /**
     * 1.注册组件：基于ffmpeg的应用程序中，几乎都是第一个被调用的。
     * 只有调用了该函数，才能使用复用器，编码器才能起作用，必须调用此函数。
     */
    av_register_all();
    avformat_network_init(); // 播放在线视频需要

    /**
     * 2.获取封装上下文: 结束后续释放
     */
    m_format_ctx = avformat_alloc_context();
    if (m_format_ctx == NULL) {
        onError(env, OPEN_FFMPEG_AV_CONTEXT_FAILED, "获取封装上下文失败");
        doneDecode();
        return false;
    }

    /**
     * 3.打开视频源
     */
    if (avformat_open_input(&m_format_ctx, m_url, NULL, NULL) < 0) {
        onError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "打开音视频源失败");
        doneDecode();
        return false;
    }

    /**
     * 4.获取视频源多媒体流信息
     */
    if (avformat_find_stream_info(m_format_ctx, NULL) < 0) {
        onError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "获取音视频信息失败");
        doneDecode();
        return false;
    }

    /**
     * 5-1.获取视频的流的索引
     */
    int idx = -1;
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == getMediaType()) {
            idx = i;
            break;
        }
    }
    if (idx == -1) {
        onError(env, NO_FOUND_CODEC, "未找到解码器");
        doneDecode();
        return false;
    }
    m_stream_index = idx;

    /**
         * 5-2.查找编解码器: 获取解码器参数
         */
    AVCodecParameters *codecPar = m_format_ctx->streams[idx]->codecpar;

    /**
     * 5-3.查找编解码器: 获取解码器
     */
    if (m_codec == NULL) {
        m_codec = avcodec_find_decoder(codecPar->codec_id);
    }

    /**
     * 6.获取解码器上下文
     */
    m_codec_ctx = avcodec_alloc_context3(m_codec);
    if (avcodec_parameters_to_context(m_codec_ctx, codecPar) != 0) {
        onError(env, OPEN_CODEC_CONTEXT_FAILED, "打开解码器上下文失败");
        doneDecode();
        return false;
    }

    /**
     * 7.打开解码器
     */
    if (avcodec_open2(m_codec_ctx, m_codec, NULL) < 0) {
        onError(env, OPEN_CODEC_FAILED, "打开解码器失败");
        doneDecode();
        return false;
    }

    /**
     * 8.获取总时长: duration以微秒为单位，转换为毫秒为单位
     */
    LOGE(TAG, "duration: %lld", m_format_ctx->duration);
    m_duration = (jlong)(((jdouble) m_format_ctx->duration) / (AV_TIME_BASE / 1000));
    LOGE(TAG, "duration: %lld", m_duration);

    /**
     * 9.初始化待解码和解码数据结构
     */
    // 1）初始化AVPacket，存放解码前的数据
    m_packet = av_packet_alloc();
    if (m_packet == NULL) {
        onError(env, CREATE_PACKET_ERROR, "初始化AVPacket失败");
        doneDecode();
        return false;
    }
    // 初始化初始化AVPacket结构体
    av_init_packet(m_packet);
    // 2）初始化AVFrame，存放解码后的数据
    m_frame = av_frame_alloc();
    if (m_frame == NULL) {
        onError(env, CREATE_FRAME_ERROR, "初始化AVFrame失败");
        doneDecode();
        return false;
    }

    return true;
}

/**
 * 开始循环解码
 */
void BaseDecoder::loopDecode(JNIEnv *env) {
    pthread_mutex_lock(&m_mutex);
    if (m_state == STOPPED) {
        return;
    } else {
        m_state = PREPARED;
    }
    pthread_mutex_unlock(&m_mutex);
    onPrepared(env);
    if (render != NULL) {
        LOGE(TAG, "loopDecode: %s", "render.prepareSync()");
        render->prepareSync(env, mediaPlayer, this);
    } else {
        LOGE(TAG, "loopDecode: %s", "render NULL");
    }
    while (isRunning()) {
        bool isDecodeQueueEmpty = false, isRenderQueueMax = false;
        if (render != NULL) {
            isDecodeQueueEmpty = render->isDecodeFrameQueueEmpty();
            isRenderQueueMax = render->isRenderFrameQueueMax();
        }
        LOGE(TAG, "isRenderQueueMax: %d", isRenderQueueMax);
        if (isDecodeQueueEmpty || isRenderQueueMax
            || m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
//            || m_state == SEEKING // 设置解码器指定位置
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
                ) {
            wait(0);
            LOGE(TAG, "loopDecode: %s", "wake up.");
        }
        LOGE(TAG, "isRunning(): %d", m_state);
        if (!isRunning()) {
            break;
        }
        isDecodeQueueEmpty = false;
        isRenderQueueMax = false;
        if (render != NULL) {
            isDecodeQueueEmpty = render->isDecodeFrameQueueEmpty();
            isRenderQueueMax = render->isRenderFrameQueueMax();
        }
        LOGE(TAG, "isRenderQueueMax: %d", isRenderQueueMax);
        if (isDecodeQueueEmpty || isRenderQueueMax
            || m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
//            || m_state == SEEKING // 设置解码器指定位置
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
                ) {
            continue;
        }
        LOGE(TAG, "loopDecode: %s", "decodeFrame().");
        decodeFrame(env, m_state == SEEKING);
    }
}

/**
 * 判断解码线程是否可以继续解码
 * @return
 */
bool BaseDecoder::isRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}

/**
 * 进入等待
 * @param second
 */
void BaseDecoder::wait(long second) {
    pthread_mutex_lock(&m_mutex);
    if (second > 0) {
        timeval now;
        timespec outTime;
        gettimeofday(&now, NULL);
        outTime.tv_sec = now.tv_sec + second;
        outTime.tv_nsec = now.tv_usec * 1000;
        pthread_cond_timedwait(&m_cond, &m_mutex, &outTime);
    } else {
        pthread_cond_wait(&m_cond, &m_mutex);
    }
    pthread_mutex_unlock(&m_mutex);
}

/**
 * 发送条件锁信息
 */
void BaseDecoder::sendSignal() {
    pthread_mutex_lock(&m_mutex);
    pthread_cond_signal(&m_cond);
    pthread_mutex_unlock(&m_mutex);
}

/**
 * 解码一帧数据
 * @return
 */
void BaseDecoder::decodeFrame(JNIEnv *env, bool isSeeking) {
    int ret = av_read_frame(m_format_ctx, m_packet);
    LOGI(TAG, "decodeFrame(): av_read_frame %d", ret)
    if (ret == 0) {
        LOGI(TAG, "decodeFrame(): %d <--> %d", m_packet->stream_index, m_stream_index)
        if (m_packet->stream_index == m_stream_index) {
            LOGI(TAG, "decodeFrame(): %s", "avcodec_send_packet")
            int ret_packet = avcodec_send_packet(m_codec_ctx, m_packet);
            LOGI(TAG, "decodeFrame(): avcodec_send_packet %d", ret_packet)
            switch (ret_packet) {
                case AVERROR_EOF: {
                    av_packet_unref(m_packet);
                    m_state = COMPLETED;
                    onComplete(env);
                    return; //解码结束
                }
                case AVERROR(EAGAIN):
                    LOGE(TAG, "Decode error: %s", av_err2str(AVERROR(EAGAIN)));
                    break;
                case AVERROR(EINVAL):
                    LOGE(TAG, "Decode error: %s", av_err2str(AVERROR(EINVAL)));
                    break;
                case AVERROR(ENOMEM):
                    LOGE(TAG, "Decode error: %s", av_err2str(AVERROR(ENOMEM)));
                    break;
                default:
//                        if (m_state == SEEKING) {
//                            m_state = PAUSED;
//                        }
                    break;
            }
            if (!isRunning()) {
                av_packet_unref(m_packet);
                return;
            }
            AVFrame *avFrame = NULL;
            if (render != NULL) {
                avFrame = render->decodeFramePop();
                if (!isRunning()) {
                    if (avFrame != NULL) {
                        av_frame_free(&avFrame);
                    }
                    av_packet_unref(m_packet);
                    return;
                }
            }
            if (avFrame != NULL) {
                LOGI(TAG, "decodeFrame(): %s", "avcodec_receive_frame")
                //TODO 这里需要考虑一个packet有可能包含多个frame的情况
                int result = avcodec_receive_frame(m_codec_ctx, avFrame);
                LOGI(TAG, "decodeFrame(): %s%d", "avcodec_receive_frame ", result)
                if (!isRunning()) {
                    av_frame_free(&avFrame);
                    av_packet_unref(m_packet);
                    return;
                }
                if (result == 0) {
                    int64_t timestamp = obtainTimeStamp(m_packet, avFrame);
                    LOGE(TAG, "decodeFrame():  #### %s", isSeeking ? "true." : "false.");
                    if (isSeeking) {
                        LOGI(TAG, "decodeFrame(): %lld #### %lld", m_seek_timestamp, timestamp)
                        // 单位：毫秒
                        int64_t abs = m_seek_timestamp - timestamp;
                        if (!(abs > 80 || abs < -80)) {
                            m_cur_t_s = timestamp;
                            if (render != NULL) {
                                render->renderFramePush(new RenderFrame(avFrame, timestamp));
                            } else {
                                av_frame_free(&avFrame);
                            }
                            if (!isRunning()) {
                                av_packet_unref(m_packet);
                                return;
                            }
                            if (m_state == SEEKING) {
                                m_state = PAUSED;
                                onSeekComplete(env);
                            }
                        } else if (render != NULL) {
                            render->decodeFramePush(avFrame);
                        } else {
                            av_frame_free(&avFrame);
                        }
                    } else {
                        m_cur_t_s = timestamp;
                        if (render != NULL) {
                            render->renderFramePush(new RenderFrame(avFrame, timestamp));
                        } else {
                            av_frame_free(&avFrame);
                        }
                    }

                } else {
                    LOGI(TAG, "Receive frame error result: %d", av_err2str(AVERROR(result)))
                    if (render != NULL) {
                        render->decodeFramePush(avFrame);
                    } else {
                        av_frame_free(&avFrame);
                    }
                }
            } else {
                avcodec_receive_frame(m_codec_ctx, m_frame);
                LOGI(TAG, "decodeFrame(): %s", "avcodec_receive_frame ....")
            }
        }
    }
    else if (ret == AVERROR(EAGAIN) || ret == AVERROR(EINVAL) || ret == AVERROR(ENOMEM)) {

    } else if(ret == AVERROR_EOF) {
        av_packet_unref(m_packet);
        m_state = COMPLETED;
        onComplete(env);
        return; //解码结束
    }
    av_packet_unref(m_packet);

    LOGI(TAG, "ret = %d", ret);
    return;
}

/**
 * 获取当前帧时间戳（单位：毫秒）
 */
int64_t BaseDecoder::obtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame) {
    int64_t m_cur_t_s = 0;
    if (avFrame == NULL) {
        return m_cur_t_s;
    }
    if(avFrame->pkt_dts != AV_NOPTS_VALUE && avPacket != NULL) {
        m_cur_t_s = avPacket->dts;
    } else if (avFrame->pts != AV_NOPTS_VALUE) {
        m_cur_t_s = avFrame->pts;
    } else {
        m_cur_t_s = 0;
    }
    m_cur_t_s = (int64_t)(m_cur_t_s * 1000 * av_q2d(m_format_ctx->streams[m_stream_index]->time_base));
    return m_cur_t_s;
}

/**
 * 解码结束调用方法
 * @param env
 */
void BaseDecoder::doneDecode() {
    // 释放缓存
    if (m_packet != NULL) {
        av_packet_free(&m_packet);
//        delete m_packet;
        m_packet = NULL;
    }
    if (m_frame != NULL) {
        av_frame_free(&m_frame);
//        delete m_packet;
        m_frame = NULL;
    }
    // 关闭解码器
    if (m_codec_ctx != NULL) {
        avcodec_close(m_codec_ctx);
        avcodec_free_context(&m_codec_ctx);
//        delete m_codec_ctx;
        m_codec_ctx = NULL;
    }
    // 关闭输入流
    if (m_format_ctx != NULL) {
        avformat_close_input(&m_format_ctx);
        avformat_free_context(m_format_ctx);
//        delete m_format_ctx;
        m_format_ctx = NULL;
    }
    // 释放url
    m_url = NULL;

    mediaPlayer = NULL;
    render = NULL;
}

/**
 * 给外部调用唤醒解码线程
 */
void BaseDecoder::wake() {
    if (m_state == PAUSED) {
        m_state = RUNNING;
    }
    sendSignal();
}

/**
 * 获取当前解码器状态
 * @return
 */
State BaseDecoder::getState() {
    return m_state;
}

/**
 * 开始解码
 */
void BaseDecoder::start() {
    if (m_state != RUNNING && m_state != STOPPED) {
        m_state = RUNNING;
        LOGE(TAG, "%s", "start()");
        sendSignal();
    }
}

/**
 * 暂停解码
 */
void BaseDecoder::pause() {
    if (m_state == RUNNING) {
        m_state = PAUSED;
        LOGE(TAG, "%s", "pause()");
    }
}

/**
 * 继续解码
 */
void BaseDecoder::resume() {
    if (m_state == PAUSED) {
        m_state = RUNNING;
        LOGE(TAG, "%s", "resume()");
        sendSignal();
    }
}

/**
 * 停止解码
 */
void BaseDecoder::stop() {
    LOGE(TAG, "%s", "stop()");
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    sendSignal();
}

/**
 * 音视频总时长
 * @return
 */
jlong BaseDecoder::getDuration() {
    return m_duration;
}

/**
 * 当前解码位置
 * @return
 */
jlong BaseDecoder::getCurrentPosition() {
    return m_cur_t_s;
}

/**
 * 指定位置解码
 * @param timestamp 毫秒
 */
bool BaseDecoder::seekTo(jlong timestamp) {
    LOGE(TAG, "seekTo() %lld", timestamp)
    State temp = m_state;
    if (temp == PAUSED || temp == RUNNING || temp == COMPLETED) {
        m_state = SEEKING;
        if (render != NULL) {
            render->setSeeking(true);
        }
        if (timestamp > m_duration) {
            timestamp = m_duration;
        }
        // 这里如果为0的话，会崩溃（具体原因未知！！！）
        int64_t zero = 0;
        int64_t min = 10;
        int64_t seek_target = av_rescale_q((timestamp == zero ? min : timestamp) * 1000, AV_TIME_BASE_Q, m_format_ctx->streams[m_stream_index]->time_base);
        if (temp == COMPLETED) {
            if (timestamp < m_duration) {
                int ret = av_seek_frame(m_format_ctx, m_stream_index, seek_target, AVSEEK_FLAG_BACKWARD);
                if (ret >= 0) {
                    avcodec_flush_buffers(m_codec_ctx);
                } else {
                    ret = av_seek_frame(m_format_ctx, m_stream_index, seek_target, AVSEEK_FLAG_ANY);
                    if (ret >= 0) {
                        avcodec_flush_buffers(m_codec_ctx);
                    }
                }
                if (ret >= 0) {
                    m_seek_timestamp = timestamp;
                    LOGE(TAG, "seek to %lld", m_seek_timestamp)
                    if (render != NULL) {
                        render->renderFrameClear();
                        render->seekTo(timestamp);
                        render->setSeeking(false);
                    }
                } else {
                    m_state = PAUSED;
                }
                LOGE(TAG, "%s%d", "seekTo() result: ", ret)
//                m_state = PAUSED;
                sendSignal();
                return ret >= 0;
            }
            m_state = PAUSED;
            sendSignal();
            return true;
        }
        int ret = av_seek_frame(m_format_ctx, m_stream_index, seek_target, AVSEEK_FLAG_BACKWARD);
        if (ret >= 0) {
            avcodec_flush_buffers(m_codec_ctx);
        } else {
            ret = av_seek_frame(m_format_ctx, m_stream_index, seek_target, AVSEEK_FLAG_ANY);
            if (ret >= 0) {
                avcodec_flush_buffers(m_codec_ctx);
            }
        }
        if (ret >= 0) {
            m_seek_timestamp = timestamp;
            LOGE(TAG, "seek to %lld", m_seek_timestamp)
            if (render != NULL) {
                render->renderFrameClear();
                render->seekTo(timestamp);
                render->setSeeking(false);
            }
        } else {
            m_state = PAUSED;
        }
        LOGE(TAG, "%s%d", "seekTo() result: ", ret)
//        m_state = PAUSED;
        sendSignal();
        return ret >= 0;
    }
    return false;
}

/**
 * 是否已解码结束
 * @return
 */
bool BaseDecoder::isCompleted() {
    return m_state == COMPLETED;
}