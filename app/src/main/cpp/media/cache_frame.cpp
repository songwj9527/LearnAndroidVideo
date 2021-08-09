//
// Created by fgrid on 2021/8/6.
//

#include "cache_frame.h"
#include <malloc.h>

CacheFrame::CacheFrame(uint8_t *data,
                       int line_size,
                       int64_t pts,
                       AVRational time_base,
                       int repeat_pict,
                       int nb_samples,
                       uint8_t *ext_data,
                       bool autoRecycle) {
    this->data = data;
    this->line_size = line_size;
    this->pts = pts;
    this->time_base = time_base;
    this->repeat_pict = repeat_pict;
    this->nb_samples = nb_samples;
    this->ext_data = ext_data;
    this->autoRecycle = autoRecycle;
}

CacheFrame::~CacheFrame() {
    if (autoRecycle) {
        if (data != NULL) {
            free(data);
            data = NULL;
        }
        if (ext_data != NULL) {
            free(ext_data);
            ext_data = NULL;
        }
    }
}