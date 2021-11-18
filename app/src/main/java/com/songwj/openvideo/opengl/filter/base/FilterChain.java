package com.songwj.openvideo.opengl.filter.base;

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

    public void onSurfaceCreate() {
        if(index >= filters.size()){
            ;
        }
        FilterChain nextFilterChain = new FilterChain(context, (index + 1), filters);
        AbstractRectFilter abstractRectFilter = filters.get(index);
        abstractRectFilter.onSurfaceCreated();
        nextFilterChain.onSurfaceCreate();
    }

    public void onSurfaceChanged(int width, int height) {
        if (context != null) {
            context.setWidth(width);
            context.setHeight(height);
        }
        if(index >= filters.size()){
            ;
        }
        FilterChain nextFilterChain = new FilterChain(context, (index + 1), filters);
        AbstractRectFilter abstractRectFilter = filters.get(index);
        abstractRectFilter.onSurfaceChanged(width, height);
        nextFilterChain.onSurfaceChanged(width, height);
    }

    public int onDrawFrame(int textureId) {
        if(index >= filters.size()){
            return textureId;
        }
        FilterChain nextFilterChain = new FilterChain(context, (index + 1), filters);
        AbstractRectFilter abstractRectFilter = filters.get(index);
        return abstractRectFilter.onDrawFrame(textureId, nextFilterChain);
    }

    public void onSurfaceDestroy() {
        if (filters != null && filters.size() > 0) {
            for (AbstractRectFilter filter : filters) {
                filter.onSurfaceDestroy();
            }
        }
    }

    public void setProjectmatrix(float[] projectMatrix, int length) {
        if (context != null) {
            context.setProjectMatrix(projectMatrix, length);
        }
    }

    public void setCameramatrix(float[] cameraMatrix, int length) {
        if (context != null) {
            context.setCameraMatrix(cameraMatrix, length);
        }
    }
}
