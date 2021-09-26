//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H
#define OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H

#include "base_track.h"
#include "i_video_track_callback.h"

class BaseVideoTrack : public BaseTrack {
protected:
    bool InitExtractor(JNIEnv *env) override;

public:
    BaseVideoTrack(JNIEnv *jniEnv, const char *source, IVideoTrackCallback *i_track_callback);
    ~BaseVideoTrack();
};

#endif //OPENVIDEO_CODEC_BASE_VIDEO_TRACK_H
