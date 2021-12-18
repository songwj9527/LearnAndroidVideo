//
// Created by fgrid on 2021/12/17.
//

#include "opensl_render.h"
#include "../../../../utils/logger.h"
#include "../../../media_codes.h"

FFmpegOpenSLRender::FFmpegOpenSLRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback,
                           AVCodecContext *codecContext,
                           AVRational streamTimeBase): FFmpegAudioRender(jniEnv, iRenderCallback, codecContext, streamTimeBase) {
    TAG = "FFmpegOpenSLRender";
    // 初始化播放状态线程锁变量
    pthread_mutex_init(&m_command_mutex, NULL);
    pthread_cond_init(&m_command_cond, NULL);

    Init(jniEnv);
}

FFmpegOpenSLRender::~FFmpegOpenSLRender(){
    ReleaseOpenSL();
    ReleaseSwr();
    pthread_cond_destroy(&m_command_cond);
    pthread_mutex_destroy(&m_command_mutex);
    LOGE(TAG, "%s", "~FFmpegOpenSLRender");
}

/**
 * 初始化
 * @param env 
 */
void FFmpegOpenSLRender::Init(JNIEnv *env) {
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);

    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<FFmpegOpenSLRender> that(this);
    std::thread th(RunPrepareThread, that);
//    std::thread th(runPrepare, this);
    th.detach();
}

/**
 * 初始化线程调用方法
 * @param that
 */
void FFmpegOpenSLRender::RunPrepareThread(std::shared_ptr<FFmpegOpenSLRender> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        if (that->m_state != STOPPED && that->m_i_render_callback != NULL) {
            that->m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO,VIDEO_RENDER_UNPREPARED, "Fail to init default opensl render thread.");
        }
        return;
    }
    that->jniEnv = env;
    if (that->PrepareOpenSL(env)) {
        int command = 0;
        while(that->IsRunning()) {
            command = that->m_command;
            if (command == 0) {
                that->WaitCommand();
            }
            if (command == 1) {
                that->m_command = 0;
                that->RestartOpenSL(env);
            } else if (command == 2) {
                av_usleep(50*1000);
                break;
            }
        }
        av_usleep(30*1000);
    }
    
    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();

    LOGE(that->TAG, "%s", "RunRender() done.");
}

/**
 * 初始化方法
 */
bool FFmpegOpenSLRender::PrepareOpenSL(JNIEnv *env) {
    if (!InitSwr(env)) {
        if (m_state != STOPPED && m_i_render_callback != NULL) {
            m_state = ERROR;
            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, AUDIO_RENDER_UNPREPARED, "Fail to init swr.");
        }
        return false;
    }
    if (!CreateEngine(env)) {
        if (m_state != STOPPED && m_i_render_callback != NULL) {
            m_state = ERROR;
            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, AUDIO_RENDER_UNPREPARED, "Fail to create engine.");
        }
        return false;
    }
    if (!CreateOutputMixer(env)) {
        if (m_state != STOPPED && m_i_render_callback != NULL) {
            m_state = ERROR;
            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, AUDIO_RENDER_UNPREPARED, "Fail to create output mixer.");
        }
        return false;
    }
    if (!ConfigPlayer(env)) {
        if (m_state != STOPPED && m_i_render_callback != NULL) {
            m_state = ERROR;
            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, AUDIO_RENDER_UNPREPARED, "Fail to config player.");
        }
        return false;
    }
    StartOpenSL(env);
    return true;
}

/**
 * 创建OpenSL ES引擎
 * @return
 */
bool FFmpegOpenSLRender::CreateEngine(JNIEnv *env) {
    SLresult result = slCreateEngine(&m_engine_obj, 0, NULL, 0, NULL, NULL);
    if (CheckError(result, "Engine")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, CREATE_ENGINE_OBJECT_FAILED, "创建OpenSLES引擎engineObject失败");
//        }
        return false;
    }

    result = (*m_engine_obj)->Realize(m_engine_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Engine Realize")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, REALIZE_ENGINE_OBJECT_FAILED, "实现OpenSLES引擎engineObject失败");
//        }
        return false;
    }

    result = (*m_engine_obj)->GetInterface(m_engine_obj, SL_IID_ENGINE, &m_engine);
    if (CheckError(result, "Engine Interface")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, GET_ENGINE_OBJECT_INTERFACE_FAILED, "获取OpenSLES引擎接口engineEngine失败");
//        }
        return false;
    }
    return true;
}

/**
 * 创建OpenSL ES音频输出混音器
 * @return
 */
