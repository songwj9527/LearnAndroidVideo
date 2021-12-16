//
// Created by fgrid on 2021/9/29.
//

#include "codec_player.h"

CodecPlayer::CodecPlayer(JNIEnv *jniEnv, jobject object) : Player(jniEnv, object){}

CodecPlayer::~CodecPlayer() {
    release();
    LOGE(TAG, "%s", "~CodecPlayer");
}

void CodecPlayer::setSurface(jobject surface) {
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
        if (is_audio_prepared && is_video_prepared) {
            if (video_track != NULL) {
                video_track->SetSurface(jniEnv, jsurface);
            }
        }
    }
}

void CodecPlayer::start() {
    if (is_audio_prepared && is_video_prepared) {
        if (state != STOPPED) {
            reset();
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

void CodecPlayer::pause() {
    if (is_audio_prepared && is_video_prepared) {
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

void CodecPlayer::resume() {
    if (is_audio_prepared && is_video_prepared) {
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

void CodecPlayer::stop() {
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
    sync_clock = 0;
    is_audio_prepared = false;
    is_video_prepared = false;
    is_audio_completed = false;
    is_video_completed = false;
    is_audio_seek_completed = false;
    is_video_seek_completed = false;
}

void CodecPlayer::reset() {
    if (is_audio_prepared && is_video_prepared) {
        if (state == PAUSED || state == COMPLETED) {
            state = PREPARED;
            if (audio_track != NULL) {
                audio_track->Reset();
            }
            if (video_track != NULL) {
                video_track->Reset();
            }
            sync_clock = 0;
        }
    }
}

void CodecPlayer::release() {
    stop();
    state = IDLE;
}

jlong CodecPlayer::getDuration() {
    jlong duratioon = 0L;
    if (is_audio_prepared && is_video_prepared) {
        if (video_track != NULL) {
            duratioon = video_track->GetDuration();
        }
        if (audio_track != NULL) {
            if (duratioon < audio_track->GetDuration()) {
                duratioon = audio_track->GetDuration();
            }
        }
    }
    return duratioon;
}

jlong CodecPlayer::getCurrentTimestamp() {
    jlong timestamp = 0L;
    if (is_audio_prepared && is_video_prepared) {
        if (video_track != NULL) {
            timestamp = video_track->GetCurrentTimestamp();
        }
        if (audio_track != NULL) {
            if (timestamp < audio_track->GetCurrentTimestamp()) {
                timestamp = audio_track->GetCurrentTimestamp();
            }
        }
    }
    return timestamp;
}

void CodecPlayer::seekTo(jlong position) {
    if (is_audio_prepared && is_video_prepared) {
        sync_clock = position;
        if (audio_track != NULL) {
            audio_track->SeekTo(position);
        }
        if (video_track != NULL) {
            video_track->SeekTo(position);
        }
    }
}

jint CodecPlayer::getMaxVolumeLevel() {
    jint volumeLevel = 0;
    if (is_audio_prepared) {
        if (audio_track != NULL) {
            volumeLevel = audio_track->GetMaxVolumeLevel();
        }
    }
    return volumeLevel;
}

jint CodecPlayer::getVolumeLevel() {
    jint volumeLevel = 0;
    if (is_audio_prepared) {
        if (audio_track != NULL) {
            volumeLevel = audio_track->GetVolumeLevel();
        }
    }
    return volumeLevel;
}

void CodecPlayer::setVolumeLevel(jint volume) {
    if (is_audio_prepared) {
        if (audio_track != NULL) {
            audio_track->SetVolumeLevel(volume);
        }
    }
}

void CodecPlayer::OnTrackPrepared(JNIEnv *env, int track_type) {
    bool prepared = false;
    if (track_type == MODULE_CODE_AUDIO) {
        is_audio_prepared = true;
    }
    else if (track_type == MODULE_CODE_VIDEO) {
        is_video_prepared = true;
    }
    if (is_audio_prepared && is_video_prepared && state == IDLE) {
        state = PREPARED;
        prepared = true;
    }
    if (prepared) {
        onPostEventToJava(env, CALLBACK_PREPARED, 0, 0, NULL);
    }
}

void CodecPlayer::OnTrackCompleted(JNIEnv *env, int track_type) {
    bool completed = false;
    if (track_type == MODULE_CODE_AUDIO) {
        is_audio_completed = true;
    }
    else if (track_type == MODULE_CODE_VIDEO) {
        is_video_completed = true;
    }
    if (is_audio_prepared && is_video_prepared) {
        if (audio_track != NULL && video_track != NULL) {
            if (is_audio_completed && is_video_completed) {
                state = COMPLETED;
                sync_clock = getDuration();
                completed = true;
            }
        }
        else if (audio_track != NULL && is_audio_completed) {
            state = COMPLETED;
            sync_clock = getDuration();
            completed = true;
        }
        else if (video_track != NULL && is_video_completed) {
            state = COMPLETED;
            sync_clock = getDuration();
            completed = true;
        }
    }
    if (completed) {
        onPostEventToJava(env, CALLBACK_COMPLETED, 0, 0, NULL);
    }
}

void CodecPlayer::OnTrackSeekingProgress(JNIEnv *env, int track_type, int progress) {

}

void CodecPlayer::OnTrackSeekCompleted(JNIEnv *env, int track_type) {
    bool completed = false;
    if (track_type == MODULE_CODE_AUDIO) {
        is_audio_seek_completed = true;
    }
    else if (track_type == MODULE_CODE_VIDEO) {
        is_video_seek_completed = true;
    }
    if (is_audio_prepared && is_video_prepared) {
        if (audio_track != NULL && video_track != NULL) {
            if (is_audio_seek_completed && is_video_seek_completed) {
                completed = true;
            }
        }
        else if (audio_track != NULL && is_audio_seek_completed) {
            completed = true;
        }
        else if (video_track != NULL && is_video_seek_completed) {
            completed = true;
        }
    }
    if (completed) {
        onPostEventToJava(jniEnv, CALLBACK_SEEK_COMPLETED, 0, 0, NULL);
    }
}

void CodecPlayer::OnTrackVideoInfo(JNIEnv *env, int video_width, int video_height, int video_rotation) {
    LOGE(TAG, "OnTrackVideoInfo: %d, %d, %d", video_width, video_height, video_rotation);
    char videoRotationChar[20];
    memset(videoRotationChar, 0, 20);
    sprintf(videoRotationChar, "%d", video_rotation);
    const char *result = videoRotationChar;
    jstring videoRotationJString = (env)->NewStringUTF(result);
    onPostEventToJava(env, CALLBACK_INFO, video_width, video_height, videoRotationJString);
}

void CodecPlayer::OnTrackError(JNIEnv *env, int track_type, int error_code, const char *error_msg) {
    bool completed = false;
    if (track_type == MODULE_CODE_AUDIO) {
        if (!is_audio_prepared) {
            is_audio_prepared = true;
            audio_track = NULL;
            is_audio_completed = false;
            is_audio_seek_completed = false;
        }
    }
    else if (track_type == MODULE_CODE_VIDEO) {
        if (!is_video_prepared) {
            is_video_prepared = true;
            video_track = NULL;
            is_video_completed = false;
            is_video_seek_completed = false;
        }
    }
    onError(env, error_code, error_msg);
    if (is_audio_prepared && is_video_prepared && state == IDLE) {
        if (audio_track != NULL || video_track != NULL) {
            state = PREPARED;
            completed = true;
        }
    }
    if (completed) {
        onPostEventToJava(env, CALLBACK_PREPARED, 0, 0, NULL);
    }
}