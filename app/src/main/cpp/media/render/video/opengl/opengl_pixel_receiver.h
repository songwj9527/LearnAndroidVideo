//
// Created by fgrid on 2021/8/6.
//

#ifndef OPENVIDEO_OPENGL_PIXEL_RECEIVER_H
#define OPENVIDEO_OPENGL_PIXEL_RECEIVER_H

#include <stdint.h>

class OpenGLPixelReceiver {
public:
    virtual void ReceivePixel(uint8_t *rgba) = 0;
};

#endif //OPENVIDEO_OPENGL_PIXEL_RECEIVER_H
