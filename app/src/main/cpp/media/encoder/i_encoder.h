//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_I_ENCODER_H
#define OPENVIDEO_I_ENCODER_H

#include "encode_frame.h"
#include "i_encode_state_cb.h"

class IEncoder {
public:
    virtual void PushFrame(EncodeFrame *encode_frame) = 0;
    virtual bool TooMuchData() = 0;
    virtual void SetStateReceiver(IEncodeStateCb *cb) = 0;
};

#endif //OPENVIDEO_I_ENCODER_H
