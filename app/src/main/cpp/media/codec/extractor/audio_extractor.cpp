//
// Created by fgrid on 2021/9/15.
//

#include "audio_extractor.h"
#include "../../utils/logger.h"

AudioExtractor::AudioExtractor() {
    TAG = "AudioExtractor";
}

AudioExtractor::~AudioExtractor() {
    LOGE(TAG, "~AudioExtractor()")
}

int32_t AudioExtractor::GetSampleRate() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_sample_rate == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                bool result = AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &m_sample_rate);
                if (!result) {
                    LOGE(TAG, "AMediaFormat_SampleRate: false")
                    return 0;
                }
                if (m_sample_rate < 0) {
                    m_sample_rate = -1;
                }
            } else {
                return 0;
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
                bool result = AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &m_channel_count);
                if (!result) {
                    LOGE(TAG, "AMediaFormat_ChannelCount: false")
                    return 0;
                }
                if (m_channel_count < 0) {
                    m_channel_count = -1;
                }
            } else {
                return 0;
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
                bool result = false;
                if (__ANDROID_API__ >= 28) {
                    result = AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_PCM_ENCODING, &m_pcm_encode_bit);
                } else {
                    result = AMediaFormat_getInt32(format, "pcm-encoding", &m_pcm_encode_bit);
                }
                if (!result) {
                    LOGE(TAG, "AMediaFormat_PcmEncoding: false")
                    return 0;
                }
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
    return position;
}