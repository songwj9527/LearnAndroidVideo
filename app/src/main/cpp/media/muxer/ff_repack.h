//
// Created by fgrid on 2021/8/3.
//

#ifndef OPENVIDEO_FF_REPACK_H
#define OPENVIDEO_FF_REPACK_H

#include <jni.h>
extern "C" {
#include <libavformat/avformat.h>
};

class FFRepack {
private:
    const char *TAG = "FFRepack";

    JNIEnv      *jniEnv = NULL;
    jobject     jSrcPath = NULL; // java层传入的视频路径的弱引用
    const char  *srcPath = NULL; // c层创建的视频路径
    jobject     jDesPath = NULL; // java层传入的视频路径的弱引用
    const char  *desPath = NULL; // c层创建的视频路径

    AVFormatContext *m_in_format_cxt = NULL;

    AVFormatContext *m_out_format_cxt = NULL;

    int OpenSrcFile(const char *srcPath);

    int InitMuxerParams(const char *destPath);

public:
    FFRepack(JNIEnv *env,jstring in_path, jstring out_path);

    void Start();

    void Write(AVPacket pkt);

    void Release();
};

#endif //OPENVIDEO_FF_REPACK_H
