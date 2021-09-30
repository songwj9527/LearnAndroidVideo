//
// Created by fgrid on 2021/9/18.
//

#include "default_video_track.h"
#include "../../../utils/logger.h"
#include "../extractor/video_extractor.h"

DefaultVideoTrack::DefaultVideoTrack(JNIEnv *jniEnv, const char *source, IVideoTrackCallback *i_track_callback) : BaseVideoTrack(jniEnv, source, i_track_callback) {
    TAG = "DefaultVideoTrack";
}

DefaultVideoTrack::~DefaultVideoTrack() {

}

bool DefaultVideoTrack::InitRender(JNIEnv *env) {
    LOGE(TAG, "InitRender()")
    if (m_i_track_callback != NULL && m_extractor != NULL) {
        VideoExtractor * video_extractor = (VideoExtractor *) m_extractor;
        ((IVideoTrackCallback *) m_i_track_callback)->OnTrackVideoInfo(env, video_extractor->GetVideoWidth(), video_extractor->GetVideoHeight(), video_extractor->GetVideoRotation());
    }
    return true;
}

void DefaultVideoTrack::Render(uint8_t *buffer_data, AMediaCodecBufferInfo *buffer_info, size_t out_size) {

}

void DefaultVideoTrack::LoopDoneVirtual(JNIEnv *env) {
    ReleaseANativeWindow();
}

void DefaultVideoTrack::SetSurface(JNIEnv *jniEnv, jobject surface) {
    LOGE(TAG, "%s%s%s", "SetSurface() ", surface == NULL ? "NULL" : "OK", jniEnv == NULL ? ", NULL" : ", OK");
    m_surface_ref = surface;
    ReleaseANativeWindow();
    if (surface != NULL && jniEnv != NULL) {
        InitANativeWindow(jniEnv);
    }
    if (m_codec != NULL && m_extractor != NULL) {
        AMediaCodec_configure(m_codec, m_extractor->GetMediaFormat(), m_native_window, NULL, 0);
    }
}