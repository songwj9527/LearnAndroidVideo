package com.songwj.openvideo.ffmpeg;

public class NativeLoader {
    /*******************************************************
     * native部分
     *******************************************************/
    static {
        System.loadLibrary("native-lib");
    }

    static private class Singleton {
        static private NativeLoader instance = new NativeLoader();
    }

    static public NativeLoader getInstance() {
        return Singleton.instance;
    }

    /**
     * 可以将System.loadLibrary("native-lib");放在该方法中以此来手动添加native库
     */
    public void loadLibrary() {

    }
}
