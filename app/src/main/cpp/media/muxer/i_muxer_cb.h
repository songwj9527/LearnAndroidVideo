//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_I_MUXER_CB_H
#define OPENVIDEO_I_MUXER_CB_H

class IMuxerCb {
public:
    virtual void OnMuxFinished() = 0;
};

#endif //OPENVIDEO_I_MUXER_CB_H
