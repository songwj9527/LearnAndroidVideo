//
// Created by fgrid on 2021/12/16.
//

#ifndef OPENVIDEO_FFMPEG_I_DECODER_CALLBACK_H
#define OPENVIDEO_FFMPEG_I_DECODER_CALLBACK_H

#include <jni.h>

class IDecoderCallback {
public:
    /**
     * 给解码器调用，用于告知MediaPlayer解码器准备完成，可准备渲染了。
     * @param decoder
     */
    virtual void OnDecoderPrepared(JNIEnv *jniEnv, int decoder) = 0;

    /**
     * 给解码器调用，用于告知MediaPlayer解码器已获取视频信息。
     * @param decoder
     * @param videoWidth
     * @param videoHeight
     * @param videoRotation
     */
    virtual void OnDecoderInfo(JNIEnv *jniEnv, int decoder, int videoWidth, int videoHeight, int videoRotation) = 0;

    /**
     * 给解码器调用，用于告知MediaPlayer解码器已解码结束。
     * @param decoder
     */
    virtual void OnDecoderCompleted(JNIEnv *jniEnv, int decoder) = 0;

    /**
     * 给解码器调用，用于告知MediaPlayer哪个解码器完成了指定位置
     * @param jniEnv
     * @param decoder
     */
    virtual void OnDecoderSeekCompleted(JNIEnv *jniEnv, int decoder) = 0;

    /**
     * 给解码器调用，用于告知MediaPlayer哪个解码器出了问题，出了什么问题；
     * 并为整个MediaPlayer是否调用、什么时候调用onError方法将异常信息返回给java层。
     * @param decoder
     * @param code
     * @param msg
     */
    virtual void OnDecoderError(JNIEnv *jniEnv, int decoder, int code, const char *msg) = 0;
};

#endif //OPENVIDEO_FFMPEG_I_DECODER_CALLBACK_H