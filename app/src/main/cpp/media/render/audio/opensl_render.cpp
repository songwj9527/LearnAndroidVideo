//
// Created by fgrid on 2/19/21.
//

#include "./opensl_render.h"
#include "../../player/default_player/media_player.h"
#include "../../decoder/audio/audio_decoder.h"

OpenSLRender::OpenSLRender(bool for_synthesizer): AudioRender(for_synthesizer) {
    TAG = "OpenSLRender";
}

OpenSLRender::~OpenSLRender(){
    LOGE(TAG, "%s", "~OpenSLRender");
    releaseOpenSL();
    releaseSwr();
}

/**
 * 渲染准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderPrepared方法）
 */
void OpenSLRender::onPrepared(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderPrepared(env, MODULE_CODE_AUDIO);
    }
}

/**
 * 渲染线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onRenderError方法）
 */
void OpenSLRender::onError(JNIEnv *env, int code, const char *msg) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderError(env, MODULE_CODE_AUDIO, code, msg);
    }
}

/**
 * 渲染已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderComplete方法）
 */
void OpenSLRender::onComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderCompleted(env, MODULE_CODE_AUDIO);
    }
}

/**
 * 初始化Render
 * @param env
 */
void OpenSLRender::prepareSync(JNIEnv *env, Player *mediaPlayer, BaseDecoder *decoder) {
    this->mediaPlayer = mediaPlayer;
    this->decoder = decoder;

    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);

    // 使用智能指针，线程结束时，自动删除本类指针
//    std::shared_ptr<OpenSLRender> that(this);
    std::thread th(runPrepare, this);
    th.detach();

    LOGE(TAG, "%s", "prepareSync()");
}

///**
// * 初始化线程调用方法
// * @param that
// */
//void OpenSLRender::runPrepare(std::shared_ptr<OpenSLRender> that) {
//    JNIEnv *env;
//    //将线程附加到虚拟机，并获取env
//    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
//        that->onError(NULL, VIDEO_RENDER_UNPREPARED, "Fail to init default video render thread");
//        return;
//    }
//    that->jniEnv = env;
//    LOGE(that->TAG, "%s", "runPrepare()");
//    that->prepare(env);
//
//    while(that->isRunning()) {
//        av_usleep(20000);
//    }
//    LOGE(that->TAG, "%s", "thread done.");
//    //解除线程和jvm关联
//    that->m_jvm_for_thread->DetachCurrentThread();
//}

/**
 * 初始化线程调用方法
 * @param that
 */
void OpenSLRender::runPrepare(OpenSLRender *that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->onError(NULL, VIDEO_RENDER_UNPREPARED, "Fail to init default video render thread");
        return;
    }
    that->jniEnv = env;
    LOGE(that->TAG, "%s", "runPrepare()");
    that->prepare(env);
//    while (that->isRunning()) {
//        av_usleep(20000);
//    }
    LOGE(that->TAG, "%s", "thread done.");
    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
}

/**
 * 更新JNIEnv
 */
void OpenSLRender::updateJNIEnv() {
    if (jniEnv == NULL) {
        JNIEnv *env;
        //将线程附加到虚拟机，并获取env
        if (m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
            onError(NULL, VIDEO_RENDER_UNPREPARED, "Fail to init default video render thread");
            return;
        }
        jniEnv = env;
    }
}

/**
 * 初始化方法
 */
void OpenSLRender::prepare(JNIEnv *env) {
    if (!initSwr(env)) return;
    if (!createEngine(env)) return;
    if (!createOutputMixer(env)) return;
    if (!configPlayer(env)) return;
    startOpenSL(env);
}

/**
 * 创建OpenSL ES引擎
 * @return
 */
bool OpenSLRender::createEngine(JNIEnv *env) {
    SLresult result = slCreateEngine(&m_engine_obj, 0, NULL, 0, NULL, NULL);
    if (checkError(result, "Engine")) {
        onError(env, CREATE_ENGINE_OBJECT_FAILED, "创建OpenSLES引擎engineObject失败");
        return false;
    }

    result = (*m_engine_obj)->Realize(m_engine_obj, SL_BOOLEAN_FALSE);
    if (checkError(result, "Engine Realize")) {
        onError(env, REALIZE_ENGINE_OBJECT_FAILED, "实现OpenSLES引擎engineObject失败");
        return false;
    }

    result = (*m_engine_obj)->GetInterface(m_engine_obj, SL_IID_ENGINE, &m_engine);
    if (checkError(result, "Engine Interface")) {
        onError(env, GET_ENGINE_OBJECT_INTERFACE_FAILED, "获取OpenSLES引擎接口engineEngine失败");
        return false;
    }
    return true;
}

