//
// Created by fgrid on 2021/12/16.
//

#include "default_player.h"

FFmpegDefaultPlayer::FFmpegDefaultPlayer(JNIEnv *jniEnv, jobject object) : Player(jniEnv, object) {

}

FFmpegDefaultPlayer::~FFmpegDefaultPlayer() {
    release();
    LOGE(TAG, "%s", "~FFmpegDefaultPlayer");
}

void FFmpegDefaultPlayer::prepareSync() {
    if (state == IDLE) {
        if (audio_track != NULL) {
            audio_track->Stop();
        }
        if (video_track != NULL) {
            video_track->Stop();
        }
        audio_track = new FFmpegAudioTrack(jniEnv, sourceURL, this);
        video_track = new FFmpegVideoTrack(jniEnv, sourceURL, this);
    }
}

void FFmpegDefaultPlayer::OnPrepared(JNIEnv *jniEnv, int track) {
    LOGE(TAG, "OnPrepared")
    if (state == IDLE) {
        ++prepare_state;
//        prepare_state = 2;
    }
    if (state == IDLE &&  prepare_state > 1) {
        state = PREPARED;
        onPostEventToJava(jniEnv, CALLBACK_PREPARED, 0, 0, NULL);
        if (prepare_state > 1 && video_track != NULL) {
            video_track->SetSurface(jniEnv, jsurface);
        }
    }
}

void FFmpegDefaultPlayer::OnInfo(JNIEnv *jniEnv, int track, int videoWidth, int videoHeight, int videoRotation) {
    LOGE(TAG, "OnInfo")
    if (state != STOPPED && state != ERROR) {
        char videoRotationChar[20];
        memset(videoRotationChar, 0, 20);
        sprintf(videoRotationChar, "%d", videoRotation);
        const char *result = videoRotationChar;
        jstring videoRotationJString = (jniEnv)->NewStringUTF(result);
        onPostEventToJava(jniEnv, CALLBACK_INFO, videoWidth, videoHeight, videoRotationJString);
    }
}

void FFmpegDefaultPlayer::OnError(JNIEnv *jniEnv, int track, int code, const char *msg) {
    LOGE(TAG, "OnError(): %d, %s", code, msg)
    if (state != STOPPED && state != ERROR) {
        state = ERROR;
        if (track == MODULE_CODE_VIDEO) {
            video_track = NULL;
            if (audio_track != NULL) {
                audio_track->Stop();
                audio_track = NULL;
            }
        }
        if (track == MODULE_CODE_AUDIO) {
            audio_track = NULL;
            if (video_track != NULL) {
                video_track->Stop();
                video_track = NULL;
            }
        }
        onError(jniEnv, code, msg);
    }
}

