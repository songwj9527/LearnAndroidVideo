//
// Created by fgrid on 2021/9/28.
//

#include "audio_track.h"
#include "../../../utils/logger.h"
#include "../extractor/audio_extractor.h"

AudioTrack::AudioTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback) : BaseTrack(jniEnv, source, i_track_callback) {
    TAG = "AudioTrack";

    // 初始化条件锁
    pthread_mutex_init(&m_play_mutex, NULL);
    pthread_cond_init(&m_play_cond, NULL);
}

AudioTrack::~AudioTrack() {
    // 释放锁
    pthread_cond_destroy(&m_play_cond);
    pthread_mutex_destroy(&m_play_mutex);
}

bool AudioTrack::CreateEngine(JNIEnv *env) {
    SLresult result = slCreateEngine(&m_engine_obj, 0, NULL, 0, NULL, NULL);
    if (CheckError(result, "Engine")) {
        LOGE(TAG, "创建OpenSLES引擎engineObject失败")
        return false;
    }

    result = (*m_engine_obj)->Realize(m_engine_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Engine Realize")) {
        LOGE(TAG, "实现OpenSLES引擎engineObject失败");
        return false;
    }

    result = (*m_engine_obj)->GetInterface(m_engine_obj, SL_IID_ENGINE, &m_engine);
    if (CheckError(result, "Engine Interface")) {
        LOGE(TAG, "获取OpenSLES引擎接口engineEngine失败");
        return false;
    }
    return true;
}

bool AudioTrack::CreateOutputMixer(JNIEnv *env) {
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    SLresult result = (*m_engine)->CreateOutputMix(m_engine, &m_output_mix_obj, 1, mids, mreq);
    if (CheckError(result, "Output Mix")) {
        LOGE(TAG, "创建OpenSLES混音器outputMixObject失败");
        return false;
    }

    result = (*m_output_mix_obj)->Realize(m_output_mix_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Output Mix Realize")) {
        LOGE(TAG, "实现OpenSLES混音器outputMixObject失败");
        return false;
    }

    result = (*m_output_mix_obj)->GetInterface(m_output_mix_obj, SL_IID_ENVIRONMENTALREVERB, &m_output_mix_evn_reverb);
    if (CheckError(result, "Output Mix Env Reverb")) {
        LOGE(TAG, "获取OpenSLES混音器接口outputMixEnv失败");
        return false;
    }

    if (result == SL_RESULT_SUCCESS) {
        (*m_output_mix_evn_reverb)->SetEnvironmentalReverbProperties(m_output_mix_evn_reverb, &m_reverb_settings);
    }
    return true;
}

bool AudioTrack::ConfigPlayer(JNIEnv *env) {
    int32_t channel = ((AudioExtractor *) m_extractor)->GetChannelCount();
    int32_t rate = ((AudioExtractor *) m_extractor)->GetSampleRate();
    int32_t bit = ((AudioExtractor *) m_extractor)->GetPcmEncodeBit();
    LOGE(TAG, "rate: %d, channel: %d, bit: %d", rate, channel, bit)
    if (channel < 1) {
        channel = 2;
    }
    m_sample_rate = GetSampleRateForOpenSLES(rate);
    m_channel_count = channel;
    m_bit = 2;

//    m_out_buffer[0] = malloc(rate * channel * 2);
    //配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, SL_QUEUE_BUFFER_COUNT};
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            m_channel_count,// 声道
            m_sample_rate,//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };
    if (channel == 2) {
        pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;//立体声（前左前右）
    } else {
        pcm.channelMask = SL_SPEAKER_FRONT_CENTER;
    }

    SLDataSource slDataSource = {&android_queue, &pcm};

    //配置音频池
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, m_output_mix_obj};
    SLDataSink slDataSink = {&outputMix, NULL};

    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    SLresult result = (*m_engine)->CreateAudioPlayer(m_engine, &m_pcm_player_obj, &slDataSource, &slDataSink, 3, ids, req);
    if (CheckError(result, "Player")) {
        LOGE(TAG, "创建OpenSLES播放器失败");
        return false;
    }

    //初始化播放器
    result = (*m_pcm_player_obj)->Realize(m_pcm_player_obj, SL_BOOLEAN_FALSE);
    if (CheckError(result, "Player Realize")) {
        LOGE(TAG, "初始化OpenSLES播放器失败");
        return false;
    }

    //得到接口后调用，获取Player接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_PLAY, &m_pcm_player);
    if (CheckError(result, "Player Interface")) {
        LOGE(TAG, "获取OpenSLES播放器接口失败");
        return false;
    }

    //注册回调缓冲区，获取缓冲队列接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_BUFFERQUEUE, &m_pcm_buffer);
    if (CheckError(result, "Player Queue Buffer")) {
        LOGE(TAG, "获取OpenSLES播放器缓存接口失败");
        return false;
    }

    //缓冲接口回调
    result = (*m_pcm_buffer)->RegisterCallback(m_pcm_buffer, sReadPcmBufferCbFun, this);
    if (CheckError(result, "Register Callback Interface")) {
        LOGE(TAG, "组册OpenSLES播放器缓存回调失败");
        return false;
    }

    //获取音量接口
    result = (*m_pcm_player_obj)->GetInterface(m_pcm_player_obj, SL_IID_VOLUME, &m_pcm_player_volume);
    if (CheckError(result, "Player Volume Interface")) {
        LOGE(TAG, "获取OpenSLES播放器音量接口失败");
        return false;
    }
    LOGI(TAG, "OpenSL ES init success")

    return true;
}

