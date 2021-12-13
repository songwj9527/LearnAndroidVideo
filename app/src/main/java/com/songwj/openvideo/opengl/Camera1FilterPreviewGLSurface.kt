package com.songwj.openvideo.opengl

import android.content.Context
import android.util.AttributeSet
import com.songwj.openvideo.opengl.filter.CameraFilter
import com.songwj.openvideo.opengl.filter.CubeFilter
import com.songwj.openvideo.opengl.filter.DuskColorFilter
import com.songwj.openvideo.opengl.filter.ScreenFilter
import com.songwj.openvideo.opengl.renders.Camera1FilterRender

class Camera1FilterPreviewGLSurface : Camera1FilterGLSurfaceView {
    constructor(context: Context?) : super(context) {
        setEGLContextClientVersion(3)
        render = Camera1FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
//        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setEGLContextClientVersion(3)
        render = Camera1FilterRender(this)
        render.addFilter(CameraFilter())
        render.addFilter(DuskColorFilter())
//        render.addFilter(CubeFilter())
        render.addFilter(ScreenFilter())
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}