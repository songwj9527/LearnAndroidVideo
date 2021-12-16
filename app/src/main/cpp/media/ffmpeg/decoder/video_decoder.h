//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_VIDEO_DECODER_H
#define OPENVIDEO_FFMPEG_VIDEO_DECODER_H

#include "base_decoder.h"

class VideoDecoder : public BaseDecoder {
private:
    int videoWidth = 0,  videoHeight = 0, videoRotation = -1;

protected:
    AVMediaType GetMediaType() override;
    void OnPrepared(JNIEnv *env) override;
    void OnError(JNIEnv *env, int code, const char *msg) override;
    void OnComplete(JNIEnv *env) override;
    void OnSeekComplete(JNIEnv *env) override;

public:
    VideoDecoder(JNIEnv *env, const char *url, IDecoderCallback *iDecoderCallback);
    ~VideoDecoder();

    int GetVideoWidth();
    int GetVideoHeight();
    int GetVideoRotation();
};

#endif //OPENVIDEO_FFMPEG_VIDEO_DECODER_H
