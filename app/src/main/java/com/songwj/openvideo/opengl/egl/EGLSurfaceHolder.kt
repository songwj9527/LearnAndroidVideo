package com.songwj.openvideo.opengl.egl

import android.opengl.EGLContext
import android.opengl.EGLSurface


/**
 * EGLSurface 持有者
 */
class EGLSurfaceHolder {

    private val TAG = "EGLSurfaceHolder"

    private lateinit var mEGLCore: EGLCore

    private var mEGLSurface: EGLSurface? = null

    fun init(shareContext: EGLContext? = null, flags: Int) {
        mEGLCore = EGLCore()
        mEGLCore.init(shareContext, flags)
    }

    fun createEGLSurface(surface: Any?, width: Int = -1, height: Int = -1) {
        mEGLSurface = if (surface != null) {
            mEGLCore.createWindowSurface(surface)
        } else {
            mEGLCore.createOffscreenSurface(width, height)
        }
        makeCurrent()
    }

    private fun makeCurrent() {
        if (mEGLSurface != null) {
            mEGLCore.makeCurrent(mEGLSurface!!)
        }
    }

    fun onDrawMs(timeMs: Long) {
        setTimestampMs(timeMs)
        swapBuffers()
    }

    fun onDrawUs(timeUs: Long) {
        setTimestampUs(timeUs)
        swapBuffers()
    }

    fun onDrawNs(timeNs: Long) {
        setTimestampNs(timeNs)
        swapBuffers()
    }

    private fun swapBuffers() {
        if (mEGLSurface != null) {
            mEGLCore.swapBuffers(mEGLSurface!!)
        }
    }

    private fun setTimestampMs(timeMs: Long) {
        if (mEGLSurface != null) {
            mEGLCore.setPresentationTime(mEGLSurface!!, timeMs * 1000 * 1000)
        }
    }

    private fun setTimestampUs(timeUs: Long) {
        if (mEGLSurface != null) {
            mEGLCore.setPresentationTime(mEGLSurface!!, timeUs * 1000)
        }
    }

    private fun setTimestampNs(timeNs: Long) {
        if (mEGLSurface != null) {
            mEGLCore.setPresentationTime(mEGLSurface!!, timeNs)
        }
    }

    fun destroyEGLSurface() {
        if (mEGLSurface != null) {
            mEGLCore.destroySurface(mEGLSurface!!)
            mEGLSurface = null
        }
    }

    fun release() {
        mEGLCore.release()
    }
}