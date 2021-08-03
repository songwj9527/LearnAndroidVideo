//
// Created by fgrid on 2/2/21.
//

#ifndef OPENVIDEO_VIDEO_DECODER_H
#define OPENVIDEO_VIDEO_DECODER_H

#include "../base_decoder.h"

class BaseVideoRender;
class VideoDecoder : public BaseDecoder {
private:
//    const char *TAG = "VideoDecoder";

    /**
     * 视频信息
     */
    int videoWidth = 0,  videoHeight = 0, videoRotation = -1;

    /**
     * 更新视频信息
     * @param videoWidth
     * @param videoHeight
     * @param videoRotation
     */
    void updateVideoInfo(int videoWidth, int videoHeight, int videoRotation);

protected:
    /**
     * 类型
     */
    AVMediaType getMediaType() override;

    /**
     * 解码准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderPrepared方法）
     */
    void onPrepared(JNIEnv *env) override;

    /**
     * 解码线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderError方法）
     */
    void onError(JNIEnv *env, int code, const char *msg) override;

    /**
     * 解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderComplete方法）
     */
    void onComplete(JNIEnv *env) override;

    /**
     * 指定位置解码已完成时调用（子类实现此方法：最终会调用MediaPlayer的onDecoderSeekComplete方法）
     */
    void onSeekComplete(JNIEnv *env) override;

public:
    VideoDecoder(JNIEnv *env, Player *mediaPlayer, const char *url, BaseVideoRender *render, bool for_synthesizer);
    ~VideoDecoder();

    /**
     * 视频宽度
     * @return
     */
    int getVideoWidth();

    /**
     * 视频高度
     * @return
     */
    int getVideoHeight();

    /**
     * 获取视频旋转角度
     * @return
     */
    int getVideoRotation();
};
#endif //OPENVIDEO_VIDEO_DECODER_H
