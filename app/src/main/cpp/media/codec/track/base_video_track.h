//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H
#define OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H

#include "base_track.h"
#include "i_video_track_callback.h"
#include "../../media_codes.h"

class BaseVideoTrack : public BaseTrack {
protected:
    // -------------------视图渲染相关-----------------------------
    // Surface引用，必须使用引用，否则无法在线程中操作
    jobject m_surface_ref = NULL;

    // 本地窗口
    ANativeWindow *m_native_window = NULL;

    //显示的目标宽
    int m_dst_w;
    //显示的目标高
    int m_dst_h;

    int GetTrackType() override {
        return MODULE_CODE_VIDEO;
    }

    bool InitExtractor(JNIEnv *env) override;

    void UpdateClock() override {}

    int64_t RenderFrameAliveTime() override;

    /**
     * 初始化渲染窗口
     * @param env
     */
    void InitANativeWindow(JNIEnv *env);

    /**
     * 释放Native屏幕资源
     */
    void ReleaseANativeWindow();

public:
    BaseVideoTrack(JNIEnv *jniEnv, const char *source, IVideoTrackCallback *i_track_callback);
    ~BaseVideoTrack();

    /**
     * 设置Surface视图窗口
     * @param env
     * @param surface
     */
    virtual void SetSurface(JNIEnv *jniEnv, jobject surface) = 0;
};

#endif //OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H
