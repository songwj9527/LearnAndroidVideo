//
// Created by fgrid on 2/2/21.
//

#include "video_decoder.h"
#include "../../player/player.h"
#include "../../render/video/base_video_render.h"

VideoDecoder::VideoDecoder(JNIEnv *env, Player *mediaPlayer, const char *url, BaseVideoRender *render, bool for_synthesizer): BaseDecoder(env, mediaPlayer, url, render, for_synthesizer) {
    TAG = "VideoDecoder";
}

VideoDecoder::~VideoDecoder() {
    LOGE(TAG, "%s", "~VideoDecoder");
}

/**
 * 音视频索引
 */
AVMediaType VideoDecoder::getMediaType() {
    return AVMEDIA_TYPE_VIDEO;
}

/**
 * 解码准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderPrepared方法）
 */
void VideoDecoder::onPrepared(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        int width = getVideoWidth();
        int height = getVideoHeight();
        int rotation = getVideoRotation();
        mediaPlayer->onDecoderInfo(env, MODULE_CODE_VIDEO, width, height, rotation);
        mediaPlayer->onDecoderPrepared(env, MODULE_CODE_VIDEO);
    }
}

/**
 * 解码线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderError方法）
 */
void VideoDecoder::onError(JNIEnv *env, int code, const char *msg) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderError(env,MODULE_CODE_VIDEO, code, msg);
    }
}

/**
 * 解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderComplete方法）
 */
void VideoDecoder::onComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderCompleted(env,MODULE_CODE_VIDEO);
    }
}

/**
 * 指定位置解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderSeekComplete方法）
 */
void VideoDecoder::onSeekComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onDecoderSeekCompleted(env,MODULE_CODE_VIDEO);
    }
}

/**
 * 视频宽度
 * @return
 */
int VideoDecoder::getVideoWidth() {
    if (videoWidth > 0) {
        return videoWidth;
    }
    return (videoWidth = m_codec_ctx == NULL ? 0 : m_codec_ctx->width);
}

/**
 * 视频高度
 * @return
 */
int VideoDecoder::getVideoHeight() {
    if (videoHeight > 0) {
        return videoHeight;
    }
    return (videoHeight = m_codec_ctx == NULL ? 0 : m_codec_ctx->height);
}

/**
 * 获取视频旋转角度
 * @return
 */
int VideoDecoder::getVideoRotation() {
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