//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_I_TRACK_CALLBACK_H
#define OPENVIDEO_CODEC_I_TRACK_CALLBACK_H

#include <jni.h>

class ITrackCallback {
public:
    virtual void OnTrackPrepared(JNIEnv *env, int track_type) = 0;
    virtual void OnTrackCompleted(JNIEnv *env, int track_type) = 0;
    virtual void OnTrackSeekingProgress(JNIEnv *env, int track_type, int progress) = 0;
    virtual void OnTrackSeekCompleted(JNIEnv *env, int track_type) = 0;
    virtual void OnTrackError(JNIEnv *env, int track_type, int error_code, const char *error_msg) = 0;
};

#endif //OPENVIDEO_CODEC_I_TRACK_CALLBACK_H