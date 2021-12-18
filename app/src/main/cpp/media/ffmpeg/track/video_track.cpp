//
// Created by fgrid on 2021/12/17.
//

#include "video_track.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"
#include "../render/video/default_video_render.h"

FFmpegVideoTrack::FFmpegVideoTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback): BaseFFmpegTrack(env, url, iTrackCallback) {
    TAG = "FFmpegVideoTrack";
}

FFmpegVideoTrack::~FFmpegVideoTrack() {
    LOGE(TAG, "%s", "~FFmpegVideoTrack");
}

AVMediaType FFmpegVideoTrack::GetMediaType() {
    return AVMEDIA_TYPE_VIDEO;
}

void FFmpegVideoTrack::CreateTargetRender(JNIEnv *env) {
    m_render = new FFmpegDefaultVideoRender(env, this,
                                      GetVideoWidth(), GetVideoHeight(),
                                      m_codec_ctx->pix_fmt,
                                      m_codec_ctx->time_base,
                                      m_format_ctx->streams[m_stream_index]->time_base);
}

void FFmpegVideoTrack::OnPrepared(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        int width = GetVideoWidth();
        int height = GetVideoHeight();
        int rotation = GetVideoRotation();
        m_i_track_callback->OnInfo(env, MODULE_CODE_VIDEO, width, height, rotation);
        m_i_track_callback->OnPrepared(env, MODULE_CODE_VIDEO);
    }
}

void FFmpegVideoTrack::OnError(JNIEnv *env, int code, const char *msg) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnError(env, MODULE_CODE_VIDEO, code, msg);
    }
}

void FFmpegVideoTrack::OnComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnCompleted(env, MODULE_CODE_VIDEO);
    }
}

void FFmpegVideoTrack::OnSeekComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnSeekCompleted(env, MODULE_CODE_VIDEO);
    }
}

int FFmpegVideoTrack::GetVideoWidth() {
    return (videoWidth = (m_codec_ctx == NULL ? 0 : m_codec_ctx->width));
}

int FFmpegVideoTrack::GetVideoHeight() {
    return (videoHeight = (m_codec_ctx == NULL ? 0 : m_codec_ctx->height));
}

int FFmpegVideoTrack::GetVideoRotation() {
    if (videoRotation != -1) {
        return videoRotation;
    }
    int m_Rotate = 0;
    if (m_format_ctx == NULL) {
        return  m_Rotate;
    }
    if (m_stream_index == -1) {
        return  m_Rotate;
    }
    AVDictionaryEntry *tag = NULL;
    tag = av_dict_get(m_format_ctx->streams[m_stream_index]->metadata, "rotate", tag, 0);
    if (tag != NULL) {
        int angle = atoi(tag->value);
        angle %= 360;
        if (angle == 90){
            m_Rotate = 90;
        }
        else if (angle == 180){
            m_Rotate = 180;
        }
        else if (angle == 270){
            m_Rotate = 270;
        }
        else{
            m_Rotate = 0;
        }
        videoRotation = m_Rotate;
    }
    return m_Rotate;
}

void FFmpegVideoTrack::SetSurface(JNIEnv *jniEnv, jobject surface) {
    if (IsRunning() && m_render != NULL) {
        ((FFmpegDefaultVideoRender*) m_render)->SetSurface(jniEnv, surface);
    }
}