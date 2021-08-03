//
// Created by fgrid on 2021/8/2.
//

#ifndef OPENVIDEO_CACHE_AV_FRAME_H
#define OPENVIDEO_CACHE_AV_FRAME_H

extern "C" {
#include <libavutil/frame.h>
};

class CacheAvFrame {
public:
    // 帧数据
    uint8_t *data[AV_NUM_DATA_POINTERS];
    // 行数（data数组指针的每位的指针长度）
    int linesize[AV_NUM_DATA_POINTERS];
    // 帧显示时间PTS
    int64_t pts;
    // 用于音视频同步时，求出扩展延时
    // extra_delay = repeat_pict / (2*fps) 显示这样图片需要延迟这么久来显示
    int repeat_pict;
    // 音频帧的频率
    int nb_samples;
    // 时间基准
    AVRational time_base;
    // 其他额外数据
    uint8_t *ext_data = NULL;

    CacheAvFrame(uint8_t **data, int *linesize, int64_t pts, int repeat_pict, int nb_samples, AVRational time_base, uint8_t *ext_data);
    ~CacheAvFrame();
};
#endif //OPENVIDEO_CACHE_AV_FRAME_H
