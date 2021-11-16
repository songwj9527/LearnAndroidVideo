package com.songwj.openvideo.camera;

class Camera2Manager {
    private Camera2Manager() {

    }
    static private class Singleton {
        static private Camera2Manager instance = new Camera2Manager();
    }

    public static Camera2Manager getInstance() {
        return Singleton.instance;
    }
}
