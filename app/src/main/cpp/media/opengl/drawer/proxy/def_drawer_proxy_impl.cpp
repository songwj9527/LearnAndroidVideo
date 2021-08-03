//
// Created by fgrid on 2021/7/14.
//

#include "def_drawer_proxy_impl.h"
#include "../../../../utils/logger.h"

void DefDrawerProxyImpl::AddDrawer(Drawer *drawer) {
    m_drawers.push_back(drawer);
}

void DefDrawerProxyImpl::SetDrawerSize(int width, int height) {
    for (int i = 0; i < m_drawers.size(); ++i) {
        m_drawers[i]->SetSize(width, height);
    }
}

void DefDrawerProxyImpl::UpdateTextureIds() {
//    GLuint *texure_ids = static_cast<GLuint *>(malloc(m_drawers.size() * sizeof(GLuint)));
//    LOGE("DefDrawerProxyImpl", "length: %d", sizeof(texure_ids)/sizeof(texure_ids[0]))
//    glGenTextures(m_drawers.size(), texure_ids);
//    LOGI("DefDrawerProxyImpl", "Create texture id : %x", glGetError())
//    for (int i = 0; i < m_drawers.size(); ++i) {
//        LOGE("DefDrawerProxyImpl", "TextureID %d: %u", i, texure_ids[i])
//        m_drawers[i]->setTextureID(texure_ids[i]);
//    }
//    free(texure_ids);
}

void DefDrawerProxyImpl::Draw(void *frame_data) {
    for (int i = 0; i < m_drawers.size(); ++i) {
        m_drawers[i]->Draw(frame_data);
    }
}

void DefDrawerProxyImpl::Release() {
    for (int i = 0; i < m_drawers.size(); ++i) {
        m_drawers[i]->Release();
        delete m_drawers[i];
    }
    m_drawers.clear();
}