/**
 * 创建OpenSL ES音频输出混音器
 * @return
 */
bool OpenSLRender::createOutputMixer(JNIEnv *env) {
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    SLresult result = (*m_engine)->CreateOutputMix(m_engine, &m_output_mix_obj, 1, mids, mreq);
    if (checkError(result, "Output Mix")) {
        onError(env, CREATE_ENGINE_OUTPUT_MIX_FAILED, "创建OpenSLES混音器outputMixObject失败");
        return false;
    }

    result = (*m_output_mix_obj)->Realize(m_output_mix_obj, SL_BOOLEAN_FALSE);
    if (checkError(result, "Output Mix Realize")) {
        onError(env, REALIZE_ENGINE_OUTPUT_MIX_FAILED, "实现OpenSLES混音器outputMixObject失败");
        return false;
    }

    result = (*m_output_mix_obj)->GetInterface(m_output_mix_obj, SL_IID_ENVIRONMENTALREVERB, &m_output_mix_evn_reverb);
    if (checkError(result, "Output Mix Env Reverb")) {
        onError(env, GET_ENGINE_ENGINE_OUTPUT_MIX_INTERFACE_FAILED, "获取OpenSLES混音器接口outputMixEnv失败");
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
bool OpenSLRender::configPlayer(JNIEnv *env) {
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
    if (checkError(result, "Player")) {
        onError(env, CREATE_OPENSL_ES_PLAYER_FAILED, "创建OpenSLES播放器失败");
        return false;
    }

    //初始化播放器
    result = (*m_pcm_player_obj)->Realize(m_pcm_player_obj, SL_BOOLEAN_FALSE);
    if (checkError(result, "Player Realize")) {
        onError(env, INIT_OPENSL_ES_PLAYER_FAILED, "初始化OpenSLES播放器失败");
        return false;
    }

    //得到接口后调用，获取Player接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_PLAY, &m_pcm_player);
    if (checkError(result, "Player Interface")) {
        onError(env, GET_OPENSL_ES_PLAYER_INTERFACE_FAILED, "获取OpenSLES播放器接口失败");
        return false;
    }

    //注册回调缓冲区，获取缓冲队列接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_BUFFERQUEUE, &m_pcm_buffer);
    if (checkError(result, "Player Queue Buffer")) {
        onError(env, GET_OPENSL_ES_PLAYER_BUFFER_INTERFACE_FAILED, "获取OpenSLES播放器缓存接口失败");
        return false;
    }

    //缓冲接口回调
    result = (*m_pcm_buffer)->RegisterCallback(m_pcm_buffer, sReadPcmBufferCbFun, this);
    if (checkError(result, "Register Callback Interface")) {
        onError(env, REGISTER_OPENSL_ES_PLAYER_BUFFER_CALLBACK_FAILED, "组册OpenSLES播放器缓存回调失败");
        return false;
    }

    //获取音量接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_VOLUME, &m_pcm_player_volume);
    if (checkError(result, "Player Volume Interface")) {
        onError(env, GET_OPENSL_ES_PLAYER_VOLUME_INTERFACE_FAILED, "获取OpenSLES播放器音量接口失败");
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
bool OpenSLRender::checkError(SLresult result, std::string hint) {
    if (SL_RESULT_SUCCESS != result) {
        LOGE(TAG, "OpenSL ES [%s] init fail", hint.c_str())
        return true;
    }
    return false;
}

/**
 * 设置OpenSL ES开始播放
 */
void OpenSLRender::startOpenSL(JNIEnv *env) {
    LOGE(TAG, "%s", "startOpenSL()");
    pthread_mutex_lock(&m_state_mutex);
    if (m_state == STOPPED) {
        return;
    } else {
        m_state = PREPARED;
    }
    pthread_mutex_unlock(&m_state_mutex);
    onPrepared(env);
    while (m_state == PREPARED) {
        waitRenderFrame(0);
        LOGE(TAG, "loopRender(): %s", "wake up.");
    }
    LOGE(TAG, "isRunning(): %d", m_state);
    if (!isRunning()) {
//        pthread_exit(0);
        return;
    }
    (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    sReadPcmBufferCbFun(m_pcm_buffer, this);
    jniEnv = NULL;
//    pthread_exit(0);
}

/**
 * OpenSL ES加载缓存回调方法
 * @param bufferQueueItf
 * @param context
 */
void OpenSLRender::sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context) {
    LOGE("OpenSLRender", "%s", "sReadPcmBufferCbFun 0.");
    OpenSLRender *player = (OpenSLRender *)context;
    player->updateJNIEnv();
    player->render();
    LOGE("OpenSLRender", "%s", "sReadPcmBufferCbFun 1.");
}

/**
 * 具体播放音频数据方法
 */
void OpenSLRender::render() {
    if (m_pcm_player == NULL) {
        return;
    }
    if (!isRunning()) {
        LOGE(TAG, "%s", "render done.");
        return;
    }

    if (decoder != NULL) {
        bool isComplete = false;
        pthread_mutex_lock(&m_state_mutex);
        if (decoder->isCompleted() && m_render_frame_queue.empty() && m_state != COMPLETED) {
            m_state = COMPLETED;
            isComplete = true;
        }
        pthread_mutex_unlock(&m_state_mutex);
        if (isComplete) {
            LOGE(TAG, "loopRender(): %s", "completed.");
//            if (m_pcm_player != NULL) {
//                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
//            }
            onComplete(jniEnv);
        }
    }
    if (m_state == PREPARED // 设置解码器准备
        || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
        || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
        || m_state == SEEKING // 设置解码器正在拉取进度（设置解码器指定位置解码时，设置此状态）
            ) {
        waitRenderFrame(0);
        LOGE(TAG, "loopRender(): %s", "wake up.");
    }
    if (m_pcm_player == NULL) {
        return;
    }
    if (!isRunning()) {
        LOGE(TAG, "%s", "render done.");
        return;
    }
    if (m_state == PREPARED // 设置解码器准备
        || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
        || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
        || m_state == SEEKING // 设置解码器正在拉取进度（设置解码器指定位置解码时，设置此状态）
            ) {
        return;
    }

    LOGE(TAG, "loopRender(): %s", "queue pop 0.");
    RenderFrame *frame = renderFramePop();
    LOGE(TAG, "loopRender(): %s", "queue pop 1.");
    if (frame == NULL) {
        return;
    }
    // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
    if (!isRunning()) {
        decodeFramePush(frame->m_frame);
        delete frame;
        LOGE(TAG, "%s", "render done.");
        return;
    }
    if (m_pcm_player == NULL) {
        decodeFramePush(frame->m_frame);
        delete frame;
        // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
        if (!isRunning()) {
            return;
        }
        if (decoder != NULL) {
            decoder->wake();
        }
        return;
    }
    if (frame->m_frame->pts != AV_NOPTS_VALUE && decoder != NULL) {
        current_render_clock = frame->m_frame->pts * av_q2d(decoder->getStreamTimeBase());
    } else {
//        current_render_clock = ((double) 1.0) * frame->m_cur_t_s;
    }

    LOGE(TAG, "loopRender(): %s", "convert");
    // 转换，返回每个通道的样本数
    int ret = swr_convert(m_swr, m_out_buffer, m_dest_data_size / 2, (const uint8_t **) frame->m_frame->data, frame->m_frame->nb_samples);
    LOGE(TAG, "loopRender(): %s%d", "convert ", ret);
    if (ret > 0) {
        LOGE(TAG, "loopRender(): %s", "buffer");
        // 缓冲区的大小
        int size = av_samples_get_buffer_size(NULL, m_out_channer_nb, frame->m_frame->nb_samples,getSampleFormat(), 1);
        LOGE(TAG, "loopRender(): %s%d", "buffer ", size);
        if (size > 0) {
            // 将音频时间赋值给同步时间
            if (decoder != NULL) {
                double time = size / ((double) getSampleRate(decoder->getCodecContext()->sample_rate) * m_out_channer_nb * ENCODE_AUDIO_DEST_CHANNEL_COUNTS);
                current_render_clock += time;
                if (mediaPlayer != NULL) {
                    mediaPlayer->setSyncClock(current_render_clock);
                }
            }

            SLresult result = (*m_pcm_buffer)->Enqueue(m_pcm_buffer, m_out_buffer[0], (SLuint32) size);
            if (result == SL_RESULT_SUCCESS) {
                // 只做已经使用标记，在下一帧数据压入前移除
                // 保证数据能正常使用，否则可能会出现破音
            }
        }
    }

    decodeFramePush(frame->m_frame);
    delete frame;
    // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
    if (!isRunning()) {
        return;
    }
    if (decoder != NULL) {
        decoder->wake();
    }
}

/**
 * 判断渲染线程是否可以继续渲染
 * @return
 */
bool OpenSLRender::isRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}

/**
 * 释放OpenSL ES相关资源
 */
void OpenSLRender::releaseOpenSL() {
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

/**
 * 开始播放
 */
void OpenSLRender::start() {
    if (m_state != RUNNING && m_state != STOPPED) {
        State temp = m_state;
        m_state = RUNNING;
        isCompletedToReset = false;
        LOGE(TAG, "%s", "start()");
        // 如果播放完成后需要重新执行播放方法
        if (temp == COMPLETED) {
            if (m_pcm_player != NULL) {
                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
                sendRenderFrameSignal();
                jniEnv = NULL;
                sReadPcmBufferCbFun(m_pcm_buffer, this);
                jniEnv = NULL;
            }
        } else {
            sendRenderFrameSignal();
            if (m_pcm_player != NULL) {
                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
            }
        }
    }
}

/**
 * 暂停播放
 */
void OpenSLRender::pause() {
    if (m_state == RUNNING) {
        m_state = PAUSED;
        LOGE(TAG, "%s", "pause()");
        if (m_pcm_player != NULL) {
            (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
        }
        sendRenderFrameSignal();
    }
}

/**
 * 继续播放
 */
void OpenSLRender::resume() {
    LOGE(TAG, "%s", "resume()0");
    if (m_state == PAUSED) {
        m_state = RUNNING;
        if (isCompletedToReset) {
            // 是否从播放完成到可以播放状态（用于声音播放，如果播放完成后需要重新执行播放方法）
            isCompletedToReset = false;
            if (m_pcm_player != NULL) {
                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
                sendRenderFrameSignal();
                jniEnv = NULL;
                sReadPcmBufferCbFun(m_pcm_buffer, this);
                jniEnv = NULL;
            }
        } else {
            sendRenderFrameSignal();
            if (m_pcm_player != NULL) {
                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
            }
        }
        LOGE(TAG, "%s", "resume()1");
    }
}

/**
 * 停止播放
 */
void OpenSLRender::stop() {
    LOGE(TAG, "%s", "stop()");
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_STOPPED);
    }
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    sendRenderFrameSignal();
    av_usleep(50000);
}

/**
 * 释放相关资源
 */
void OpenSLRender::release() {
    LOGE("OpenSLRender", "%s", "release()0")
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_STOPPED);
    }
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    sendRenderFrameSignal();
    av_usleep(50000);
    LOGE("OpenSLRender", "%s", "release()1");
}

/**
 * 获取最大音量
 * @return
 */
jint OpenSLRender::getMaxVolumeLevel() {
    if (m_pcm_player_volume == NULL) {
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

/**
 * 获取当前音量
 * @return
 */
jint OpenSLRender::getVolumeLevel() {
    if (m_pcm_player_volume == NULL) {
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

/**
 * 设置音量
 * @param volume
 */
void OpenSLRender::setVolumeLevel(jint volume) {
    if (m_pcm_player_volume == NULL) {
        return;
    }
    SLresult result = (*m_pcm_player_volume)->SetVolumeLevel(m_pcm_player_volume, volume);
    if (result != SL_RESULT_SUCCESS) {
        LOGE(TAG, "%s", "OpenSL ES setVolumeLevel failed.")
    }
}