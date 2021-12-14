package com.songwj.openvideo.opengl

import android.content.Context
import android.util.AttributeSet
import com.songwj.openvideo.opengl.filter.*
import com.songwj.openvideo.opengl.renders.Camera2FilterRender

class Camera2FilterPreviewGLSurface : Camera2FilterGLSurfaceView {
    constructor(context: Context?) : super(context) {
        setEGLContextClientVersion(3)
        render = Camera2FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
        render.addFilter(SoulFilter())
//        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setEGLContextClientVersion(3)
        render = Camera2FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
        render.addFilter(SoulFilter())
//        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}