package com.songwj.openvideo.ffmpeg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.songwj.openvideo.MyApplication;
import com.songwj.openvideo.cache.CommonUtil;
import com.songwj.openvideo.cache.HttpProxyCacheManager;
import com.songwj.openvideo.cache.StorageUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigInteger;

public class NativePlayer implements CacheListener {
    public enum PlayerType {
        DEFAULT_PLAYER,
        OPENGL_PLAYER,
        CODEC_PLAYER,
    }

    public native String ffmpegInfo();
    public native void videoTypeTransform(String srcFilePath, String destFilePath);

    private native int nativeCreatePlayer();
    private native int nativeCreateGLPlayer();
    private native int nativeCreateCodecPlayer();
    private native void nativeSetDataSource(int nativePlayer, String url);
    private native void nativePrepareSync(int nativePlayer);
    private native void nativeSetSurface(int nativePlayer, Surface surface);
    private native void nativeStart(int nativePlayer);
    private native void nativePause(int nativePlayer);
    private native void nativeResume(int nativePlayer);
    private native void nativeStop(int nativePlayer);
    private native void nativeReset(int nativePlayer);
    private native void nativeRelease(int nativePlayer);
    private native long nativeGetDuration(int nativePlayer);
    private native long nativeGetCurrentTimestamp(int nativePlayer);
    private native void nativeSeekTo(int nativePlayer, long position);
    private native int nativeGetMaxVolumeLevel(int nativePlayer);
    private native int nativeGetVolumeLevel(int nativePlayer);
    private native void nativeSetVolumeLevel(int nativePlayer, int volume);

    /*******************************************************
     * java部分
     *******************************************************/
    public NativePlayer(PlayerType type) {
        playerType = type;
        switch (type) {
            case OPENGL_PLAYER:
                nativePlayer = nativeCreateGLPlayer();
                break;
            case CODEC_PLAYER:
                nativePlayer = nativeCreateCodecPlayer();
                break;
            default:
                nativePlayer = nativeCreatePlayer();
                break;
        }
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            eventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            eventHandler = new EventHandler(this, looper);
        } else {
            eventHandler = null;
        }

