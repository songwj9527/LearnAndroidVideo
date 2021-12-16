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
    release();
    LOGE(TAG, "%s", "~FFmpegPlayer");
}

/**
 * 设置视频渲染窗口
 * @param surface
 */
void FFmpegPlayer::setSurface(jobject surface) {
    LOGE(TAG, "%s", "setSurface()");
    pthread_mutex_lock(&state_mutex);
    if (state != STOPPED) {
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
    pthread_mutex_unlock(&state_mutex);
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
    pthread_mutex_lock(&state_mutex);
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (state != RUNNING && state != STOPPED) {
            state = RUNNING;
            LOGE(TAG, "%s", "start()");
            if (videoRender != NULL) {
                videoRender->start();
            }
            if (openSlRender != NULL) {
                openSlRender->start();
            }
            if (videoDecoder != NULL) {
                videoDecoder->start();
            }
            if (audioDecoder != NULL) {
                audioDecoder->start();
            }
        }
    }
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 继续播放
 */
void FFmpegPlayer::resume() {
    LOGE(TAG, "%s", "resume() 0 ");
    pthread_mutex_lock(&state_mutex);
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
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 暂停播放
 */
void FFmpegPlayer::pause() {
    LOGE(TAG, "%s", "pause()");
    pthread_mutex_lock(&state_mutex);
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
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 停止播放
 */
void FFmpegPlayer::stop() {
    LOGE(TAG, "%s", "stop()");
    pthread_mutex_lock(&state_mutex);
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (state != STOPPED) {
            state = STOPPED;
        }
    }
    if (videoRender != NULL) {
        videoRender->stop();
    }
    if (openSlRender != NULL) {
        // 这里可能会奔溃，具体原因还没找到
        openSlRender->stop();
//        delete openSlRender;
    }
    if (videoDecoder != NULL) {
        videoDecoder->stop();
    }
    if (audioDecoder != NULL) {
        audioDecoder->stop();
    }
    videoDecoder = NULL;
    audioDecoder = NULL;
    videoRender = NULL;
    openSlRender = NULL;
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 重置播放器
 */
void FFmpegPlayer::reset() {
    pthread_mutex_lock(&state_mutex);
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
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 释放播放器
 */
void FFmpegPlayer::release() {
    LOGE(TAG, "%s", "release()");
    pthread_mutex_lock(&state_mutex);
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (videoRender != NULL) {
        videoRender->release();
    }
    if (openSlRender != NULL) {
        // 这里可能会奔溃，具体原因还没找到
        openSlRender->release();
//        delete openSlRender;
    }
    if (videoDecoder != NULL) {
        videoDecoder->stop();
    }
    if (audioDecoder != NULL) {
        audioDecoder->stop();
    }
    videoRender = NULL;
    videoDecoder = NULL;
    openSlRender = NULL;
    audioDecoder = NULL;
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
    pthread_mutex_unlock(&state_mutex);
}

/**
 * 获取当前视频时长（单位：ms）
 * @return
 */
jlong FFmpegPlayer::getDuration() {
    LOGE(TAG, "%s", "getDuration() 0");
    jlong ret = 0;
    pthread_mutex_lock(&state_mutex);
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (videoDecoder != NULL) {
            ret = videoDecoder->getDuration();
        }
        if (audioDecoder != NULL) {
            if (ret < audioDecoder->getDuration()) {
                ret = audioDecoder->getDuration();
            }
        }
    }
    pthread_mutex_unlock(&state_mutex);
    LOGE(TAG, "getDuration() %lld", ret);
    return ret;
}

/**
 * 获取当前视频播放位置（单位：ms）
 * @return
 */
jlong FFmpegPlayer::getCurrentTimestamp() {
    jlong timestamp = 0L;
    pthread_mutex_lock(&state_mutex);
    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
//    if (audioDecoderPrepared && audioRenderPrepared) {
//    if (videoDecoderPrepared && videoRenderPrepared) {
        if (videoRender != NULL && state != COMPLETED) {
            timestamp = videoRender->getCurrentPosition();
        }
        if (timestamp < ((jlong) (sync_clock * 1000))) {
            timestamp =  (jlong) (sync_clock * 1000);
        }
    }
    pthread_mutex_unlock(&state_mutex);
    return timestamp;
}

/**
 * 指定位置播放
 * @param position
 */
void FFmpegPlayer::seekTo(jlong position) {
    pthread_mutex_lock(&state_mutex);
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
    pthread_mutex_unlock(&state_mutex);
//    onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
}

/**
 * 获取最大音量
 * @return
 */
jint FFmpegPlayer::getMaxVolumeLevel() {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    jint volumeLevel = 0;
    pthread_mutex_lock(&state_mutex);
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            volumeLevel = openSlRender->getMaxVolumeLevel();
        }
    }
    pthread_mutex_unlock(&state_mutex);
    return volumeLevel;
}

