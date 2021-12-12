package com.songwj.openvideo.opengl.filter.base;

import android.opengl.GLES30;

import com.songwj.openvideo.opengl.utils.TextureUtils;

abstract public class AbstractFboRectFilter extends AbstractChainRectFilter {

    public AbstractFboRectFilter(String vertexShaderStr, String fragmentShaderStr) {
        super(vertexShaderStr, fragmentShaderStr);
    }

    protected int[] frameBuffer;
    protected int[] frameTextures;

    synchronized protected void createFboFrame(int width, int height) {
        if (frameTextures != null) {
            return;
        }
        //創建FBO
        /**
         * 1、创建FBO + FBO中的纹理
         */
        frameBuffer = new int[1];
        frameTextures = new int[1];
        GLES30.glGenFramebuffers(1, frameBuffer, 0);
        TextureUtils.glGenTextures(frameTextures);

        /**
         * 2、fbo与纹理关联
         */
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameTextures[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA,
                width,
                height,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                null);
        //纹理关联 fbo
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);  //綁定FBO
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                frameTextures[0],
                0);

        /**
         * 3、解除绑定
         */
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, GLES30.GL_NONE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    @Override
    public int proceed(int textureId, FilterChain filterChain) {
        createFboFrame(filterChain.getContext().width, filterChain.getContext().height);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);
        super.onDrawFrame(textureId, filterChain);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, GLES30.GL_NONE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
        //!! 返回的是FBO创建的纹理frameTextures[0]
        return filterChain.proceed(frameTextures[0]);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseFrame();
    }

    synchronized private void releaseFrame() {
        if (frameTextures != null) {
            GLES30.glDeleteTextures(1, frameTextures, 0);
            frameTextures = null;
        }

        if (frameBuffer != null) {
            GLES30.glDeleteFramebuffers(1, frameBuffer, 0);
        }
    }
}
