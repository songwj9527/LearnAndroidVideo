//
// Created by fgrid on 2021/7/20.
//

#include "player.h"

#include "../decoder/video/video_decoder.h"
#include "../render/video/base_video_render.h"
#include "../decoder/audio/audio_decoder.h"
#include "../render/audio/opensl_render.h"

Player::Player(JNIEnv *jniEnv, jobject object) {
    this->jniEnv = jniEnv;
    this->jplayer = jniEnv->NewWeakGlobalRef(object);
}

Player::~Player() {
    // 此处不需要 delete 成员指针
    // 在BaseDecoder中的线程已经使用智能指针，会自动释放
    LOGE(TAG, "%s", "~Player");
    release();
}

/**
 * 释放java层player对象的弱应用
 */
void Player::releaseJPlayer() {
    if (jniEnv == NULL) {
        return;
    }
    if (jplayer != NULL) {
        if (!jniEnv->IsSameObject(jplayer, NULL)) {
            jniEnv->DeleteWeakGlobalRef(jplayer);
        }
        jplayer = NULL;
    }
    if (jsurface != NULL) {
        if (!jniEnv->IsSameObject(jsurface, NULL)) {
            jniEnv->DeleteGlobalRef(jsurface);
        }
        jsurface = NULL;
    }
}

/**
 * 释放source url相关资源
 */
void Player::releaseSourceURL() {
    if (jniEnv == NULL) {
        return;
    }
    // 释放转换参数
    if (jsourceURL != NULL && sourceURL != NULL) {
        if (!jniEnv->IsSameObject(jsourceURL, NULL)) {
            jniEnv->ReleaseStringUTFChars((jstring) jsourceURL, sourceURL);
            jniEnv->DeleteGlobalRef(jsourceURL);
        }
    }
    sourceURL = NULL;
    jsourceURL = NULL;
}

/**
 * 准备播放（初始化解码器等）
 * @param url
 */
