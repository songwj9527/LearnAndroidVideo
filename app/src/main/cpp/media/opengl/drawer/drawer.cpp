//
// Created by fgrid on 2021/7/2.
//

#include "drawer.h"
#include <malloc.h>
#include "../../../utils/logger.h"

Drawer::Drawer(int origin_width, int origin_height):
        m_origin_width(origin_width),
        m_origin_height(origin_height) {
}

Drawer::~Drawer() {
}

void Drawer::setTextureID(int texture_id) {
    m_texture_id = texture_id;
    m_program_id = 0;
}

void Drawer::SetSize(int width, int height) {
    this->m_origin_width = width;
    this->m_origin_height = height;
}

void Drawer::Draw(void *frame_data) {
    LOGE(TAG, "Draw(): %d, %d", m_origin_width, m_origin_height)
    cst_data = frame_data;
    if (IsReadyToDraw()) {
        CreateTextureId();
        CreateProgram();
        BindTexture();
        PrepareDraw();
        DoDraw();
        DoneDraw();
    }
}

bool Drawer::IsReadyToDraw() {
    return m_origin_width > 0 && m_origin_height > 0;
}

void Drawer::CreateTextureId() {
//    if (m_texture_id == ((GLuint) 0)) {
    if (!m_texture_id) {
        GLuint texture_id[1];
        glGenTextures(1, texture_id);
        GLenum error_code = glGetError();
        LOGI(TAG, "Create texture id : %u, %x", texture_id[0], error_code)
        if (texture_id[0]) {
            m_texture_id = texture_id[0];
            return;
        }
//        glGenTextures(1, &m_texture_id);
//        LOGI(TAG, "Create texture id : %u, %x", m_texture_id, glGetError())
    }
}

void Drawer::CreateProgram() {
//    LOGI(TAG, "current gl program : %u", m_program_id)
//    if (m_program_id == ((GLuint) 0)) {
    if (!m_program_id) {
        //创建一个空的OpenGLES程序，注意：需要在OpenGL渲染线程中创建，否则无法渲染
        m_program_id = glCreateProgram();
        GLenum error_code = glGetError();
        LOGI(TAG, "create gl program : %d, %x", m_program_id, error_code)
        if (!m_program_id) {
            return;
        }

        GLuint vertexShader = LoadShader(GL_VERTEX_SHADER, GetVertexShader());
        GLuint fragmentShader = LoadShader(GL_FRAGMENT_SHADER, GetFragmentShader());

        //将顶点着色器加入到程序
        glAttachShader(m_program_id, vertexShader);
        //将片元着色器加入到程序中
        glAttachShader(m_program_id, fragmentShader);
        //连接到着色器程序
        glLinkProgram(m_program_id);

        //检查错误
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(m_program_id, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(m_program_id, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(m_program_id, bufLength, NULL, buf);
                    free(buf);
                }
            }
            glDeleteProgram(m_program_id);
            m_program_id = 0;
            LOGI(TAG, "Failed to link program %d", m_program_id);
            return;
        }
        //使用OpenGL程序
        glUseProgram(m_program_id);

        m_vertex_pos_handler = glGetAttribLocation(m_program_id, "aPosition");
        m_texture_handler = glGetUniformLocation(m_program_id, "uTexture");
        m_texture_pos_handler = glGetAttribLocation(m_program_id, "aCoordinate");

        InitOtherShaderHandler();

//        glDeleteShader(vertexShader);
//        glDeleteShader(fragmentShader);
    } else {
        glUseProgram(m_program_id);
    }
}

GLuint Drawer::LoadShader(GLenum type, const GLchar *shader_code) {
    LOGI(TAG, "Load shader:\n%s", shader_code)
    //根据type创建顶点着色器或者片元着色器
    GLuint shader = glCreateShader(type);
    if (shader) {
        //将资源加入到着色器中，并编译
        glShaderSource(shader, 1, &shader_code, NULL);
        glCompileShader(shader);

        GLint compiled;
        // 检查编译状态
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;

            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);

            if (infoLen > 1) {
                GLchar* infoLog = (GLchar*) malloc(sizeof(GLchar) * infoLen);

                glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
                LOGI(TAG, "Error compiling shader:\n%s\n", infoLog);

                free(infoLog);
            }

            glDeleteShader(shader);
            return 0;
        }
    }
    return shader;
}

void Drawer::ActivateTexture(GLenum type, GLuint texture_id, GLenum index, int texture_handler) {
    if (!texture_id || texture_handler < 0) {
        return;
    }
    if (index < 0) {
        index = 0;
    }
    //激活指定纹理单元
    glActiveTexture(GL_TEXTURE0 + index);
    //绑定纹理ID到纹理单元
    glBindTexture(type, texture_id);
    //将活动的纹理单元传递到着色器里面
    glUniform1i(texture_handler, index);
    //配置边缘过渡参数
    glTexParameterf(type, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterf(type, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(type, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(type, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
}

void Drawer::DoDraw() {
    //使用OpenGL程序
    if (m_program_id) {
        LOGI(TAG, "DoDraw（）")
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        //启用顶点的句柄
        glEnableVertexAttribArray(m_vertex_pos_handler);
        glEnableVertexAttribArray(m_texture_pos_handler);
        //设置着色器参数
        glVertexAttribPointer(m_vertex_pos_handler, 2, GL_FLOAT, GL_FALSE, 0, m_vertex_coors);
        glVertexAttribPointer(m_texture_pos_handler, 2, GL_FLOAT, GL_FALSE, 0, m_texture_coors);
        // 添加其他绘制（如FBO灵魂抖动绘制）
        DoOtherDraw();
        //开始绘制
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
}

void Drawer::Release() {
    m_origin_width = 0;
    m_origin_height = 0;
    ReleaseOthers();
    if (m_program_id) {
        glDisableVertexAttribArray(m_vertex_pos_handler);
        glDisableVertexAttribArray(m_texture_pos_handler);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
    if (m_texture_id) {
        glDeleteTextures(1, &m_texture_id);
    }
    if (m_program_id) {
        glDeleteProgram(m_program_id);
    }
    cst_data = NULL;
}