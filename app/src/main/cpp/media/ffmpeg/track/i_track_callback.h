//
// Created by fgrid on 2021/12/17.
//

#ifndef OPENVIDEO_FFMPEG_I_TRACK_CALLBACK_H
#define OPENVIDEO_FFMPEG_I_TRACK_CALLBACK_H

#include <jni.h>

class IFFmpegTrackCallback {
public:
    /**
     * 用于告知外部准备完成，可准备播放了。
     * @param jniEnv
     * @param track
     */
    virtual void OnPrepared(JNIEnv *jniEnv, int track) = 0;

    /**
     * 用于告知外部已获取视频信息。
     * @param jniEnv
     * @param track
     * @param videoWidth
     * @param videoHeight
     * @param videoRotation
     */
    virtual void OnInfo(JNIEnv *jniEnv, int track, int videoWidth, int videoHeight, int videoRotation) = 0;

    /**
     * 用于告知外部播放已解码结束。
     * @param jniEnv
     * @param track
     */
    virtual void OnCompleted(JNIEnv *jniEnv, int track) = 0;

    /**
     * 用于告知外部完成了指定位置
     * @param jniEnv
     * @param track
     */
    virtual void OnSeekCompleted(JNIEnv *jniEnv, int track) = 0;

    /**
     * 用于告知外部出了问题，出了什么问题；
     * 并为整个外部是否调用、什么时候调用onError方法将异常信息返回给java层。
     * @param jniEnv
     * @param track
     * @param code
     * @param msg
     */
    virtual void OnError(JNIEnv *jniEnv, int track, int code, const char *msg) = 0;

    /**
     * 更新同步时钟
     * @param syncClock
     */
    virtual void UpdateSyncClock(jdouble syncClock) = 0;

    /**
     * 获取同步时钟
     * @return
     */
    virtual jdouble GetSyncClock() = 0;
};
#endif //OPENVIDEO_FFMPEG_I_TRACK_CALLBACK_H
