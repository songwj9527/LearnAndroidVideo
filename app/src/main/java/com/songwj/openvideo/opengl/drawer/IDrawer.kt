package com.songwj.openvideo.opengl.drawer

import android.graphics.SurfaceTexture
import javax.microedition.khronos.opengles.GL10

interface IDrawer {
    fun setVideoSize(videoW: Int, videoH: Int)
    fun setVideoInfo(videoW: Int, videoH: Int, videoR: Int)
    fun setWorldSize(worldW: Int, worldH: Int)
    fun setAlpha(alpha: Float)
    fun draw()
    fun setTextureID(id: Int)
    fun getSurfaceTexture(cb: (st: SurfaceTexture)->Unit) {}
    fun release()
}