//
// Created by fgrid on 2021/6/29.
//

#ifndef OPENVIDEO_EGL_CORE_H
#define OPENVIDEO_EGL_CORE_H

#include "../../../utils/logger.h"

extern "C" {
#include <EGL/egl.h>
#include <EGL/eglext.h>
};

class EglCore {
private:
    const int FLAG_RECORDABLE = 0x01;

    const char *TAG = "EglCore";

    // EGL显示窗口
    EGLDisplay m_egl_dsp = EGL_NO_DISPLAY;

    // EGL上下文
    EGLContext m_egl_cxt = EGL_NO_CONTEXT;

    // EGL配置
    EGLConfig m_egl_cfg;

    EGLConfig GetEGLConfig();

public:
    EglCore();
    ~EglCore();

    bool Init(EGLContext share_ctx);

    /**
     * 根据本地窗口创建显示表面
     * @param window 本地窗口
     * @return
     */
    EGLSurface CreateWindowSurface(ANativeWindow *window);

    /**
     * 创建离屏渲染表面
     * @param width 表面宽
     * @param height 表面高
     * @return
     */
    EGLSurface CreateOffScreenSurface(int width, int height);

    /**
     * 将OpenGL上下文和线程进行绑定
     * @param egl_surface
     */
    void MakeCurrent(EGLSurface egl_surface);
    void MakeCurrent(EGLSurface egl_draw_surface, EGLSurface egl_read_surface);

    /**
     * 将缓存数据交换到前台进行显示
     * @param egl_surface
     */
    void SwapBuffers(EGLSurface egl_surface);

    /**
     * 释放显示
     * @param elg_surface
     */
    void DestroySurface(EGLSurface elg_surface);

    /**
     * 释放ELG
     */
    void Release();
};

#endif //OPENVIDEO_EGL_CORE_H
