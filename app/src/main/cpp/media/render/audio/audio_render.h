//
// Created by fgrid on 2/19/21.
//

#ifndef OPENVIDEO_AUDIO_RENDER_H
#define OPENVIDEO_AUDIO_RENDER_H

#include "../base_render.h"
#include "../../const.h"

extern "C" {
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/audio_fifo.h>
}

class AudioRender : public BaseRender {
private:
//    const char *TAG = "AudioRender";

protected:
    // -------------------音频数据转换相关-----------------------------
    // 音频转换器
    SwrContext *m_swr = NULL;

    // 音频通道数
    int m_out_channer_nb;

    // 输出缓冲
    uint8_t *m_out_buffer[2] = {NULL, NULL};

    // 重采样后，每个通道包含的采样数
    // acc默认为1024，重采样后可能会变化
    int m_dest_nb_sample = 1024;

    // 重采样以后，一帧数据的大小
    size_t m_dest_data_size = 0;

    /**
     * 初始化转换工具：将帧数据转换成音频数据
     */
    bool initSwr(JNIEnv *env);

    /**
     * 释放转换工具相关资源（包括缓冲区）
     */
    void releaseSwr();

    /**
     * 采样格式：16位
     */
    AVSampleFormat getSampleFormat() {
        if (ForSynthesizer()) {
            return ENCODE_AUDIO_DEST_FORMAT;
        } else {
            return AV_SAMPLE_FMT_S16;
        }
    }

    /**
     * 采样率
     */
    int getSampleRate(int spr) {
        if (ForSynthesizer()) {
            return ENCODE_AUDIO_DEST_SAMPLE_RATE;
        } else {
            return spr;
        }
    }

public:
    AudioRender(bool for_synthesizer);
    virtual ~AudioRender();
};
#endif //OPENVIDEO_AUDIO_RENDER_H
