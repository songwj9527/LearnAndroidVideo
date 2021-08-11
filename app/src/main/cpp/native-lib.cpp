#include <jni.h>
#include <string>
#include <pthread.h>
#include "./utils/logger.h"
#include "media/player/default_player/media_player.h"
#include "media/player/opengl_player/opengl_player.h"
#include "media/muxer/ff_repack.h"
#include "media/synthesizer/synthesizer.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavcodec/jni.h>
}

JNIEXPORT jstring JNICALL ffmpegInfo(JNIEnv *env, jobject obj) {
    char info[40000] = {0};
    AVCodec *c_temp = av_codec_next(NULL);
    while (c_temp != NULL) {
        if (c_temp->decode != NULL) {
            sprintf(info, "%sdecode:", info);
        } else {
            sprintf(info, "%sencode:", info);
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s(video):", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s(audio):", info);
                break;
            default:
                sprintf(info, "%s(other):", info);
                break;
        }
        c_temp = c_temp->next;
    }
    return env->NewStringUTF(info);
}

struct transform_info {
    const char *src;
    const char *dest;
    const char *type;
};

void endTransform(AVOutputFormat *ofmt, AVFormatContext *ifmt_ctx, AVFormatContext *ofmt_ctx = NULL) {
    avformat_close_input(&ifmt_ctx);
    // 关闭输出
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE)) {
        avio_close(ofmt_ctx->pb);
    }
    avformat_free_context(ofmt_ctx);
}

