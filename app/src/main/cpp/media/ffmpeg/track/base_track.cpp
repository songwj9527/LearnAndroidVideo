//
// Created by fgrid on 2021/12/17.
//

#include "base_track.h"

#include "../../../utils/logger.h"
#include "../../media_codes.h"

BaseFFmpegTrack::BaseFFmpegTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback) {
    this->m_url = url;
    this->m_i_track_callback = iTrackCallback;

    // 初始化条件锁
    pthread_mutex_init(&m_state_mutex, NULL);
    pthread_cond_init(&m_state_cond, NULL);

    CreateDecoder(env);
}

BaseFFmpegTrack::~BaseFFmpegTrack() {
    m_render = NULL;
    // 释放锁
    pthread_cond_destroy(&m_state_cond);
    pthread_mutex_destroy(&m_state_mutex);

    LOGE(TAG, "%s", "~BaseFFmpegTrack");
}

void BaseFFmpegTrack::CreateDecoder(JNIEnv *env) {
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建解码线程
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseFFmpegTrack> that(this);
    std::thread th(RunDecoderThread, that);
    th.detach();
}

void BaseFFmpegTrack::RunDecoderThread(std::shared_ptr<BaseFFmpegTrack> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        if (that->m_state != STOPPED) {
            that->m_state = ERROR;
            that->OnError(NULL, OPEN_DECODER_THREAD_FAILED, "Fail to Init decode thread");
        }
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
    that->DoneDecoder(env);
    av_usleep(20*1000);

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();

    LOGE(that->TAG, "%s", "RunDecoder() done.");
}

bool BaseFFmpegTrack::InitDecoder(JNIEnv *env) {
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
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, OPEN_FFMPEG_AV_CONTEXT_FAILED, "获取封装上下文失败");
        }
        return false;
    }

    /**
     * 3.打开视频源
     */
    if (avformat_open_input(&m_format_ctx, m_url, NULL, NULL) < 0) {
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "打开音视频源失败");
        }
        return false;
    }

    /**
     * 4.获取视频源多媒体流信息
     */
    if (avformat_find_stream_info(m_format_ctx, NULL) < 0) {
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, OPEN_FFMPEG_AV_SOURCE_FAILED, "获取音视频信息失败");
        }
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
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, NO_FOUND_CODEC, "未找到解码器");
        }
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
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, OPEN_CODEC_CONTEXT_FAILED, "打开解码器上下文失败");
        }
        return false;
    }

    /**
     * 7.打开解码器
     */
    if (avcodec_open2(m_codec_ctx, m_codec, NULL) < 0) {
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, OPEN_CODEC_FAILED, "打开解码器失败");
        }
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
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, CREATE_PACKET_ERROR, "初始化AVPacket失败");
        }
        return false;
    }
    // 初始化初始化AVPacket结构体
    av_init_packet(m_packet);
    // 2）初始化AVFrame，存放解码后的数据
    m_frame = av_frame_alloc();
    if (m_frame == NULL) {
        if (m_state != STOPPED) {
            m_state = ERROR;
            OnError(env, CREATE_FRAME_ERROR, "初始化AVFrame失败");
        }
        return false;
    }

    return true;
}

void BaseFFmpegTrack::LoopDecoder(JNIEnv *env) {
    LOGE(TAG, "LoopDecoder")
    CreateRender(env);
    LOGE(TAG, "LoopDecoder(): Loop.");
    while (IsRunning()) {
        if (m_state == PREPARED
        || m_state == PAUSED
        || m_state == COMPLETED
        || isFrameEOS
        ) {
            LOGE(TAG, "LoopDecoder(): Sleep.");
            WaitState();
            LOGE(TAG, "LoopDecoder(): %s", "wake up.");
        }
        if (!IsRunning()) {
            break;
        }
        if (m_state == PREPARED
        || m_state == PAUSED
        || m_state == COMPLETED
        || isFrameEOS
        ) {
            continue;
        }
        DecodeFrame(env, m_state == SEEKING);
    }
}