bool FFmpegOpenSLRender::CreateOutputMixer(JNIEnv *env) {
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    SLresult result = (*m_engine)->CreateOutputMix(m_engine, &m_output_mix_obj, 1, mids, mreq);
    if (CheckError(result, "Output Mix")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, CREATE_ENGINE_OUTPUT_MIX_FAILED, "创建OpenSLES混音器outputMixObject失败");
//        }
        return false;
    }

    result = (*m_output_mix_obj)->Realize(m_output_mix_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Output Mix Realize")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, REALIZE_ENGINE_OUTPUT_MIX_FAILED, "实现OpenSLES混音器outputMixObject失败");
//        }
        return false;
    }

    result = (*m_output_mix_obj)->GetInterface(m_output_mix_obj, SL_IID_ENVIRONMENTALREVERB, &m_output_mix_evn_reverb);
    if (CheckError(result, "Output Mix Env Reverb")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, GET_ENGINE_ENGINE_OUTPUT_MIX_INTERFACE_FAILED, "获取OpenSLES混音器接口outputMixEnv失败");
//        }
        return false;
    }

    if (result == SL_RESULT_SUCCESS) {
        (*m_output_mix_evn_reverb)->SetEnvironmentalReverbProperties(m_output_mix_evn_reverb, &m_reverb_settings);
    }
    return true;
}

/**
 * 配置OpenSL ES音频播放器
 * @return
 */
bool FFmpegOpenSLRender::ConfigPlayer(JNIEnv *env) {
    //配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, SL_QUEUE_BUFFER_COUNT};
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            (SLuint32)2,//2个声道（立体声）
            SL_SAMPLINGRATE_44_1,//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };
    SLDataSource slDataSource = {&android_queue, &pcm};

    //配置音频池
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, m_output_mix_obj};
    SLDataSink slDataSink = {&outputMix, NULL};

    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    SLresult result = (*m_engine)->CreateAudioPlayer(m_engine, &m_pcm_player_obj, &slDataSource, &slDataSink, 3, ids, req);
    if (CheckError(result, "Player")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, CREATE_OPENSL_ES_PLAYER_FAILED, "创建OpenSLES播放器失败");
//        }
        return false;
    }

    //初始化播放器
    result = (*m_pcm_player_obj)->Realize(m_pcm_player_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Player Realize")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, INIT_OPENSL_ES_PLAYER_FAILED, "初始化OpenSLES播放器失败");
//        }
        return false;
    }

    //得到接口后调用，获取Player接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_PLAY, &m_pcm_player);
    if (CheckError(result, "Player Interface")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, GET_OPENSL_ES_PLAYER_INTERFACE_FAILED, "获取OpenSLES播放器接口失败");
//        }
        return false;
    }

    //注册回调缓冲区，获取缓冲队列接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_BUFFERQUEUE, &m_pcm_buffer);
    if (CheckError(result, "Player Queue Buffer")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, GET_OPENSL_ES_PLAYER_BUFFER_INTERFACE_FAILED, "获取OpenSLES播放器缓存接口失败");
//        }
        return false;
    }

    //缓冲接口回调
    result = (*m_pcm_buffer)->RegisterCallback(m_pcm_buffer, sReadPcmBufferCbFun, this);
    if (CheckError(result, "Register Callback Interface")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, REGISTER_OPENSL_ES_PLAYER_BUFFER_CALLBACK_FAILED, "组册OpenSLES播放器缓存回调失败");
//        }
        return false;
    }

    //获取音量接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_VOLUME, &m_pcm_player_volume);
    if (CheckError(result, "Player Volume Interface")) {
//        if (m_state != STOPPED && m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, GET_OPENSL_ES_PLAYER_VOLUME_INTERFACE_FAILED, "获取OpenSLES播放器音量接口失败");
//        }
        return false;
    }
    LOGI(TAG, "OpenSL ES init success")

    return true;
}

/**
 * 用于判断OpenSL ES相关错误
 * @param result
 * @param hint
 * @return
 */
bool FFmpegOpenSLRender::CheckError(SLresult result, std::string hint) {
    if (SL_RESULT_SUCCESS != result) {
        LOGE(TAG, "OpenSL ES [%s] init fail", hint.c_str())
        return true;
    }
    return false;
}

/**
 * 设置OpenSL ES开始播放
 */
