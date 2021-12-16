//
// Created by fgrid on 2021/12/16.
//

#include "base_decoder.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"

BaseDecoder::BaseDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback) {
    this->m_url = url;
    this->m_i_decoder_callback = iDecoderCallback;
    Init(env);
}

BaseDecoder::~BaseDecoder() {
    // 释放锁
    pthread_cond_destroy(&m_cond);
    pthread_mutex_destroy(&m_mutex);

    LOGE(TAG, "%s", "~BaseDecoder");
}

void BaseDecoder::Init(JNIEnv *env) {
    // 初始化条件锁
    pthread_mutex_init(&m_mutex, NULL);
    pthread_cond_init(&m_cond, NULL);

    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建解码线程
    CreateDecoderThread();
    LOGE(TAG, "%s", "Init()");
}

void BaseDecoder::CreateDecoderThread() {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseDecoder> that(this);
    std::thread th(RunDecoder, that);
    th.detach();
}

void BaseDecoder::RunDecoder(std::shared_ptr<BaseDecoder> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->OnError(NULL, OPEN_DECODER_THREAD_FAILED, "Fail to Init decode thread");
        return;
    }

    // 初始化解码器
    if (!that->InitDecoder(env)) {
        //解除线程和jvm关联
        that->m_jvm_for_thread->DetachCurrentThread();
        return;
    }
    // 开始解码
    that->LoopDecoder(env);
    // 结束解码
    that->DoneDecoder();

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();

    LOGE(that->TAG, "%s", "RunDecoder() done.");
}

bool BaseDecoder::InitDecoder(JNIEnv *env) {
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
        OnError(env, OPEN_FFMPEG_AV_CONTEXT_FAILED, "获取封装上下文失败");
        return false;
    }

    /**
     * 3.打开视频源
     */
    if (avformat_open_input(&m_format_ctx, m_url, NULL, NULL) < 0) {
        OnError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "打开音视频源失败");
        return false;
    }

    /**
     * 4.获取视频源多媒体流信息
     */
    if (avformat_find_stream_info(m_format_ctx, NULL) < 0) {
        OnError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "获取音视频信息失败");
        return false;
    }

    /**
     * 5-1.获取视频的流的索引
     */
    int idx = -1;
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == GetMediaType()) {
            idx = i;
            break;
        }
    }
    if (idx == -1) {
        OnError(env, NO_FOUND_CODEC, "未找到解码器");
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
        OnError(env, OPEN_CODEC_CONTEXT_FAILED, "打开解码器上下文失败");
        return false;
    }

    /**
     * 7.打开解码器
     */
    if (avcodec_open2(m_codec_ctx, m_codec, NULL) < 0) {
        OnError(env, OPEN_CODEC_FAILED, "打开解码器失败");
        return false;
    }

    /**
     * 8.获取总时长: duration以微秒为单位，转换为毫秒为单位
     */
    LOGE(TAG, "duration us: %lld", m_format_ctx->duration);
    m_duration = (jlong)(((jdouble) m_format_ctx->duration) / (AV_TIME_BASE / 1000));
    LOGE(TAG, "duration ms: %lld", m_duration);

    /**
     * 9.初始化待解码和解码数据结构
     */
    // 1）初始化AVPacket，存放解码前的数据
    m_packet = av_packet_alloc();
    if (m_packet == NULL) {
        OnError(env, CREATE_PACKET_ERROR, "初始化AVPacket失败");
        return false;
    }
    // 初始化初始化AVPacket结构体
    av_init_packet(m_packet);
    // 2）初始化AVFrame，存放解码后的数据
    m_frame = av_frame_alloc();
    if (m_frame == NULL) {
        OnError(env, CREATE_FRAME_ERROR, "初始化AVFrame失败");
        return false;
    }

    return true;
}

void BaseDecoder::LoopDecoder(JNIEnv *env) {
    bool prepared = true;
    pthread_mutex_lock(&m_mutex);
    if (m_state == STOPPED) {
        prepared = false;
    } else {
        m_state = PREPARED;
    }
    if (prepared) {
        OnPrepared(env);
    }
    pthread_mutex_unlock(&m_mutex);
    if (!prepared) {
        return;
    }
    while (IsRunning()) {
        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
            // || m_state == SEEKING // 设置解码器指定位置
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
            ) {
            Wait();
        }

        if (!IsRunning()) {
            break;
        }

        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
            // || m_state == SEEKING // 设置解码器指定位置
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
            ) {
            continue;
        }

        DecodeFrame(env, m_state == SEEKING);
    }
}

void BaseDecoder::DoneDecoder() {
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
    m_i_decoder_callback = NULL;
}

