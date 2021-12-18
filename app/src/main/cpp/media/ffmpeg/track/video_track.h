//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_VIDEO_TRACK_H
#define OPENVIDEO_FFMPEG_VIDEO_TRACK_H

#include "base_track.h"

class FFmpegVideoTrack : public BaseFFmpegTrack {
private:
    int videoWidth = 0,  videoHeight = 0, videoRotation = -1;

protected:
    AVMediaType GetMediaType() override;
    void CreateTargetRender(JNIEnv *env) override;
    void OnPrepared(JNIEnv *env) override;
    void OnError(JNIEnv *env, int code, const char *msg) override;
    void OnComplete(JNIEnv *env) override;
    void OnSeekComplete(JNIEnv *env) override;

public:
    FFmpegVideoTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback);
    ~FFmpegVideoTrack();

    int GetVideoWidth();
    int GetVideoHeight();
    int GetVideoRotation();

    void SetSurface(JNIEnv *jniEnv, jobject surface);
};

#endif //OPENVIDEO_FFMPEG_VIDEO_TRACK_H
