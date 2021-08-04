package com.songwj.openvideo.ffmpeg;

public class NativeRepack {
    private native int nativeCreateRepack(String srcPath, String desPath);
    private native void nativeStartRepack(int repack);
    private native void nativeReleaseRepack(int repack);

    private Integer nativeRepack = null;

    public NativeRepack() {

    }

    public void CreateRepack(String srcPath, String desPath) {
        nativeRepack = nativeCreateRepack(srcPath, desPath);
    }

    public void startRepack() {
        if (nativeRepack != null) {
            nativeStartRepack(nativeRepack);
        }
    }

    public void releaseRepack() {
        if (nativeRepack != null) {
            nativeReleaseRepack(nativeRepack);
        }
        nativeRepack = null;
    }
}
