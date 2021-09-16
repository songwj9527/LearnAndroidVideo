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

int64_t AudioExtractor::SeekTo(int64_t position) {
    if (m_extractor != NULL) {
        AMediaExtractor_seekTo(m_extractor, position, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    }
}