package com.songwj.openvideo;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

/**
 * @date 2017/04/19
 * @desc 视频播放surface holder
 */
public class TextureRenderView extends TextureView {

    /**
     * 视图外观比例
     */
    public enum SurfaceAspectRatioMode {
        // 自适配填充父布局（等比例显示，宽高至少有一个占满父布局）
        EQUAL_PROPORTION_FIT_PARENT,
        // 等比例占满父布局（超出则裁剪）
        EQUAL_PROPORTION_FILL_PARENT,
        // 非等比例占满付布局（可能发生变形）
        MATCH_PARENT,
        // 宽高比例16:9（可能发生变形，宽高至少有一个占满父布局）
        AR_16_9_FIT_PARENT,
        // 宽高比例4:3（可能发生变形，宽高至少有一个占满父布局）
        AR_4_3_FIT_PARENT
    }
    // 默认自适配
    private SurfaceAspectRatioMode surfaceAspectRatioMode = SurfaceAspectRatioMode.EQUAL_PROPORTION_FIT_PARENT;

    // 是否已计算了视图窗口尺寸
    private boolean isMeasuredSize = false;

    // 视频帧宽高
    private int videoWidth, videoHeight;
    // PAR —— Pixel Aspect Ratio 像素纵横比
    // DAR —— Display Aspect Ratio 显示纵横比
    // SAR —— Sample Aspect Ratio 采样纵横比
    // 采样纵横比：横向上的像素数目/纵向上的像素数目*SAR(or PAR) = DAR（每个像素可以看做一个采样）
    private int videoSarNum, videoSarDen;
    // 视频旋转角度（有的视频原始画并不是正常的，而是被旋转了一定的角度）
    private int videoRotationDegree;
    // 记录视频窗口宽高（主要用于暂停视频时，获取视频背景图片的尺寸）
    private int sizeW, sizeH;

    public TextureRenderView(Context context) {
        super(context);
    }

    public TextureRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextureRenderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackgroundResource(android.R.color.darker_gray);
    }

    public void setSurfaceAspectRatioMode(SurfaceAspectRatioMode surfaceAspectRatioMode) {
        this.surfaceAspectRatioMode = surfaceAspectRatioMode;
//        if (isMeasuredSize) {
//            requestLayout();
//        }
    }

    public void setVideoFrame(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen) {
        this.videoSarNum = videoSarNum;
        this.videoSarDen = videoSarDen;
    }

    public void setVideoRotationDegree(int videoRotationDegree) {
        this.videoRotationDegree = videoRotationDegree;
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
        int viewRotation = (int) getRotation();

//        Log.e("TextureRenderView>>>>", "videoWidth: " + videoWidth
//                + ", videoHeight: "+videoHeight
//                + ", videoSarNum: "+videoSarNum
//                + ", videoSarDen: "+videoSarDen
//                + ", videoRotation: "+videoRotationDegree
//                + ", viewRotation: "+viewRotation);

        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);

