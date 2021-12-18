//
// Created by fgrid on 2021/12/18.
//

#include "opengl_video_track.h"
#include "../../../utils/logger.h"
#include "../render/video/opengl_video_render.h"
#include "../../media_codes.h"

FFmpegOpenGLVideoTrack::FFmpegOpenGLVideoTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback): BaseFFmpegTrack(env, url, iTrackCallback) {
    TAG = "FFmpegOpenGLVideoTrack";
}

FFmpegOpenGLVideoTrack::~FFmpegOpenGLVideoTrack() {
    LOGE(TAG, "%s", "~FFmpegOpenGLVideoTrack");
}

AVMediaType FFmpegOpenGLVideoTrack::GetMediaType() {
    return AVMEDIA_TYPE_VIDEO;
}

void FFmpegOpenGLVideoTrack::CreateTargetRender(JNIEnv *env) {
    m_render = new FFmpegOpenGLVideoRender(env,this,
                                           GetVideoWidth(), GetVideoHeight(),
                                           m_codec_ctx->pix_fmt,
                                           m_codec_ctx->time_base,
                                           m_format_ctx->streams[m_stream_index]->time_base);
}

void FFmpegOpenGLVideoTrack::OnRenderPrepared(JNIEnv *jniEnv, int render) {
    LOGE(TAG, "OnRenderPrepared: %d", prepare_count)
    ++prepare_count;
    if (m_state != ERROR && m_state != STOPPED &&prepare_count > 1) {
        m_state = PREPARED;
        WakeUpState();
        OnPrepared(jniEnv);
    }
}

void FFmpegOpenGLVideoTrack::OnPrepared(JNIEnv *env) {
    if (prepare_count > 1 && m_i_track_callback != NULL) {
        int width = GetVideoWidth();
        int height = GetVideoHeight();
        int rotation = GetVideoRotation();
        m_i_track_callback->OnInfo(env, MODULE_CODE_VIDEO, width, height, rotation);
        m_i_track_callback->OnPrepared(env, MODULE_CODE_VIDEO);
    }
}

void FFmpegOpenGLVideoTrack::OnError(JNIEnv *env, int code, const char *msg) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnError(env, MODULE_CODE_VIDEO, code, msg);
    }
}

void FFmpegOpenGLVideoTrack::OnComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnCompleted(env, MODULE_CODE_VIDEO);
    }
}

void FFmpegOpenGLVideoTrack::OnSeekComplete(JNIEnv *env) {
    if (m_i_track_callback != NULL) {
        m_i_track_callback->OnSeekCompleted(env, MODULE_CODE_VIDEO);
    }
}

int FFmpegOpenGLVideoTrack::GetVideoWidth() {
    return (videoWidth = (m_codec_ctx == NULL ? 0 : m_codec_ctx->width));
}

int FFmpegOpenGLVideoTrack::GetVideoHeight() {
    return (videoHeight = (m_codec_ctx == NULL ? 0 : m_codec_ctx->height));
}

int FFmpegOpenGLVideoTrack::GetVideoRotation() {
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

void FFmpegOpenGLVideoTrack::SetSurface(JNIEnv *jniEnv, jobject surface) {
    if (IsRunning() && m_render != NULL) {
        ((FFmpegOpenGLVideoRender*) m_render)->SetSurface(jniEnv, surface);
    }
}

void FFmpegOpenGLVideoTrack::AddDraw(Drawer *drawer) {
    if (m_state != ERROR && m_state != STOPPED && m_render != NULL) {
        ((FFmpegOpenGLVideoRender*) m_render)->AddDrawer(drawer);
    }
}