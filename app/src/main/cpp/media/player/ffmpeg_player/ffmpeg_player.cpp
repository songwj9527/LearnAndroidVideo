//
// Created by fgrid on 2021/7/20.
//

#include "ffmpeg_player.h"

#include "../../decoder/video/video_decoder.h"
#include "../../render/video/base_video_render.h"
#include "../../decoder/audio/audio_decoder.h"
#include "../../render/audio/opensl_render.h"

FFmpegPlayer::FFmpegPlayer(JNIEnv *jniEnv, jobject object) : Player(jniEnv, object) {}

FFmpegPlayer::~FFmpegPlayer() {
    // 此处不需要 delete 成员指针
    // 在BaseDecoder中的线程已经使用智能指针，会自动释放
    LOGE(TAG, "%s", "~FFmpegPlayer");
    release();
}

/**
 * 设置视频渲染窗口
 * @param surface
 */
void FFmpegPlayer::setSurface(jobject surface) {
    LOGE(TAG, "%s", "setSurface()");
    if (state == STOPPED) {
        return;
    }
    if (jsurface != NULL) {
        if (!jniEnv->IsSameObject(jsurface, NULL)) {
            jniEnv->DeleteGlobalRef(jsurface);
        }
        jsurface = NULL;
    }
    if (surface != NULL) {
        jsurface = jniEnv->NewGlobalRef(surface);
        LOGE(TAG, "%s%s", "setSurface() ", jsurface == NULL ? "NULL" : "OK");
    }
    if (videoRenderPrepared && videoRender != NULL) {
        videoRender->setSurface(jniEnv, jsurface);
    }
}

/**
 * 音视频同步基准时钟
 */
void FFmpegPlayer::setSyncClock(double clock) {
//    int64_t timestamp = (int64_t)((clock - sync_clock) * 1000);
    sync_clock = clock;
}

/**
 * 音视频同步基准时钟
 */
double FFmpegPlayer::getSyncClock() {
    return sync_clock;
}

/**
 * 开始播放
 */
void FFmpegPlayer::start() {
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (state != RUNNING && state != STOPPED) {
            state = RUNNING;
            LOGE(TAG, "%s", "start()");
            if (videoDecoder != NULL) {
                videoDecoder->start();
            }
            if (audioDecoder != NULL) {
                audioDecoder->start();
            }
            if (videoRender != NULL) {
                videoRender->start();
            }
            if (openSlRender != NULL) {
                openSlRender->start();
            }
        }
    }
}

/**
 * 继续播放
 */
void FFmpegPlayer::resume() {
    LOGE(TAG, "%s", "resume() 0 ");
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        LOGE(TAG, "%s", "resume() 1");
        if (state == PAUSED) {
            LOGE(TAG, "%s", "resume() 2");
            state = RUNNING;
            if (videoDecoder != NULL) {
                videoDecoder->resume();
            }
            if (videoRender != NULL) {
                videoRender->resume();
            }
            if (audioDecoder != NULL) {
                audioDecoder->resume();
            }
            if (openSlRender != NULL) {
                openSlRender->resume();
            }
        }
    }
}

/**
 * 暂停播放
 */
void FFmpegPlayer::pause() {
    LOGE(TAG, "%s", "pause()");
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (state == RUNNING) {
            state = PAUSED;
            if (videoRender != NULL) {
                videoRender->pause();
            }
            if (openSlRender != NULL) {
                openSlRender->pause();
            }
            if (videoDecoder != NULL) {
                videoDecoder->pause();
            }
            if (audioDecoder != NULL) {
                audioDecoder->pause();
            }
        }
    }
}

/**
 * 停止播放
 */
void FFmpegPlayer::stop() {
    LOGE(TAG, "%s", "stop()");
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (state != STOPPED) {
            state = STOPPED;
        }
    }
    if (videoDecoder != NULL) {
        videoDecoder->stop();
        videoDecoder = NULL;
    }
    if (audioDecoder != NULL) {
        audioDecoder->stop();
        audioDecoder = NULL;
    }
    if (videoRender != NULL) {
        videoRender->stop();
        videoRender = NULL;
    }
    if (openSlRender != NULL) {
        // 这里可能会奔溃，具体原因还没找到
        openSlRender->stop();
//        delete openSlRender;
        openSlRender = NULL;
    }
}

/**
 * 重置播放器
 */