void FFmpegDefaultPlayer::OnCompleted(JNIEnv *jniEnv, int track) {
    if (state != STOPPED && state != ERROR) {
        ++complete_state;
//        complete_state = 2;
    }
    if (state != STOPPED && state != ERROR &&  complete_state > 1) {
        state = COMPLETED;
        onPostEventToJava(jniEnv, CALLBACK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegDefaultPlayer::OnSeekCompleted(JNIEnv *jniEnv, int track) {
    if (state != STOPPED && state != ERROR) {
        ++seek_complete_state;
//        seek_complete_state = 2;
    }
    if (state != STOPPED && state != ERROR &&  seek_complete_state > 1) {
        if (state != COMPLETED) {
            state = PAUSED;
        }
        onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
    }
}

void FFmpegDefaultPlayer::setSurface(jobject surface) {
    LOGD(TAG, "%s", "setSurface()");
    if (state != STOPPED && state != ERROR) {
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
        if (video_track != NULL) {
            video_track->SetSurface(jniEnv, jsurface);
        }
    }
}

void FFmpegDefaultPlayer::start() {
    LOGE(TAG, "%s", "start()");
    if (prepare_state > 1) {
        if (state == PREPARED || state == PAUSED) {
            state = RUNNING;
            if (audio_track != NULL) {
                audio_track->Start();
            }
            if (video_track != NULL) {
                video_track->Start();
            }
        }
    }
}

void FFmpegDefaultPlayer::resume() {
    LOGE(TAG, "%s", "resume()");
    if (prepare_state > 1) {
        if (state == PAUSED) {
            state = RUNNING;
            if (audio_track != NULL) {
                audio_track->Resume();
            }
            if (video_track != NULL) {
                video_track->Resume();
            }
        }
    }
}

void FFmpegDefaultPlayer::pause() {
    LOGE(TAG, "%s", "pause()");
    if (prepare_state > 1) {
        if (state == RUNNING) {
            state = PAUSED;
            if (audio_track != NULL) {
                audio_track->Pause();
            }
            if (video_track != NULL) {
                video_track->Pause();
            }
        }
    }
}

void FFmpegDefaultPlayer::stop() {
    LOGE(TAG, "%s", "stop()");
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (audio_track != NULL) {
        audio_track->Stop();
        audio_track = NULL;
    }
    if (video_track != NULL) {
        video_track->Stop();
        video_track = NULL;
    }
}

void FFmpegDefaultPlayer::reset() {
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (audio_track != NULL) {
        audio_track->Stop();
        audio_track = NULL;
    }
    if (video_track != NULL) {
        video_track->Stop();
        video_track = NULL;
    }
    prepare_state = 0;
    complete_state = 0;
    seek_complete_state = 0;
    sync_clock = 0;
    state = IDLE;
}

void FFmpegDefaultPlayer::release() {
    LOGE(TAG, "%s", "release()");
    if (state != STOPPED) {
        state = STOPPED;
    }
    if (audio_track != NULL) {
        audio_track->Stop();
        audio_track = NULL;
    }
    if (video_track != NULL) {
        video_track->Stop();
        video_track = NULL;
    }
    prepare_state = 0;
    complete_state = 0;
    seek_complete_state = 0;
    sync_clock = 0;
    state = IDLE;

    releaseSourceURL();
}

jlong FFmpegDefaultPlayer::getDuration() {
    jlong ret = 0;
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        if (audio_track != NULL) {
            ret = audio_track->GetDuration();
        }
        if (video_track != NULL) {
            if (ret < video_track->GetDuration()) {
                ret = video_track->GetDuration();
            }
        }
    }
    return ret;
}

jlong FFmpegDefaultPlayer::getCurrentTimestamp() {
    jlong timestamp = 0L;
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        if (video_track != NULL && state != COMPLETED) {
            timestamp = video_track->GetCurrentTimestamp();
        }
        if (timestamp < ((jlong) (sync_clock * 1000))) {
            timestamp =  (jlong) (sync_clock * 1000);
        }
    }
    return timestamp;
}

void FFmpegDefaultPlayer::seekTo(jlong position) {
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        state = SEEKING;
        if (audio_track != NULL) {
            audio_track->SeekTo(position);
        }
        if (video_track != NULL) {
            video_track->SeekTo(position);
        }
    }
}

jint FFmpegDefaultPlayer::getMaxVolumeLevel() {
    jint volumeLevel = 0;
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        if (audio_track != NULL) {
            volumeLevel = audio_track->GetMaxVolumeLevel();
        }
    }
    return volumeLevel;
}

jint FFmpegDefaultPlayer::getVolumeLevel() {
    jint volumeLevel = 0;
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        if (audio_track != NULL) {
            volumeLevel = audio_track->GetVolumeLevel();
        }
    }
    return volumeLevel;
}

void FFmpegDefaultPlayer::setVolumeLevel(jint volume) {
    if (prepare_state > 1 && state != IDLE && state != STOPPED && state != ERROR) {
        if (audio_track != NULL) {
            audio_track->SetVolumeLevel(volume);
        }
    }
}