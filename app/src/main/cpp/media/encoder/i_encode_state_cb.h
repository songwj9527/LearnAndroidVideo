//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_I_ENCODE_STATE_CB_H
#define OPENVIDEO_I_ENCODE_STATE_CB_H

class IEncodeStateCb {
public:
    virtual void EncodeStart() = 0;
    virtual void EncodeSend() = 0;
    virtual void EncodeFrame(void *data) = 0;
    virtual void EncodeProgress(long time) = 0;
    virtual void EncodeFinish() = 0;
};

#endif //OPENVIDEO_I_ENCODE_STATE_CB_H