void FFmpegPlayer::reset() {
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (videoDecoder != NULL) {
        videoDecoder->stop();
        videoDecoder = NULL;
    }
    if (audioDecoder != NULL) {
        audioDecoder->stop();
        audioDecoder = NULL;
    }
    if (videoRender != NULL) {
        videoRender->release();
        videoRender = NULL;
    }
    if (openSlRender != NULL) {
        // 这里可能会奔溃，具体原因还没找到
        openSlRender->release();
//        delete openSlRender;
        openSlRender = NULL;
    }
    videoDecoderPrepared = false;
    videoRenderPrepared = false;
    audioDecoderPrepared = false;
    audioRenderPrepared = false;
    videoDecoderCompleted = false;
    videoRenderCompleted = false;
    audioDecoderCompleted = false;
    audioRenderCompleted = false;
    state = IDLE;
}

/**
 * 释放播放器
 */
void FFmpegPlayer::release() {
    LOGE(TAG, "%s", "release()");
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (videoDecoder != NULL) {
        videoDecoder->stop();
        videoDecoder = NULL;
    }
    if (audioDecoder != NULL) {
        audioDecoder->stop();
        audioDecoder = NULL;
    }
    if (videoRender != NULL) {
        LOGE(TAG, "%s", "VideoRender release() 0");
        videoRender->release();
        LOGE(TAG, "%s", "VideoRender release() 1");
        videoRender = NULL;
    }
    if (openSlRender != NULL) {
        LOGE(TAG, "%s", "openSlRender release() 0");
        // 这里可能会奔溃，具体原因还没找到
        openSlRender->release();
        LOGE(TAG, "%s", "openSlRender release() 1")
//        delete openSlRender;
        openSlRender = NULL;
    }
    videoDecoderPrepared = false;
    videoRenderPrepared = false;
    audioDecoderPrepared = false;
    audioRenderPrepared = false;
    videoDecoderCompleted = false;
    videoRenderCompleted = false;
    audioDecoderCompleted = false;
    audioRenderCompleted = false;

    LOGE(TAG, "%s", "release() 1");
    // 释放source url相关资源
    releaseSourceURL();
    LOGE(TAG, "%s", "release() 2");
}

/**
 * 获取当前视频时长（单位：ms）
 * @return
 */
jlong FFmpegPlayer::getDuration() {
    LOGE(TAG, "%s", "getDuration() 0");
    jlong ret = 0;
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (videoDecoder != NULL) {
            ret = videoDecoder->getDuration();
        }
        else if (audioDecoder != NULL) {
            ret = audioDecoder->getDuration();
        }
    }
    LOGE(TAG, "getDuration() %lld", ret);
    return ret;
}

/**
 * 获取当前视频播放位置（单位：ms）
 * @return
 */
jlong FFmpegPlayer::getCurrentPosition() {
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (videoRender != NULL && state != COMPLETED) {
            return videoRender->getCurrentPosition();
        }
        return (jlong) (sync_clock * 1000);
    }
    return (jlong) 0;
}

/**
 * 指定位置播放
 * @param position
 */
void FFmpegPlayer::seekTo(jlong position) {
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        State temp = state;
        state = SEEKING;
        videoDecoderSeekCompleted = false;
        audioDecoderSeekCompleted = false;
        videoDecoderCompleted = false;
        audioDecoderCompleted = false;
        videoRenderCompleted = false;
        audioRenderCompleted = false;
        if (videoDecoder != NULL) {
            videoDecoder->seekTo(position);
        }
        if (audioDecoder != NULL) {
            audioDecoder->seekTo(position);
        }
    }
//    onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
}

/**
 * 获取最大音量
 * @return
 */
jint FFmpegPlayer::getMaxVolumeLevel() {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            return openSlRender->getMaxVolumeLevel();
        }
    }
    return 0;
}

/**
 * 获取当前音量
 * @return
 */
jint FFmpegPlayer::getVolumeLevel() {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            return openSlRender->getVolumeLevel();
        }
    }
    return 0;
}

/**
 * 设置音量
 * @param volume
 */
void FFmpegPlayer::setVolumeLevel(jint volume) {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            openSlRender->setVolumeLevel(volume);
        }
    }
}

void FFmpegPlayer::onDecoderInfo(JNIEnv *env, int decoder, int videoWidth, int videoHeight, int videoRotation) {
    LOGE(TAG, "onDecoderInfo: %d, %d, %d, %d", decoder, videoWidth, videoHeight, videoRotation);
    char videoRotationChar[20];
    memset(videoRotationChar, 0, 20);
    sprintf(videoRotationChar, "%d", videoRotation);
    const char *result = videoRotationChar;
    jstring videoRotationJString = (env)->NewStringUTF(result);
    onPostEventToJava(env, CALLBACK_INFO, videoWidth, videoHeight, videoRotationJString);
}

void FFmpegPlayer::onDecoderPrepared(JNIEnv *env, int decoder) {
    LOGE(TAG, "onDecoderPrepared: %d", decoder);
    if (decoder == MODULE_CODE_VIDEO) {
        videoDecoderPrepared = true;
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        audioDecoderPrepared = true;
    }
}