void FFmpegOpenSLRender::StartOpenSL(JNIEnv *env) {
    LOGE(TAG, "StartOpenSL(): start.");
    if (m_state == IDLE) {
        m_state = PREPARED;
        m_i_render_callback->OnRenderPrepared(env, MODULE_CODE_AUDIO);
    }
    LOGE(TAG, "StartOpenSL(): PREPARED.");
    while (m_state == PREPARED) {
        WaitState();
        LOGE(TAG, "StartOpenSL(): %s", "wake up.");
    }
    LOGE(TAG, "StartOpenSL(): %s", "check state.");
    if (!IsRunning()) {
        return;
    }
    LOGE(TAG, "StartOpenSL(): Run.");
    (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    sReadPcmBufferCbFun(m_pcm_buffer, this);
    jniEnv = NULL;
}

void FFmpegOpenSLRender::RestartOpenSL(JNIEnv *env) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    }
    jniEnv = NULL;
    sReadPcmBufferCbFun(m_pcm_buffer, this);
    jniEnv = NULL;
}

/**
 * OpenSL ES加载缓存回调方法
 * @param bufferQueueItf
 * @param context
 */
void FFmpegOpenSLRender::sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context) {
    LOGE("FFmpegOpenSLRender", "sReadPcmBufferCbFun() 0");
    FFmpegOpenSLRender *player = (FFmpegOpenSLRender *)context;
    if (player != NULL) {
        LOGE("FFmpegOpenSLRender", "sReadPcmBufferCbFun() 1");
        player->UpdateJNIEnv();
        player->Render();
    }
}

/**
 * 更新JNIEnv
 */
void FFmpegOpenSLRender::UpdateJNIEnv() {
    if (jniEnv == NULL) {
        JNIEnv *env;
        //将线程附加到虚拟机，并获取env
        if (m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
//            if (m_state != STOPPED && m_i_render_callback != NULL) {
//                m_i_render_callback->OnRenderError(NULL, MODULE_CODE_AUDIO, AUDIO_RENDER_UNPREPARED, "Fail to update JNIEnv.");
//            }
            return;
        }
        jniEnv = env;
    }
}

/**
 * 具体播放音频数据方法
 */
