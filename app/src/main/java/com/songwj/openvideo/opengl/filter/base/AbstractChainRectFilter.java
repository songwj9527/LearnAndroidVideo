package com.songwj.openvideo.opengl.filter.base;

abstract public class AbstractChainRectFilter extends AbstractRectFilter {

    public AbstractChainRectFilter(String vertexShaderStr, String fragmentShaderStr) {
        super(vertexShaderStr, fragmentShaderStr);
    }

    public int proceed(int textureId, FilterChain filterChain) {
        return filterChain.proceed(onDrawFrame(textureId, filterChain));
    }
}
