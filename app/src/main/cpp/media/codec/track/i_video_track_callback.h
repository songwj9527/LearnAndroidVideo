//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_I_VIDEO_TRACK_CALLBACK_H
#define OPENVIDEO_CODEC_I_VIDEO_TRACK_CALLBACK_H

#include "i_track_callback.h"

class IVideoTrackCallback : public ITrackCallback {
public:
    virtual void OnTrackVideoInfo(JNIEnv *env, int video_width, int video_height, int video_rotation) = 0;
};

#endif //OPENVIDEO_CODEC_I_VIDEO_TRACK_CALLBACK_H
