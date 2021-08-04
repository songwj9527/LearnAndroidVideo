package com.songwj.openvideo.ffmpeg;

public class NativeLoader {
    /*******************************************************
     * native部分
     *******************************************************/
    static{
        System.loadLibrary("native-lib");
    }

    static private class Singleton {
        static private NativeLoader instance = new NativeLoader();
    }

    static public NativeLoader getInstance() {
        return Singleton.instance;
    }

    public void loadLibrary() {

    }
}
