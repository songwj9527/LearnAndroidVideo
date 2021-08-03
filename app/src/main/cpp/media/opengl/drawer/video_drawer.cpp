//
// Created by fgrid on 2021/7/14.
//

#include "video_drawer.h"
#include "../../../utils/logger.h"

VideoDrawer::VideoDrawer(): Drawer(0, 0) {
}

VideoDrawer::~VideoDrawer() {

}

const char * VideoDrawer::GetVertexShader() {
//    "  gl_Position = uMatrix*aPosition;\n"
//    const GLbyte shader[] = "attribute vec4 aPosition;\n"
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

const char * VideoDrawer::GetFragmentShader() {
//    const GLbyte shader[] = "precision mediump float;\n"
    const auto shader = "precision mediump float;\n"
                            "uniform sampler2D uTexture;\n"
                            "varying vec2 vCoordinate;\n"
                            "void main() {\n"
                            "  vec4 color = texture2D(uTexture, vCoordinate);\n"
                            "  gl_FragColor = color;\n"
                            "}";
    return (char *)shader;
}

void VideoDrawer::InitOtherShaderHandler() {

}

void VideoDrawer::BindTexture() {
    ActivateTexture(GL_TEXTURE_2D, m_texture_id, 0, m_texture_handler);
}

void VideoDrawer::PrepareDraw() {
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

void VideoDrawer::DoOtherDraw() {

}

void VideoDrawer::DoneDraw() {

}

void VideoDrawer::ReleaseOthers() {

}