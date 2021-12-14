package com.songwj.openvideo.mediarecord;

import android.opengl.EGLContext;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.songwj.openvideo.audio.AudioCapture;
import com.songwj.openvideo.mediacodec.encoder.AudioEncoder;
import com.songwj.openvideo.mediacodec.encoder.VideoEncoder;
import com.songwj.openvideo.mediacodec.muxer.MMuxer;
import com.songwj.openvideo.opengl.egl.EGLSurfaceHolder;
import com.songwj.openvideo.opengl.filter.base.AbstractChainRectFilter;
import com.songwj.openvideo.opengl.filter.base.FilterChain;
import com.songwj.openvideo.opengl.filter.base.FilterContext;

import java.util.ArrayList;
import java.util.List;

public class Camera2EglRecorder implements MMuxer.IMuxerStateListener, AudioCapture.OnAudioCaptureListener {
    private String filePath = "";
    private int videoWidth = 0;
    private int videoHeight = 0;

    private MMuxer mmuxer = null;

    private VideoEncoder videoEncoder = null;
    private volatile boolean isVideoEncoderPrepared = false;
    private HandlerThread handlerThread = null;
    private Handler handler = null;
    private EGLContext eglContext = null;
    private EGLSurfaceHolder eglSurfaceHolder = null;
    private static final int EGL_RECORDABLE_ANDROID_FLAG = 0x3142;
    private FilterChain filterChain = null;

    private AudioEncoder audioEncoder = null;
    private volatile boolean isAudioEncoderPrepared = false;
    private AudioCapture audioCapture = null;

    private volatile boolean isPrepared = false;
    private volatile boolean isStarting = false;
    private volatile boolean isStarted = false;

    public Camera2EglRecorder(String filePath, int videoWidth, int videoHeight, EGLContext eglContext) {
        this.filePath = filePath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.eglContext = eglContext;
    }

    public Camera2EglRecorder(int videoWidth, int videoHeight, EGLContext eglContext) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.eglContext = eglContext;
    }

    public void setDataSource(String filePath) {
        this.filePath = filePath;
    }

    public Surface getSurface() {
        if (videoEncoder == null) {
            return null;
        }
        return videoEncoder.getSurface();
    }

    synchronized public boolean start() {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        if (isStarting || isStarted) {
            return false;
        }
        isStarting = true;
        Log.e("Camera2EglRecorder", "filePath: " + filePath + "\n"
                + "videoWidth: " + videoWidth + ", videoHeight: " + videoHeight);

        try {
            mmuxer = new MMuxer(filePath);
            mmuxer.setStateListener(this);
            audioEncoder = new AudioEncoder(mmuxer);
            audioEncoder.start();
            audioCapture = new AudioCapture();
            audioCapture.setOnAudioCaptureListener(this);
            audioCapture.start();
            videoEncoder = new VideoEncoder(mmuxer, videoWidth, videoHeight, false);
            videoEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (mmuxer != null) {
                mmuxer.setStateListener(null);
                mmuxer.stopMuxer();
                mmuxer = null;
            }
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
                videoEncoder = null;
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
                audioEncoder = null;
            }
            if (audioCapture != null) {
                audioCapture.setOnAudioCaptureListener(null);
                audioCapture.stop();
                audioEncoder = null;
            }
        }

        if (mmuxer == null) {
            isStarting = false;
            return false;
        }
        handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                eglSurfaceHolder = new EGLSurfaceHolder();
                eglSurfaceHolder.init(eglContext, EGL_RECORDABLE_ANDROID_FLAG);
                eglSurfaceHolder.createEGLSurface(videoEncoder.getSurface(), videoWidth, videoHeight);

                RecordRender recordRender = new RecordRender();
                List<AbstractChainRectFilter> filterList = new ArrayList<>();
                filterList.add(recordRender);

                FilterContext filterContext = new FilterContext();
                filterContext.setWidth(videoWidth);
                filterContext.setHeight(videoHeight);

                filterChain = new FilterChain(filterContext, 0, filterList);
                filterChain.setSize(videoWidth, videoHeight);
                filterChain.init();

                isStarted = true;
                isStarting = false;
            }
        });
        return true;
    }

    synchronized public void stop() {
        if (isStarted) {
            if (mmuxer != null) {
                mmuxer.stopMuxerPrepare();
            }
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
            }
            if (audioCapture != null) {
                audioCapture.stop();
                audioCapture.setOnAudioCaptureListener(null);
            }
            videoEncoder = null;
            audioCapture = null;
            audioEncoder = null;
            mmuxer = null;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (filterChain != null) {
                        filterChain.release();
                        filterChain = null;
                    }
                    if (eglSurfaceHolder != null) {
                        eglSurfaceHolder.destroyEGLSurface();
                        eglSurfaceHolder.release();
                        eglSurfaceHolder = null;
                    }
                    handler.getLooper().quitSafely();
                    handlerThread = null;
                    handler = null;
                    isStarted = false;
                }
            });
        }
    }

    @Override
    public void onMuxerStart() {
        isPrepared = true;
        if (videoEncoder != null) {
            videoEncoder.startEncode();
        }
        if (audioEncoder != null) {
            audioEncoder.startEncode();
        }
    }

    @Override
    public void onMuxerFinish() {
        if (isStarted) {
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
            }
            if (audioCapture != null) {
                audioCapture.stop();
                audioCapture.setOnAudioCaptureListener(null);
            }
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
            }
            if (mmuxer != null) {
                mmuxer = null;
            }
            isStarted = false;
        }
    }

    @Override
    public void onAudioFrameUpdate(byte[] data, int dataSize) {
//        if (isStarted && isPrepared) {
        if (isStarted) {
            Log.i("Camera2EglRecorder", "onAudioFrameUpdate()");
            if (audioEncoder != null) {
                audioEncoder.dequeueFrame(data, dataSize);
            }
        }
    }

    public void onDrawFrame(final int textureId, final long timestampNs){
//        if (isStarted && isPrepared && !isVideoEncodeManually) {
        if (isStarted) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (filterChain != null) {
                        filterChain.proceed(textureId);
                    }
                    eglSurfaceHolder.onDrawNs(timestampNs);
                }
            });
        }
    }

    private class RecordRender extends AbstractChainRectFilter {
        public RecordRender() {
            super("attribute vec4 vPositionCoord;\n" + // 顶点坐标
                    "attribute vec2 vTextureCoord;\n" + //纹理坐标
                    "varying vec2 aTextureCoord;\n" +
                    "void main(){\n" +
                    //内置变量： 把坐标点赋值给gl_position 就Ok了。
                    "    gl_Position = vPositionCoord;\n" +
                    "    aTextureCoord = vTextureCoord;\n" +
                    "}",
                    "precision mediump float;\n" + // 数据精度
                    "varying vec2 aTextureCoord;\n" +
                    "uniform sampler2D  vTexture;\n" + // samplerExternalOES: 图片， 采样器
                    "void main(){\n" +
                    "    vec4 rgba = texture2D(vTexture, aTextureCoord);\n" +
                    "    gl_FragColor = rgba;\n" +
                    "}");
        }

        @Override
        protected void activeTexture(int textureId) {
            //激活指定纹理单元
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            //绑定纹理ID到纹理单元
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            //将激活的纹理单元传递到着色器里面
            GLES30.glUniform1i(vTextureHandler, 0);
            //配置边缘过渡参数
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        }

        @Override
        protected void beforeDraw(int textureId, FilterChain filterChain) {

        }

        @Override
        protected void afterDraw(int textureId, FilterChain filterChain) {

        }
    }
}
