//
// Created by fgrid on 2021/9/18.
//

#include <android/native_window_jni.h>
#include "base_video_track.h"
#include "../extractor/video_extractor.h"
#include "../../../utils/logger.h"

BaseVideoTrack::BaseVideoTrack(JNIEnv *jniEnv, const char *source, IVideoTrackCallback *i_track_callback) : BaseTrack(jniEnv, source, i_track_callback) {
    TAG = "BaseVideoTrack";
}

BaseVideoTrack::~BaseVideoTrack() {
    ReleaseANativeWindow();
}

bool BaseVideoTrack::InitExtractor(JNIEnv *env) {
    LOGE(TAG, "InitExtractor()")
    m_extractor = new VideoExtractor();
    if (AMEDIA_OK != m_extractor->SetDataSource(m_source)) {
        delete m_extractor;
        m_extractor = NULL;
        return false;
    }
    return true;
}

int64_t BaseVideoTrack::RenderFrameAliveTime() {
    if (m_extractor != NULL) {
        return (1000000L / ((VideoExtractor *) m_extractor)->GetVideoFps()) / 1000L;
    }
    return 0;
}

void BaseVideoTrack::InitANativeWindow(JNIEnv *env) {
    if (m_extractor != NULL) {
        int video_height = 0, video_width = 0;
        video_width = ((VideoExtractor *) m_extractor)->GetVideoWidth();
        video_height = ((VideoExtractor *) m_extractor)->GetVideoHeight();

        LOGE(TAG, "%s", "InitANativeWindow() 0");
        // 初始化窗口
        m_native_window = ANativeWindow_fromSurface(env, m_surface_ref);
        LOGE(TAG, "%s", "initANativeWindow() 1");

        // 绘制区域的宽高
        int windowWidth = ANativeWindow_getWidth(m_native_window);
        int windowHeight = ANativeWindow_getHeight(m_native_window);

        // 计算目标视频的宽高
        m_dst_w = windowWidth;
        m_dst_h = m_dst_w * video_height / video_width;
        if (m_dst_h > windowHeight) {
            m_dst_h = windowHeight;
            m_dst_w = windowHeight * video_width / video_height;
        }
        LOGE(TAG, "windowW: %d, windowH: %d, dstVideoW: %d, dstVideoH: %d",windowWidth, windowHeight, m_dst_w, m_dst_h)

        //设置宽高限制缓冲区中的像素数量
        ANativeWindow_setBuffersGeometry(m_native_window, windowWidth,windowHeight, WINDOW_FORMAT_RGBA_8888);
    }
}

void BaseVideoTrack::ReleaseANativeWindow() {
    if (m_native_window != NULL) {
        ANativeWindow_release(m_native_window);
        m_native_window = NULL;
    }
}