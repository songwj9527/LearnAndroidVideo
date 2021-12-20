package com.songwj.openvideo.push;

public class VideoInfo {
    public int cameraId;
    public int width;
    public int height;
    public int rotation;
    public int bitrates = 3 * 1280 * 720;
    public int fps = 30; // 帧率默认为30fps
}
