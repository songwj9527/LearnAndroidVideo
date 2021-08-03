//
// Created by fgrid on 2021/7/14.
//

#ifndef OPENVIDEO_DRAWER_PROXY_H
#define OPENVIDEO_DRAWER_PROXY_H

#include "../drawer.h"

class DrawerProxy {
public:
    virtual ~DrawerProxy() {}
    virtual void AddDrawer(Drawer *drawer) = 0;
    virtual void SetDrawerSize(int width, int height) = 0;
    virtual void UpdateTextureIds() = 0;
    virtual void Draw(void *frame_data) = 0;
    virtual void Release() = 0;
};

#endif //OPENVIDEO_DRAWER_PROXY_H
