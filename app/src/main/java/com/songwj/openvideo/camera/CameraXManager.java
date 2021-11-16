package com.songwj.openvideo.camera;

class CameraXManager {
    private CameraXManager() {

    }
    static private class Singleton {
        static private CameraXManager instance = new CameraXManager();
    }

    public static CameraXManager getInstance() {
        return Singleton.instance;
    }
}
