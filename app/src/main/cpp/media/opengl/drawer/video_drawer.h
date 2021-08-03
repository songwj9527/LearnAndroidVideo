//
// Created by fgrid on 2021/7/13.
//

#ifndef OPENVIDEO_VIDEO_DRAWER_H
#define OPENVIDEO_VIDEO_DRAWER_H

#include "drawer.h"

class VideoDrawer: public Drawer {
private:
    const char *TAG = "VideoDrawer";

protected:
    void DoOtherDraw() override;
    void ReleaseOthers() override;
public:

    VideoDrawer();
    ~VideoDrawer();

    const char* GetVertexShader() override;
    const char* GetFragmentShader() override;
    void InitOtherShaderHandler() override;
    void BindTexture() override;
    void PrepareDraw() override;
    void DoneDraw() override;
};

#endif //OPENVIDEO_VIDEO_DRAWER_H