void Player::setDataSource(jstring url) {
    if (jniEnv == NULL) {
        return;
    }
    releaseSourceURL();
    jsourceURL = jniEnv->NewGlobalRef(url);
    const char *urlChar = jniEnv->GetStringUTFChars(url, NULL);
    if (urlChar == NULL) {
        sourceURL = NULL;
        onError(jniEnv, JAVA_PATH_2_C_CHARS_FAILED, "传入的url转chars失败");
        return;
    }
    if (strlen(urlChar) == 0) {
        sourceURL = NULL;
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    sourceURL = urlChar;
    LOGE(TAG, "setDataSource(): %s", sourceURL);
}

/**
 * 设置视频渲染窗口
 * @param surface
 */
void Player::setSurface(jobject surface) {
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
void Player::setSyncClock(double clock) {
//    int64_t timestamp = (int64_t)((clock - sync_clock) * 1000);
    sync_clock = clock;
}

/**
 * 音视频同步基准时钟
 */
double Player::getSyncClock() {
    return sync_clock;
}

/**
 * 开始播放
 */
void Player::start() {
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
void Player::resume() {
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
void Player::pause() {
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
void Player::stop() {
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
        delete openSlRender;
        openSlRender = NULL;
    }
}

/**
 * 重置播放器
 */
void Player::reset() {
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
        delete openSlRender;
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
void Player::release() {
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
        delete openSlRender;
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
    // 释放java层player对象的弱应用
    releaseJPlayer();
    LOGE(TAG, "%s", "release() 2");
    // 释放source url相关资源
    releaseSourceURL();
    LOGE(TAG, "%s", "release() 3");
}

/**
 * 获取当前视频时长（单位：ms）
 * @return
 */
jlong Player::getDuration() {
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
jlong Player::getCurrentPosition() {
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
void Player::seekTo(jlong position) {
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
jint Player::getMaxVolumeLevel() {
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
jint Player::getVolumeLevel() {
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
void Player::setVolumeLevel(jint volume) {
//    if (videoDecoderPrepared && videoRenderPrepared && audioDecoderPrepared && audioRenderPrepared) {
    if (audioDecoderPrepared && audioRenderPrepared) {
        if (openSlRender != NULL) {
            openSlRender->setVolumeLevel(volume);
        }
    }
}

void Player::onDecoderInfo(JNIEnv *env, int decoder, int videoWidth, int videoHeight, int videoRotation) {
    LOGE(TAG, "onDecoderInfo: %d, %d, %d, %d", decoder, videoWidth, videoHeight, videoRotation);
    char videoRotationChar[20];
    memset(videoRotationChar, 0, 20);
    sprintf(videoRotationChar, "%d", videoRotation);
    const char *result = videoRotationChar;
    jstring videoRotationJString = (env)->NewStringUTF(result);
    onPostEventToJava(env, CALLBACK_INFO, videoWidth, videoHeight, videoRotationJString);
}

void Player::onDecoderPrepared(JNIEnv *env, int decoder) {
    LOGE(TAG, "onDecoderPrepared: %d", decoder);
    if (decoder == MODULE_CODE_VIDEO) {
        videoDecoderPrepared = true;
    }
    else if (decoder == MODULE_CODE_AUDIO) {
        audioDecoderPrepared = true;
    }
}

void Player::onDecoderBufferUpdate(JNIEnv *env, int decoder, int progress) {}

void Player::onDecoderCompleted(JNIEnv *env, int decoder) {
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
void Player::onDecoderSeekCompleted(JNIEnv *jniEnv, int decoder) {
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

void Player::onDecoderError(JNIEnv *env, int decoder, int code, const char *msg) {
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
            delete openSlRender;
            openSlRender = NULL;
        }
    }
}

void Player::onRenderPrepared(JNIEnv *env, int render) {
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

void Player::onRenderCompleted(JNIEnv *env, int render) {
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

void Player::onRenderError(JNIEnv *env, int render, int code, const char *msg) {
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
            delete openSlRender;
            openSlRender = NULL;
        }
    }
}

/**
 * 异常或错误调用方法
 * @param errorCode
 * @param errorMsg
 */
void Player::onError(JNIEnv *env, int errorCode, const char *errorMsg) {
    if (env == NULL) {
        env = jniEnv;
    }
    jstring jerrorMsg;
    if (errorMsg != NULL && strlen(errorMsg)) {
        jerrorMsg = env->NewStringUTF(errorMsg);
    }
    onPostEventToJava(env, CALLBACK_ERROR, errorCode, 0, jerrorMsg);
}

/**
 * 给Java层回调方法
 * @param event
 * @param what
 * @param args1
 * @param args2
 */
void Player::onPostEventToJava(JNIEnv *env, int event, int what, int args1, jobject args2) {
    LOGE(TAG, "onPostEventToJava 0: %d, %d, %d", event, what, args1);
    if (env == NULL) {
        env = jniEnv;
    }
    //检查是否发送异常：env->ExceptionOccurred()检查当前是否异常
    // 或(*env)->ExceptionCheck(env)
    jthrowable ex = NULL;
    jclass clazz = NULL;
    if (jplayer != NULL) {
        if (JNI_TRUE != env->IsSameObject(jplayer, NULL)) {
            clazz = env->GetObjectClass(jplayer);
        } else {
            return;
        }
    } else {
//        clazz = jniEnv->FindClass("com/songwj/openvideo/ffmpeg/NativePlayer");
        return;
    }
    LOGE(TAG, "onPostEventToJava %d:", 1);
    ex = env->ExceptionOccurred();
    if (ex != NULL && clazz == NULL) {
        //让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();

        jclass newExc = env->FindClass("java/lang/IllegalArgumentException");
        if (newExc == NULL) {
            return;
        }
        env->ThrowNew(newExc, "Throw exception from JNI: FindClass for NativePlayer failed");
        return;
    } else if (clazz == NULL) {
        return;
    } else if (ex != NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();
    }
    LOGE(TAG, "onPostEventToJava %d:", 2);
    ex = NULL;
    // 获取jmethodID
    jmethodID methodId = env->GetStaticMethodID(clazz, "postEventFromNative", "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    ex = env->ExceptionOccurred();
    // 判断异常是否发送
    if (ex != NULL && methodId == NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();

        jclass newExc = env->FindClass("java/lang/NoSuchMethodException");
        if (newExc == NULL) {
            return;
        }
        env->ThrowNew(newExc, "Throw exception from JNI: GetMethodID for postEventFromNative() failed");
    } else if (methodId != NULL) {
        LOGE(TAG, "onPostEventToJava %d:", 3);
        env->CallStaticVoidMethod(clazz, methodId, jplayer, event, what, args1, args2);
        LOGE(TAG, "onPostEventToJava %d:", 4);
    } else if (ex != NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();
    }
}