void BaseFFmpegTrack::CreateRender(JNIEnv *env) {
    if (m_state == IDLE) {
        CreateTargetRender(env);
        WaitState();
    }
}

void BaseFFmpegTrack::DoneDecoder(JNIEnv * env) {
    if (m_render != NULL) {
        m_render->Stop();
        m_render = NULL;
    }
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
    m_i_track_callback = NULL;
}

void  BaseFFmpegTrack::DecodeFrame(JNIEnv *env, bool isSeeking) {
    int ret = av_read_frame(m_format_ctx, m_packet);
    if (ret == 0) {
        if (m_packet->stream_index == m_stream_index) {
            int ret_packet = avcodec_send_packet(m_codec_ctx, m_packet);
            switch (ret_packet) {
                case AVERROR_EOF: {
                    av_packet_unref(m_packet);

                    isFrameEOS = true;
                    // 解码结束
                    if (IsRunning()) {
                        if (m_render != NULL) {
                            m_render->EnableFrameEOS();
                        }
                    }
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

            bool newFrame = true;
            AVFrame *avFrame = av_frame_alloc();
            if (avFrame == NULL) {
                avFrame = m_frame;
                newFrame = false;
            }
            // 这里需要考虑一个packet有可能包含多个frame的情况
            int result = avcodec_receive_frame(m_codec_ctx, avFrame);
            if (!IsRunning()) {
                av_packet_unref(m_packet);
                if (newFrame) {
                    av_frame_free(&avFrame);
                }
                return;
            }
            if (result == 0) {
                int64_t timestamp = ObtainTimeStamp(m_packet, avFrame);
                LOGD(TAG, "decodeFrame():  %lld #### %s", timestamp, isSeeking ? "true." : "false.");
                if (isSeeking) {
                    LOGD(TAG, "decodeFrame(): %lld #### %lld", m_seek_timestamp, timestamp)
                    // 单位：毫秒
                    int64_t abs = m_seek_timestamp - timestamp;
                    if (!(abs > 80 || abs < -80)) {
                        LOGD(TAG, "decodeFrame(): SeekTo %lld", timestamp)
                        m_current = ((jdouble) timestamp / 1000);
                        if (IsRunning()) {
                            // 入帧队列
                            if (newFrame) {
                                if (m_render != NULL) {
                                    m_render->PushSeekFrame(avFrame, timestamp);
                                }
                            } else {
                                if (m_render != NULL) {
                                    m_render->PushSeekFrameNull(timestamp);
                                }
                            }
                        } else if (newFrame) {
                            av_frame_free(&avFrame);
                        }
                        if (m_state == SEEKING) {
                            m_state = PAUSED;
                            OnSeekComplete(env);
                            LOGD(TAG, "decodeFrame(): OnSeekComplete")
                        }
                    }  else if (newFrame) {
                        av_frame_free(&avFrame);
                    }
                } else {
                    if (IsRunning() && newFrame) {
                        // 入帧队列
                        if (m_render != NULL) {
                            m_render->PushFrame(avFrame);
                        }
                    } else if (newFrame) {
                        av_frame_free(&avFrame);
                    }
                }

            } else {
                LOGI(TAG, "Receive frame error result: %d", av_err2str(AVERROR(result)))
                if (newFrame) {
                    av_frame_free(&avFrame);
                }
            }
        }
    }
    else if (ret == AVERROR(EAGAIN) || ret == AVERROR(EINVAL) || ret == AVERROR(ENOMEM)) {

    } else if(ret == AVERROR_EOF) {
        isFrameEOS = true;
        // 解码结束
        if (IsRunning()) {
            if (m_render != NULL) {
                m_render->EnableFrameEOS();
            }
        }
    }
    av_packet_unref(m_packet);
    LOGI(TAG, "ret = %d", ret);
    return;
}

int64_t BaseFFmpegTrack::ObtainTimeStamp(AVPacket *avPacket, AVFrame *avFrame) {
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

void BaseFFmpegTrack::WaitState() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_wait(&m_state_cond, &m_state_mutex);
    pthread_mutex_unlock(&m_state_mutex);
}

void BaseFFmpegTrack::WakeUpState() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_signal(&m_state_cond);
    pthread_mutex_unlock(&m_state_mutex);
}

void BaseFFmpegTrack::OnRenderPrepared(JNIEnv *jniEnv, int render) {
    LOGE(TAG, "OnRenderPrepared")
    if (m_state == IDLE) {
        m_state = PREPARED;
        WakeUpState();
        OnPrepared(jniEnv);
    }
}

void BaseFFmpegTrack::OnRenderCompleted(JNIEnv *jniEnv, int render) {
    LOGE(TAG, "OnRenderCompleted")
    if (IsRunning() && m_state != COMPLETED) {
        m_state = COMPLETED;
        OnComplete(jniEnv);
    }
}

void BaseFFmpegTrack::OnRenderError(JNIEnv *jniEnv, int render, int code, const char *msg) {
    LOGE(TAG, "OnRenderError")
    m_render = NULL;
    if (m_state != STOPPED && m_state != ERROR) {
        m_state = ERROR;
        OnError(jniEnv, code, msg);
    }
}

void BaseFFmpegTrack::UpdateSyncClock(jdouble syncClock) {
    m_i_track_callback->UpdateSyncClock(syncClock);
}

jdouble BaseFFmpegTrack::GetSyncClock() {
    return m_i_track_callback->GetSyncClock();
}

bool BaseFFmpegTrack::IsRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}


