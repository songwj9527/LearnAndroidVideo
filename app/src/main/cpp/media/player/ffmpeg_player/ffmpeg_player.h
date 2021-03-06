//
// Created by fgrid on 2021/7/20.
//

#ifndef OPENVIDEO_FFMPEG_PLAYER_H
#define OPENVIDEO_FFMPEG_PLAYER_H

#include <jni.h>
#include <string>
#include <thread>

#include "../player.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
}

class VideoDecoder;
class BaseVideoRender;
class AudioDecoder;
class OpenSLRender;

class FFmpegPlayer : public Player {
private:
    const char *TAG = "FFmpegPlayer";

protected:
    // 视频解码器
    VideoDecoder *videoDecoder = NULL;
    // 视频渲染器
    BaseVideoRender *videoRender = NULL;

    // 音频解码器
    AudioDecoder *audioDecoder = NULL;
    // 音频渲染器
    OpenSLRender *openSlRender = NULL;

    // 音视频同步基准时钟
    volatile double sync_clock;

    volatile int prepare_state = 0;
    volatile int complete_state = 0;
    volatile int seek_complete_state = 0;

//    volatile bool videoDecoderPrepared = false;
//    volatile bool videoRenderPrepared = false;
//    volatile bool audioDecoderPrepared = false;
//    volatile bool audioRenderPrepared = false;
//    volatile bool videoDecoderCompleted = false;
//    volatile bool videoRenderCompleted = false;
//    volatile bool audioDecoderCompleted = false;
//    volatile bool audioRenderCompleted = false;
//    volatile bool videoDecoderSeekCompleted = false;
//    volatile bool audioDecoderSeekCompleted = false;

public:

    FFmpegPlayer(JNIEnv *jniEnv, jobject object);
    ~FFmpegPlayer();

    /**
     * 设置视频渲染窗口
     */
    void setSurface(jobject surface) override;

    /**
     * 音视频同步基准时钟
     */
    void setSyncClock(double clock);

    /**
     * 音视频同步基准时钟
     */
    double getSyncClock();

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

    /**
     * 给解码器调用，用于告知MediaPlayer解码器准备完成，可准备渲染了。
     * @param decoder
     */
    void onDecoderPrepared(JNIEnv *jniEnv, int decoder);

    /**
     * 给解码器调用，用于告知MediaPlayer解码器已获取视频信息。
     * @param decoder
     * @param videoWidth
     * @param videoHeight
     * @param videoRotation
     */
    void onDecoderInfo(JNIEnv *jniEnv, int decoder, int videoWidth, int videoHeight, int videoRotation);

    /**
     * 给解码器调用，用于告知MediaPlayer解码器的缓存进度。
     * @param decoder
     */
    void onDecoderBufferUpdate(JNIEnv *jniEnv, int decoder, int progress);

    /**
     * 给解码器调用，用于告知MediaPlayer解码器已解码结束。
     * @param decoder
     */
    void onDecoderCompleted(JNIEnv *jniEnv, int decoder);

    /**
     * 给解码器调用，用于告知MediaPlayer哪个解码器出了问题，出了什么问题；
     * 并为整个MediaPlayer是否调用、什么时候调用onError方法将异常信息返回给java层。
     * @param decoder
     * @param code
     * @param msg
     */
    void onDecoderError(JNIEnv *jniEnv, int decoder, int code, const char *msg);

    /**
     * 给解码器调用，用于告知MediaPlayer哪个解码器完成了指定位置
     * @param jniEnv
     * @param decoder
     */
    void onDecoderSeekCompleted(JNIEnv *jniEnv, int decoder);

    /**
     * 给渲染器调用，用于告知MediaPlayer渲染器准备完成，可开始渲染了。
     * @param render
     */
    void onRenderPrepared(JNIEnv *jniEnv, int render);

    /**
    * 给渲染器调用，用于告知MediaPlayer哪个渲染器出了问题，出了什么问题；
    * 并为整个MediaPlayer是否调用、什么时候调用onError方法将异常信息返回给java层。
    * @param render
    * @param code
    * @param msg
    */
    void onRenderError(JNIEnv *jniEnv, int render, int code, const char *msg);

    /**
     * 给渲染器调用，用于告知MediaPlayer渲染器已渲染结束。
     * @param decoder
     */
    void onRenderCompleted(JNIEnv *jniEnv, int decoder);
};

#endif //OPENVIDEO_FFMPEG_PLAYER_H
