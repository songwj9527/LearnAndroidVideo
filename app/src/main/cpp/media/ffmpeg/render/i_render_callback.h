//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H
#define OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H

#include <jni.h>

class IRenderCallback {
    /**
     * 给渲染器调用，用于告知MediaPlayer渲染器准备完成，可准备渲染了。
     * @param decoder
     */
    virtual void OnRenderPrepared(JNIEnv *jniEnv, int render) = 0;

    /**
     * 给渲染器调用，用于告知MediaPlayer渲染器已渲染结束。
     * @param decoder
     */
    virtual void OnRenderCompleted(JNIEnv *jniEnv, int decoder) = 0;

    /**
     * 给渲染器调用，用于告知MediaPlayer哪个渲染器完成了指定位置
     * @param jniEnv
     * @param decoder
     */
    virtual void OnRenderSeekCompleted(JNIEnv *jniEnv, int decoder) = 0;

    /**
     * 给渲染器调用，用于告知MediaPlayer哪个渲染器出了问题，出了什么问题；
     * 并为整个MediaPlayer是否调用、什么时候调用onError方法将异常信息返回给java层。
     * @param decoder
     * @param code
     * @param msg
     */
    virtual void OnRenderError(JNIEnv *jniEnv, int decoder, int code, const char *msg) = 0;
};

#endif //OPENVIDEO_FFMPEG_I_RENDER_CALLBACK_H
