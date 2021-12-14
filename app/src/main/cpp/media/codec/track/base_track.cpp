//
// Created by fgrid on 2021/9/18.
//

#include "base_track.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"

BaseTrack::BaseTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback) {
    this->m_source = source;
    this->m_i_track_callback = i_track_callback;

    // 初始化条件锁
    pthread_mutex_init(&m_state_mutex, NULL);
    pthread_cond_init(&m_state_cond, NULL);
    pthread_mutex_init(&m_source_mutex, NULL);

    // 获取JVM虚拟机，为创建线程作准备
    jniEnv->GetJavaVM(&m_jvm_for_thread);
    // 新建解码线程
    CreateTrackThread(jniEnv);
}

BaseTrack::~BaseTrack() {
    LOGE(TAG, "~BaseTrack()")
    // 释放锁
    pthread_cond_destroy(&m_state_cond);
    pthread_mutex_destroy(&m_state_mutex);
    pthread_mutex_destroy(&m_source_mutex);
}

void BaseTrack::CreateTrackThread(JNIEnv *env) {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseTrack> that(this);
    std::thread th(RunTrackThread, that);
    th.detach();
}

void BaseTrack::RunTrackThread(std::shared_ptr<BaseTrack> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        if (that->m_i_track_callback != NULL) {
            that->m_i_track_callback->OnTrackError(NULL, that->GetTrackType(), OPEN_MEDIA_TRACK_THREAD_FAILED, "Fail To Init Track Thread.");
        }
        return;
    }
    if (!that->Init(env)) {
        //解除线程和jvm关联
        that->m_jvm_for_thread->DetachCurrentThread();
        return;
    }

    that->LoopTrack(env);
    that->LoopDone(env);

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
}

bool BaseTrack::Init(JNIEnv *env) {
    // 初始化数据提取器
    if (!InitExtractor(env)) {
        if (m_i_track_callback != NULL) {
            m_i_track_callback->OnTrackError(env, GetTrackType(), INIT_MEDIA_EXTRACTOR_FAILED, "Fail To Init Extractor.");
        }
        return false;
    }
    m_duration = m_extractor->GetFormatDurationUs() / 1000;// 将微妙转换成毫秒

    // 初始化解码器
    if (!InitCodec(env)) {
        m_extractor->Stop();
        m_extractor = NULL;
        if (m_i_track_callback != NULL) {
            m_i_track_callback->OnTrackError(env, GetTrackType(), INIT_MEDIA_CODEC_FAILED, "Fail To Init Codec.");
        }
        return false;
    }

    // 初始化渲染器
    if (!InitRender(env)) {
        AMediaCodec_stop(m_codec);
        AMediaCodec_delete(m_codec);
        m_codec = NULL;
        m_extractor->Stop();
        delete m_extractor;
        m_extractor = NULL;
        if (m_i_track_callback != NULL) {
            m_i_track_callback->OnTrackError(env, GetTrackType(), INIT_MEDIA_RENDER_FAILED, "Fail To Init Render.");
        }
        return false;
    }

    return true;
}

bool BaseTrack::InitCodec(JNIEnv *env) {
    LOGE(TAG, "InitCodec() %s", m_extractor->GetFormatMineType())
    m_codec = AMediaCodec_createDecoderByType(m_extractor->GetFormatMineType());
    if (m_codec == NULL) {
        return false;
    }
    media_status_t status = AMediaCodec_configure(m_codec, m_extractor->GetMediaFormat(), NULL, NULL, 0);
    LOGI(TAG, "AMediaCodec_configure %d", status)
    status = AMediaCodec_start(m_codec);
    m_extractor->SeekTo(0);
    LOGI(TAG, "AMediaCodec_start %d", status)
    return true;
}

