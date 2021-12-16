//
// Created by fgrid on 2021/7/20.
//

#include "opengl_player.h"
#include "../../../decoder/video/video_decoder.h"
#include "../../../render/video/opengl/opengl_render.h"
#include "../../../decoder/audio/audio_decoder.h"
#include "../../../render/audio/opensl_render.h"
#include "../../../opengl/drawer/video_drawer.h"
#include "../../../opengl/drawer/fbo_soul_video_drawer.h"

OpenGLPlayer::OpenGLPlayer(JNIEnv *jniEnv, jobject object) : FFmpegPlayer(jniEnv, object) {}

OpenGLPlayer::~OpenGLPlayer() {
    LOGE(TAG, "%s", "~OpenGLPlayer");
}

/**
 * 准备播放（初始化解码器等）
 */
void OpenGLPlayer::prepareSync() {
    if (jniEnv == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    if (sourceURL == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    reset();
    videoRender = new OpenGLRender(false);
//    ((OpenGLRender *) videoRender)->addDrawer(new VideoDrawer());
    ((OpenGLRender *) videoRender)->addDrawer(new FBOSoulVideoDrawer());
    videoDecoder = new VideoDecoder(jniEnv, this, sourceURL, videoRender, false);
    openSlRender = new OpenSLRender(false);
    audioDecoder = new AudioDecoder(jniEnv, this, sourceURL, openSlRender, false);
    LOGE(TAG, "%s", "prepareSync()");
}