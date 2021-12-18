//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_AUDIO_TRACK_H
#define OPENVIDEO_FFMPEG_AUDIO_TRACK_H

#include "base_track.h"

class FFmpegAudioTrack : public BaseFFmpegTrack {
protected:
    AVMediaType GetMediaType() override;
    void CreateTargetRender(JNIEnv *env) override;
    void OnPrepared(JNIEnv *env) override;
    void OnError(JNIEnv *env, int code, const char *msg) override;
    void OnComplete(JNIEnv *env) override;
    void OnSeekComplete(JNIEnv *env) override;

public:
    FFmpegAudioTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback);
    ~FFmpegAudioTrack();

    jint GetMaxVolumeLevel();
    jint GetVolumeLevel();
    void SetVolumeLevel(jint volume);
};

#endif //OPENVIDEO_FFMPEG_AUDIO_TRACK_H
