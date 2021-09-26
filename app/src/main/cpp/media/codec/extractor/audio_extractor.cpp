//
// Created by fgrid on 2021/9/15.
//

#include "audio_extractor.h"
#include "../../utils/logger.h"

AudioExtractor::AudioExtractor() {

}

AudioExtractor::~AudioExtractor() {
    LOGE(TAG, "~AudioExtractor()")
}

int32_t AudioExtractor::GetSampleRate() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_sample_rate == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "sample-rate", &m_sample_rate);
                if (m_sample_rate < 0) {
                    m_sample_rate = -1;
                }
            }
        }
        return m_sample_rate;
    }
    return  0;
}

int32_t AudioExtractor::GetChannelCount() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_channel_count == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "channel-count", &m_channel_count);
                if (m_channel_count < 0) {
                    m_channel_count = -1;
                }
            }
        }
        return m_channel_count;
    }
    return  0;
}

int AudioExtractor::GetPcmEncodeBit() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_pcm_encode_bit == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "pcm-encoding", &m_pcm_encode_bit);
                if (m_pcm_encode_bit < 0) {
                    m_pcm_encode_bit = -1;
                }
            }
        }
        return m_pcm_encode_bit;
    }
    return  0;
}

int64_t AudioExtractor::SeekTo(int64_t position) {
    if (m_extractor != NULL) {
        AMediaExtractor_seekTo(m_extractor, position, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    }
}