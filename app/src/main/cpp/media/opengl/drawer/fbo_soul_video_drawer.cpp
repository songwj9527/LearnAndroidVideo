//
// Created by fgrid on 2021/8/2.
//

#include <malloc.h>
#include "fbo_soul_video_drawer.h"
#include "../../../utils/logger.h"

extern "C" {
#include <libavutil/time.h>
};

FBOSoulVideoDrawer::FBOSoulVideoDrawer(): Drawer(0, 0) {
}

FBOSoulVideoDrawer::~FBOSoulVideoDrawer() {

}

const char * FBOSoulVideoDrawer::GetVertexShader() {
//    "  gl_Position = uMatrix*aPosition;\n"
//    "const GLbyte shader[] = "attribute vec4 aPosition;\n"
    const auto shader = "attribute vec4 aPosition;\n"
                        "uniform mat4 uMatrix;\n"
                        "attribute vec2 aCoordinate;\n"
                        "varying vec2 vCoordinate;\n"
                        "void main() {\n"
                        "  gl_Position = aPosition;\n"
                        "  vCoordinate = aCoordinate;\n"
                        "}";
    return (char *)shader;
}

const char * FBOSoulVideoDrawer::GetFragmentShader() {
//    const GLbyte shader[] = "precision mediump float;\n"
    const auto shader = "precision mediump float;\n"
                        "uniform sampler2D uTexture;\n"
                        "varying vec2 vCoordinate;\n"
                        "uniform float progress;\n"
                        "uniform int drawFbo;\n"
                        "uniform sampler2D uSoulTexture;\n"
                        "void main() {\n"
//                        "  vec4 color = texture2D(uTexture, vCoordinate);\n"
//                        "  gl_FragColor = color;\n"
                        // 透明度[0,0.4]
                        "   float alpha = 0.6 * (1.0 - progress);\n"
                        // 缩放比例[1.0,1.8]
                        "   float scale = 1.0 + (1.5 - 1.0) * progress;\n"
                        // 放大纹理坐标
                        // 根据放大比例，得到放大纹理坐标 [0,0],[0,1],[1,1],[1,0]
                        "   float soulX = 0.5 + (vCoordinate.x - 0.5) / scale;\n"
                        "   float soulY = 0.5 + (vCoordinate.y - 0.5) / scale;\n"
                        "   vec2 soulTextureCoords = vec2(soulX, soulY);\n"
                        // 获取对应放大纹理坐标下的纹素(颜色值rgba)
                        "   vec4 soulMask = texture2D(uSoulTexture, soulTextureCoords);\n"

                        "   vec4 color = texture2D(uTexture, vCoordinate);\n"

                        "   if (drawFbo == 0) {\n"
                        // 颜色混合 默认颜色混合方程式 = mask * (1.0-alpha) + weakMask * alpha
                        "       gl_FragColor = color * (1.0 - alpha) + soulMask * alpha;\n"
                        "   } else {\n"
                        "       gl_FragColor = vec4(color.r, color.g, color.b, 1.0);\n"
                        "   }"
                        "}";
    return (char *)shader;
}

void FBOSoulVideoDrawer::InitOtherShaderHandler() {
    m_vertex_matrix_handler = glGetUniformLocation(m_program_id, "uMatrix");
    m_soul_texture_handler = glGetUniformLocation(m_program_id, "uSoulTexture");
    m_soul_progress_handler = glGetUniformLocation(m_program_id, "progress");
    m_draw_fob_handler = glGetUniformLocation(m_program_id, "drawFbo");
}

void FBOSoulVideoDrawer::CreateVertexMatrix() {
    if (!is_matrix_created) {
        is_matrix_created = true;
        gmMatrix4 pre_matrix;
        gmVector4 pre_matrix_area;
        pre_matrix_area.x = -1;
        pre_matrix_area.y = 1;
        pre_matrix_area.z = -1;
        pre_matrix_area.w = 1;
        gmVector2 pre_matrix_deep;
        pre_matrix_deep.x = 3;
        pre_matrix_deep.y = 5;
        gmMatrixOrthoM(&pre_matrix, &pre_matrix_area, &pre_matrix_deep);

        gmMatrix4 view_matrix;
        gmVector3 eye, at, up;
        eye.x = 0;
        eye.y = 0;
        eye.z = 5.0f;
        at.x = 0;
        at.y = 0;
        at.z = 0;
        up.x = 0;
        up.y = 1.0f;
        up.z = 0;
        gmMatrixLookAtLH(&view_matrix, &eye, &at, &up);

        gmMatrixMultiply(&m_matrix, &pre_matrix, &view_matrix);
    }
}

void FBOSoulVideoDrawer::UpdateFBO() {
    if (m_origin_width > 0 && m_origin_height > 0) {
        CreateFBOTexture();
        CreateFrameBuffer();
        // av_gettime()得到的时微妙
        int64_t ms = av_gettime() / 1000; // 转换为毫秒
        if (ms - m_modify_time > 500) {
            m_modify_time = ms;
            // 绑定FBO
            BindFBO();
            // 配置FBO窗口
            ConfigFboViewport();
            // 激活默认纹理
            ActivateTexture(GL_TEXTURE_2D, m_texture_id, 0, m_texture_handler);
            // 更新纹理
            PrepareDraw();
            // 绘制到FBO
            DoDraw();
            // 解绑FBO
            UnbindFBO();
            // 恢复默认绘制窗口
            ConfigDefaultViewport();
        }
    }
}


