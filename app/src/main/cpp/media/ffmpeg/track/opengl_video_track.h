//
// Created by fgrid on 2021/12/18.
//

#ifndef OPENVIDEO_FFMPEG_OPENGL_VIDEO_TRACK_H
#define OPENVIDEO_FFMPEG_OPENGL_VIDEO_TRACK_H

#include "base_track.h"
#include "../../opengl/drawer/drawer.h"

class FFmpegOpenGLVideoTrack : public BaseFFmpegTrack {
private:
    int videoWidth = 0,  videoHeight = 0, videoRotation = -1;
    volatile int prepare_count = 0;

protected:
    AVMediaType GetMediaType() override;
    void CreateTargetRender(JNIEnv *env) override;
    void OnPrepared(JNIEnv *env) override;
    void OnError(JNIEnv *env, int code, const char *msg) override;
    void OnComplete(JNIEnv *env) override;
    void OnSeekComplete(JNIEnv *env) override;

public:
    FFmpegOpenGLVideoTrack(JNIEnv *env, const char *url, IFFmpegTrackCallback *iTrackCallback);
    ~FFmpegOpenGLVideoTrack();

    void OnRenderPrepared(JNIEnv *jniEnv, int render) override;

    int GetVideoWidth();
    int GetVideoHeight();
    int GetVideoRotation();

    void SetSurface(JNIEnv *jniEnv, jobject surface);

    void AddDraw(Drawer *drawer);
};

#endif //OPENVIDEO_FFMPEG_OPENGL_VIDEO_TRACK_H
