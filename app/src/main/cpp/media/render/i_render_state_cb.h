//
// Created by fgrid on 2021/8/10.
//

#ifndef OPENVIDEO_I_RENDER_STATE_CB_H
#define OPENVIDEO_I_RENDER_STATE_CB_H

#include "../encoder/encode_cache_frame.h"

class BaseRender;
class IRenderStateCb {
public:
    virtual void RenderPrepare(BaseRender *render) = 0;
    virtual void RenderRunning(BaseRender *render) = 0;
    virtual void RenderPause(BaseRender *render) = 0;
    virtual bool RenderOneFrame(BaseRender *render, EncodeCacheFrame *frame) = 0;
    virtual void RenderFinish(BaseRender *render) = 0;
    virtual void RenderStop(BaseRender *render) = 0;

};

#endif //OPENVIDEO_I_RENDER_STATE_CB_H
