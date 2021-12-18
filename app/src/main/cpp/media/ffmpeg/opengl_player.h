//
// Created by fgrid on 2021/12/18.
//

#ifndef OPENVIDEO_FFMPEG_OPENGL_PLAYER_H
#define OPENVIDEO_FFMPEG_OPENGL_PLAYER_H

#include "../player/player.h"
#include "track/i_track_callback.h"
#include "track/audio_track.h"
#include "track/opengl_video_track.h"

class FFmpegOpenGLPlayer : public Player, public IFFmpegTrackCallback {
private:
    const char *TAG = "FFmpegOpenGLPlayer";

    // 音视频同步基准时钟
    volatile jdouble sync_clock = 0;

    FFmpegAudioTrack *audio_track = NULL;
    FFmpegOpenGLVideoTrack *video_track = NULL;

    volatile int prepare_state = 0;
    volatile int complete_state = 0;
    volatile int seek_complete_state = 0;


public:
    FFmpegOpenGLPlayer(JNIEnv *jniEnv, jobject object);
    ~FFmpegOpenGLPlayer();

    void prepareSync() override;
    void setSurface(jobject surface) override;
    void start() override;
    void resume() override;
    void pause() override;
    void seekTo(jlong position) override;
    void stop() override;
    void reset() override;
    void release() override;

    jlong getDuration() override;
    jlong getCurrentTimestamp() override;

    jint getMaxVolumeLevel() override;
    jint getVolumeLevel() override;
    void setVolumeLevel(jint volume) override;

    void OnPrepared(JNIEnv *jniEnv, int track) override;
    void OnInfo(JNIEnv *jniEnv, int track, int videoWidth, int videoHeight, int videoRotation) override;
    void OnError(JNIEnv *jniEnv, int track, int code, const char *msg) override;
    void OnCompleted(JNIEnv *jniEnv, int track) override;
    void OnSeekCompleted(JNIEnv *jniEnv, int track) override;

    void UpdateSyncClock(jdouble syncClock) override {
        sync_clock = syncClock;
    }

    jdouble GetSyncClock() override {
        return sync_clock;
    }
};

#endif //OPENVIDEO_FFMPEG_OPENGL_PLAYER_H