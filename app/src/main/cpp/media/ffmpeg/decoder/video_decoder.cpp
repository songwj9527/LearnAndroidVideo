//
// Created by fgrid on 2021/12/16.
//

#include "video_decoder.h"
#include "../../../utils/logger.h"
#include "../../media_codes.h"

VideoDecoder::VideoDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback): BaseDecoder(env, url, iDecoderCallback) {
    TAG = "VideoDecoder";
}

VideoDecoder::~VideoDecoder() {
    LOGE(TAG, "%s", "~VideoDecoder");
}

AVMediaType VideoDecoder::GetMediaType() {
    return AVMEDIA_TYPE_VIDEO;
}

void VideoDecoder::OnPrepared(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        int width = GetVideoWidth();
        int height = GetVideoHeight();
        int rotation = GetVideoRotation();
        m_i_decoder_callback->OnDecoderInfo(env, MODULE_CODE_VIDEO, width, height, rotation);
        m_i_decoder_callback->OnDecoderPrepared(env, MODULE_CODE_VIDEO);
    }
}

void VideoDecoder::OnError(JNIEnv *env, int code, const char *msg) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderError(env,MODULE_CODE_VIDEO, code, msg);
    }
}

void VideoDecoder::OnComplete(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderCompleted(env,MODULE_CODE_VIDEO);
    }
}

void VideoDecoder::OnSeekComplete(JNIEnv *env) {
    if (m_i_decoder_callback != NULL) {
        m_i_decoder_callback->OnDecoderSeekCompleted(env,MODULE_CODE_VIDEO);
    }
}

int VideoDecoder::GetVideoWidth() {
    return (videoWidth = (m_codec_ctx == NULL ? 0 : m_codec_ctx->width));
}

int VideoDecoder::GetVideoHeight() {
    return (videoHeight = (m_codec_ctx == NULL ? 0 : m_codec_ctx->height));
}

int VideoDecoder::GetVideoRotation() {
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