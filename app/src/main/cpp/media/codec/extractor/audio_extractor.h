//
// Created by fgrid on 2021/9/15.
//

#ifndef OPENVIDEO_CODEC_AUDIO_EXTRACTOR_H
#define OPENVIDEO_CODEC_AUDIO_EXTRACTOR_H

#include "base_extractor.h"

class AudioExtractor : public BaseExtractor {
private:
    /**采样率*/
    int32_t m_sample_rate = -1;

    /**声音通道数量*/
    int32_t m_channel_count = -1;

    /**PCM采样位数*/
    int32_t m_pcm_encode_bit = -1;

protected:
    const char * GetMineTypeFlag() override {
        return "audio/";
    }

    void InitVirtual() override {
        GetSampleRate();
        GetChannelCount();
        GetPcmEncodeBit();
    }

    void StopVirtual() override {
        m_sample_rate = -1;
        m_channel_count = -1;
        m_pcm_encode_bit = -1;
    }

public:
    AudioExtractor();
    ~AudioExtractor();

    /**
     * 获取采样率
     * @return
     */
    int32_t GetSampleRate();

    /**
     * 获取声音通道数量
     * @return
     */
    int32_t GetChannelCount();

    /**
     * 获取PCM采样位数
     * @return
     */
    int32_t GetPcmEncodeBit();

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     * @param position  seek位置（单位：microseconds微妙）
     * @return
     */
    int64_t SeekTo(int64_t position) override;
};

#endif //OPENVIDEO_CODEC_AUDIO_EXTRACTOR_H