//        if (getRotation() == 90 || getRotation() % 270 == 0) {
//            int tempSpec = widthMeasureSpec;
//            widthMeasureSpec = heightMeasureSpec;
//            heightMeasureSpec = tempSpec;
//        }
//
//        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
//            int tempSpec = videoWidth;
//            videoWidth = videoHeight;
//            videoHeight = tempSpec;
//        }

        Log.e("TextureRenderView>>>>", "videoWidth: " + videoWidth
                + ", videoHeight: "+videoHeight
                + ", videoSarNum: "+videoSarNum
                + ", videoSarDen: "+videoSarDen
                + ", videoRotation: "+videoRotationDegree
                + ", viewRotation: "+viewRotation
                + ", width: "+width
                + ", height: "+height);


        if (surfaceAspectRatioMode == SurfaceAspectRatioMode.MATCH_PARENT) {
            width = View.MeasureSpec.getSize(widthMeasureSpec);
            height = View.MeasureSpec.getSize(heightMeasureSpec);
        } else if (videoWidth > 0 && videoHeight > 0) {
            int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
            Log.e("TextureRenderView>>>>", "widthSpecSize: " + widthSpecSize + ", heightSpecSize: "+heightSpecSize);
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                float specAspectRatio = (float) widthSpecSize / (float) heightSpecSize;
                float displayAspectRatio;
                if (surfaceAspectRatioMode == SurfaceAspectRatioMode.AR_16_9_FIT_PARENT) {
                    displayAspectRatio = 16.0f / 9.0f;
//                    if (videoRotationDegree == 90 || videoRotationDegree == 270)
//                        displayAspectRatio = 1.0f / displayAspectRatio;
                } else if (surfaceAspectRatioMode == SurfaceAspectRatioMode.AR_4_3_FIT_PARENT) {
                    displayAspectRatio = 4.0f / 3.0f;
//                    if (videoRotationDegree == 90 || videoRotationDegree == 270)
//                        displayAspectRatio = 1.0f / displayAspectRatio;
                } else {
                    displayAspectRatio = (float) videoWidth / (float) videoHeight;
                    if (videoSarNum > 0 && videoSarDen > 0)
                        displayAspectRatio = displayAspectRatio * videoSarNum / videoSarDen;
//                    if (videoRotationDegree == 90 || videoRotationDegree == 270)
//                        displayAspectRatio = 1.0f / displayAspectRatio;
                }
                Log.e("TextureRenderView>>>>", "displayAspectRatio: " + displayAspectRatio + ", specAspectRatio: "+specAspectRatio);
                boolean shouldBeWider = displayAspectRatio > specAspectRatio;
                if (surfaceAspectRatioMode == SurfaceAspectRatioMode.EQUAL_PROPORTION_FIT_PARENT
                        || surfaceAspectRatioMode == SurfaceAspectRatioMode.AR_16_9_FIT_PARENT
                        || surfaceAspectRatioMode == SurfaceAspectRatioMode.AR_4_3_FIT_PARENT) {
                    if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                        if (displayAspectRatio >= 1f && specAspectRatio >= 1f) {
                            width = heightSpecSize;
                            height = (int) (width / displayAspectRatio + 0.5f);
                        }
                        else if (displayAspectRatio < 1f && specAspectRatio >= 1f) {
                            displayAspectRatio = 1f / displayAspectRatio;
                            if (displayAspectRatio >= specAspectRatio) {
                                height = widthSpecSize;
                                width = (int) (height / displayAspectRatio + 0.5f);
                            } else {
                                width = heightSpecSize;
                                height = (int) (width * displayAspectRatio + 0.5f);
                            }
                        }
                        else if (displayAspectRatio >= 1f && specAspectRatio < 1f) {
                            specAspectRatio = 1f / specAspectRatio;
                            if (displayAspectRatio >= specAspectRatio) {
                                width = heightSpecSize;
                                height = (int) (width * displayAspectRatio + 0.5f);
                            } else {
                                height = widthSpecSize;
                                width = (int) (height / displayAspectRatio + 0.5f);
                            }
                        } else {
                            width = widthSpecSize;
                            height = (int) (width * displayAspectRatio + 0.5f);
                        }
                    } else {
                        if (shouldBeWider) {
                            // too wide, fix width
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio + 0.5f);
                        } else {
                            // too high, fix height
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio + 0.5f);
                        }
                    }
                } else if (surfaceAspectRatioMode == SurfaceAspectRatioMode.EQUAL_PROPORTION_FILL_PARENT){
                    if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                        displayAspectRatio = 1f / displayAspectRatio;
                        specAspectRatio = 1f / specAspectRatio;
                        shouldBeWider = displayAspectRatio > specAspectRatio;
                        if (shouldBeWider) {
                            // not high enough, fix height
                            height = heightSpecSize;
                            width = (int) (height * displayAspectRatio + 0.5f);
                        } else {
                            // not wide enough, fix width
                            width = widthSpecSize;
                            height = (int) (width / displayAspectRatio + 0.5f);
                        }
                    }
                    else if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize;
                        width = (int) (height * displayAspectRatio + 0.5f);
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize;
                        height = (int) (width / displayAspectRatio + 0.5f);
                    }
                } else {
                    if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                        width = Math.min(videoHeight, heightSpecSize);
                        height = Math.min(videoWidth, widthSpecSize);
                    }
                    else if (shouldBeWider) {
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
        Log.e("TextureRenderView>>>>", "sizeW: " + sizeW + ", sizeH: "+sizeH);

        setMeasuredDimension(sizeW, sizeH);
    }
}
