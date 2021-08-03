//
// Created by fgrid on 2/2/21.
//

#include "audio_decoder.h"
#include "../../player/player.h"
#include "../../render/audio/opensl_render.h"

AudioDecoder::AudioDecoder(JNIEnv *env, Player *mediaPlayer, const char *url, OpenSLRender *render, bool forSynthesizer) : BaseDecoder(env, mediaPlayer, url, render, forSynthesizer) {
    TAG = "AudioDecoder";
}

AudioDecoder::~AudioDecoder() {
    LOGE(TAG, "%s", "~AudioDecoder");
}

/**
 * 音视频索引
 */
AVMediaType AudioDecoder::getMediaType() {
    return AVMEDIA_TYPE_AUDIO;
}

/**
 * 解码准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderPrepared方法）
 */
void AudioDecoder::onPrepared(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderPrepared(env, MODULE_CODE_AUDIO);
    }
}

/**
 * 解码线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderError方法）
 */
void AudioDecoder::onError(JNIEnv *env, int code, const char *msg) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderError(env, MODULE_CODE_AUDIO, code, msg);
    }
}

/**
 * 解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderComplete方法）
 */
void AudioDecoder::onComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderCompleted(env, MODULE_CODE_AUDIO);
    }
}

/**
 * 指定位置解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderSeekComplete方法）
 */
void AudioDecoder::onSeekComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderSeekCompleted(env,MODULE_CODE_AUDIO);
    }
}