void BaseDecoder::DecodeFrame(JNIEnv *env, bool isSeeking) {
    int ret = av_read_frame(m_format_ctx, m_packet);
    if (ret == 0) {
        if (m_packet->stream_index == m_stream_index) {
            int ret_packet = avcodec_send_packet(m_codec_ctx, m_packet);
            switch (ret_packet) {
                case AVERROR_EOF: {
                    av_packet_unref(m_packet);

                    pthread_mutex_lock(&m_mutex);
                    if (IsRunning()) {
                        m_state = COMPLETED;
                        OnComplete(env);
                    }
                    pthread_mutex_unlock(&m_mutex);
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
                    break;
            }
            if (!IsRunning()) {
                av_packet_unref(m_packet);
                return;
            }
//            AVFrame *avFrame = av_frame_alloc();
//            if (avFrame != NULL) {
//                // 这里需要考虑一个packet有可能包含多个frame的情况
//                int result = avcodec_receive_frame(m_codec_ctx, avFrame);
//                if (!IsRunning()) {
//                    av_frame_free(&avFrame);
//                    av_packet_unref(m_packet);
//                    return;
//                }
//                if (result == 0) {
//                    int64_t timestamp = ObtainTimeStamp(m_packet, avFrame);
//                    LOGD(TAG, "decodeFrame():  #### %s", isSeeking ? "true." : "false.");
//                    if (isSeeking) {
//                        LOGD(TAG, "decodeFrame(): %lld #### %lld", m_seek_timestamp, timestamp)
//                        // 单位：毫秒
//                        int64_t abs = m_seek_timestamp - timestamp;
//                        if (!(abs > 80 || abs < -80)) {
//                            m_cur_t_s = timestamp;
//
//                            bool isOk = false;
//                            pthread_mutex_lock(&m_mutex);
//                            if (IsRunning()) {
//                                isOk = true;
//                                // 入帧队列
//                                if (m_state == SEEKING) {
//                                    m_state = PAUSED;
//                                    OnSeekComplete(env);
//                                }
//                            }
//                            pthread_mutex_unlock(&m_mutex);
//                            if (!isOk) {
//                                av_frame_free(&avFrame);
//                            }
//                        } else {
//                            av_frame_free(&avFrame);
//                        }
//                    } else {
//                        m_cur_t_s = timestamp;
//
//                        bool isOk = false;
//                        pthread_mutex_lock(&m_mutex);
//                        if (IsRunning()) {
//                            isOk = true;
//
//                            // 入帧队列
//                        }
//                        pthread_mutex_unlock(&m_mutex);
//                        if (!isOk) {
//                            av_frame_free(&avFrame);
//                        }
//                    }
//
//                } else {
//                    LOGI(TAG, "Receive frame error result: %d", av_err2str(AVERROR(result)))
//                    av_frame_free(&avFrame);
//                }
//            } else {
//                avcodec_receive_frame(m_codec_ctx, m_frame);
//                LOGI(TAG, "decodeFrame(): %s", "avcodec_receive_frame ....")
//            }

            // 这里需要考虑一个packet有可能包含多个frame的情况
            int result = avcodec_receive_frame(m_codec_ctx, m_frame);
            if (!IsRunning()) {
                av_packet_unref(m_packet);
                return;
            }
            if (result == 0) {
                int64_t timestamp = ObtainTimeStamp(m_packet, m_frame);
                LOGD(TAG, "decodeFrame():  #### %s", isSeeking ? "true." : "false.");
                if (isSeeking) {
                    LOGD(TAG, "decodeFrame(): %lld #### %lld", m_seek_timestamp, timestamp)
                    // 单位：毫秒
                    int64_t abs = m_seek_timestamp - timestamp;
                    if (!(abs > 80 || abs < -80)) {
                        m_cur_t_s = timestamp;

                        bool isOk = false;
                        pthread_mutex_lock(&m_mutex);
                        if (IsRunning()) {
                            isOk = true;
                            AVFrame *avFrame = av_frame_clone(m_frame);

                            // 入帧队列
                            if (m_state == SEEKING) {
                                m_state = PAUSED;
                                OnSeekComplete(env);
                            }
                        }
                        pthread_mutex_unlock(&m_mutex);
                    }
                } else {
                    m_cur_t_s = timestamp;

                    pthread_mutex_lock(&m_mutex);
                    if (IsRunning()) {
                        AVFrame *avFrame = av_frame_clone(m_frame);

                        // 入帧队列
                    }
                    pthread_mutex_unlock(&m_mutex);
                }

            } else {
                LOGI(TAG, "Receive frame error result: %d", av_err2str(AVERROR(result)))
            }
        }
    }
    else if (ret == AVERROR(EAGAIN) || ret == AVERROR(EINVAL) || ret == AVERROR(ENOMEM)) {
        av_usleep(10000);
    } else if(ret == AVERROR_EOF) {
        // 解码结束
        pthread_mutex_lock(&m_mutex);
        if (IsRunning()) {
            m_state = COMPLETED;
            OnComplete(env);
        }
        pthread_mutex_unlock(&m_mutex);
    }
    av_packet_unref(m_packet);
    LOGI(TAG, "ret = %d", ret);
    return;
}

int64_t BaseDecoder::ObtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame) {
    int64_t m_cur_t_s = 0;
    if (avFrame == NULL) {
        return m_cur_t_s;
    }

    if (avFrame->pts != AV_NOPTS_VALUE) {
        m_cur_t_s = avFrame->pts;
    }
    else if(avFrame->pkt_dts != AV_NOPTS_VALUE && avPacket != NULL) {
        m_cur_t_s = avPacket->dts;
    }
    else {
        m_cur_t_s = 0;
    }
    m_cur_t_s = (int64_t)(m_cur_t_s * 1000 * av_q2d(m_format_ctx->streams[m_stream_index]->time_base));
    return m_cur_t_s;
}

