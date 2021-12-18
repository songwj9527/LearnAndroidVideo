//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H
#define OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H

#include <jni.h>

class IFFmpegRenderCallback {
public:
    /**
     * 给渲染器调用，用于告知外部渲染器准备完成，可准备渲染了。
     * @param decoder
     */
    virtual void OnRenderPrepared(JNIEnv *jniEnv, int render) = 0;

    /**
     * 给渲染器调用，用于告知外部渲染器已渲染结束。
     * @param decoder
     */
    virtual void OnRenderCompleted(JNIEnv *jniEnv, int render) = 0;

    /**
     * 给渲染器调用，用于告知外部哪个渲染器出了问题，出了什么问题；
     * 并为整个外部是否调用、什么时候调用onError方法将异常信息返回给java层。
     * @param decoder
     * @param code
     * @param msg
     */
    virtual void OnRenderError(JNIEnv *jniEnv, int render, int code, const char *msg) = 0;

    /**
     * 更新同步时钟
     * @param syncClock
     */
    virtual void UpdateSyncClock(jdouble syncClock) = 0;

    /**
     * 获取同步时钟
     * @return
     */
    virtual jdouble GetSyncClock() = 0;
};

#endif //OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H
