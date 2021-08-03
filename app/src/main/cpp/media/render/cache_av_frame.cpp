//
// Created by fgrid on 2021/8/2.
//

#include "cache_av_frame.h"
#include "../../utils/logger.h"

CacheAvFrame::CacheAvFrame(uint8_t **data, int *linesize, int64_t pts, int repeat_pict, int nb_samples, AVRational time_base, uint8_t *ext_data) {
    for (int i = 0; i < AV_NUM_DATA_POINTERS; i++) {
        LOGE("CacheAvFrame", "%d, %d", sizeof(data[i]) / sizeof(uint8_t), linesize[i])
        this->linesize[i] = linesize[i];
        this->data[i] = data[i];
    }
    this->pts = pts;
    this->repeat_pict = repeat_pict;
    this->nb_samples = nb_samples;
    this->time_base = time_base;
    this->ext_data = ext_data;
}

CacheAvFrame::~CacheAvFrame() {
    for (int i = 0; i < AV_NUM_DATA_POINTERS; i++) {
//        free(this->data[i]);
        this->data[i] = NULL;
    }
}