/**
 * 获取当前音量
 * @return
 */
jint FFmpegPlayer::getVolumeLevel() {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    jint volumeLevel = 0;
    pthread_mutex_lock(&state_mutex);
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            volumeLevel = openSlRender->getVolumeLevel();
        }
    }
    pthread_mutex_unlock(&state_mutex);
    return volumeLevel;
}

/**
 * 设置音量
 * @param volume
 */
void FFmpegPlayer::setVolumeLevel(jint volume) {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    pthread_mutex_lock(&state_mutex);
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            openSlRender->setVolumeLevel(volume);
        }
    }
    pthread_mutex_unlock(&state_mutex);
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
    pthread_mutex_lock(&state_mutex);
    if (decoder == MODULE_CODE_VIDEO) {
        videoDecoderPrepared = true;
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        audioDecoderPrepared = true;
    }
    pthread_mutex_unlock(&state_mutex);
}

void FFmpegPlayer::onDecoderBufferUpdate(JNIEnv *env, int decoder, int progress) {}

void FFmpegPlayer::onDecoderCompleted(JNIEnv *env, int decoder) {
    LOGE(TAG, "onDecoderCompleted: %d", decoder);
    bool completed = false;
    pthread_mutex_lock(&state_mutex);
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
        completed = true;
    }
    pthread_mutex_unlock(&state_mutex);
    if (completed) {
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
    bool completed = false;
    pthread_mutex_lock(&state_mutex);
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
        completed = true;
    }
    pthread_mutex_unlock(&state_mutex);
    if (completed) {
        onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onDecoderError(JNIEnv *env, int decoder, int code, const char *msg) {
    LOGE(TAG, "onDecoderError: %d, %d, %s", decoder, code, msg);
    onError(env, code, msg);
    if (decoder == MODULE_CODE_VIDEO) {
        pthread_mutex_lock(&state_mutex);
        if (videoDecoder != NULL) {
            videoDecoder->stop();
            videoDecoder = NULL;
        }
        if (videoRender != NULL) {
            videoRender->stop();
            videoRender = NULL;
        }
        pthread_mutex_unlock(&state_mutex);
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        pthread_mutex_lock(&state_mutex);
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
        pthread_mutex_unlock(&state_mutex);
    }
}

void FFmpegPlayer::onRenderPrepared(JNIEnv *env, int render) {
    LOGE(TAG, "onRenderPrepared(): %d", render);
    bool prepared = false;
    pthread_mutex_lock(&state_mutex);
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
        prepared = true;
    }
    pthread_mutex_unlock(&state_mutex);
    if (prepared) {
        onPostEventToJava(env, CALLBACK_PREPARED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onRenderCompleted(JNIEnv *env, int render) {
    LOGE(TAG, "onRenderCompleted: %d", render);
    bool completed = false;
    pthread_mutex_lock(&state_mutex);
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
        completed = true;
    }
    pthread_mutex_unlock(&state_mutex);
    if (completed) {
        onPostEventToJava(env, CALLBACK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegPlayer::onRenderError(JNIEnv *env, int render, int code, const char *msg) {
    LOGE(TAG, "onRenderError: %d, %d, %s", render, code, msg);
    onError(env, code, msg);
    if (render == MODULE_CODE_VIDEO) {
        pthread_mutex_lock(&state_mutex);
        if (videoDecoder != NULL) {
            videoDecoder->stop();
            videoDecoder = NULL;
        }
        if (videoRender != NULL) {
            videoRender->stop();
            videoRender = NULL;
        }
        pthread_mutex_unlock(&state_mutex);
    }
    else if (render == MODULE_CODE_AUDIO) {
        pthread_mutex_lock(&state_mutex);
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
        pthread_mutex_unlock(&state_mutex);
    }
}