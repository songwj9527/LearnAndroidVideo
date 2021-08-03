//
// Created by fgrid on 2021/7/1.
//

#ifndef OPENVIDEO_DRAWER_H
#define OPENVIDEO_DRAWER_H

extern "C" {
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
};

class Drawer {
private:
    const char *TAG = "Drawer";

protected:
    const GLfloat m_def_vertex_coors[8] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };

    const GLfloat m_texture_coors[8] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    const GLfloat *m_vertex_coors = m_def_vertex_coors;

    GLuint m_program_id = ((GLuint) 0);

    GLuint m_texture_id = ((GLuint) 0);

    GLint m_vertex_pos_handler = -1;

    GLint m_texture_pos_handler = -1;

    GLint m_texture_handler = -1;

    int m_origin_width = 0;

    int m_origin_height = 0;

    // 自定义用户数据，可用于存放画面数据
    void *cst_data = NULL;

    void CreateTextureId();
    void CreateProgram();
    void ActivateTexture(GLenum type, GLuint texture_id, GLenum index, int texture_handler);
    GLuint LoadShader(GLenum type, const GLchar *shader_code);
    void DoDraw();

    virtual const char* GetVertexShader() = 0;
    virtual const char* GetFragmentShader() = 0;
    virtual void InitOtherShaderHandler() = 0;
    virtual void BindTexture() = 0;
    virtual void PrepareDraw() = 0;
    virtual void DoOtherDraw() = 0;
    virtual void DoneDraw() = 0;

    virtual void ReleaseOthers() = 0;

public:
    Drawer(int origin_width, int origin_height);
    virtual ~Drawer();

    void Draw(void *frame_data);

    void setTextureID(int texture_id);

    int origin_width() {
        return m_origin_width;
    }

    int origin_height() {
        return m_origin_height;
    }

    bool IsReadyToDraw();

    /**
     * 释放OpenGL
     */
    void Release();

    /**
     * 设置绘制窗口宽高尺寸
     * @param width
     * @param height
     */
    void SetSize(int width, int height);
};

#endif //OPENVIDEO_DRAWER_H