        proxy = HttpProxyCacheManager.getInstance().getProxy(MyApplication.Companion.getInstance());
        cacheFile = new File(HttpProxyCacheManager.getInstance().getCacheDirectory(MyApplication.Companion.getInstance()));
    }

    private static final String TAG = "NativePlayer";
    private PlayerType playerType;
    private Integer nativePlayer;
    private EventHandler eventHandler;
    private State state = State.IDLE;
    private OnPreparedListener onPreparedListener;
    private OnInfoListener onInfoListener;
    private OnSeekCompletedListener onSeekCompletedListener;
    private OnBufferingUpdateListener onBufferingUpdateListener;
    private OnCompletedListener onCompletedListener;
    private OnErrorListener onErrorListener;

    private HttpProxyCacheServer proxy = null; // 缓存代理
    private File cacheFile = null;
    private boolean isCacheFile = false; // 是否是缓存的文件
    private String currentUrl; // 播放地址
    private String originUrl; //原来的url
    private int currentBufferPoint = 0; // 当前缓冲百分比

    /**
     * 设置视频源
     * @param url
     */
    public void setDataSource(String url) {
        if (nativePlayer == null) {
            synchronized(NativePlayer.class) {
                if (nativePlayer == null) {
//                    nativePlayer = nativeCreatePlayer(WeakReference<NativePlayer>(this))
                    nativePlayer = nativeCreatePlayer();
                }
            }
        }
        if (nativePlayer == null) {
            if (onErrorListener != null) {
                onErrorListener.onError(this, -1, -1, "Create native player failed.");
            }
            return;
        }
        if (TextUtils.isEmpty(url)) {
            if (onErrorListener != null) {
                onErrorListener.onError(this, -1, -1, "URL is empty.");
            }
            return;
        }
        if (!TextUtils.isEmpty(currentUrl) && !currentUrl.contains("127.0.0.1")) {
            if (cacheFile != null
                    || !cacheFile.getAbsolutePath().equals(HttpProxyCacheManager.getInstance().getCacheDirectory(MyApplication.Companion.getInstance()))) {
                if (proxy != null) {
                    proxy.shutdown();
                }
                proxy = HttpProxyCacheManager.getInstance().getProxy(MyApplication.Companion.getInstance());
            }
            if (proxy != null) {
                proxy.unregisterCacheListener(this, currentUrl);
            }
        }
        originUrl = url;
        boolean cachePlay = url.startsWith("http");
        // 初始化缓存
        if (cachePlay && !url.contains("127.0.0.1")) {
            if (proxy == null) {
                proxy = HttpProxyCacheManager.getInstance().getProxy(MyApplication.Companion.getInstance());
            }
            //此处转换了url，然后再赋值给mUrl。
            url = proxy.getProxyUrl(url).replace("file://", "");
            isCacheFile = (!url.startsWith("http"));
            //注册上缓冲监听
            if (!isCacheFile) {
                proxy.registerCacheListener(this, originUrl);
            }
        }
        currentUrl = url;
        nativeSetDataSource(nativePlayer, currentUrl);
        state = State.IDLE;
    }

    /**
     * 缓存监听
     * @param cacheFile
     * @param url
     * @param percentsAvailable
     */
    @Override
    public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
        Log.d(TAG, "onCacheAvailable(): "+percentsAvailable);
        currentBufferPoint = percentsAvailable;
    }

    /**
     * 播放错误的时候，删除缓存文件
     */
    private void deleteCacheFileWhenError() {
        clearCurrentCache();
        Log.e(TAG, "Link Or mCache Error, Please Try Again" + currentUrl);
        currentUrl = originUrl;
    }

    /**
     * 清除当前缓存
     */
    public void clearCurrentCache() {
        if (isCacheFile) {
            //是否为缓存文件
            Log.e(TAG, " mCacheFile Local Error " + currentUrl);
            //可能是因为缓存文件除了问题
            CommonUtil.deleteFile(currentUrl.replace("file://", ""));
            currentUrl = originUrl;
        } else if (!TextUtils.isEmpty(currentUrl) && currentUrl.contains("127.0.0.1")) {
            //是否为缓存了未完成的文件
            Md5FileNameGenerator md5FileNameGenerator = new Md5FileNameGenerator();
            String name = md5FileNameGenerator.generate(originUrl);
            if (cacheFile != null) {
                String path = cacheFile.getAbsolutePath() + File.separator + name + ".download";
                CommonUtil.deleteFile(path);
            } else {
                String path = StorageUtils.getIndividualCacheDirectory(MyApplication.Companion.getInstance()).getAbsolutePath()
                        + File.separator + name + ".download";
                CommonUtil.deleteFile(path);
            }
        }

    }

    /**
     * 准备播放器
     */
    public void prepareSync() {
        if (nativePlayer == null) {
            return;
        }
        nativePrepareSync(nativePlayer);
    }

    public void setSurface(Surface surface) {
        if (nativePlayer == null) {
            return;
        }
        nativeSetSurface(nativePlayer, surface);
    }

    /**
     * 开始播放
     */
    public void start() {
        if (nativePlayer == null) {
            return;
        }
        if (state != State.RUNNING && state != State.STOPED) {
            state = State.RUNNING;
            nativeStart(nativePlayer);
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (nativePlayer == null) {
            return;
        }
        if (state == State.RUNNING) {
            state = State.PAUSED;
            nativePause(nativePlayer);
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        Log.e("NativePlayer", "resume()");
        if (nativePlayer == null) {
            return;
        }
        if (state == State.PAUSED) {
            state = State.RUNNING;
            nativeResume(nativePlayer);
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (nativePlayer == null) {
            return;
        }
        if (state != State.IDLE && state != State.STOPED) {
            state = State.STOPED;
            nativeStop(nativePlayer);
        }
    }

    /**
     * 重置播放器
     */
    public void reset() {
        if (nativePlayer == null) {
            return;
        }
        if (state != State.IDLE) {
            state = State.IDLE;
            nativeReset(nativePlayer);
//            resetListener();
        }
        currentBufferPoint = 0;
    }

    private void resetListener() {
        onPreparedListener = null;
        onInfoListener = null;
        onSeekCompletedListener = null;
        onBufferingUpdateListener = null;
        onCompletedListener = null;
        onErrorListener = null;
    }

    /**
     * 释放播放器
     */
    public void release() {
        if (nativePlayer == null) {
            return;
        }
        state = State.IDLE;
        nativeRelease(nativePlayer);
        resetListener();
        nativePlayer = null;
        currentBufferPoint = 0;
    }

    /**
     * 获取播放状态
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * 获取视频时长
     */
    public long getDuration() {
        if (nativePlayer == null) {
            return 0L;
        }
        return nativeGetDuration(nativePlayer);
    }

    /**
     * 获取视频当前播放位置
     */
    public long getCurrentTimestamp() {
        if (nativePlayer == null) {
            return 0L;
        }
        return nativeGetCurrentTimestamp(nativePlayer);
    }

    /**
     * 指定位置播放
     * @param position
     */
    public void seekTo(long position) {
        if (nativePlayer == null) {
            return;
        }
        state = State.PAUSED;
        eventHandler.post(new Runnable() {
            @Override
            public void run() {
                nativeSeekTo(nativePlayer, position);
            }
        });
    }

    /**
     * 获取最大音量
     * @return
     */
    public int getMaxVolumeLevel() {
        if (nativePlayer == null) {
            return 0;
        }
        return nativeGetMaxVolumeLevel(nativePlayer);
    }

    /**
     * 获取最大音量
     * @return
     */
    public int getVolumeLevel() {
        if (nativePlayer == null) {
            return 0;
        }
        return nativeGetVolumeLevel(nativePlayer);
    }

    /**
     * 设置音量
     * @param volume
     */
    public void setVolumeLevel(int volume) {
        if (nativePlayer == null) {
            return;
        }
        nativeSetVolumeLevel(nativePlayer, volume);
    }

    /**
     * native层调用此方法返回相应状态
     */
    private static void postEventFromNative(Object weakThis, int what, int arg1, int arg2, Object obj) {
        if (weakThis != null) {
            NativePlayer nativePlayer = null;
            if (weakThis instanceof WeakReference) {
                nativePlayer = ((WeakReference<NativePlayer>) weakThis).get();
            } else if (weakThis instanceof NativePlayer) {
                nativePlayer = (NativePlayer) weakThis;
            }
            if (nativePlayer != null && nativePlayer.eventHandler != null) {
                nativePlayer.eventHandler.sendMessage(nativePlayer.eventHandler.obtainMessage(what, arg1, arg2, obj));
            }
        }
    }

    /**
     * 处理native返回的事件
     */
    private static final int NATIVE_CALLBACK_EVENT_NONE                 = 0;
    private static final int NATIVE_CALLBACK_EVENT_ERROR                = 1;
    private static final int NATIVE_CALLBACK_EVENT_PREPARED             = 2;
    private static final int NATIVE_CALLBACK_EVENT_INFO                 = 3;
    private static final int NATIVE_CALLBACK_VIDEO_SIZE_CHANGED         = 4;
    private static final int NATIVE_CALLBACK_EVENT_BUFFERING_UPDATE     = 5;
    private static final int NATIVE_CALLBACK_EVENT_COMPLETED            = 6;
    private static final int NATIVE_CALLBACK_EVENT_SEEK_COMPLETED       = 7;
    private class EventHandler extends Handler {
        private final WeakReference<NativePlayer> weakNativePlayer;
        public EventHandler(NativePlayer nativePlayer, Looper looper) {
            super(looper);
            weakNativePlayer = new WeakReference(nativePlayer);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                int code = msg.what;
                switch (code) {
                    case NATIVE_CALLBACK_EVENT_PREPARED:
                        NativePlayer player1 = weakNativePlayer.get();
                        player1.state = State.PREPARED;
                        if (player1 != null && player1.onPreparedListener != null) {
                            player1.onPreparedListener.onPrepared(player1);
                        }
                        break;
                    case NATIVE_CALLBACK_EVENT_INFO:
                        NativePlayer player2 = weakNativePlayer.get();
                        if (player2 != null && player2.onInfoListener != null && msg.obj != null && msg.obj instanceof String) {
                            String value = (String) (msg.obj);
                            BigInteger temp = new BigInteger(value);
                            player2.onInfoListener.onInfo(player2, msg.arg1, msg.arg2, temp.intValue());
                        }
                        break;
                    case NATIVE_CALLBACK_EVENT_BUFFERING_UPDATE:
                        NativePlayer player4 = weakNativePlayer.get();
                        if (player4 != null && player4.onBufferingUpdateListener != null) {
                            player4.onBufferingUpdateListener.onBufferingUpdate(player4, msg.arg1);
                        }
                        break;
                    case NATIVE_CALLBACK_EVENT_COMPLETED:
                        NativePlayer player6 = weakNativePlayer.get();
                        player6.state = State.COMPLETED;
                        if (player6 != null && player6.onCompletedListener != null) {
                            player6.onCompletedListener.onCompleted(player6);
                        }
                        break;
                    case NATIVE_CALLBACK_EVENT_SEEK_COMPLETED:
                        NativePlayer player7 = weakNativePlayer.get();
                        if (player7 != null && player7.onSeekCompletedListener != null) {
                            player7.onSeekCompletedListener.onSeekCompleted(player7);
                        }
                        break;
                    case NATIVE_CALLBACK_EVENT_ERROR:
                        NativePlayer player8 = weakNativePlayer.get();
                        if (player8 != null && player8.onErrorListener != null) {
                            player8.deleteCacheFileWhenError();
                            String errorMsg = "";
                            if (msg.obj != null) {
                                if (msg.obj instanceof String) {
                                    errorMsg = (String) msg.obj;
                                } else {
                                    errorMsg = msg.obj.toString();
                                }
                            }
                            player8.onErrorListener.onError(player8, msg.arg1, msg.arg2, errorMsg);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        this.onInfoListener = onInfoListener;
    }

    public void setOnSeekCompletedListener(OnSeekCompletedListener onSeekCompletedListener) {
        this.onSeekCompletedListener = onSeekCompletedListener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener onBufferingUpdateListener) {
        this.onBufferingUpdateListener = onBufferingUpdateListener;
    }

    public void setOnCompletedListener(OnCompletedListener onCompletedListener) {
        this.onCompletedListener = onCompletedListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    /**
     * 播放状态
     */
    public enum State {
        /** 初始状态（未进行任何操作） */
        IDLE,
        /** 准备完成状态（初始化数据提取器、参数、渲染器等） */
        PREPARED,
        /** 播放中  */
        RUNNING,
        /** 播放暂停  */
        PAUSED,
        /** 播放停止  */
        STOPED,
        /** 正在快进  */
        SEEKING,
        /** 播放结束  */
        COMPLETED
    }

    public interface OnPreparedListener {
        public void onPrepared(NativePlayer player);
    }

    public interface OnInfoListener {
        public void onInfo(NativePlayer player, int videoWidth, int videoHeight, int videoRotation);
    }

    public interface OnSeekCompletedListener {
        public void onSeekCompleted(NativePlayer player);
    }

    public interface OnBufferingUpdateListener {
        public void onBufferingUpdate(NativePlayer player, int progress);
    }

    public interface OnCompletedListener {
        public void onCompleted(NativePlayer player);
    }

    public interface OnErrorListener {
        public void onError(NativePlayer player, int what, int extra, String msg);
    }
}