//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_AUDIO_DECODER_H
#define OPENVIDEO_FFMPEG_AUDIO_DECODER_H

#include "base_decoder.h"

class AudioDecoder : public BaseDecoder {
protected:
    AVMediaType GetMediaType() override;
    void OnPrepared(JNIEnv *env) override;
    void OnError(JNIEnv *env, int code, const char *msg) override;
    void OnComplete(JNIEnv *env) override;
    void OnSeekComplete(JNIEnv *env) override;

public:
    AudioDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback);
    ~AudioDecoder();
};

#endif //OPENVIDEO_FFMPEG_AUDIO_DECODER_H