void FFmpegOpenSLRender::Render() {
//    pthread_mutex_lock(&m_state_mutex);
//    if (!IsRunning() || m_pcm_player == NULL) {
//        pthread_mutex_unlock(&m_state_mutex);
//        LOGE(TAG, "%s", "Render done.");
//        return;
//    }
//    if (isFrameEOS && m_frame_queue.empty() && m_state != COMPLETED && m_state != STOPPED) {
//        m_state = COMPLETED;
//        if (m_pcm_player != NULL) {
//            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
//        }
//        if (m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderCompleted(jniEnv, MODULE_CODE_AUDIO);
//        }
//        pthread_mutex_unlock(&m_state_mutex);
//        LOGE(TAG, "%s", "Render Complete.");
//    }
//    if (m_state == PREPARED // 设置解码器准备
//        || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
//        || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
//        ) {
//        pthread_cond_wait(&m_state_cond, &m_state_mutex);
//        LOGE(TAG, "Render(): %s", "wake up.");
//    }
//    pthread_mutex_unlock(&m_state_mutex);
//
//    if (!IsRunning() || m_pcm_player == NULL) {
//        LOGE(TAG, "%s", "Render done.");
//        return;
//    }
//
//    LOGD(TAG, "Render(): %s", "queue pop 0.");
//    AVFrame *frame = PopFrame();
//    LOGD(TAG, "Render(): %s", "queue pop 1.");
//    if (frame == NULL) {
//        return;
//    }
//    // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
//    if (!IsRunning() || m_pcm_player == NULL) {
//        av_frame_free(&frame);
//        return;
//    }
//    if (frame->pts != AV_NOPTS_VALUE) {
//        m_current_timestamp = frame->pts * av_q2d(m_stream_time_base);
//    } else if(frame->pkt_dts != AV_NOPTS_VALUE) {
//        m_current_timestamp = frame->pkt_dts * av_q2d(m_stream_time_base);
//    } else {
////        current_render_clock = ((double) 1.0) * frame->m_cur_t_s;
//    }
//
//    // 转换，返回每个通道的样本数
//    int ret = swr_convert(m_swr, m_out_buffer, m_dest_data_size / 2, (const uint8_t **) frame->data, frame->nb_samples);
//    if (ret > 0) {
//        // 缓冲区的大小
//        int size = av_samples_get_buffer_size(NULL, m_out_channer_nb, frame->nb_samples,GetSampleFormat(), 1);
//        LOGE(TAG, "LoopRender(): %s%d", "buffer ", size);
//        if (size > 0) {
//            // 将音频时间赋值给同步时间
//            double time = size / ((double) GetSampleRate(m_codec_context->sample_rate) * m_out_channer_nb * ENCODE_AUDIO_DEST_CHANNEL_COUNTS);
//            m_current_timestamp += time;
//            SLresult result = (*m_pcm_buffer)->Enqueue(m_pcm_buffer, m_out_buffer[0], (SLuint32) size);
//            if (result == SL_RESULT_SUCCESS) {
//                // 只做已经使用标记，在下一帧数据压入前移除
//                // 保证数据能正常使用，否则可能会出现破音
//            }
//
//            if (IsRunning() && m_i_render_callback != NULL) {
//                m_i_render_callback->UpdateSyncClock(m_current_timestamp);
//            }
//        }
//    }
//    av_frame_free(&frame);

    LOGE(TAG, "%s", "Render() Start.");
    if (!IsRunning() || m_pcm_player == NULL) {
        LOGE(TAG, "%s", "Render done.");
        return;
    }

    if (isFrameEOS && m_frame_queue.empty() && m_state != COMPLETED && m_state != STOPPED) {
        m_state = COMPLETED;
        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
        }
        if (m_i_render_callback != NULL) {
            m_i_render_callback->OnRenderCompleted(jniEnv, MODULE_CODE_AUDIO);
        }
    }
    if (m_state == PREPARED // 设置解码器准备
        || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
        || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
        ) {
        WaitState();
        LOGE(TAG, "Render(): %s", "wake up.");
    }

    if (!IsRunning() || m_pcm_player == NULL) {
        LOGE(TAG, "%s", "Render done.");
        return;
    }

    LOGD(TAG, "Render(): %s", "queue pop 0.");
    AVFrame *frame = PopFrame();
    LOGD(TAG, "Render(): %s", "queue pop 1.");
    if (frame == NULL) {
        return;
    }
    // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
    if (!IsRunning() || m_pcm_player == NULL) {
        av_frame_free(&frame);
        LOGE(TAG, "%s", "Render done.");
        return;
    }
    if (frame->pts != AV_NOPTS_VALUE) {
        m_current_timestamp = frame->pts * av_q2d(m_stream_time_base);
    } else if(frame->pkt_dts != AV_NOPTS_VALUE) {
        m_current_timestamp = frame->pkt_dts * av_q2d(m_stream_time_base);
    } else {
//        current_render_clock = ((double) 1.0) * frame->m_cur_t_s;
    }

    // 转换，返回每个通道的样本数
    int ret = swr_convert(m_swr, m_out_buffer, m_dest_data_size / 2, (const uint8_t **) frame->data, frame->nb_samples);
    if (ret > 0) {
        // 缓冲区的大小
        int size = av_samples_get_buffer_size(NULL, m_out_channer_nb, frame->nb_samples,GetSampleFormat(), 1);
        LOGE(TAG, "LoopRender(): %s%d", "buffer ", size);
        if (size > 0) {
            // 将音频时间赋值给同步时间
            double time = size / ((double) GetSampleRate(m_codec_context->sample_rate) * m_out_channer_nb * ENCODE_AUDIO_DEST_CHANNEL_COUNTS);
            m_current_timestamp += time;
            SLresult result = (*m_pcm_buffer)->Enqueue(m_pcm_buffer, m_out_buffer[0], (SLuint32) size);
            if (result == SL_RESULT_SUCCESS) {
                // 只做已经使用标记，在下一帧数据压入前移除
                // 保证数据能正常使用，否则可能会出现破音
            }
            if (IsRunning() && m_i_render_callback != NULL) {
                m_i_render_callback->UpdateSyncClock(m_current_timestamp);
            }
        }
    }
    av_frame_free(&frame);

    if (isFrameEOS && m_frame_queue.empty() && m_state != COMPLETED && m_state != STOPPED) {
        m_state = COMPLETED;
        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
        }
        if (m_i_render_callback != NULL) {
            m_i_render_callback->OnRenderCompleted(jniEnv, MODULE_CODE_AUDIO);
        }
    }
    if (m_state == PREPARED // 设置解码器准备
        || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
        || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
        ) {
        WaitState();
        LOGE(TAG, "Render(): %s", "wake up.");
    }
}

/**
 * 释放OpenSL ES相关资源
 */
void FFmpegOpenSLRender::ReleaseOpenSL() {
    // 设置停止状态
    if (m_pcm_player) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_STOPPED);
        m_pcm_player = NULL;
    }

    //销毁播放器
    if (m_pcm_player_obj) {
        (*m_pcm_player_obj)->Destroy(m_pcm_player_obj);
        m_pcm_player_obj = NULL;
        m_pcm_buffer = NULL;
        m_pcm_player_volume = NULL;
    }
    //销毁混音器
    if (m_output_mix_obj) {
        (*m_output_mix_obj)->Destroy(m_output_mix_obj);
        m_output_mix_obj = NULL;
    }
    //销毁引擎
    if (m_engine_obj) {
        (*m_engine_obj)->Destroy(m_engine_obj);
        m_engine_obj = NULL;
        m_engine = NULL;
    }
}

