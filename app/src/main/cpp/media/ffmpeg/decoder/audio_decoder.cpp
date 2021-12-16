//
// Created by fgrid on 2021/12/16.
//

#include "audio_decoder.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"

AudioDecoder::AudioDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback): BaseDecoder(env, url, iDecoderCallback) {
    TAG = "AudioDecoder";
}

AudioDecoder::~AudioDecoder() {
    LOGE(TAG, "%s", "~AudioDecoder");
}

AVMediaType AudioDecoder::GetMediaType() {
    return AVMEDIA_TYPE_AUDIO;
}

void AudioDecoder::OnPrepared(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderPrepared(env, MODULE_CODE_AUDIO);
    }
}

void AudioDecoder::OnError(JNIEnv *env, int code, const char *msg) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderError(env,MODULE_CODE_AUDIO, code, msg);
    }
}

void AudioDecoder::OnComplete(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderCompleted(env,MODULE_CODE_AUDIO);
    }
}

void AudioDecoder::OnSeekComplete(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderSeekCompleted(env,MODULE_CODE_AUDIO);
    }
}