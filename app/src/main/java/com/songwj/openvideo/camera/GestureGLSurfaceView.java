package com.songwj.openvideo.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GestureGLSurfaceView extends GLSurfaceView {

    public GestureGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public GestureGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private  final long DELAY_TIME = 200;
    private  float clickDistance = 0f;

    private float downX = 0f;
    private float downY = 0f;
    private long touchTime = 0L;
    private long downTime = 0L;

    private OnGestureListener listener = null;

    private void init(Context context) {
        Point point = Camera2Utils.getDisplaySize(context);
        clickDistance = point.x / 20;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = System.currentTimeMillis();
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                touchTime = System.currentTimeMillis() - downTime;
                detectGesture(downX, event.getX(), downY, event.getY());
                break;
        }
        return true;
    }

    private void detectGesture(float downX, float upX, float downY, float upY) {
        float distanceX = upX - downX;
        float distanceY = upY - downY;
        if (Math.abs(distanceX) < clickDistance
                && Math.abs(distanceY) < clickDistance
                && touchTime < DELAY_TIME) {
            listener.onClick(upX, upY);
        }
    }

    public void setOnGestureListener(OnGestureListener mListener) {
        this.listener = mListener;
    }

    public interface OnGestureListener {
        void onClick(float x, float y);
    }
}
