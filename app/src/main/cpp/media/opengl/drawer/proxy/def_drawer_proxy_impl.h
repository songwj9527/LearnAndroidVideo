//
// Created by fgrid on 2021/7/14.
//

#ifndef OPENVIDEO_DEF_DRAWER_PROXY_IMPL_H
#define OPENVIDEO_DEF_DRAWER_PROXY_IMPL_H

#include "drawer_proxy.h"
#include <vector>

class DefDrawerProxyImpl: public DrawerProxy {

private:
    std::vector<Drawer *> m_drawers;

public:
    void AddDrawer(Drawer *drawer) override;
    void SetDrawerSize(int width, int height) override;
    void UpdateTextureIds() override;
    void Draw(void *frame_data) override;
    void Release() override;
};

#endif //OPENVIDEO_DEF_DRAWER_PROXY_IMPL_H
