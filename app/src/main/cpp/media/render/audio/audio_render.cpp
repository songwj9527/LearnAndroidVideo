//
// Created by fgrid on 2/19/21.
//

#include "./audio_render.h"
#include "../../player/default_player/media_player.h"
#include "../../decoder/audio/audio_decoder.h"

AudioRender::AudioRender(bool for_synthesizer) : BaseRender(for_synthesizer) {
    TAG = "AudioRender";
}

AudioRender::~AudioRender() {
    LOGE(TAG, "%s", "~AudioRender");
    releaseSwr();
    mediaPlayer = NULL;
    decoder = NULL;
}

/**
 * 初始化转换工具：将帧数据转换成音频数据
 */
bool AudioRender::initSwr(JNIEnv *env) {
    if (decoder == NULL) {
        return false;
    }

    AVCodecContext *codeCtx = decoder->getCodecContext();

    //初始化格式转换工具
    m_swr = swr_alloc();
    if (m_swr == NULL) {
        onError(env, CREATE_AUDIO_SWR_CONTEXT_FAILED, "初始化音频转换上下文失败");
        return false;
    }

    // mp3里面所包含的编码格式转换成pcm
    // 声道
    av_opt_set_int(m_swr, "in_channel_layout", codeCtx->channel_layout, 0);
    av_opt_set_int(m_swr, "out_channel_layout", ENCODE_AUDIO_DEST_CHANNEL_LAYOUT, 0);

    // 采样率
    av_opt_set_int(m_swr, "in_sample_rate", codeCtx->sample_rate, 0);
//    av_opt_set_int(m_swr, "out_sample_rate", codeCtx->sample_rate, 0);
//    av_opt_set_int(m_swr, "out_sample_rate", ENCODE_AUDIO_DEST_SAMPLE_RATE, 0);
    av_opt_set_int(m_swr, "out_sample_rate", getSampleRate(codeCtx->sample_rate), 0);

    // 采样位数
    av_opt_set_sample_fmt(m_swr, "in_sample_fmt", codeCtx->sample_fmt, 0);
//    av_opt_set_sample_fmt(m_swr, "out_sample_fmt", AV_SAMPLE_FMT_S16,  0);
    av_opt_set_sample_fmt(m_swr, "out_sample_fmt", getSampleFormat(),  0);

    swr_init(m_swr);

    LOGI(TAG, "sample rate: %d, channel: %d, format: %d, frame_size: %d, layout: %lld",
         codeCtx->sample_rate, codeCtx->channels, codeCtx->sample_fmt, codeCtx->frame_size,codeCtx->channel_layout)

    // 重采样后一个通道采样数
//    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, codeCtx->sample_rate, codeCtx->sample_rate, AV_ROUND_UP);
//    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, ENCODE_AUDIO_DEST_SAMPLE_RATE, codeCtx->sample_rate, AV_ROUND_UP);
    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, getSampleRate(codeCtx->sample_rate), codeCtx->sample_rate, AV_ROUND_UP);
    // 重采样后一帧数据的大小
    m_dest_data_size = (size_t)av_samples_get_buffer_size(
            NULL, ENCODE_AUDIO_DEST_CHANNEL_COUNTS,
            m_dest_nb_sample, getSampleFormat(), 1);
    m_out_channer_nb = av_get_channel_layout_nb_channels(ENCODE_AUDIO_DEST_CHANNEL_LAYOUT);
    if (ForSynthesizer()) {
        m_out_buffer[0] = (uint8_t *) malloc(m_dest_data_size / 2);
        m_out_buffer[1] = (uint8_t *) malloc(m_dest_data_size / 2);
        if (m_out_buffer[0] == NULL || m_out_buffer[1] == NULL) {
            onError(env, CREATE_AUDIO_BUFFER_FAILED, "初始化音频转换缓存数组失败");
            return false;
        }
    } else {
        m_out_buffer[0] = (uint8_t *) malloc(m_dest_data_size);
        if (m_out_buffer[0] == NULL) {
            onError(env, CREATE_AUDIO_BUFFER_FAILED, "初始化音频转换缓存数组失败");
            return false;
        }
    }
    LOGI(TAG, "out_channer_nb: %d, dest_data_size: %d",
         m_out_channer_nb, m_dest_data_size)
    return true;
}

/**
 * 释放转换工具相关资源（包括缓冲区）
 */
void AudioRender::releaseSwr() {
    if (m_swr != NULL) {
        swr_free(&m_swr);
        m_swr = NULL;
    }
    if (m_out_buffer[0] != NULL) {
        free(m_out_buffer[0]);
        m_out_buffer[0] = NULL;
    }
    if (m_out_buffer[1] != NULL) {
        free(m_out_buffer[1]);
        m_out_buffer[1] = NULL;
    }
}