void BaseTrack::LoopTrack(JNIEnv *env) {
    m_state = PREPARED;
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnTrackPrepared(env, GetTrackType());
    }
    ssize_t out_index = -1;
    size_t out_size = -1;
    uint8_t *out_data = NULL;
    while (IsRunning()) {
        if (m_state != RUNNING && m_state != SEEKING) {
            HoldOn();
            LOGE(TAG, "LoopTrack: wake up.");
        }
        if (!IsRunning()) {
            break;
        }
        if (m_state != RUNNING && m_state != SEEKING) {
            continue;
        }

        pthread_mutex_lock(&m_source_mutex);
        /**
         * 这里可以将解码和获取解码好的数据(渲染)操作分别在独立线程中进行，效率更好：
         * decoder thread 来DequeueInputBuffer()
         * render thread 来DequeueOutputBuffer()和渲染
         */
        // 如果数据没有解码完毕，将数据推入解码器解码
        if (!m_is_end_of_stream) {
            //【见数据压入解码器输入缓冲】
            m_is_end_of_stream = DequeueInputBuffer();
            LOGE(TAG, "DequeueInputBuffer（EndOfStream）: %s", m_is_end_of_stream ? "true" : "false");
        }
        //【将解码好的数据从缓冲区拉取出来】
        out_index = DequeueOutputBuffer();
        if (out_index >= 0) {
            // 更新同步时钟
            UpdateClock();
            // 渲染
            out_data = NULL;
            out_size = 0;
            if (m_codec != NULL) {
                out_data = AMediaCodec_getOutputBuffer(m_codec, out_index, &out_size);
                LOGI(TAG, "out_size: %d, m_buffer_info.size: %d", out_size, m_buffer_info.size)
                if (out_data != NULL) {
                    Render(out_data, &m_buffer_info, out_size);
                }
            }

            //【释放输出缓冲】
            if (m_codec != NULL) {
                AMediaCodec_releaseOutputBuffer(m_codec, out_index, true);
            }
        }
        //【判断解码是否完成】
        if ((m_buffer_info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM)) {
            LOGE(TAG, "Flag: %d, %d", m_buffer_info.flags, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
            if (m_codec != NULL) {
                AMediaCodec_flush(m_codec);
            }
            m_state = COMPLETED;
            if (m_state == SEEKING) {
                if (m_i_track_callback != NULL) {
                    m_i_track_callback->OnTrackSeekCompleted(env, GetTrackType());
                }
            }
            if (m_i_track_callback != NULL) {
                m_i_track_callback->OnTrackCompleted(env, GetTrackType());
            }
        }
        //【判断是否正在seek】
        else if (out_index >= 0 && m_state == SEEKING) {
            m_state = m_state_prev;
            if (m_i_track_callback != NULL) {
                m_i_track_callback->OnTrackSeekCompleted(env, GetTrackType());
            }
        }
        pthread_mutex_unlock(&m_source_mutex);
        // 【音视频同步】
        SyncClock();
    }
}

bool BaseTrack::DequeueInputBuffer() {
    bool is_end_of_stream = false;
    if (m_codec != NULL && m_extractor != NULL) {
        ssize_t index = AMediaCodec_dequeueInputBuffer(m_codec, 10000);
        size_t input_size = 0;
        ssize_t sample_size = 0;
        int64_t sample_time = 0;
        if (index >= 0) {
            LOGE(TAG, "AMediaCodec_dequeueInputBuffer(): %d", index);
            uint8_t* input_buffer = AMediaCodec_getInputBuffer(m_codec, index, &input_size);
            if (input_buffer != NULL) {
                LOGE(TAG, "AMediaCodec_getInputBuffer(): %d", input_size);
                memset(input_buffer, 0, input_size);
                sample_size = m_extractor->ReadBuffer(input_buffer);
                LOGE(TAG, "ReadBuffer(): %d", sample_size);
                sample_time = m_extractor->GetCurrentTimestampUs();
                LOGE(TAG, "GetCurrentTimestampUs(): %d", sample_time);
                if (sample_size < 0) {
                    AMediaCodec_queueInputBuffer(m_codec, index, 0, 0, sample_time, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                    is_end_of_stream = true;
                    LOGE(TAG, "DequeueInputBuffer: true");
                } else {
                    AMediaCodec_queueInputBuffer(m_codec, index, 0, sample_size, sample_time, 0);
                }
            }
        }
    }
    return is_end_of_stream;
}

int BaseTrack::DequeueOutputBuffer() {
    // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
    ssize_t result = AMediaCodec_dequeueOutputBuffer(m_codec, &m_buffer_info, 10000);
    // AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG = 2,
    //    AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM = 4,
    //    AMEDIACODEC_BUFFER_FLAG_PARTIAL_FRAME = 8,
    //
    //    AMEDIACODEC_CONFIGURE_FLAG_ENCODE = 1,
    //    AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED = -3,
    //    AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED = -2,
    //    AMEDIACODEC_INFO_TRY_AGAIN_LATER = -1,
    LOGE(TAG, "DequeueOutputBuffer(): %d", result);
    if (AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED == result) {

    }
    else if (AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED == result) {

    }
    else if (AMEDIACODEC_INFO_TRY_AGAIN_LATER == result) {

    } else {
        return result;
    }
    return -1;
}

void BaseTrack::SyncClock() {
    if (m_state == COMPLETED) {
        return;
    }
    if (m_i_sync_clock_receiver != NULL) {
        int64_t sync_clock = m_i_sync_clock_receiver->GetSyncClock();
        int64_t frame_alive_time = RenderFrameAliveTime();
        int64_t current_time = m_buffer_info.presentationTimeUs / 1000;
        int64_t abs = (current_time + frame_alive_time) - sync_clock;
        if (abs > 0) {
            av_usleep(abs * 1000L); // 单位：微秒
        }
    }

}

void BaseTrack::LoopDone(JNIEnv *env) {
    if (m_codec != NULL) {
        AMediaCodec_stop(m_codec);
        AMediaCodec_delete(m_codec);
        m_codec = NULL;
    }
    if (m_extractor != NULL) {
        m_extractor->Stop();
        delete m_extractor;
        m_extractor = NULL;
    }
    LoopDoneVirtual(env);
}

void BaseTrack::HoldOn() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_wait(&m_state_cond, &m_state_mutex);
    pthread_mutex_unlock(&m_state_mutex);
}