State BaseFFmpegTrack::GetState() {
    return m_state;
}
void BaseFFmpegTrack::Start() {
    LOGD(TAG, "%s", "Start()");
    if (m_state == PREPARED || m_state == PAUSED) {
        LOGE(TAG, "%s", "Start(): run.");
        m_state = RUNNING;
        if (m_render != NULL) {
            m_render->Start();
        }
        LOGE(TAG, "Start(): WakeUp 0.");
        WakeUpState();
        LOGE(TAG, "Start(): WakeUp 1.");
    }
}

void BaseFFmpegTrack::Pause() {
    LOGD(TAG, "%s", "Pause()");
    if (m_state == RUNNING || m_state == PREPARED) {
        m_state = PAUSED;
        if (m_render != NULL) {
            m_render->Pause();
        }
    }
}

void BaseFFmpegTrack::Resume() {
    LOGD(TAG, "%s", "Resume()");
    if (m_state == PAUSED) {
        m_state = RUNNING;
        if (m_render != NULL) {
            m_render->Resume();
        }
        WakeUpState();
    }
}

void BaseFFmpegTrack::Stop() {
    LOGD(TAG, "%s", "Stop()");
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    WakeUpState();
    if (m_render != NULL) {
        m_render->Stop();
        m_render = NULL;
    }
}

bool BaseFFmpegTrack::SeekTo(int64_t timestamp) {
    LOGD(TAG, "SeekTo() %lld", timestamp)
    bool result = false;
    State temp = m_state;
    if (m_state == PREPARED || m_state == PAUSED || m_state == RUNNING || m_state == COMPLETED) {
        result = true;
        if (timestamp > m_duration) {
            timestamp = m_duration;
        }
        if (m_state == COMPLETED && timestamp == m_duration) {
            result = false;
        }
    }
    if (result) {
        isFrameEOS = false;
        m_state = PAUSED;
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
            if (m_render != NULL) {
                m_render->SeekTo(timestamp);
            }
        } else {
            m_state = temp;
            result = false;
        }
        WakeUpState();
    }
    return result;
}

jlong BaseFFmpegTrack::GetDuration() {
    return m_duration;
}

jlong BaseFFmpegTrack::GetCurrentTimestamp() {
    if (!IsRunning()) {
        return 0L;
    }
    if (m_render != NULL) {
        m_current = m_render->GetCurrentTimestamp();
    }
    return (jlong) (m_current * 1000);
}