//
// Created by fgrid on 2021/12/17.
//

#include "audio_track.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"
#include "../render/audio/opensl_render.h"

FFmpegAudioTrack::FFmpegAudioTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback): BaseFFmpegTrack(env, url, iTrackCallback) {
    TAG = "FFmpegAudioTrack";
}

FFmpegAudioTrack::~FFmpegAudioTrack() {
    LOGE(TAG, "%s", "~FFmpegAudioTrack");
}

AVMediaType FFmpegAudioTrack::GetMediaType() {
    return AVMEDIA_TYPE_AUDIO;
}

void FFmpegAudioTrack::CreateTargetRender(JNIEnv *env) {
    m_render = new FFmpegOpenSLRender(env, this, m_codec_ctx, m_codec_ctx->time_base);
}

void FFmpegAudioTrack::OnPrepared(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnPrepared(env, MODULE_CODE_AUDIO);
    }
}

void FFmpegAudioTrack::OnError(JNIEnv *env, int code, const char *msg) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnError(env, MODULE_CODE_AUDIO, code, msg);
    }
}

void FFmpegAudioTrack::OnComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnCompleted(env, MODULE_CODE_AUDIO);
    }
}

void FFmpegAudioTrack::OnSeekComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnSeekCompleted(env, MODULE_CODE_AUDIO);
    }
}

jint FFmpegAudioTrack::GetMaxVolumeLevel() {
    if (IsRunning() && m_render != NULL) {
        return ((FFmpegOpenSLRender *) m_render)->GetMaxVolumeLevel();
    }
    return 0;
}

jint FFmpegAudioTrack::GetVolumeLevel() {
    if (IsRunning() && m_render != NULL) {
        return ((FFmpegOpenSLRender *) m_render)->GetVolumeLevel();
    }
    return 0;
}

void FFmpegAudioTrack::SetVolumeLevel(jint volume) {
    if (IsRunning() && m_render != NULL) {
        ((FFmpegOpenSLRender *) m_render)->SetVolumeLevel(volume);
    }
}