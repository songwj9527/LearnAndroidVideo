//
// Created by fgrid on 2021/9/15.
//

#ifndef OPENVIDEO_AUDIO_EXTRACTOR_H
#define OPENVIDEO_AUDIO_EXTRACTOR_H

#include "i_extractor.h"

class AudioExtractor : public IExtractor {
private:
    const char *TAG = "AudioExtractor";

protected:
    char * GetMineTypeFlag() override {
        return "audio/";
    }

    void InitVirtual() override {}

    void StopVirtual() override {}

public:
    AudioExtractor();
    ~AudioExtractor();

    int64_t SeekTo(int64_t position) override;
};

#endif //OPENVIDEO_AUDIO_EXTRACTOR_H
