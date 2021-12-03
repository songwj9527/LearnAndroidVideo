package com.songwj.openvideo.mediarecord;

import android.opengl.EGLContext;
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
import com.songwj.openvideo.opengl.filter.base.AbstractRectFilter;

public class Camera2Recorder implements MMuxer.IMuxerStateListener, AudioCapture.OnAudioCaptureListener {
    private String filePath = "";
    private int videoWidth = 0;
    private int videoHeight = 0;

    private MMuxer mmuxer = null;

    private VideoEncoder videoEncoder = null;
    private boolean isVideoEncodeManually = false;
    private boolean isVideoEncoderPrepared = false;
    private HandlerThread handlerThread = null;
    private Handler handler = null;
    private EGLContext eglContext = null;
    private EGLSurfaceHolder eglSurfaceHolder = null;
    private static final int EGL_RECORDABLE_ANDROID_FLAG = 0x3142;
    private RecordRender recordRender = null;

    private AudioEncoder audioEncoder = null;
    private boolean isAudioEncoderPrepared = false;
    private AudioCapture audioCapture = null;

    private boolean isPrepared = false;
    private volatile boolean isStarted = false;

    public Camera2Recorder(String filePath, int videoWidth, int videoHeight, boolean isVideoEncodeManually) {
        this.filePath = filePath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.isVideoEncodeManually = isVideoEncodeManually;
    }

    public Surface getSurface() {
        if (videoEncoder == null) {
            return null;
        }
        return videoEncoder.getSurface();
    }

    public void setEGLContext(EGLContext eglContext) {
        this.eglContext = eglContext;
    }

    synchronized public boolean start() {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        if (isStarted) {
            return true;
        }
        Log.e("MediaRecorder", "filePath: " + filePath + "\n"
                + "videoWidth: " + videoWidth + ", videoHeight: " + videoHeight);
        try {
            mmuxer = new MMuxer(filePath);
            mmuxer.setStateListener(this);
            audioEncoder = new AudioEncoder(mmuxer);
            audioEncoder.start();
            audioCapture = new AudioCapture();
            audioCapture.setOnAudioCaptureListener(this);
            audioCapture.start();
            videoEncoder = new VideoEncoder(mmuxer, videoWidth, videoHeight, isVideoEncodeManually);
            videoEncoder.start();
            isStarted = true;
            frameIndex = 0L;

            if (!isVideoEncodeManually) {
                handlerThread = new HandlerThread("codec-gl");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        eglSurfaceHolder = new EGLSurfaceHolder();
                        eglSurfaceHolder.init(eglContext, EGL_RECORDABLE_ANDROID_FLAG);
                        eglSurfaceHolder.createEGLSurface(videoEncoder.getSurface(), videoWidth, videoHeight);
                        recordRender = new RecordRender();
                        recordRender.onSurfaceCreated();
                        recordRender.onSurfaceChanged(videoWidth, videoHeight);
                    }
                });
            }
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
            if (!isVideoEncodeManually && handler!= null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recordRender.onSurfaceDestroy();
                        recordRender = null;
                        eglContext = null;
                        eglSurfaceHolder.destroyEGLSurface();
                        eglSurfaceHolder.release();
                        eglSurfaceHolder = null;
                        handler.getLooper().quitSafely();
                        handlerThread = null;
                        handler = null;
                    }
                });
            }
        }
        return isStarted;
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
            if (!isVideoEncodeManually) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recordRender.onSurfaceDestroy();
                        recordRender = null;
                        eglContext = null;
                        eglSurfaceHolder.destroyEGLSurface();
                        eglSurfaceHolder.release();
                        eglSurfaceHolder = null;
                        handler.getLooper().quitSafely();
                        handlerThread = null;
                        handler = null;
                    }
                });
            }
            isStarted = false;
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
            if (videoEncoder != null) {
                videoEncoder.stopEncode();
                videoEncoder = null;
            }
            if (audioCapture != null) {
                audioCapture.setOnAudioCaptureListener(null);
                audioCapture.stop();
                audioCapture = null;
            }
            if (audioEncoder != null) {
                audioEncoder.stopEncode();
                audioEncoder = null;
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
            Log.i("MediaRecorder", "onAudioFrameUpdate()");
            if (audioEncoder != null) {
                audioEncoder.dequeueFrame(data, dataSize);
            }
        }
    }

    public void onVideoFrameUpdate(byte[] data) {
//        if (isStarted && isPrepared) {
        if (isStarted) {
            Log.i("MediaRecorder", "onVideoFrameUpdate()");
            if (videoEncoder != null) {
                videoEncoder.dequeueFrame(data, computePresentationTime(frameIndex++, 30));
            }
        }
    }

    private long frameIndex = 0L;
    private long computePresentationTime(long frameIndex, int frameRate) {
        //帧率是frameRate  132是偏移量 frameIndex单位是微秒(us)
        return 132 + frameIndex * 1000000 / frameRate;
    }

    public void onDrawFrame(final int textureId, final long timestampNs){
//        if (isStarted && isPrepared && !isVideoEncodeManually) {
        if (isStarted && !isVideoEncodeManually) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recordRender.onDrawFrame(textureId);
                    eglSurfaceHolder.onDrawNs(timestampNs);
                }
            });
        }
    }

    class RecordRender extends AbstractRectFilter {
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
                    //  texture2D: vTexture采样器，采样  aCoord 这个像素点的RGBA值
                    "    vec4 rgba = texture2D(vTexture, aTextureCoord);\n" + // rgba
                    //    gl_FragColor = vec4(1.-rgba.r,1.-rgba.g,1.-rgba.b,rgba.a);
                    "    gl_FragColor = rgba;\n" +
                    //    float c = (rgba.r*0.3+ rgba.g*0.59+rgba.b*0.11) /3.0;
                    //    gl_FragColor = vec4(c, c, c, 1.0);
                    "}");
        }

        @Override
        protected void beforeDraw() {

        }

        @Override
        protected void afterDraw() {

        }
    }
}
