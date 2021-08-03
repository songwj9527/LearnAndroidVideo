//
// Created by fgrid on 1/25/21.
//
#include "./media_player.h"
#include "../../decoder/video/video_decoder.h"
#include "../../render/video/default/default_video_render.h"
#include "../../decoder/audio/audio_decoder.h"
#include "../../render/audio/opensl_render.h"

MediaPlayer::MediaPlayer(JNIEnv *jniEnv, jobject object) : Player(jniEnv, object) {}

MediaPlayer::~MediaPlayer() {
    LOGE(TAG, "%s", "~MediaPlayer");
}

/**
 * 准备播放（初始化解码器等）
 */
void MediaPlayer::prepareSync() {
    if (jniEnv == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    if (sourceURL == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    reset();
    videoRender = new DefaultVideoRender();
    videoDecoder = new VideoDecoder(jniEnv, this, sourceURL, videoRender, false);
    openSlRender = new OpenSLRender();
    audioDecoder = new AudioDecoder(jniEnv, this, sourceURL, openSlRender, false);
    LOGE(TAG, "%s", "prepareSync()");
}
