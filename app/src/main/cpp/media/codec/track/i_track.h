//
// Created by fgrid on 2021/9/18.
//

#ifndef OPENVIDEO_CODEC_I_TRACK_H
#define OPENVIDEO_CODEC_I_TRACK_H

#include <jni.h>

class ITrack {
public:
    virtual void  Start() = 0;
    virtual void  Pause() = 0;
    virtual void  Resume() = 0;
    virtual void  Stop() = 0;
    virtual void  Release() = 0;
    virtual jlong GetDuration() = 0;
    virtual jlong GetCurrentTimestamp() = 0;
    virtual bool  SeekTo(int64_t timestamp) = 0;
};

#endif //OPENVIDEO_CODEC_I_TRACK_H