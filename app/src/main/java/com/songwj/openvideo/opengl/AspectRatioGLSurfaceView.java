package com.songwj.openvideo.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class AspectRatioGLSurfaceView extends GLSurfaceView {

    /**
     * 视图外观比例
     */
    public enum AspectRatioMode {
        // 自适配填充父布局（等比例显示，宽高至少有一个占满父布局）
        EQUAL_PROPORTION_FIT_PARENT,
        // 等比例占满父布局（超出则裁剪）
        EQUAL_PROPORTION_FILL_PARENT,
        // 非等比例占满付布局（可能发生变形）
        MATCH_PARENT,
    }

    private AspectRatioMode ratioMode = AspectRatioMode.MATCH_PARENT;

    // 是否已计算了视图窗口尺寸
    private boolean isMeasuredSize = false;

    // 视图宽高
    private int videoWidth, videoHeight;
    // 记录视图窗口宽高（主要用于暂停视频时，获取视频背景图片的尺寸）
    private int sizeW, sizeH;

    public AspectRatioGLSurfaceView(Context context) {
        super(context);
    }

    public AspectRatioGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRatioMode(AspectRatioMode ratioMode) {
        this.ratioMode = ratioMode;
    }

    public void setVideoFrame(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public int getSizeW() {
        return sizeW;
    }

    public int getSizeH() {
        return sizeH;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        doMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void doMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);

        Log.d("AspectRatioGLSurfaceView", "videoWidth: " + videoWidth
                + ", videoHeight: "+videoHeight
                + ", width: "+width
                + ", height: "+height);


        if (ratioMode == AspectRatioMode.MATCH_PARENT) {
            width = View.MeasureSpec.getSize(widthMeasureSpec);
            height = View.MeasureSpec.getSize(heightMeasureSpec);
        } else if (videoWidth > 0 && videoHeight > 0) {
            int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
            Log.e("AspectRatioGLSurfaceView", "widthSpecSize: " + widthSpecSize + ", heightSpecSize: "+heightSpecSize);
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                float specAspectRatio = (float) widthSpecSize / (float) heightSpecSize;
                float displayAspectRatio;
                displayAspectRatio = (float) videoWidth / (float) videoHeight;
                Log.e("TextureRenderView>>>>", "displayAspectRatio: " + displayAspectRatio + ", specAspectRatio: "+specAspectRatio);
                boolean shouldBeWider = displayAspectRatio > specAspectRatio;
                if (ratioMode == AspectRatioMode.EQUAL_PROPORTION_FIT_PARENT) {
                    if (shouldBeWider) {
                        // too wide, fix width
                        width = widthSpecSize;
                        height = (int) (width / displayAspectRatio + 0.5f);
                    } else {
                        // too high, fix height
                        height = heightSpecSize;
                        width = (int) (height * displayAspectRatio + 0.5f);
                    }
                } else if (ratioMode == AspectRatioMode.EQUAL_PROPORTION_FILL_PARENT){
                    if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize;
                        width = (int) (height * displayAspectRatio + 0.5f);
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize;
                        height = (int) (width / displayAspectRatio + 0.5f);
                    }
                } else {
                    if (shouldBeWider) {
                        // too wide, fix width
                        width = Math.min(videoWidth, widthSpecSize);
                        height = (int) (width / displayAspectRatio + 0.5f);
                    } else {
                        // too high, fix height
                        height = Math.min(videoHeight, heightSpecSize);
                        width = (int) (height * displayAspectRatio + 0.5f);
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if (videoWidth * height < width * videoHeight) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * videoWidth / videoHeight;
                } else if (videoWidth * height > width * videoHeight) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * videoHeight / videoWidth;
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * videoHeight / videoWidth;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * videoWidth / videoHeight;
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = videoWidth;
                height = videoHeight;
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * videoWidth / videoHeight;
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * videoHeight / videoWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }

        sizeW = width;
        sizeH = height;
        Log.e("AspectRatioGLSurfaceView", "sizeW: " + sizeW + ", sizeH: "+sizeH);

        setMeasuredDimension(sizeW, sizeH);
    }
}