pthread_t pthread_transform;
bool transformRunning = false;
void *transform_thread_fun(void *args) {
    pthread_detach(pthread_self());
    LOGE("native-lib", "thread %lld running", pthread_self());

    transform_info *info = (transform_info *) args;
    if (info == NULL || info->src == NULL || info->dest == NULL) {
        transformRunning = false;
        free(info);
        pthread_exit(0);
    }
    LOGE("native-lib", "src: %s\ndest: %s", info->src, info->dest);

    AVOutputFormat *ofmt = NULL;
    AVBitStreamFilterContext *vbsf_ctx = NULL;
    // 定义输入、输出AVFormatContext
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;


    AVPacket pkt;
    int ret, i;
    int frame_index = 0;

    /**
     * 1.注册组件：基于ffmpeg的应用程序中，几乎都是第一个被调用的。
     * 只有调用了该函数，才能使用复用器，编码器才能起作用，必须调用此函数。
     */
    av_register_all();

    // 打开输入文件
    if ((ret = avformat_open_input(&ifmt_ctx, info->src, 0, 0)) < 0) {
        LOGE("native-lib", "Could not open input file.");
        endTransform(ofmt, ifmt_ctx, ofmt_ctx);
        transformRunning = false;
        free(info);
        pthread_exit(0);
    }
    // 获取视频信息
    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        LOGE("native-lib", "Failed to retrieve input stream information.");
        endTransform(ofmt, ifmt_ctx, ofmt_ctx);
        transformRunning = false;
        free(info);
        pthread_exit(0);
    }

    //获取视频流的索引位置
    int v_stream_idx = -1;
    int index = 0;
    for(index = 0; index < ifmt_ctx->nb_streams; index++){
        if(ifmt_ctx->streams[index]->codec->codec_type == AVMEDIA_TYPE_VIDEO){
            v_stream_idx = index;
            break;
        }
    }

    // MP4中使用的H.264编码有两种封装模式：
    // 一种为annexb模式，它为传统模式，有startcode，SPS和PPS在Element Stream中；
    // 另一种为mp4模式，一般MP4、MKV、AVI都没有startcode，SPS和PPS以及其他信息被封装在容器中。
    // 每一帧前面是这一帧的长度值，很多编码器只支持annexb模式，因此需要对MP4模式做转换。
    // 在FFmpeg中用h264_mp4toannexb_filter可以进行模式转换，对应命令为- bsf h264_mp4toannexb
    if (v_stream_idx != -1) {
        // H265
//        if (ifmt_ctx->streams[index]->codec->codec_id == AV_CODEC_ID_HEVC) {
//            vbsf_ctx = av_bitstream_filter_init("h265_mp4toannexb");
//        } else {
//            vbsf_ctx = av_bitstream_filter_init("h264_mp4toannexb");
//        }
        LOGE("native-lib", "HEVC: %s", (ifmt_ctx->streams[index]->codec->codec_id == AV_CODEC_ID_HEVC) ? "true" : "false");
        vbsf_ctx = av_bitstream_filter_init("h264_mp4toannexb");
    } else{
        vbsf_ctx = av_bitstream_filter_init("h264_mp4toannexb");
    }

    /*
     * 将多媒体文件中的信息打印出来
     * 第二个参数是流的索引值，写0就可以
     * 第三个参数是视频路径
     * 第四个参数是输入流还是输出流，输入流是0，输出流是1,现在是输入文件所以是0
     */
    av_dump_format(ifmt_ctx, 0, info->src, 0);

    // 初始化输出视频码流的AVFormatContext
    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL,info->dest);
    if (!ofmt_ctx) {
        LOGE("native-lib", "Could not create output context.");
        ret = AVERROR_UNKNOWN;
        endTransform(ofmt, ifmt_ctx, ofmt_ctx);
        transformRunning = false;
        free(info);
        pthread_exit(0);
    }
    ofmt = ofmt_ctx->oformat;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        // 通过输入的AVStream创建输出的AVStream
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
        if (!out_stream) {
            LOGE("native-lib", "Failed to allocating output stream.");
            ret = AVERROR_UNKNOWN;
            endTransform(ofmt, ifmt_ctx, ofmt_ctx);
            transformRunning = false;
            free(info);
            pthread_exit(0);
        }

        // 复制AVCodecContext的设置属性
        out_stream->codec->codec_tag = 0;
        if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER) {
            out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
        }
    }

    // 将多媒体文件中的信息打印出来
    av_dump_format(ofmt_ctx, 0, info->dest, 1);

    // 打开输出文件
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, info->dest, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("native-lib", "Could not open output file: %s", info->dest);
            endTransform(ofmt, ifmt_ctx, ofmt_ctx);
            transformRunning = false;
            free(info);
            pthread_exit(0);
        }
    }

    // 写文件头
    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        LOGE("native-lib", "Error occurred when opening output file.");
        endTransform(ofmt, ifmt_ctx, ofmt_ctx);
        transformRunning = false;
        free(info);
        pthread_exit(0);
    }

    while(1) {
        AVStream *in_stream, *out_stream;
        // 得到一个AVPacket
        ret = av_read_frame(ifmt_ctx, &pkt);
        if (ret < 0) {
            break;
        }
        in_stream = ifmt_ctx->streams[pkt.stream_index];
        out_stream = ofmt_ctx->streams[pkt.stream_index];

        // 转换 PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        if (pkt.stream_index == 0) {
            AVPacket fpkt = pkt;
            int a = av_bitstream_filter_filter(vbsf_ctx, out_stream->codec, NULL,
                    &fpkt.data, &fpkt.size,
                    pkt.data, pkt.size, pkt.flags & AV_PKT_FLAG_KEY);
            pkt.data = fpkt.data;
            pkt.size = fpkt.size;
        }

        // 将AVPacket（存储音视频压缩码流数据）写入文件
        if (av_write_frame(ofmt_ctx, &pkt) < 0) {
            LOGE("native-lib", "Error muxing packet.");
            break;
        }

        LOGE("native-lib", "Write %8d frames to output file.", frame_index);
        av_packet_unref(&pkt);
        frame_index++;
    }

    // 文件结尾
    av_write_trailer(ofmt_ctx);

    avformat_close_input(&ifmt_ctx);
    // 关闭输出
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE)) {
        avio_close(ofmt_ctx->pb);
    }
    avformat_free_context(ofmt_ctx);

    transformRunning = false;
    free(info);
    pthread_exit(0);
}

JNIEXPORT void JNICALL videoTypeTransform(JNIEnv *env, jobject obj, jstring srcFilePath, jstring destFilePath) {
    LOGE("native-lib","newThread begin");
    const char *src = env->GetStringUTFChars(srcFilePath, 0);
    if (src == NULL) {
        return;
    }
    if (strlen(src) == 0) {
        return;
    }
    const char *dest = env->GetStringUTFChars(destFilePath, 0);
    if (dest == NULL) {
        return;
    }
    if (strlen(dest) == 0) {
        return;
    }

    if (transformRunning) {
        return;
    }
    transformRunning = true;

    transform_info *info = (transform_info *) malloc(sizeof(transform_info));
    info->src = src;
    info->dest = dest;
    int ret = pthread_create(&pthread_transform, NULL, transform_thread_fun, info);
    LOGE("native-lib","newThread ret: %d", ret);
}

JNIEXPORT jint JNICALL nativeCreatePlayer(JNIEnv *env, jobject obj) {
    MediaPlayer *mediaPlayer = new MediaPlayer(env, obj);
    return (jint) mediaPlayer;
}

