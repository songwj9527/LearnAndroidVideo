package com.songwj.openvideo.opengl.filter.base;

import android.util.Log;

import java.util.List;

public class FilterChain {
    private FilterContext context;
    private int index = 0;
    private List<AbstractRectFilter> filters;

    public FilterChain(FilterContext context, int index, List<AbstractRectFilter> filters) {
        this.context = context;
        this.index = index;
        this.filters = filters;
    }

    public FilterContext getContext() {
        return context;
    }

    public void setSize(int width, int height) {
        if (context != null) {
            context.width = width;
            context.height = height;
        }
    }

    public void init() {
        if (filters != null && filters.size() > 0) {
            for (AbstractRectFilter filter : filters) {
                Log.e("FilterChain", filter.getClass().getSimpleName() + ".onCreated()");
                filter.onCreated();
            }
        }
    }

    public int proceed(int textureId) {
        if (filters == null| filters.size() == 0) {
            return textureId;
        }
        if(index >= filters.size()){
            return textureId;
        }
        FilterChain nextFilterChain = new FilterChain(context, (index + 1), filters);
        AbstractRectFilter abstractRectFilter = filters.get(index);
        Log.e("FilterChain", abstractRectFilter.getClass().getSimpleName() + ".onDrawFrame()");
        return abstractRectFilter.onDrawFrame(textureId, nextFilterChain);
    }

    public void release() {
        if (filters != null && filters.size() > 0) {
            for (AbstractRectFilter filter : filters) {
                filter.onDestroy();
            }
            filters.clear();
        }
    }

    public void setCameraMatrix(float[] cameraMatrix, int length) {
        if (context != null) {
            context.setCameraMatrix(cameraMatrix, length);
        }
    }
}