void BaseTrack::WakeUp() {
    pthread_mutex_lock(&m_state_mutex);
    pthread_cond_signal(&m_state_cond);
    pthread_mutex_unlock(&m_state_mutex);
}

//    bool  SeekTo(int64_t timestamp) override;
void BaseTrack::Start() {
    if (m_state != RUNNING && m_state != STOPPED) {
        LOGE(TAG, "%s", "Start()");
        State prev = m_state;
        m_state = RUNNING;
        WakeUp();
        StartVirtual(prev);
    }
}

void BaseTrack::Pause() {
    if (m_state == RUNNING) {
        LOGE(TAG, "%s", "Pause()");
        State prev = m_state;
        m_state = PAUSED;
        PauseVirtual(prev);
    }
}

void BaseTrack::Resume() {
    if (m_state == PAUSED) {
        LOGE(TAG, "%s", "Resume()");
        State prev = m_state;
        m_state = RUNNING;
        WakeUp();
        ResumeVirtual(prev);
    }
}

void BaseTrack::Stop() {
    LOGE(TAG, "%s", "Stop()");
    State prev = m_state;
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    StopVirtual(prev);
    WakeUp();
}

void BaseTrack::Reset() {
    if (m_state == PAUSED || m_state == COMPLETED) {
        State prev = m_state;
        m_state = PREPARED;
        AMediaCodec_flush(m_codec);
        m_is_end_of_stream = false;
        m_buffer_info.flags = AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG;
        m_buffer_info.presentationTimeUs = 0L;
//        m_extractor->SeekTo(0L);
        if (m_i_sync_clock_receiver != NULL) {
            m_i_sync_clock_receiver->SetSyncClock(0L);
        }
        ResetVirtual(prev);
        WakeUp();
    }
}

jlong BaseTrack::GetDuration() {
    return m_duration;
}

jlong BaseTrack::GetCurrentTimestamp() {
    if (m_state == COMPLETED) {
        return m_duration;
    }
    // 将微妙转换成毫秒
    return m_buffer_info.presentationTimeUs / 1000;
}

bool BaseTrack::SeekTo(int64_t timestamp) {
    if (IsRunning() && m_state != SEEKING && m_codec != NULL && m_extractor != NULL) {
        m_state_prev = m_state;
        pthread_mutex_lock(&m_source_mutex);
        m_state = PAUSED;
        m_extractor->SeekTo(timestamp);
        AMediaCodec_flush(m_codec);
        m_buffer_info.presentationTimeUs = timestamp * 1000;
        m_state = SEEKING;
        if (m_i_sync_clock_receiver != NULL) {
            m_i_sync_clock_receiver->SetSyncClock(timestamp);
        }
        pthread_mutex_unlock(&m_source_mutex);
        WakeUp();
    }
}