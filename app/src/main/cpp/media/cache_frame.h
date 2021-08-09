//
// Created by fgrid on 2021/8/6.
//

#ifndef OPENVIDEO_CACHE_FRAME_H
#define OPENVIDEO_CACHE_FRAME_H

extern "C" {
#include <libavutil/rational.h>
};

class CacheFrame {
public:
    // 主数据
    uint8_t *data = NULL;
    // 行大小
    int line_size;
    //PTS时间戳
    int64_t pts;
    // 时间基
    AVRational time_base;
    // 需要求出扩展延时：
    // extra_delay = repeat_pict / (2*fps) 显示这样图片需要延迟这么久来显示
    int repeat_pict;

    // 音频帧频率
    int nb_samples;
    // 额外数据（音频解码数据的第二组数据）
    uint8_t *ext_data = NULL;

    // 是否自动回收data和ext_data
    bool autoRecycle = true;

    CacheFrame(uint8_t *data,
               int line_size,
               int64_t pts,
               AVRational time_base,
               int repeat_pict,
               int nb_samples = 0,
               uint8_t *ext_data = NULL,
               bool autoRecycle = true);

    ~CacheFrame();
};
#endif //OPENVIDEO_CACHE_FRAME_H
