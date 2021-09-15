//
// Created by fgrid on 2/2/21.
//

#ifndef OPENVIDEO_AUDIO_DECODER_H
#define OPENVIDEO_AUDIO_DECODER_H

#include "../base_decoder.h"

class OpenSLRender;
class AudioDecoder : public BaseDecoder {
private:
//    const char *TAG = "AudioDecoder";

protected:
    /**
     * 音视频索引
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
    AudioDecoder(JNIEnv *env, FFmpegPlayer *mediaPlayer, const char *url, OpenSLRender *render, bool for_synthesizer);
    ~AudioDecoder();
};
#endif //OPENVIDEO_AUDIO_DECODER_H