void BaseDecoder::Wait() {
    pthread_mutex_lock(&m_mutex);
//    if (second > 0) {
//        timeval now;
//        timespec outTime;
//        gettimeofday(&now, NULL);
//        outTime.tv_sec = now.tv_sec + second;
//        outTime.tv_nsec = now.tv_usec * 1000;
//        pthread_cond_timedwait(&m_cond, &m_mutex, &outTime);
//    } else {
//        pthread_cond_wait(&m_cond, &m_mutex);
//    }
    pthread_cond_wait(&m_cond, &m_mutex);
    pthread_mutex_unlock(&m_mutex);
}

void BaseDecoder::WakeUp() {
    pthread_mutex_lock(&m_mutex);
    pthread_cond_signal(&m_cond);
    pthread_mutex_unlock(&m_mutex);
}

bool BaseDecoder::IsRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}


State BaseDecoder::GetState() {
    return m_state;
}
void BaseDecoder::Start() {
    LOGD(TAG, "%s", "Start()");
    pthread_mutex_lock(&m_mutex);
    if (m_state != RUNNING && m_state != STOPPED) {
        m_state = RUNNING;
        pthread_cond_signal(&m_cond);
    }
    pthread_mutex_unlock(&m_mutex);
}

void BaseDecoder::Pause() {
    LOGD(TAG, "%s", "Pause()");
    pthread_mutex_lock(&m_mutex);
    if (m_state == RUNNING) {
        m_state = PAUSED;
    }
    pthread_mutex_unlock(&m_mutex);
}

void BaseDecoder::Resume() {
    LOGD(TAG, "%s", "Resume()");
    pthread_mutex_lock(&m_mutex);
    if (m_state == PAUSED) {
        m_state = RUNNING;
        pthread_cond_signal(&m_cond);
    }
    pthread_mutex_unlock(&m_mutex);
}

void BaseDecoder::Stop() {
    LOGD(TAG, "%s", "Stop()");
    pthread_mutex_lock(&m_mutex);
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    pthread_cond_signal(&m_cond);
    pthread_mutex_unlock(&m_mutex);
}

bool BaseDecoder::SeekTo(int64_t timestamp) {
    LOGD(TAG, "SeekTo() %lld", timestamp)
    bool result = false;
    pthread_mutex_lock(&m_mutex);
    State temp = m_state;
    if (temp == PAUSED || temp == RUNNING || temp == COMPLETED) {
        m_state = PAUSED;
//        if (render != NULL) {
//            render->setSeeking(true);
//        }
        if (timestamp > m_duration) {
            timestamp = m_duration;
        }
        if (temp == COMPLETED && timestamp == m_duration) {
            result = true;
            m_state = temp;
        }
        if (!result) {
            // 这里如果为0的话，会崩溃（具体原因未知！！！）
            int64_t zero = 0;
            int64_t min = 10;
            int64_t seek_target = av_rescale_q((timestamp == zero ? min : timestamp) * 1000, AV_TIME_BASE_Q, m_format_ctx->streams[m_stream_index]->time_base);
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
                m_state = SEEKING;
                result = true;
                LOGE(TAG, "seek to %lld", m_seek_timestamp)
//        if (render != NULL) {
//            render->setSeeking(true);
//        }
            } else {
                m_state = temp;
            }
            LOGE(TAG, "%s%d", "seekTo() result: ", ret)
        }
    }
    pthread_mutex_unlock(&m_mutex);
    return result;
}

jlong BaseDecoder::GetDuration() {
    return m_duration;
}

jlong BaseDecoder::GetCurrentPosition() {
    return m_cur_t_s;
}