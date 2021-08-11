//
// Created by fgrid on 2021/8/6.
//

#ifndef OPENVIDEO_VIDEO_ENCODER_H
#define OPENVIDEO_VIDEO_ENCODER_H

#include "../base_encoder.h"

class VideoEncoder: public BaseEncoder {
private:

    // 视频格式转化工具
    SwsContext *m_sws_ctx = NULL;

    // 一阵 YUV 数据
    AVFrame *m_yuv_frame = NULL;

    // 目标视频宽高
    int m_width = 0, m_height = 0;

    void InitYUVFrame();

protected:
    void InitContext(AVCodecContext *codec_ctx) override;

    int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) override;

    AVFrame* DealFrame(EncodeCacheFrame *encode_frame) override;

    void Release() override;

public:
    VideoEncoder(JNIEnv *env, Mp4Muxer *muxer, int width, int height);
};

#endif //OPENVIDEO_VIDEO_ENCODER_H
