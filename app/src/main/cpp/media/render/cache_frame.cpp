//
// Created by fgrid on 2/18/21.
//

#include "cache_frame.h"

/*****************************************************************
 * 自定义帧数据：CacheFrame
 ****************************************************************/
CacheFrame::CacheFrame(AVFrame *m_frame, int64_t m_cur_t_s) {
    this->m_frame = m_frame;
    this->m_cur_t_s = m_cur_t_s;
}

CacheFrame::~CacheFrame() {
    LOGE("CacheFrame", "~CacheFrame");
    // 这里av_frame_free(&m_frame)会奔溃，
    // 原因应该是m_frame是m_decode_frame_queue列表中的元素，当退出时会统一释放，这里无需av_frame_free(&m_frame)，
    // 所以目前只需要赋NULL
//    if (this->m_frame != NULL) {
//        av_frame_free(&m_frame);
//        this->m_frame = NULL;
//    }
    this->m_frame = NULL;
}