void FFmpegOpenSLRender::Start() {
    LOGD(TAG, "%s", "Start()");
    if (m_state == PREPARED || m_state == PAUSED) {
//        State temp = m_state;
//        m_state = RUNNING;
//        isCompletedToReset = false;
//        // 如果播放完成后需要重新执行播放方法
//        if (temp == COMPLETED) {
////            if (m_pcm_player != NULL) {
////                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
////                sendRenderFrameSignal();
////                jniEnv = NULL;
////                sReadPcmBufferCbFun(m_pcm_buffer, this);
////                jniEnv = NULL;
////            }
//            WakeUpState();
//            SendCommand(1);
//        } else {
//            WakeUpState();
//            if (m_pcm_player != NULL) {
//                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
//            }
//        }

        LOGE(TAG, "%s", "Start(): run.");
        m_state = RUNNING;
        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
        }
        LOGE(TAG, "Start(): WakeUp 0.");
        WakeUpState();
        LOGE(TAG, "Start(): WakeUp 1.");
    }
}

void FFmpegOpenSLRender::Pause() {
    LOGD(TAG, "%s", "Pause()");
    if (m_state == RUNNING) {
        m_state = PAUSED;
        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
        }
        WakeUpState();
    }
}

void FFmpegOpenSLRender::Resume() {
    LOGD(TAG, "%s", "Resume()");
    if (m_state == PAUSED) {
        m_state = RUNNING;
//        if (isCompletedToReset) {
//            // 是否从播放完成到可以播放状态（用于声音播放，如果播放完成后需要重新执行播放方法）
//            isCompletedToReset = false;
////            if (m_pcm_player != NULL) {
////                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
////                sendRenderFrameSignal();
////                jniEnv = NULL;
////                sReadPcmBufferCbFun(m_pcm_buffer, this);
////                jniEnv = NULL;
////            }
//            WakeUpState();
//            SendCommand(1);
//        } else {
//            WakeUpState();
//            if (m_pcm_player != NULL) {
//                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
//            }
//        }

        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
        }
        WakeUpState();
    }
}

void FFmpegOpenSLRender::Stop() {
    LOGD(TAG, "%s", "Stop()");
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_STOPPED);
    }
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    WakeUpState();
    WakeUpFrameQueue();
    SendCommand(2);
}

void FFmpegOpenSLRender::SeekTo(int64_t timestamp) {
//    pthread_mutex_lock(&m_state_mutex);
//    if (m_state == COMPLETED) {
//        // 是否从播放完成到可以播放状态（用于声音播放，如果播放完成后需要重新执行播放方法）
//        isCompletedToReset = true;
//    }
//    m_state = PAUSED;
//    pthread_cond_signal(&m_state_cond);
//    pthread_mutex_unlock(&m_state_mutex);

    if (m_state != STOPPED && m_state != ERROR) {
        m_state = PAUSED;
        isFrameEOS = false;
        ClearFrameQueue();
    }
}

jint FFmpegOpenSLRender::GetMaxVolumeLevel() {
    if (m_pcm_player_volume == NULL || !IsRunning()) {
        return 0;
    }
    SLmillibel volume = 0;
    SLresult result = (*m_pcm_player_volume)->GetMaxVolumeLevel(m_pcm_player_volume, &volume);
    if (result == SL_RESULT_SUCCESS) {
        return volume;
    }
    LOGE(TAG, "%s", "OpenSL ES getMaxVolumeLevel failed.")
    return 0;
}

jint FFmpegOpenSLRender::GetVolumeLevel() {
    if (m_pcm_player_volume == NULL || !IsRunning()) {
        return 0;
    }
    SLmillibel volume = 0;
    SLresult result = (*m_pcm_player_volume)->GetVolumeLevel(m_pcm_player_volume, &volume);
    if (result == SL_RESULT_SUCCESS) {
        return volume;
    }
    LOGE(TAG, "%s", "OpenSL ES getVolumeLevel failed.")
    return 0;
}

void FFmpegOpenSLRender::SetVolumeLevel(jint volume) {
    if (m_pcm_player_volume == NULL || !IsRunning()) {
        return;
    }
    SLresult result = (*m_pcm_player_volume)->SetVolumeLevel(m_pcm_player_volume, volume);
    if (result != SL_RESULT_SUCCESS) {
        LOGE(TAG, "%s", "OpenSL ES setVolumeLevel failed.")
    }
}