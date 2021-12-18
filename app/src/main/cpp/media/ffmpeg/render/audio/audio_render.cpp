//
// Created by fgrid on 2021/12/17.
//

#include "audio_render.h"
#include "../../../../utils/logger.h"
#include "../../../media_codes.h"

FFmpegAudioRender::FFmpegAudioRender(JNIEnv *jniEnv, IFFmpegRenderCallback *iRenderCallback, AVCodecContext *codecContext, AVRational streamTimeBase) : BaseFFmpegRender(jniEnv, iRenderCallback) {
    TAG = "FFmpegAudioRender";
    this->m_codec_context = codecContext;
    this->m_stream_time_base.num = streamTimeBase.num;
    this->m_stream_time_base.den = streamTimeBase.den;
}

FFmpegAudioRender::~FFmpegAudioRender() {
    ReleaseSwr();
    m_codec_context = NULL;
    LOGE(TAG, "%s", "~FFmpegAudioRender");
}

/**
 * 初始化转换工具：将帧数据转换成音频数据
 */
bool FFmpegAudioRender::InitSwr(JNIEnv *env) {
    //初始化格式转换工具
    m_swr = swr_alloc();
    if (m_swr == NULL) {
//        if (m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(env, MODULE_CODE_AUDIO, CREATE_AUDIO_SWR_CONTEXT_FAILED, "初始化音频转换上下文失败");
//        }
        return false;
    }

    // mp3里面所包含的编码格式转换成pcm
    // 声道
    av_opt_set_int(m_swr, "in_channel_layout", m_codec_context->channel_layout, 0);
    av_opt_set_int(m_swr, "out_channel_layout", ENCODE_AUDIO_DEST_CHANNEL_LAYOUT, 0);

    // 采样率
    av_opt_set_int(m_swr, "in_sample_rate", m_codec_context->sample_rate, 0);
//    av_opt_set_int(m_swr, "out_sample_rate", m_codec_context->sample_rate, 0);
//    av_opt_set_int(m_swr, "out_sample_rate", ENCODE_AUDIO_DEST_SAMPLE_RATE, 0);
    av_opt_set_int(m_swr, "out_sample_rate", GetSampleRate(m_codec_context->sample_rate), 0);

    // 采样位数
    av_opt_set_sample_fmt(m_swr, "in_sample_fmt", m_codec_context->sample_fmt, 0);
//    av_opt_set_sample_fmt(m_swr, "out_sample_fmt", AV_SAMPLE_FMT_S16,  0);
    av_opt_set_sample_fmt(m_swr, "out_sample_fmt", GetSampleFormat(),  0);

    swr_init(m_swr);

    LOGI(TAG, "sample rate: %d, channel: %d, format: %d, frame_size: %d, layout: %lld",
         m_codec_context->sample_rate, m_codec_context->channels, m_codec_context->sample_fmt, m_codec_context->frame_size,m_codec_context->channel_layout)

    // 重采样后一个通道采样数
//    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, m_codec_context->sample_rate, m_codec_context->sample_rate, AV_ROUND_UP);
//    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, ENCODE_AUDIO_DEST_SAMPLE_RATE, m_codec_context->sample_rate, AV_ROUND_UP);
    m_dest_nb_sample = (int)av_rescale_rnd(ACC_NB_SAMPLES, GetSampleRate(m_codec_context->sample_rate), m_codec_context->sample_rate, AV_ROUND_UP);
    // 重采样后一帧数据的大小
    m_dest_data_size = (size_t)av_samples_get_buffer_size(
            NULL, ENCODE_AUDIO_DEST_CHANNEL_COUNTS,
            m_dest_nb_sample, GetSampleFormat(), 1);
    m_out_channer_nb = av_get_channel_layout_nb_channels(ENCODE_AUDIO_DEST_CHANNEL_LAYOUT);
    m_out_buffer[0] = (uint8_t *) malloc(m_dest_data_size);
    if (m_out_buffer[0] == NULL) {
//        if (m_i_render_callback != NULL) {
//            m_i_render_callback->OnRenderError(env, MODULE_CODE_AUDIO, CREATE_AUDIO_BUFFER_FAILED, "初始化音频转换缓存数组失败");
//        }
        return false;
    }
    LOGI(TAG, "out_channer_nb: %d, dest_data_size: %d",
         m_out_channer_nb, m_dest_data_size)
    return true;
}

/**
 * 释放转换工具相关资源（包括缓冲区）
 */
void FFmpegAudioRender::ReleaseSwr() {
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