package com.songwj.openvideo.opengl.egl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.view.*
import com.songwj.openvideo.opengl.OpenGLTools
import com.songwj.openvideo.opengl.drawer.IDrawer
import java.lang.ref.WeakReference


/**
 * 自定义的OpenGL渲染器
 * 包含EGL的初始化，线程与OpenGL上下文绑定，渲染循环，资源销毁等
 */
class CustomerGLSurfaceTextureRender {

    private val mThread = RenderThread()
    private var mTextureView: WeakReference<TextureView>? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mSurface: Surface? = null
    private val mDrawers = mutableListOf<IDrawer>()

    init {
        mThread.start()
    }

    fun setSurface(textureView: TextureView) {
        mTextureView = WeakReference(textureView)
        textureView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener{
            override fun onViewDetachedFromWindow(v: View?) {
                stop()
            }

            override fun onViewAttachedToWindow(v: View?) {
            }
        })
    }

    fun setSurface(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        mSurfaceTexture = surfaceTexture
        surfaceTexture?.let {
            mSurface = Surface(surfaceTexture)
        }
        mThread.onSurfaceCreate()
        mThread.onSurfaceChange(width, height)
    }

    fun setRenderMode(mode: RenderMode) {
        mThread.setRenderMode(mode)
    }

    fun notifySwap(timeUs: Long) {
        mThread.notifySwap(timeUs)
    }

    fun addDrawer(drawer: IDrawer) {
        mDrawers.add(drawer)
    }

    fun stop() {
        mThread.onSurfaceStop()
        mSurfaceTexture = null
    }

    fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        mSurfaceTexture = surface
        surface?.let {
            mSurface = Surface(surface)
        }
        mThread.onSurfaceCreate()
        mThread.onSurfaceChange(width, height)
    }

    fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        mThread.onSurfaceChange(width, height)
    }

    fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        mSurfaceTexture = surface
        surface?.let {
            mSurface = Surface(surface)
        }
    }

    fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        mThread.onSurfaceDestroy()
        mDrawers.forEach {
            it.release()
        }
        surface?.release()
        return true
    }

    inner class RenderThread: Thread() {

        // 渲染状态
        private var mState = RenderState.NO_SURFACE

        private var mEGLSurface: EGLSurfaceHolder? = null

        // 是否绑定了EGLSurface
        private var mHaveBindEGLContext = false

        //是否已经新建过EGL上下文，用于判断是否需要生产新的纹理ID
        private var mNeverCreateEglContext = true

        private var mWidth = 0
        private var mHeight = 0

        private val mWaitLock = Object()

        private var mCurTimestamp = 0L

        private var mLastTimestamp = 0L

        private var mRenderMode = RenderMode.RENDER_WHEN_DIRTY

        private fun holdOn() {
            synchronized(mWaitLock) {
                mWaitLock.wait()
            }
        }

        private fun notifyGo() {
            synchronized(mWaitLock) {
                mWaitLock.notify()
            }
        }

        fun setRenderMode(mode: RenderMode) {
            mRenderMode = mode
        }

        fun onSurfaceCreate() {
            mState = RenderState.FRESH_SURFACE
            notifyGo()
        }

        fun onSurfaceChange(width: Int, height: Int) {
            mWidth = width
            mHeight = height
            mState = RenderState.SURFACE_CHANGE
            notifyGo()
        }

        fun onSurfaceDestroy() {
            mState = RenderState.SURFACE_DESTROY
            notifyGo()
        }

        fun onSurfaceStop() {
            mState = RenderState.STOP
            notifyGo()
        }

        fun notifySwap(timeUs: Long) {
            synchronized(mCurTimestamp) {
                mCurTimestamp = timeUs
            }
            notifyGo()
        }

        override fun run() {
            initEGL()
            while (true) {
                when (mState) {
                    RenderState.FRESH_SURFACE -> {
                        createEGLSurfaceFirst()
                        holdOn()
                    }
                    RenderState.SURFACE_CHANGE -> {
                        createEGLSurfaceFirst()
                        GLES20.glViewport(0, 0, mWidth, mHeight)
                        configWordSize()
                        mState = RenderState.RENDERING
                    }
                    RenderState.RENDERING -> {
                        render()
                        if (mRenderMode == RenderMode.RENDER_WHEN_DIRTY) {
                            holdOn()
                        }
                    }
                    RenderState.SURFACE_DESTROY -> {
                        destroyEGLSurface()
                        mState = RenderState.NO_SURFACE
                    }
                    RenderState.STOP -> {
                        releaseEGL()
                        return
                    }
                    else -> {
                        holdOn()
                    }
                }
                if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                    sleep(16)
                }
            }
        }

        private fun initEGL() {
            mEGLSurface = EGLSurfaceHolder()
            mEGLSurface?.init(null, EGL_RECORDABLE_ANDROID)
        }

        private fun createEGLSurfaceFirst() {
            if (!mHaveBindEGLContext) {
                mHaveBindEGLContext = true
                createEGLSurface()
                if (mNeverCreateEglContext) {
                    mNeverCreateEglContext = false
                    GLES20.glClearColor(0f, 0f, 0f, 0f)
                    //开启混合，即半透明
                    GLES20.glEnable(GLES20.GL_BLEND)
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                    generateTextureID()
                }
            }
        }

        private fun createEGLSurface() {
//            mEGLSurface?.createEGLSurface(mSurface)
//            mEGLSurface?.makeCurrent()
            mEGLSurface?.createEGLSurface(mSurface)
        }

        private fun generateTextureID() {
            val textureIds = OpenGLTools.createTextureIds(mDrawers.size)
            for ((idx, drawer) in mDrawers.withIndex()) {
                drawer.setTextureID(textureIds[idx])
            }
        }

        private fun configWordSize() {
            mDrawers.forEach { it.setWorldSize(mWidth, mHeight) }
        }

        private fun render() {
            val render = if (mRenderMode == RenderMode.RENDER_CONTINUOUSLY) {
                true
            } else {
                synchronized(mCurTimestamp) {
                    if (mCurTimestamp > mLastTimestamp) {
                        mLastTimestamp = mCurTimestamp
                        true
                    } else {
                        false
                    }
                }
            }

            if (render) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
                mDrawers.forEach { it.draw() }
//                mEGLSurface?.setTimestamp(mCurTimestamp)
//                mEGLSurface?.swapBuffers()
                mEGLSurface?.onDrawUs(mCurTimestamp)
            }
        }

        private fun destroyEGLSurface() {
            mEGLSurface?.destroyEGLSurface()
            mHaveBindEGLContext = false
        }

        private fun releaseEGL() {
            mEGLSurface?.release()
        }
    }

    /**
     * 渲染状态
     */
    enum class RenderState {
        NO_SURFACE, //没有有效的surface
        FRESH_SURFACE, //持有一个未初始化的新的surface
        SURFACE_CHANGE, //surface尺寸变化
        RENDERING, //初始化完毕，可以开始渲染
        SURFACE_DESTROY, //surface销毁
        STOP //停止绘制
    }

    enum class RenderMode {
        RENDER_CONTINUOUSLY,
        RENDER_WHEN_DIRTY
    }
}