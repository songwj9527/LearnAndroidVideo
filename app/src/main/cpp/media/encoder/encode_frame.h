//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_ENCODE_FRAME_H
#define OPENVIDEO_ENCODE_FRAME_H

#include <malloc.h>
#include "../../utils/logger.h"

extern "C" {
#include <libavutil/rational.h>
};

class EncodeFrame {
public:
    uint8_t *data = NULL;
    int line_size;
    int64_t pts;
    AVRational time_base;
    uint8_t *ext_data = NULL;

    EncodeFrame(uint8_t *data, int line_size, int64_t pts, AVRational time_base, uint8_t *ext_data = NULL) {
        this->data = data;
        this->line_size = line_size;
        this->pts = pts;
        this->time_base = time_base;
        this->ext_data = ext_data;
    }

    ~EncodeFrame() {
        if (data != NULL) {
            free(data);
            data = NULL;
        }
        if (ext_data != NULL) {
            free(ext_data);
            ext_data = NULL;
        }
    }
};
#endif //OPENVIDEO_ENCODE_FRAME_H
