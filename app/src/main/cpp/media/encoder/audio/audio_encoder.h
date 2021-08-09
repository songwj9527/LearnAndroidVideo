//
// Created by fgrid on 2021/8/6.
//

#ifndef OPENVIDEO_AUDIO_ENCODER_H
#define OPENVIDEO_AUDIO_ENCODER_H

#include "../base_encoder.h"

class AudioEncoder: public BaseEncoder {

private:
    AVFrame *m_frame = NULL;

    void InitFrame();

protected:
    void InitContext(AVCodecContext *codec_ctx) override;

    int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) override;

    AVFrame* DealFrame(EncodeFrame *encode_frame) override;

    void Release() override;

public:
    AudioEncoder(JNIEnv *env, Mp4Muxer *muxer);
};

#endif //OPENVIDEO_AUDIO_ENCODER_H
