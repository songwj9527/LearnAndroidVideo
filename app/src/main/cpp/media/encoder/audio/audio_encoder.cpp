//
// Created by fgrid on 2021/8/6.
//

#include "audio_encoder.h"
#include "../../const.h"

AudioEncoder::AudioEncoder(JNIEnv *env, Mp4Muxer *muxer)
        : BaseEncoder(env, muxer, AV_CODEC_ID_AAC) {
    TAG = "AudioEncoder";
}

void AudioEncoder::InitContext(AVCodecContext *codec_ctx) {
    codec_ctx->codec_type = AVMEDIA_TYPE_AUDIO;
    codec_ctx->sample_fmt = ENCODE_AUDIO_DEST_FORMAT;
    codec_ctx->sample_rate = ENCODE_AUDIO_DEST_SAMPLE_RATE;
    codec_ctx->channel_layout = ENCODE_AUDIO_DEST_CHANNEL_LAYOUT;
    codec_ctx->channels = ENCODE_AUDIO_DEST_CHANNEL_COUNTS;
    codec_ctx->bit_rate = ENCODE_AUDIO_DEST_BIT_RATE;

    InitFrame();
}

void AudioEncoder::InitFrame() {
    m_frame = av_frame_alloc();
    m_frame->nb_samples = ACC_NB_SAMPLES;
    m_frame->format = ENCODE_AUDIO_DEST_FORMAT;
    m_frame->channel_layout = ENCODE_AUDIO_DEST_CHANNEL_LAYOUT;

    int size = av_samples_get_buffer_size(NULL, ENCODE_AUDIO_DEST_CHANNEL_COUNTS, m_frame->nb_samples,
                                          ENCODE_AUDIO_DEST_FORMAT, 1);
    uint8_t *frame_buf = (uint8_t *) av_malloc(size);
    avcodec_fill_audio_frame(m_frame, ENCODE_AUDIO_DEST_CHANNEL_COUNTS, ENCODE_AUDIO_DEST_FORMAT,
                             frame_buf, size, 1);
}

int AudioEncoder::ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) {
    return muxer->AddAudioStream(ctx);
}

AVFrame* AudioEncoder::DealFrame(EncodeFrame *encode_frame) {
    m_frame->pts = encode_frame->pts;
    // ACC_NB_SAMPLES * 编码格式对应的字节数(AV_SAMPLE_FMT_FLTP为32位4个字节，如果AV_SAMPLE_FMT_S16为2个字节)
    // = 1024 * 4 = 4096
    memcpy(m_frame->data[0], encode_frame->data, 4096);
    memcpy(m_frame->data[1], encode_frame->ext_data, 4096);
    return m_frame;
}

void AudioEncoder::Release() {
    m_muxer->EndAudioStream();
}