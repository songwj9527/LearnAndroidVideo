//
// Created by fgrid on 2021/7/20.
//

#ifndef OPENVIDEO_PLAYER_H
#define OPENVIDEO_PLAYER_H

#include <jni.h>
#include <string>
#include <thread>

#include "../../utils/logger.h"
#include "../media_codes.h"
#include "player_state.h"

class Player {
private:
    const char *TAG = "Player";

    /**
     * 释放java层player对象的弱应用
     */
    void releaseJPlayer();


protected:
    /**
     * 变量区
     */
    // JNI Env
    JNIEnv      *jniEnv;
    // java层player对象的弱引用
    jobject     jplayer = NULL;
    // 视频流地址
    jobject     jsourceURL = NULL; // java层传入的视频流地址的弱引用
    const char  *sourceURL = NULL; // c层创建的视频流地址

    // java层surface对象的弱引用
    jobject     jsurface = NULL;
    // 播放状态
    volatile State state = IDLE;

    /**
     * 释放source url相关资源
     */
    void releaseSourceURL();

    /**
     * 异常或错误调用方法
     * @param errorCode
     * @param errorMsg
     */
    void onError(JNIEnv *env, int errorCode, const char *errorMsg);

    /**
     * 给Java层回调方法
     * @param event
     * @param what
     * @param args1
     * @param args2
     */
    void onPostEventToJava(JNIEnv *jniEnv, int event, int what, int args1, jobject args2);

public:
    Player(JNIEnv *jniEnv, jobject object);
    ~Player();

    /**
     * 设置视频源URL
     * @param url
     */
    void setDataSource(jstring url);

    /**
     * 准备播放（初始化解码器等）
     */
    virtual void prepareSync() = 0;

    /**
     * 设置视频渲染窗口
     */
    virtual void setSurface(jobject surface) = 0;

    /**
     * 开始播放
     */
    virtual void start() = 0;

    /**
     * 继续播放
     */
    virtual void resume() = 0;

    /**
     * 暂停播放
     */
    virtual void pause() = 0;

    /**
     * 停止播放
     */
    virtual void stop() = 0;

    /**
     * 重置播放器
     */
    virtual void reset() = 0;

    /**
     * 释放播放器
     */
    virtual void release() = 0;

    /**
     * 获取当前视频时长（单位：ms）
     * @return
     */
    virtual jlong getDuration() = 0;

    /**
     * 获取当前视频播放位置（单位：ms）
     * @return
     */
    virtual jlong getCurrentPosition() = 0;

    /**
     * 指定位置播放
     * @param position
     */
    virtual void seekTo(jlong position) = 0;

    /**
     * 获取最大音量
     * @return
     */
    virtual jint getMaxVolumeLevel() = 0;

    /**
     * 获取当前音量
     * @return
     */
    virtual jint getVolumeLevel() = 0;

    /**
     * 设置音量
     * @param volume
     */
    virtual void setVolumeLevel(jint volume) = 0;
};

#endif //OPENVIDEO_PLAYER_H