JNIEXPORT void JNICALL nativeSetDataSource(JNIEnv *env, jobject obj, jint player, jstring url) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->setDataSource(url);
}

JNIEXPORT void JNICALL nativePrepareSync(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->prepareSync();
}

JNIEXPORT void JNICALL nativeSetSurface(JNIEnv *env, jobject obj, jint player, jobject surface) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->setSurface(surface);
}

JNIEXPORT void JNICALL nativeStart(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->start();
}

JNIEXPORT void JNICALL nativePause(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->pause();
}

JNIEXPORT void JNICALL nativeResume(JNIEnv *env, jobject obj, jint player) {
    LOGE("nativeResume", "%s", "nativeResume()");
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->resume();
}

JNIEXPORT void JNICALL nativeStop(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->stop();
}

JNIEXPORT void JNICALL nativeReset(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->reset();
}

JNIEXPORT void JNICALL nativeRelease(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->release();
    av_usleep(50000);
    delete mediaPlayer;
}

JNIEXPORT jlong JNICALL nativeGetDuration(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return (jlong) 0;
    }
    return mediaPlayer->getDuration();
}

JNIEXPORT jlong JNICALL nativeGetCurrentPosition(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return 0;
    }
    return mediaPlayer->getCurrentPosition();
}

JNIEXPORT void JNICALL nativeSeekTo(JNIEnv *env, jobject obj, jint player, jlong position) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->seekTo(position);
}

JNIEXPORT jint JNICALL nativeGetMaxVolumeLevel(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return 0;
    }
    return mediaPlayer->getMaxVolumeLevel();
}

JNIEXPORT jint JNICALL nativeGetVolumeLevel(JNIEnv *env, jobject obj, jint player) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return 0;
    }
    return mediaPlayer->getVolumeLevel();
}

JNIEXPORT void JNICALL nativeSetVolumeLevel(JNIEnv *env, jobject obj, jint player, jint volume) {
    Player *mediaPlayer = (Player *) player;
    if (mediaPlayer == NULL) {
        return;
    }
    mediaPlayer->setVolumeLevel(volume);
}

JNIEXPORT jint JNICALL nativeCreateGLPlayer(JNIEnv *env, jobject obj) {
    OpenGLPlayer *mediaPlayer = new OpenGLPlayer(env, obj);
    return (jint) mediaPlayer;
}

JNIEXPORT jint JNICALL nativeCreateRepack(JNIEnv *env, jobject obj, jstring srcPath, jstring destPath) {
    FFRepack *ffRepack = new FFRepack(env, srcPath, destPath);
    return (jint) ffRepack;
}

JNIEXPORT void JNICALL nativeStartRepack(JNIEnv *env, jobject obj, jint repack) {
    FFRepack *ffRepack = (FFRepack *) repack;
    if (ffRepack == NULL) {
        return;
    }
    ffRepack->Start();
}

JNIEXPORT void JNICALL nativeReleaseRepack(JNIEnv *env, jobject obj, jint repack) {
    FFRepack *ffRepack = (FFRepack *) repack;
    if (ffRepack == NULL) {
        return;
    }
    ffRepack->Release();
    delete ffRepack;
}

JNIEXPORT jint JNICALL nativeCreateSynthesizer(JNIEnv *env, jobject obj, jstring srcPath, jstring destPath) {
    Synthesizer *synthesizer = new Synthesizer(env, obj);
    synthesizer->setSourceUrls(srcPath, destPath);
    return (jint) synthesizer;
}

JNIEXPORT void JNICALL nativeStartSynthesizer(JNIEnv *env, jobject obj, jint synthesizer) {
    Synthesizer *m_synthesizer = (Synthesizer *) synthesizer;
    if (m_synthesizer == NULL) {
        return;
    }
    m_synthesizer->Start();
}

JNIEXPORT void JNICALL nativeReleaseSynthesizer(JNIEnv *env, jobject obj, jint synthesizer) {
    Synthesizer *m_synthesizer = (Synthesizer *) synthesizer;
    if (m_synthesizer == NULL) {
        return;
    }
    m_synthesizer->release();
    delete m_synthesizer;
}


static const JNINativeMethod gMethods_NativePlayer[] = {
        {"ffmpegInfo", "()Ljava/lang/String;", (void *)ffmpegInfo},
        {"videoTypeTransform", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)videoTypeTransform},
