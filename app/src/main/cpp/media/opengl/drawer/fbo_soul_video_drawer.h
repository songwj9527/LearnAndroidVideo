//
// Created by fgrid on 2021/8/2.
//

#ifndef OPENVIDEO_FBO_SOUL_VIDEO_DRAWER_H
#define OPENVIDEO_FBO_SOUL_VIDEO_DRAWER_H

#include "drawer.h"
#include "matrix.h"

class FBOSoulVideoDrawer : public Drawer {
private:
    const char *TAG = "Drawer";

    /**上下颠倒的顶点矩阵*/
    const GLfloat m_reserve_vertex_coors[8] = {
            -1.0f, 1.0f,
            1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f
    };

    //-------------灵魂出窍相关的变量--------------
    // 灵魂帧缓冲
    GLuint* m_soul_frame_buffer = NULL;
    // 灵魂纹理ID
    GLuint m_soul_texture_id = ((GLuint) 0);
    // 灵魂纹理接收者
    GLint m_soul_texture_handler = -1;
    // 灵魂缩放进度接收者
    GLint m_soul_progress_handler = -1;
    // 是否更新FBO纹理
    int m_draw_fbo = -1;
    // 更新FBO标记接收者
    GLint m_draw_fob_handler = -1;
    // 一帧灵魂的展示时间
    int64_t m_modify_time = 0;

    // 渲染纹理矩阵（渲染纹理的尺寸）
    gmMatrix4 m_matrix;
    // 渲染纹理矩阵（渲染纹理的尺寸）接收者
    GLint m_vertex_matrix_handler = -1;
    bool is_matrix_created = false;


    void CreateVertexMatrix();
    void UpdateFBO();
    void ConfigFboViewport();
    void ConfigDefaultViewport();
    void CreateFBOTexture();
    void CreateFrameBuffer();
    void BindFBO();
    void UnbindFBO();
    void DeleteFBO();

protected:
    void DoOtherDraw() override;
    void ReleaseOthers() override;

public:
    FBOSoulVideoDrawer();
    ~FBOSoulVideoDrawer();

    const char* GetVertexShader() override;
    const char* GetFragmentShader() override;
    void InitOtherShaderHandler() override;
    void BindTexture() override;
    void PrepareDraw() override;
    void DoneDraw() override;
};

#endif //OPENVIDEO_FBO_SOUL_VIDEO_DRAWER_H