SLuint32 AudioTrack::GetSampleRateForOpenSLES(int sample_rate) {
    int rate = 0;
    switch (sample_rate) {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            rate = SL_SAMPLINGRATE_11_025;
            break;
        case 12000:
            rate = SL_SAMPLINGRATE_12;
            break;
        case 16000:
            rate = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            rate = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            rate = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            rate = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            rate = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            rate = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            rate = SL_SAMPLINGRATE_192;
            break;
        default:
            rate =  SL_SAMPLINGRATE_44_1;
    }
    return rate;
}

bool AudioTrack::CheckError(SLresult result, std::string hint) {
    if (SL_RESULT_SUCCESS != result) {
//        LOGE(TAG, "OpenSL ES [%s] init fail", hint.c_str())
        return true;
    }
    return false;
}

void AudioTrack::sReadPcmBufferCbFun(SLAndroidSimpleBufferQueueItf bufferQueueItf, void *context) {
    LOGE("AudioTrack", "sReadPcmBufferCbFun")
    if (context != NULL) {
        AudioTrack *audioTrack = (AudioTrack *) context;
        audioTrack->Play();
    }
}

void AudioTrack::Play() {
    if (!IsRunning()) {
        return;
    }
    if (m_state != RUNNING) {
        HoldOnPlay();
    }
    if (m_state != RUNNING) {
        if (m_state == COMPLETED) {
            if (m_pcm_player != NULL) {
                (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
            }
        }
        return;
    }
}

void AudioTrack::HoldOnPlay() {
    pthread_mutex_lock(&m_play_mutex);
    pthread_cond_wait(&m_play_cond, &m_play_mutex);
    pthread_mutex_unlock(&m_play_mutex);
}

void AudioTrack::WakeUpPlay() {
    pthread_mutex_lock(&m_play_mutex);
    pthread_cond_signal(&m_play_cond);
    pthread_mutex_unlock(&m_play_mutex);
}

void AudioTrack::ReleaseOpenSL() {
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

bool AudioTrack::InitExtractor(JNIEnv *env) {
    LOGE(TAG, "InitExtractor()")
    m_extractor = new AudioExtractor();
    if (AMEDIA_OK != m_extractor->SetDataSource(m_source)) {
        delete m_extractor;
        m_extractor = NULL;
        return false;
    }
    return true;
}

bool AudioTrack::InitRender(JNIEnv *env) {
    LOGE(TAG, "InitRender()")
    if (!CreateEngine(env)) {
        return false;
    }
    if (!CreateOutputMixer(env)) {
        return false;
    }
    if (!ConfigPlayer(env)) {
        return false;
    }
    (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    sReadPcmBufferCbFun(m_pcm_buffer, this);
    return true;
}

void AudioTrack::Render(uint8_t *buffer_data, AMediaCodecBufferInfo *buffer_info, size_t out_size) {
    LOGE(TAG, "Render(): %ll", m_buffer_info.size);
    if (IsRunning() && m_state != SEEKING) {
        if (buffer_data != NULL && buffer_info != NULL && m_pcm_buffer != NULL) {
            SLresult result = (*m_pcm_buffer)->Enqueue(m_pcm_buffer, buffer_data, out_size);
            if (result == SL_RESULT_SUCCESS) {
                // 只做已经使用标记，在下一帧数据压入前移除
                // 保证数据能正常使用，否则可能会出现破音
            }
        }
    }
}

int64_t AudioTrack::RenderFrameAliveTime() {
//    if (m_pcm_player != NULL) {
//        return size / (m_sample_rate * m_channel_count * m_bit);
//    }
    return 0;
}

void AudioTrack::UpdateClock() {
    if (m_i_sync_clock_receiver != NULL) {
        m_i_sync_clock_receiver->SetSyncClock(m_buffer_info.presentationTimeUs / 1000);
    }
    LOGE(TAG, "UpdateClock(): %d", m_buffer_info.presentationTimeUs / 1000);
}

void AudioTrack::LoopDoneVirtual(JNIEnv *env) {
    ReleaseOpenSL();
}

void  AudioTrack::StartVirtual(State prev) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    }
    WakeUpPlay();
}
void  AudioTrack::PauseVirtual(State prev) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PAUSED);
    }
//    WakeUpPlay();
}
void  AudioTrack::ResumeVirtual(State prev) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    }
    WakeUpPlay();
}
void  AudioTrack::StopVirtual(State prev) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_STOPPED);
    }
    WakeUpPlay();
}
void  AudioTrack::ResetVirtual(State prev) {
    if (m_pcm_player != NULL) {
        (*m_pcm_player)->SetPlayState(m_pcm_player, SL_PLAYSTATE_PLAYING);
    }
    sReadPcmBufferCbFun(m_pcm_buffer, this);
}

jint AudioTrack::GetMaxVolumeLevel() {
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

jint AudioTrack::GetVolumeLevel() {
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

void AudioTrack::SetVolumeLevel(jint volume) {
    if (m_pcm_player_volume == NULL) {
        return;
    }
    SLresult result = (*m_pcm_player_volume)->SetVolumeLevel(m_pcm_player_volume, volume);
    if (result != SL_RESULT_SUCCESS) {
        LOGE(TAG, "%s", "OpenSL ES setVolumeLevel failed.")
    }
}