void FFmpegPlayer::onDecoderBufferUpdate(JNIEnv *env, int decoder, int progress) {}

void FFmpegPlayer::onDecoderCompleted(JNIEnv *env, int decoder) {
    LOGE(TAG, "onDecoderCompleted: %d", decoder);
    if (decoder == MODULE_CODE_VIDEO) {
        videoDecoderCompleted = true;
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        audioDecoderCompleted = true;
    }
    if (videoRenderCompleted && audioRenderCompleted) {
//    if (audioRenderCompleted) {
//    if (videoRenderCompleted) {
        state = COMPLETED;
        onPostEventToJava(env, CALLBACK_COMPLETED, 0, 0, NULL);
    }
}

/**
 * 给解码器调用，用于告知MediaPlayer哪个解码器完成了指定位置
 * @param jniEnv
 * @param decoder
 */
void FFmpegPlayer::onDecoderSeekCompleted(JNIEnv *jniEnv, int decoder) {
    LOGE(TAG, "onDecoderSeekCompleted: %d", decoder);
    if (decoder == MODULE_CODE_VIDEO) {
        videoDecoderSeekCompleted = true;
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        audioDecoderSeekCompleted = true;
    }
    if (videoDecoderSeekCompleted && audioDecoderSeekCompleted) {
//    if (audioDecoderSeekCompleted) {
//    if (videoDecoderSeekCompleted) {
        if (state != COMPLETED) {
            state = PAUSED;
        }
        onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onDecoderError(JNIEnv *env, int decoder, int code, const char *msg) {
    LOGE(TAG, "onDecoderError: %d, %d, %s", decoder, code, msg);
    onError(env, code, msg);
    if (decoder == MODULE_CODE_VIDEO) {
        if (videoDecoder != NULL) {
            videoDecoder->stop();
            videoDecoder = NULL;
        }
        if (videoRender != NULL) {
            videoRender->stop();
            videoRender = NULL;
        }
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        if (audioDecoder != NULL) {
            audioDecoder->stop();
            audioDecoder = NULL;
        }
        if (openSlRender != NULL) {
            LOGE(TAG, "%s", "openSlRender release() 0");
            // 这里可能会奔溃，具体原因还没找到
            openSlRender->stop();
            LOGE(TAG, "%s", "openSlRender release() 1");
//            delete openSlRender;
            openSlRender = NULL;
        }
    }
}

void FFmpegPlayer::onRenderPrepared(JNIEnv *env, int render) {
    LOGE(TAG, "onRenderPrepared(): %d", render);
    if (render == MODULE_CODE_VIDEO) {
        videoRenderPrepared = true;
        if (videoRender != NULL) {
            videoRender->setSurface(env, jsurface);
        }
    }
    else if (render == MODULE_CODE_AUDIO) {
        audioRenderPrepared = true;
    }
    if (videoRenderPrepared && audioRenderPrepared && state == IDLE) {
//    if (audioRenderPrepared && state == IDLE) {
//    if (videoRenderPrepared && state == IDLE) {
        state = PREPARED;
        onPostEventToJava(env, CALLBACK_PREPARED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onRenderCompleted(JNIEnv *env, int render) {
    LOGE(TAG, "onRenderCompleted: %d", render);
    if (render == MODULE_CODE_VIDEO) {
        videoRenderCompleted = true;
    }
    else if (render == MODULE_CODE_AUDIO) {
        audioRenderCompleted = true;
    }
    if (videoRenderCompleted && audioRenderCompleted) {
//    if (audioRenderCompleted) {
//    if (videoRenderCompleted) {
        state = COMPLETED;
        sync_clock = getDuration();
        onPostEventToJava(env, CALLBACK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onRenderError(JNIEnv *env, int render, int code, const char *msg) {
    LOGE(TAG, "onRenderError: %d, %d, %s", render, code, msg);
    onError(env, code, msg);
    if (render == MODULE_CODE_VIDEO) {
        if (videoDecoder != NULL) {
            videoDecoder->stop();
            videoDecoder = NULL;
        }
        if (videoRender != NULL) {
            videoRender->stop();
            videoRender = NULL;
        }
    }
    else if (render == MODULE_CODE_AUDIO) {
        if (audioDecoder != NULL) {
            audioDecoder->stop();
            audioDecoder = NULL;
        }
        if (openSlRender != NULL) {
            LOGE(TAG, "%s", "openSlRender release() 0");
            // 这里可能会奔溃，具体原因还没找到
            openSlRender->stop();
            LOGE(TAG, "%s", "openSlRender release() 1");
//            delete openSlRender;
            openSlRender = NULL;
        }
    }
}