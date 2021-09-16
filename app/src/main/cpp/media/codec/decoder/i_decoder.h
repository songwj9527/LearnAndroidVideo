//
// Created by fgrid on 2021/9/15.
//

#ifndef OPENVIDEO_I_DECODER_H
#define OPENVIDEO_I_DECODER_H

#include <jni.h>
#include "../../player/player_state.h"

class IDecoder {
public:
    virtual State getState() = 0;
    virtual void start() = 0;
    virtual void pause() = 0;
    virtual void resume() = 0;
    virtual void stop() = 0;
    virtual jlong getDuration() = 0;
    virtual jlong getCurrentPosition() = 0;
    virtual bool seekTo(int64_t timestamp) = 0;
};
#endif //OPENVIDEO_I_DECODER_H
