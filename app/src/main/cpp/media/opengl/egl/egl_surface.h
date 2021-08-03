//
// Created by fgrid on 2021/6/29.
//

#ifndef OPENVIDEO_EGL_SURFACE_H
#define OPENVIDEO_EGL_SURFACE_H

#include <android/native_window.h>
#include "egl_core.h"

class EglSurface {
private:

    const char *TAG = "EglSurface";

    ANativeWindow *m_native_window = NULL;

    EglCore *m_core = NULL;

    EGLSurface m_surface = NULL;

public:
    EglSurface();
    ~EglSurface();

    bool Init();
    void CreateEglSurface(ANativeWindow *native_window, int width, int height);
    void MakeCurrent();
    void SwapBuffers();
    void DestroyEglSurface();
    void Release();
};

#endif //OPENVIDEO_EGL_SURFACE_H
