//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_I_ENCODE_STATE_CB_H
#define OPENVIDEO_I_ENCODE_STATE_CB_H

class BaseEncoder;
class IEncodeStateCb {
public:
    virtual void EncodeStart(BaseEncoder *encoder) = 0;
    virtual void EncodeSend(BaseEncoder *encoder) = 0;
    virtual void EncodeFrame(BaseEncoder *encoder, void *data) = 0;
    virtual void EncodeProgress(BaseEncoder *encoder, long time) = 0;
    virtual void EncodeFinish(BaseEncoder *encoder) = 0;
};

#endif //OPENVIDEO_I_ENCODE_STATE_CB_H