void FBOSoulVideoDrawer::CreateFBOTexture() {
    if (m_origin_width > 0 && m_origin_height > 0) {
        if (!m_soul_texture_id) {
            GLuint texture_id[1];
            glGenTextures(1, texture_id);
            GLenum error_code = glGetError();
            LOGI(TAG, "Create texture id : %u, %x", texture_id[0], error_code)
            if (error_code == GL_NO_ERROR) {
                m_soul_texture_id = texture_id[0];
            }
            glBindTexture(GL_TEXTURE_2D, m_soul_texture_id);
            glTexImage2D(GL_TEXTURE_2D,
                         0, // level一般为0
                         GL_RGBA, //纹理内部格式
                         origin_width(), origin_height(), // 画面宽高
                         0, // 必须为0
                         GL_RGBA, // 数据格式，必须和上面的纹理格式保持一直
                         GL_UNSIGNED_BYTE, // RGBA每位数据的字节数，这里是BYTE​: 1 byte
                         NULL);// 画面数据
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D,0);
        }
    }
}
void FBOSoulVideoDrawer::CreateFrameBuffer() {
    if (m_soul_frame_buffer == NULL) {
        m_soul_frame_buffer = static_cast<GLuint *>(malloc(sizeof(GLuint) * 1));
        glGenFramebuffers(1, m_soul_frame_buffer);
    }
}

void FBOSoulVideoDrawer::BindFBO() {
    if (m_soul_texture_id && m_soul_frame_buffer != NULL) {
        glBindFramebuffer(GL_FRAMEBUFFER, m_soul_frame_buffer[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, m_soul_texture_id, 0);
    }
}

void FBOSoulVideoDrawer::UnbindFBO() {
    if (m_soul_texture_id && m_soul_frame_buffer != NULL) {
        glBindRenderbuffer(GL_RENDERBUFFER, GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, GL_NONE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}

void FBOSoulVideoDrawer::DeleteFBO() {
    if (m_soul_frame_buffer != NULL) {
        //删除Render Buffer
        glBindRenderbuffer(GL_RENDERBUFFER, GL_NONE);
        //删除Frame Buffer
        glBindFramebuffer(GL_FRAMEBUFFER, GL_NONE);
        glDeleteFramebuffers(1, m_soul_frame_buffer);
    }
    if (m_soul_texture_id) {
        //删除纹理
        glBindTexture(GL_TEXTURE_2D, 0);
        GLuint texture_id[1];
        texture_id[0] = m_soul_texture_id;
        glDeleteTextures(1, texture_id);
    }
}

void FBOSoulVideoDrawer::ConfigFboViewport() {
    m_draw_fbo = 1;
    if (is_matrix_created) {
        // 将变换矩阵回复为单位矩阵（将画面拉升到整个窗口大小，设置窗口宽高和FBO纹理宽高一致，画面刚好可以正常绘制到FBO纹理上）
        gmMatrix4IdentityM(&m_matrix);
    }
    // 设置颠倒的顶点坐标
    m_vertex_coors = m_reserve_vertex_coors;
    //设置一个颜色状态
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    //使能颜色状态的值来清屏
    glClear(GL_COLOR_BUFFER_BIT);
}

void FBOSoulVideoDrawer::ConfigDefaultViewport() {
    m_draw_fbo = 0;
    is_matrix_created = false;
    // 恢复顶点坐标
    m_vertex_coors = m_def_vertex_coors;
//    CreateVertexMatrix();
}

void FBOSoulVideoDrawer::BindTexture() {
//    CreateVertexMatrix();
    UpdateFBO();
    // 【激活并绑定灵魂纹理单元】
    ActivateTexture(GL_TEXTURE_2D, m_soul_texture_id, 1, m_soul_texture_handler);
    //【激活并绑定默认纹理单元】
    ActivateTexture(GL_TEXTURE_2D, m_texture_id, 0, m_texture_handler);
}

void FBOSoulVideoDrawer::PrepareDraw() {
    if (cst_data != NULL) {
        LOGE(TAG, "PrepareDraw()")
//        glTexImage2D(GL_TEXTURE_2D,
        glTexImage2D(GL_TEXTURE_2D,
                     0, // level一般为0
                     GL_RGBA, //纹理内部格式
                     origin_width(), origin_height(), // 画面宽高
                     0, // 必须为0
                     GL_RGBA, // 数据格式，必须和上面的纹理格式保持一直
                     GL_UNSIGNED_BYTE, // RGBA每位数据的字节数，这里是BYTE​: 1 byte
                     cst_data);// 画面数据
    }
}

void FBOSoulVideoDrawer::DoOtherDraw() {
    if (m_vertex_matrix_handler != -1) {
        glUniformMatrix4fv(m_vertex_matrix_handler, 1, false, m_matrix.m);
    }
    int64_t ms = av_gettime() / 1000;
    int64_t progress = ms - m_modify_time;
    if (progress > 500) {
        progress = 500;
    }
    glUniform1f(m_soul_progress_handler, (progress / 500.0));
    glUniform1i(m_draw_fob_handler, m_draw_fbo);
}

void FBOSoulVideoDrawer::DoneDraw() {

}

void FBOSoulVideoDrawer::ReleaseOthers() {
    DeleteFBO();
}