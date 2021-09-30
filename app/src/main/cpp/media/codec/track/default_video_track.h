//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_DEFAULT_VIDEO_TRACK_H
#define OPENVIDEO_CODEC_DEFAULT_VIDEO_TRACK_H

#include "base_video_track.h"

class DefaultVideoTrack : public BaseVideoTrack {
protected:
    bool InitRender(JNIEnv *env) override;
    void Render(uint8_t *buffer_data, AMediaCodecBufferInfo *buffer_info, size_t out_size) override;
    void LoopDoneVirtual(JNIEnv *env) override;

    void  StartVirtual(State prev) override {

    }
    void  PauseVirtual(State prev) override {

    }
    void  ResumeVirtual(State prev) override {

    }
    void  StopVirtual(State prev) override {

    }
    void  ResetVirtual(State prev) override {

    }

public:
    DefaultVideoTrack(JNIEnv *jniEnv, const char *source, IVideoTrackCallback *i_track_callback);
    ~DefaultVideoTrack();

    void SetSurface(JNIEnv *jniEnv, jobject surface) override;
};

#endif //OPENVIDEO_CODEC_DEFAULT_VIDEO_TRACK_H