//        {"nativeCreatePlayer", "(Ljava.lang.ref.WeakReference;)I", (void *)nativeCreatePlayer},
        {"nativeCreatePlayer", "()I", (void *)nativeCreatePlayer},
        {"nativeSetDataSource", "(ILjava/lang/String;)V", (void *)nativeSetDataSource},
        {"nativePrepareSync", "(I)V", (void *)nativePrepareSync},
        {"nativeSetSurface", "(ILandroid/view/Surface;)V", (void *)nativeSetSurface},
        {"nativeStart", "(I)V", (void *)nativeStart},
        {"nativePause", "(I)V", (void *)nativePause},
        {"nativeResume", "(I)V", (void *)nativeResume},
        {"nativeStop", "(I)V", (void *)nativeStop},
        {"nativeReset", "(I)V", (void *)nativeReset},
        {"nativeRelease", "(I)V", (void *)nativeRelease},
        {"nativeGetDuration", "(I)J", (void *)nativeGetDuration},
        {"nativeGetCurrentPosition", "(I)J", (void *)nativeGetCurrentPosition},
        {"nativeSeekTo", "(IJ)V", (void *)nativeSeekTo},
        {"nativeGetMaxVolumeLevel", "(I)I", (void *)nativeGetMaxVolumeLevel},
        {"nativeGetVolumeLevel", "(I)I", (void *)nativeGetVolumeLevel},
        {"nativeSetVolumeLevel", "(II)V", (void *)nativeSetVolumeLevel},
        {"nativeCreateGLPlayer", "()I", (void *)nativeCreateGLPlayer},
        {"nativeCreateSynthesizer", "(Ljava/lang/String;Ljava/lang/String;)I", (void *)nativeCreateSynthesizer},
        {"nativeStartSynthesizer", "(I)V", (void *)nativeStartSynthesizer},
        {"nativeReleaseSynthesizer", "(I)V", (void *)nativeReleaseSynthesizer},
};
#define JNI_CLASS_NATIVE_PLAYER "com/songwj/openvideo/ffmpeg/NativePlayer"
static int registerNatives_NativePlayer(JNIEnv *env) {
    //获取对应声明native方法的Java类
    jclass clazz = env->FindClass(JNI_CLASS_NATIVE_PLAYER);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    //注册方法，成功返回正确的JNIVERSION。
    if (env->RegisterNatives(clazz, gMethods_NativePlayer, sizeof(gMethods_NativePlayer)/ sizeof(gMethods_NativePlayer[0])) != JNI_OK) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static const JNINativeMethod gMethods_NativeRepack[] = {
        {"nativeCreateRepack", "(Ljava/lang/String;Ljava/lang/String;)I", (void *)nativeCreateRepack},
        {"nativeStartRepack", "(I)V", (void *)nativeStartRepack},
        {"nativeReleaseRepack", "(I)V", (void *)nativeReleaseRepack},
};
#define JNI_CLASS_NATIVE_REPACK "com/songwj/openvideo/ffmpeg/NativeRepack"
static int registerNatives_NativeRepack(JNIEnv *env) {
    //获取对应声明native方法的Java类
    jclass clazz = env->FindClass(JNI_CLASS_NATIVE_REPACK);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    //注册方法，成功返回正确的JNIVERSION。
    if (env->RegisterNatives(clazz, gMethods_NativeRepack, sizeof(gMethods_NativeRepack)/ sizeof(gMethods_NativeRepack[0])) != JNI_OK) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    //OnLoad方法是没有JNIEnv参数的，需要通过vm获取。
    JNIEnv *env = NULL;
//    if (vm->AttachCurrentThread(&env, NULL) == JNI_OK) {
//        //获取对应声明native方法的Java类
//        jclass  clazz = env->FindClass(JNI_CLASS);
//        if (clazz == NULL) {
//            return JNI_FALSE;
//        }
//        //注册方法，成功返回正确的JNIVERSION。
//        if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods)/ sizeof(gMethods[0]))==JNI_OK) {
//            return JNI_VERSION_1_4;
//        }
//    }

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("native-lib", "ERROR: GetEnv failed\n");
        return -1;
    }
    assert(env != NULL);

    if (registerNatives_NativePlayer(env) != JNI_TRUE) {
        LOGE("native-lib", "ERROR: registerNatives NativePlayer failed\n");
        return JNI_FALSE;
    }
    if (registerNatives_NativeRepack(env) != JNI_TRUE) {
        LOGE("native-lib", "ERROR: registerNatives NativeRepack failed\n");
        return JNI_FALSE;
    }
    return JNI_VERSION_1_4;
}