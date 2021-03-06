//
// Created by fgrid on 2/18/21.
//

#ifndef OPENVIDEO_RENDER_FRAME_H
#define OPENVIDEO_RENDER_FRAME_H

#include "../../utils/logger.h"

extern "C" {
#include <libavformat/avformat.h>
}

/*****************************************************************
 * 自定义帧数据：CacheFrame
 ****************************************************************/
class RenderFrame {
public:
    RenderFrame(AVFrame *m_frame, int64_t m_cur_t_s);
    ~RenderFrame();
    // 最终解码数据
    AVFrame *m_frame = NULL;
    // 当前播放时间
    int64_t m_cur_t_s = 0;
};

#endif //OPENVIDEO_RENDER_FRAME_H
