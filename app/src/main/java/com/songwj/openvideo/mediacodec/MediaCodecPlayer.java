package com.songwj.openvideo.mediacodec;

import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import com.songwj.openvideo.mediacodec.decoder.AudioDecoder;
import com.songwj.openvideo.mediacodec.decoder.BaseDecoder;
import com.songwj.openvideo.mediacodec.decoder.IDecoderStateListener;
import com.songwj.openvideo.mediacodec.decoder.VideoDecoder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaCodecPlayer {
    private static final String TAG = "MediaCodecPlayer";

    public enum PlayMode {
        ONLY_VIDEO,
        ONLY_AUDIO,
        ALL
    }

    /**
     * 解码状态
     */
    public enum State {
        /** 初始状态（未进行任何操作）**/
        IDLE,
        /** 准备完成状态（初始化数据提取器、参数、渲染器等）**/
        PREPARED,
        /** 解码中 **/
        STARTED,
        /** 解码暂停 **/
        PAUSED,
        /** 解码器停止 **/
        STOPED,
        /** 正在快进 **/
        SEEKING,
        /** 解码完成 **/
        COMPLETED
    }

    private PlayMode playMode = PlayMode.ALL;
    private State state = State.IDLE;
    private OnPreparedListener onPreparedListener;
    private OnProgressListener onProgressListener;
    private OnSeekCompleteListener onSeekCompleteListener;
    private OnCompleteListener onCompleteListener;
    private OnInfoListener onInfoListener;
    private OnErrorListener onErrorListener;
    private OnFrameListener onFrameListener;

    private String filePath;
    private ExecutorService threadPool;
    private VideoDecoder videoDecoder = null;
    private Surface surface = null;
    private AudioDecoder audioDecoder = null;
    private boolean videoInited = false, videoOK = false, audioInited = false, audioOK = false;

    public MediaCodecPlayer() {
        threadPool = Executors.newFixedThreadPool(8);
    }

    synchronized public void setDataSource(String filePath) {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (onErrorListener != null) {
                onErrorListener.onError(this, 0, "" + filePath + ": 文件不存在");
            }
            return;
        }
        this.filePath = filePath;

        state = State.IDLE;
        preState = State.IDLE;
        videoInited = false;
        videoOK = false;
        audioInited = false;
        audioOK = false;
        if (videoDecoder != null) {
            videoDecoder.setStateListener(null);
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
            surface = null;
        }
        if (audioDecoder != null) {
            audioDecoder.setStateListener(null);
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }

        if (playMode != PlayMode.ONLY_AUDIO) {
            Log.e(">>>>>>", "PlayMode: " + playMode.name());
            videoDecoder = new VideoDecoder(filePath, surface);
            videoDecoder.setStateListener(new IDecoderStateListener() {

                @Override
                public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video prepared: ");
                    synchronized (MediaCodecPlayer.class) {
                        if (videoDecoder != null) {
                            videoInited = true;
                            videoOK = true;
                        }

                        if (audioDecoder != null) {
                            if (audioInited) {
                                if (!audioOK) {
//                                decodeJob.withoutSync();
//                                audioDecoder.withoutSync();
                                }
                                state = State.PREPARED;
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            }
                        }
                    }

                    if (videoDecoder != null) {
                        int width = videoDecoder.getVideoWidth();
                        int height = videoDecoder.getVideoHeight();
                        int rotation = videoDecoder.getVideoRotation();
                        Log.e(TAG, "width: " + width+", height: " + height +", rotation: " + rotation);

                        Info info = new Info();
                        info.setWidth(width);
                        info.setHeight(height);
                        info.setRotation(rotation);
                        if (onInfoListener != null) {
                            onInfoListener.onInfo(MediaCodecPlayer.this, info);
                        }
                    }
                }

                @Override
                public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video running: ");
                }

                @Override
                public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                    Log.e(TAG, "video progress: " + progress);
                    if (onProgressListener != null) {
                        onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                    }
                }

                @Override
                public void decoderPause(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video pause: ");
                }

                @Override
                public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                    Log.e(TAG, "video one frame: ");
                    if (onFrameListener != null) {
                        onFrameListener.onFrame(MediaCodecPlayer.this, decodeJob, frame);
                    }
                }

                @Override
                public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video seek completed: ");
                    if (preVideoState == State.STARTED) {
                        decodeJob.start();
                        preVideoState = State.IDLE;
                        if (preAudioState == State.IDLE) {
                            state = State.STARTED;
                            preState = State.IDLE;

                            if (onSeekCompleteListener != null) {
                                onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                            }
                        }
                    }
                    else if (preVideoState == State.PAUSED || preVideoState == State.COMPLETED) {
                        decodeJob.pause();
                        preVideoState = State.IDLE;
                        if (preAudioState == State.IDLE) {
                            state = State.PAUSED;
                            preState = State.IDLE;

                            if (onSeekCompleteListener != null) {
                                onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                            }
                        }
                    }
                    else if (onSeekCompleteListener != null) {
                        onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                    }
                }

                @Override
                public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video completed: ");
                    if (state != State.COMPLETED) {
                        state = State.COMPLETED;
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(MediaCodecPlayer.this);
                        }
                    }
                }

                @Override
                public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "video destroy: ");
                }

                @Override
                public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                    Log.e(TAG, "video error: " + msg);
                    synchronized (MediaCodecPlayer.class) {
                        if (videoDecoder != null) {
                            if (!videoInited) {
                                videoInited = true;
                                decodeJob.withoutSync();
                                if (audioDecoder != null) {
                                    audioDecoder.withoutSync();
                                    if (audioInited) {
                                        if (audioOK) {
                                            state = State.PREPARED;
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else {
                                            if (onErrorListener != null) {
                                                onErrorListener.onError(MediaCodecPlayer.this, 1, msg);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (onErrorListener != null) {
                                    onErrorListener.onError(MediaCodecPlayer.this, 3, msg);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (playMode != PlayMode.ONLY_VIDEO) {
            audioDecoder = new AudioDecoder(filePath);
            audioDecoder.setStateListener(new IDecoderStateListener() {

                @Override
                public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio repared: ");
                    synchronized (MediaCodecPlayer.class) {
                        if (audioDecoder != null) {
                            audioInited = true;
                            audioOK = true;
                        }

                        if (videoDecoder != null) {
                            if (videoInited) {
                                if (!videoOK) {
//                                videoDecoder.withoutSync();
//                                decodeJob.withoutSync();
                                }
                                state = State.PREPARED;
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            }
                        }
                    }
                }

                @Override
                public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio running: ");
                }

                @Override
                public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                    Log.e(TAG, "audio progress: " + progress);
                    if (onProgressListener != null) {
                        onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                    }
                }

                @Override
                public void decoderPause(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio pause: ");
                }

                @Override
                public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                    Log.e(TAG, "audio one frame: ");
                }

                @Override
                public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio seek completed: ");
                    if (preAudioState == State.STARTED) {
                        decodeJob.start();
                        preAudioState = State.IDLE;
                        if (preVideoState == State.IDLE) {
                            state = State.STARTED;
                            preState = State.IDLE;

                            if (onSeekCompleteListener != null) {
                                onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                            }
                        }
                    }
                    else if (preAudioState == State.PAUSED || preAudioState == State.COMPLETED) {
                        decodeJob.pause();
                        preAudioState = State.IDLE;
                        if (preVideoState == State.IDLE) {
                            state = State.PAUSED;
                            preState = State.IDLE;

                            if (onSeekCompleteListener != null) {
                                onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                            }
                        }
                    }
                    else if (onSeekCompleteListener != null) {
                        onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                    }
                }

                @Override
                public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio completed: ");
                    if (state != State.COMPLETED) {
                        state = State.COMPLETED;
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(MediaCodecPlayer.this);
                        }
                    }
                }

                @Override
                public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                    Log.e(TAG, "audio destroy: ");
                }

                @Override
                public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                    Log.e(TAG, "audio error: " + msg);
                    synchronized (MediaCodecPlayer.class) {
                        if (audioDecoder != null) {
                            if (!audioInited) {
                                audioInited = true;
                                decodeJob.withoutSync();
                                if (videoDecoder != null) {
                                    videoDecoder.withoutSync();
                                    if (videoInited) {
                                        if (videoOK) {
                                            state = State.PREPARED;
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else {
                                            if (onErrorListener != null) {
                                                onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (onErrorListener != null) {
                                    onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                                }
                            }
                        }
                    }
                }
            });
        }

        if (playMode != PlayMode.ONLY_AUDIO) {
            threadPool.execute(videoDecoder);
        }
        if (playMode != PlayMode.ONLY_VIDEO) {
            threadPool.execute(audioDecoder);
        }
    }

    synchronized public void setDataSource(String filePath, PlayMode playMode) {
        this.playMode = playMode;
        setDataSource(filePath);
    }

    synchronized public void setDisplay(Surface surface) {
        this.surface = surface;
        if (videoDecoder != null) {
            videoDecoder.setDisplay(this.surface);
        }
    }

    synchronized public void start() {
        boolean ok = false;
        if (videoDecoder != null && audioDecoder != null) {
            ok = videoInited && audioInited;
        }
        else if (videoDecoder != null) {
            ok = videoInited;
        }
        else if (audioDecoder != null) {
            ok = audioInited;
        }
        if (!ok) {
            Log.e(TAG, "to setVideoSource");
            return;
        }
        if (state != State.IDLE && state != State.STOPED) {
            state = State.STARTED;
            if (videoDecoder != null
                    && !videoDecoder.isIDLE()
                    && !videoDecoder.isStoped()) {
                videoDecoder.start();
            }
            if (audioDecoder != null
                    && !audioDecoder.isIDLE()
                    && !audioDecoder.isStoped()) {
                audioDecoder.start();
            }
        }
    }

    synchronized public void restart() {
        boolean ok = false;
        if (videoDecoder != null && audioDecoder != null) {
            ok = videoInited && audioInited;
        }
        else if (videoDecoder != null) {
            ok = videoInited;
        }
        else if (audioDecoder != null) {
            ok = audioInited;
        }
        if (!ok) {
            Log.e(TAG, "to setVideoSource");
            return;
        }
        if (state != State.IDLE && state != State.STOPED && state == State.COMPLETED) {
            state = State.STARTED;
            seekTo(0);
        }
    }

    private State preState = State.IDLE;
    private volatile State preVideoState = State.IDLE;
    private volatile State preAudioState = State.IDLE;
    synchronized public void seekTo(int position) {
        if (state != State.IDLE && state != State.STOPED) {
//            if (State.COMPLETED == state) {
//                reset();
//                state  = State.SEEKING;
//                if (audioDecoder != null) {
//                    threadPool.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            audioDecoder.seekTo(position);
//                        }
//                    });
//                }
//                if (videoDecoder != null) {
//                    threadPool.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            videoDecoder.seekTo(position);
//                        }
//                    });
//                }
//                return;
//            }
            preState = preVideoState = preAudioState = state;
            state  = State.SEEKING;
            if (audioDecoder != null
                    && !audioDecoder.isIDLE()
                    && !audioDecoder.isStoped()) {
                audioDecoder.pause();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        audioDecoder.seekTo(position);
                    }
                });
            }
            if (videoDecoder != null
                    && !videoDecoder.isIDLE()
                    && !videoDecoder.isStoped()) {
                videoDecoder.pause();
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        videoDecoder.seekTo(position);
                    }
                });
            }
        }
    }

    synchronized public void pause() {
        if (state != State.IDLE
                && state != State.STOPED
                && state != State.PAUSED) {
            if (audioDecoder != null
                    && !audioDecoder.isIDLE()
                    && !audioDecoder.isPaused()
                    && !audioDecoder.isStoped()) {
                audioDecoder.pause();
            }
            if (videoDecoder != null
                    && !videoDecoder.isIDLE()
                    && !videoDecoder.isPaused()
                    && !videoDecoder.isStoped()) {
                videoDecoder.pause();
            }
            state = State.PAUSED;
        }
    }

    synchronized public void resume() {
        if (state != State.IDLE
                && state != State.STOPED
                && state == State.PAUSED) {
            if (videoDecoder != null
                    && !videoDecoder.isIDLE()
                    && !videoDecoder.isStoped()
                    && videoDecoder.isPaused()) {
                videoDecoder.resume();
            }
            if (audioDecoder != null
                    && !audioDecoder.isIDLE()
                    && !audioDecoder.isStoped()
                    && audioDecoder.isPaused()) {
                audioDecoder.resume();
            }
            state = State.STARTED;
        }
    }

    synchronized public void stop() {
        if (videoDecoder != null) {
            videoDecoder.setStateListener(null);
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
        if (audioDecoder != null) {
            audioDecoder.setStateListener(null);
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
        state = State.STOPED;
    }

    synchronized public void reset() {
        state = State.IDLE;
        videoInited = false;
        videoOK = false;
        audioInited = false;
        audioOK = false;
        if (videoDecoder != null) {
            videoDecoder.setStateListener(null);
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
//            surface = null;
        }
        if (audioDecoder != null) {
            audioDecoder.setStateListener(null);
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
        if (preState == State.COMPLETED) {
            preState = State.PAUSED;
        } else if (preState == State.PREPARED
                || preState == State.STOPED
                || preState == State.SEEKING) {
            preState = State.IDLE;
        }

        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (onErrorListener != null) {
                onErrorListener.onError(this, 0, "" + filePath + ": 文件不存在");
            }
            return;
        }

        videoDecoder = new VideoDecoder(filePath, surface);
        videoDecoder.setStateListener(new IDecoderStateListener() {

            @Override
            public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video prepared: ");
                synchronized (MediaCodecPlayer.class) {
                    if (videoDecoder != null) {
                        videoInited = true;
                        videoOK = true;
                    }

                    if (audioDecoder != null) {
                        if (audioInited) {
                            if (!audioOK) {
//                                decodeJob.withoutSync();
//                                audioDecoder.withoutSync();
                            }
                            if (preState == State.IDLE) {
                                state = State.PREPARED;
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            } else if (preState == State.STARTED) {
                                preState = State.IDLE;
                                start();
                            } else {
                                state = preState;
                                preState = State.IDLE;
                            }
                        }
                    }
                }

                if (videoDecoder != null) {
                    int width = videoDecoder.getVideoWidth();
                    int height = videoDecoder.getVideoHeight();
                    int rotation = videoDecoder.getVideoRotation();
                    Log.e(TAG, "width: " + width+", height: " + height +", rotation: " + rotation);

                    Info info = new Info();
                    info.setWidth(width);
                    info.setHeight(height);
                    info.setRotation(rotation);
                    if (onInfoListener != null) {
                        onInfoListener.onInfo(MediaCodecPlayer.this, info);
                    }
                }
            }

            @Override
            public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video running: ");
            }

            @Override
            public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                Log.e(TAG, "video progress: " + progress);
                if (onProgressListener != null) {
                    onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                }
            }

            @Override
            public void decoderPause(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video pause: ");
            }

            @Override
            public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                Log.e(TAG, "video one frame: ");
            }

            @Override
            public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video seek completed: ");
                if (preVideoState == State.STARTED) {
                    decodeJob.start();
                    preVideoState = State.IDLE;
                    if (preAudioState == State.IDLE) {
                        state = State.STARTED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (preVideoState == State.PAUSED || preVideoState == State.COMPLETED) {
                    decodeJob.pause();
                    preVideoState = State.IDLE;
                    if (preAudioState == State.IDLE) {
                        state = State.PAUSED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (onSeekCompleteListener != null) {
                    onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                }
            }

            @Override
            public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video completed: ");
                if (state != State.COMPLETED) {
                    state = State.COMPLETED;
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(MediaCodecPlayer.this);
                    }
                }
            }

            @Override
            public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video destroy: ");
            }

            @Override
            public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                Log.e(TAG, "video error: " + msg);
                synchronized (MediaCodecPlayer.class) {
                    if (videoDecoder != null) {
                        if (!videoInited) {
                            videoInited = true;
                            decodeJob.withoutSync();
                            if (audioDecoder != null) {
                                audioDecoder.withoutSync();
                                if (audioInited) {
                                    if (audioOK) {
                                        if (preState == State.IDLE) {
                                            state = State.PREPARED;
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else if (preState == State.STARTED) {
                                            preState = State.IDLE;
                                            start();
                                        } else {
                                            state = preState;
                                            preState = State.IDLE;
                                        }
                                    } else {
                                        if (onErrorListener != null) {
                                            onErrorListener.onError(MediaCodecPlayer.this, 1, msg);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (onErrorListener != null) {
                                onErrorListener.onError(MediaCodecPlayer.this, 3, msg);
                            }
                        }
                    }
                }
            }
        });
        audioDecoder = new AudioDecoder(filePath);
        audioDecoder.setStateListener(new IDecoderStateListener() {

            @Override
            public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio repared: ");
                synchronized (MediaCodecPlayer.class) {
                    if (audioDecoder != null) {
                        audioInited = true;
                        audioOK = true;
                    }

                    if (videoDecoder != null) {
                        if (videoInited) {
                            if (!videoOK) {
//                                videoDecoder.withoutSync();
//                                decodeJob.withoutSync();
                            }
                            if (preState == State.IDLE) {
                                state = State.PREPARED;
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            } else if (preState == State.STARTED) {
                                preState = State.IDLE;
                                start();
                            } else {
                                state = preState;
                                preState = State.IDLE;
                            }
                        }
                    }
                }
            }

            @Override
            public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio running: ");
            }

            @Override
            public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                Log.e(TAG, "audio progress: " + progress);
                if (onProgressListener != null) {
                    onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                }
            }

            @Override
            public void decoderPause(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio pause: ");
            }

            @Override
            public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                Log.e(TAG, "audio one frame: ");
            }

            @Override
            public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio seek completed: ");
                if (preAudioState == State.STARTED) {
                    decodeJob.start();
                    preAudioState = State.IDLE;
                    if (preVideoState == State.IDLE) {
                        state = State.STARTED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (preAudioState == State.PAUSED || preAudioState == State.COMPLETED) {
                    decodeJob.pause();
                    preAudioState = State.IDLE;
                    if (preVideoState == State.IDLE) {
                        state = State.PAUSED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (onSeekCompleteListener != null) {
                    onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                }
            }

            @Override
            public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio completed: ");
                if (state != State.COMPLETED) {
                    state = State.COMPLETED;
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(MediaCodecPlayer.this);
                    }
                }
            }

            @Override
            public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio destroy: ");
            }

            @Override
            public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                Log.e(TAG, "audio error: " + msg);
                synchronized (MediaCodecPlayer.class) {
                    if (audioDecoder != null) {
                        if (!audioInited) {
                            audioInited = true;
                            decodeJob.withoutSync();
                            if (videoDecoder != null) {
                                videoDecoder.withoutSync();
                                if (videoInited) {
                                    if (videoOK) {
                                        if (preState == State.IDLE) {
                                            state = State.PREPARED;
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else if (preState == State.STARTED) {
                                            preState = State.IDLE;
                                            start();
                                        } else {
                                            state = preState;
                                            preState = State.IDLE;
                                        }
                                    } else {
                                        if (onErrorListener != null) {
                                            onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (onErrorListener != null) {
                                onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                            }
                        }
                    }
                }
            }
        });

        threadPool.execute(videoDecoder);
        threadPool.execute(audioDecoder);
    }

    synchronized public void returnToPosition(int position) {
        state = State.IDLE;
        videoInited = false;
        videoOK = false;
        audioInited = false;
        audioOK = false;
        if (videoDecoder != null) {
            videoDecoder.setStateListener(null);
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
//            surface = null;
        }
        if (audioDecoder != null) {
            audioDecoder.setStateListener(null);
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
        if (preState == State.COMPLETED) {
            preState = State.PAUSED;
        } else if (preState == State.PREPARED
                || preState == State.STOPED
                || preState == State.SEEKING) {
            preState = State.IDLE;
        }

        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            if (onErrorListener != null) {
                onErrorListener.onError(this, 0, "" + filePath + ": 文件不存在");
            }
            return;
        }

        videoDecoder = new VideoDecoder(filePath, surface);
        videoDecoder.setStateListener(new IDecoderStateListener() {

            @Override
            public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video prepared: ");
                synchronized (MediaCodecPlayer.class) {
                    if (videoDecoder != null) {
                        videoInited = true;
                        videoOK = true;
                    }

                    if (audioDecoder != null) {
                        if (audioInited) {
                            if (!audioOK) {
//                                decodeJob.withoutSync();
//                                audioDecoder.withoutSync();
                            }
                            if (preState == State.IDLE) {
                                state = State.PREPARED;
                                if (position > 0) {
                                    seekTo(position);
                                }
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            } else if (preState == State.STARTED) {
                                preState = State.IDLE;
                                if (position > 0) {
                                    seekTo(position);
                                } else {
                                    start();
                                }
                            } else {
                                state = preState;
                                preState = State.IDLE;
                            }
                        }
                    }
                }

                if (videoDecoder != null) {
                    int width = videoDecoder.getVideoWidth();
                    int height = videoDecoder.getVideoHeight();
                    int rotation = videoDecoder.getVideoRotation();
                    Log.e(TAG, "width: " + width+", height: " + height +", rotation: " + rotation);

                    Info info = new Info();
                    info.setWidth(width);
                    info.setHeight(height);
                    info.setRotation(rotation);
                    if (onInfoListener != null) {
                        onInfoListener.onInfo(MediaCodecPlayer.this, info);
                    }
                }
            }

            @Override
            public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video running: ");
            }

            @Override
            public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                Log.e(TAG, "video progress: " + progress);
                if (onProgressListener != null) {
                    onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                }
            }

            @Override
            public void decoderPause(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video pause: ");
            }

            @Override
            public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                Log.e(TAG, "video one frame: ");
                if (onFrameListener != null) {
                    onFrameListener.onFrame(MediaCodecPlayer.this, decodeJob, frame);
                }
            }

            @Override
            public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video seek completed: ");
                if (preVideoState == State.STARTED) {
                    decodeJob.start();
                    preVideoState = State.IDLE;
                    if (preAudioState == State.IDLE) {
                        state = State.STARTED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (preAudioState == State.PAUSED || preAudioState == State.COMPLETED) {
                    decodeJob.pause();
                    preVideoState = State.IDLE;
                    if (preAudioState == State.IDLE) {
                        state = State.PAUSED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (onSeekCompleteListener != null) {
                    onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                }
            }

            @Override
            public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video completed: ");
                if (state != State.COMPLETED) {
                    state = State.COMPLETED;
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(MediaCodecPlayer.this);
                    }
                }
            }

            @Override
            public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "video destroy: ");
            }

            @Override
            public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                Log.e(TAG, "video error: " + msg);
                synchronized (MediaCodecPlayer.class) {
                    if (videoDecoder != null) {
                        if (!videoInited) {
                            videoInited = true;
                            decodeJob.withoutSync();
                            if (audioDecoder != null) {
                                audioDecoder.withoutSync();
                                if (audioInited) {
                                    if (audioOK) {
                                        if (preState == State.IDLE) {
                                            state = State.PREPARED;
                                            if (position > 0) {
                                                seekTo(position);
                                            }
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else if (preState == State.STARTED) {
                                            preState = State.IDLE;
                                            if (position > 0) {
                                                seekTo(position);
                                            } else {
                                                start();
                                            }
                                        } else {
                                            state = preState;
                                            preState = State.IDLE;
                                        }
                                    } else {
                                        if (onErrorListener != null) {
                                            onErrorListener.onError(MediaCodecPlayer.this, 1, msg);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (onErrorListener != null) {
                                onErrorListener.onError(MediaCodecPlayer.this, 3, msg);
                            }
                        }
                    }
                }
            }
        });
        audioDecoder = new AudioDecoder(filePath);
        audioDecoder.setStateListener(new IDecoderStateListener() {

            @Override
            public void decoderPrepared(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio repared: ");
                synchronized (MediaCodecPlayer.class) {
                    if (audioDecoder != null) {
                        audioInited = true;
                        audioOK = true;
                    }

                    if (videoDecoder != null) {
                        if (videoInited) {
                            if (!videoOK) {
//                                videoDecoder.withoutSync();
//                                decodeJob.withoutSync();
                            }
                            if (preState == State.IDLE) {
                                state = State.PREPARED;
                                if (position > 0) {
                                    seekTo(position);
                                }
                                if (onPreparedListener != null) {
                                    onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                }
                            } else if (preState == State.STARTED) {
                                preState = State.IDLE;
                                if (position > 0) {
                                    seekTo(position);
                                } else {
                                    start();
                                }
                            } else {
                                state = preState;
                                preState = State.IDLE;
                            }
                        }
                    }
                }
            }

            @Override
            public void decoderRunning(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio running: ");
            }

            @Override
            public void decoderProgress(@Nullable BaseDecoder decodeJob, long progress) {
                Log.e(TAG, "audio progress: " + progress);
                if (onProgressListener != null) {
                    onProgressListener.onProgress(MediaCodecPlayer.this, progress > 100 ? 100 : (progress < 0 ? 0 : progress));
                }
            }

            @Override
            public void decoderPause(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio pause: ");
            }

            @Override
            public void decodeOneFrame(@Nullable BaseDecoder decodeJob, @NotNull Frame frame) {
                Log.e(TAG, "audio one frame: ");
            }

            @Override
            public void decoderSeekCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio seek completed: ");
                if (preAudioState == State.STARTED) {
                    decodeJob.start();
                    preAudioState = State.IDLE;
                    if (preVideoState == State.IDLE) {
                        state = State.STARTED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (preAudioState == State.PAUSED || preAudioState == State.COMPLETED) {
                    decodeJob.pause();
                    preAudioState = State.IDLE;
                    if (preVideoState == State.IDLE) {
                        state = State.PAUSED;
                        preState = State.IDLE;

                        if (onSeekCompleteListener != null) {
                            onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                        }
                    }
                }
                else if (onSeekCompleteListener != null) {
                    onSeekCompleteListener.onSeekComplete(MediaCodecPlayer.this);
                }
            }

            @Override
            public void decoderCompleted(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio completed: ");
                if (state != State.COMPLETED) {
                    state = State.COMPLETED;
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(MediaCodecPlayer.this);
                    }
                }
            }

            @Override
            public void decoderDestroy(@Nullable BaseDecoder decodeJob) {
                Log.e(TAG, "audio destroy: ");
            }

            @Override
            public void decoderError(@Nullable BaseDecoder decodeJob, @NotNull String msg) {
                Log.e(TAG, "audio error: " + msg);
                synchronized (MediaCodecPlayer.class) {
                    if (audioDecoder != null) {
                        if (!audioInited) {
                            audioInited = true;
                            decodeJob.withoutSync();
                            if (videoDecoder != null) {
                                videoDecoder.withoutSync();
                                if (videoInited) {
                                    if (videoOK) {
                                        if (preState == State.IDLE) {
                                            state = State.PREPARED;
                                            if (position > 0) {
                                                seekTo(position);
                                            }
                                            if (onPreparedListener != null) {
                                                onPreparedListener.onPrepared(MediaCodecPlayer.this);
                                            }
                                        } else if (preState == State.STARTED) {
                                            preState = State.IDLE;
                                            if (position > 0) {
                                                seekTo(position);
                                            } else {
                                                start();
                                            }
                                        } else {
                                            state = preState;
                                            preState = State.IDLE;
                                        }
                                    } else {
                                        if (onErrorListener != null) {
                                            onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                                        }
                                    }
                                }
                            }
                        } else {
                            if (onErrorListener != null) {
                                onErrorListener.onError(MediaCodecPlayer.this, 2, msg);
                            }
                        }
                    }
                }
            }
        });

        threadPool.execute(videoDecoder);
        threadPool.execute(audioDecoder);
    }

    synchronized public void release() {
        state = State.IDLE;
        if (videoDecoder != null) {
            videoDecoder.setStateListener(null);
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
        if (audioDecoder != null) {
            audioDecoder.setStateListener(null);
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
    }

    synchronized public State getState() {
        return state;
    }

    synchronized public long getDuration() {
        if (videoDecoder != null) {
            return videoDecoder.getDuration() / 1000L;
        }
        if (audioDecoder != null) {
            return audioDecoder.getDuration() / 1000L;
        }
        return 0L;
    }

    synchronized public long getCurrentTimestamp() {
        if (audioDecoder != null) {
            return audioDecoder.getCurrentTimestamp() / 1000L;
        }
        if (videoDecoder != null) {
            return videoDecoder.getCurrentTimestamp() / 1000L;
        }
        return 0L;
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener onSeekCompleteListener) {
        this.onSeekCompleteListener = onSeekCompleteListener;
    }

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        this.onInfoListener = onInfoListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    public void setOnFrameListener(OnFrameListener onFrameListener) {
        this.onFrameListener = onFrameListener;
    }

    public interface OnPreparedListener {
        public void onPrepared(MediaCodecPlayer mediaCodecPlayer);
    }

    public interface OnProgressListener {
        public void onProgress(MediaCodecPlayer mediaCodecPlayer, long progress);
    }

    public interface OnSeekCompleteListener {
        public void onSeekComplete(MediaCodecPlayer mediaCodecPlayer);
    }

    public interface OnCompleteListener {
        public void onComplete(MediaCodecPlayer mediaCodecPlayer);
    }

    public interface OnInfoListener {
        public void onInfo(MediaCodecPlayer mediaCodecPlayer, Info info);
    }

    public interface OnErrorListener {
        public void onError(MediaCodecPlayer mediaCodecPlayer, int what, String msg);
    }

    public interface OnFrameListener {
        public void onFrame(MediaCodecPlayer mediaCodecPlayer, BaseDecoder baseDecoder, Frame frame);
    }

    public static class Info {
        private int width;
        private int height;
        private int rotation;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getRotation() {
            return rotation;
        }

        public void setRotation(int rotation) {
            this.rotation = rotation;
        }
    }
}
