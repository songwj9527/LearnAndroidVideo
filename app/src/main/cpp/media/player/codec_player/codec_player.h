//
// Created by fgrid on 2021/9/29.
//

#ifndef OPENVIDEO_CODEC_PLAYER_H
#define OPENVIDEO_CODEC_PLAYER_H

#include "../player.h"
#include "../../codec/track/audio_track.h"
#include "../../codec/track/i_video_track_callback.h"
#include "../../codec/track/base_video_track.h"

class CodecPlayer : public Player, public IVideoTrackCallback, public ISyncClockReceiver {
private:
    const char *TAG = "CodecPlayer";
protected:
    AudioTrack *audio_track = NULL;
    BaseVideoTrack *video_track = NULL;

    // 音视频同步基准时钟
    volatile int64_t sync_clock;
    volatile bool is_audio_prepared = false;
    volatile bool is_video_prepared = false;
    volatile bool is_audio_completed = false;
    volatile bool is_video_completed = false;
    volatile bool is_audio_seek_completed = false;
    volatile bool is_video_seek_completed = false;

public:
    CodecPlayer(JNIEnv *jniEnv, jobject object);
    ~CodecPlayer();

    /**
     * 设置视频渲染窗口
     */
    void setSurface(jobject surface) override;

    /**
     * 开始播放
     */
    void start() override;

    /**
     * 继续播放
     */
    void resume() override;

    /**
     * 暂停播放
     */
    void pause() override;

    /**
     * 停止播放
     */
    void stop() override;

    /**
     * 重置播放器
     */
    void reset() override;

    /**
     * 释放播放器
     */
    void release() override;

    /**
     * 获取当前视频时长（单位：ms）
     * @return
     */
    jlong getDuration() override;

    /**
     * 获取当前视频播放位置（单位：ms）
     * @return
     */
    jlong getCurrentTimestamp() override;

    /**
     * 指定位置播放
     * @param position
     */
    void seekTo(jlong position) override;

    /**
     * 获取最大音量
     * @return
     */
    jint getMaxVolumeLevel() override;

    /**
     * 获取当前音量
     * @return
     */
    jint getVolumeLevel() override;

    /**
     * 设置音量
     * @param volume
     */
    void setVolumeLevel(jint volume) override;

    void OnTrackPrepared(JNIEnv *env, int track_type) override;
    void OnTrackCompleted(JNIEnv *env, int track_type) override;
    void OnTrackSeekingProgress(JNIEnv *env, int track_type, int progress) override;
    void OnTrackSeekCompleted(JNIEnv *env, int track_type) override;
    void OnTrackError(JNIEnv *env, int track_type, int error_code, const char *error_msg) override;
    void OnTrackVideoInfo(JNIEnv *env, int video_width, int video_height, int video_rotation) override;

    int64_t GetSyncClock() override {
        return sync_clock;
    }

    void SetSyncClock(int64_t clock) override {
        sync_clock = clock;
    }
};

#endif //OPENVIDEO_CODEC_PLAYER_H
