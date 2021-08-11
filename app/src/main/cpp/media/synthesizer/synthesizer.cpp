//
// Created by fgrid on 2021/8/6.
//

#include "synthesizer.h"
#include "../decoder/video/video_decoder.h"
#include "../decoder/audio/audio_decoder.h"
#include "../render/video/opengl/opengl_render.h"
#include "../render/audio/opensl_render.h"
#include "../opengl/drawer/video_drawer.h"
#include "../opengl/drawer/fbo_soul_video_drawer.h"

Synthesizer::Synthesizer(JNIEnv *env, jobject object) : Player(env, object) {

}

Synthesizer::~Synthesizer() {
    LOGE(TAG, "%s", "~Synthesizer");
    releaseDstURL();
}

void Synthesizer::releaseDstURL() {
    if (jniEnv == NULL) {
        return;
    }
    // 释放转换参数
    if (jdstURL != NULL && dstURL != NULL) {
        if (!jniEnv->IsSameObject(jdstURL, NULL)) {
            jniEnv->ReleaseStringUTFChars((jstring) jdstURL, dstURL);
            jniEnv->DeleteGlobalRef(jdstURL);
        }
    }
    dstURL = NULL;
    jdstURL = NULL;
}

void Synthesizer::setSourceUrls(jstring src_path, jstring dst_path) {
    setDataSource(src_path);
    if (jniEnv == NULL) {
        return;
    }
    releaseDstURL();
    jdstURL = jniEnv->NewGlobalRef(dst_path);
    const char *urlChar = jniEnv->GetStringUTFChars(dst_path, NULL);
    if (urlChar == NULL) {
        dstURL = NULL;
        onError(jniEnv, JAVA_PATH_2_C_CHARS_FAILED, "传入的dst url转chars失败");
        return;
    }
    if (strlen(urlChar) == 0) {
        dstURL = NULL;
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的dst url为空");
        return;
    }
    dstURL = urlChar;
    LOGE(TAG, "setDstSource(): %s", dstURL);
}

void Synthesizer::prepareSync() {
    if (strlen(sourceURL) == 0 || strlen(dstURL) == 0) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    // 封装器
    m_mp4_muxer = new Mp4Muxer();
    m_mp4_muxer->Init(jniEnv, dstURL);
    m_mp4_muxer->SetMuxFinishCallback(this);

    // --------------------------视频配置--------------------------
    // 视频编码器
    m_v_encoder = new VideoEncoder(jniEnv, m_mp4_muxer, 825, 464);
    m_v_encoder->SetStateReceiver(this);

    // 视频渲染器
    videoRender = new OpenGLRender(true);
//    ((OpenGLRender *) videoRender)->addDrawer(new VideoDrawer());
    ((OpenGLRender *) videoRender)->addDrawer(new FBOSoulVideoDrawer());
    videoRender->setIRenderStateCb(this);
    ((OpenGLRender *) videoRender)->SetPixelReceiver(this);
    // 视频解码器
    videoDecoder = new VideoDecoder(jniEnv, this, sourceURL, videoRender, true);


    //--------------------------音频配置--------------------------
    // 音频编码器
    m_a_encoder = new AudioEncoder(jniEnv, m_mp4_muxer);
    // 监听编码状态
    m_a_encoder->SetStateReceiver(this);

    // 音频播放器
    openSlRender = new OpenSLRender(true);
    openSlRender->setIRenderStateCb(this);
    // 音频解码器
    audioDecoder = new AudioDecoder(jniEnv, this, sourceURL, openSlRender, true);
    LOGE(TAG, "%s", "prepareSync()");
}

void Synthesizer::Start() {
    if (m_v_encoder != NULL && m_a_encoder != NULL) {
        start();
    }
}

void Synthesizer::RenderPrepare(BaseRender *render) {

}

void Synthesizer::RenderRunning(BaseRender *render) {}
void Synthesizer::RenderPause(BaseRender *render) {}
void Synthesizer::RenderStop(BaseRender *render) {}
bool Synthesizer::RenderOneFrame(BaseRender *render, EncodeCacheFrame *frame) {
    if (render == videoRender) {
//        while (m_cur_v_frame) {
//            av_usleep(2000);
//        }
        m_cur_v_frame = frame;
        ((OpenGLRender*) videoRender)->RequestRgbaData();
        return m_v_encoder->TooMuchData();
    } else {
        m_cur_a_frame = frame;
        m_a_encoder->PushFrame(frame);
        return m_a_encoder->TooMuchData();
    }
}
void Synthesizer::RenderFinish(BaseRender *render) {
    // 编码结束，压入一帧空数据，通知编码器结束编码
    if (render == videoRender) {
        if (m_v_encoder != NULL) {
            m_v_encoder->PushFrame(new EncodeCacheFrame(NULL, 0, 0, AVRational{1, 25}, NULL));
        }
    } else {
        if (m_a_encoder != NULL) {
            m_a_encoder->PushFrame(new EncodeCacheFrame(NULL, 0, 0, AVRational{1, 25}, NULL));
        }
    }
}

void Synthesizer::ReceivePixel(uint8_t *rgba) {
    if (m_v_encoder != NULL) {
        EncodeCacheFrame *rgbFrame = new EncodeCacheFrame(rgba, m_cur_v_frame->line_size,
                                                m_cur_v_frame->pts, m_cur_v_frame->time_base);
        m_v_encoder->PushFrame(rgbFrame);
        m_cur_v_frame = NULL;
    }
}

void Synthesizer::EncodeStart(BaseEncoder *encoder) {}
void Synthesizer::EncodeSend(BaseEncoder *encoder) {}
void Synthesizer::EncodeProgress(BaseEncoder *encoder, long time) {}
void Synthesizer::EncodeFrame(BaseEncoder *encoder, void *data) {}
void Synthesizer::EncodeFinish(BaseEncoder *encoder) {
    LOGI("Synthesizer", "EncodeFinish ...");
    if (encoder == m_v_encoder) {
        m_v_encoder = NULL;
    } else {
        m_a_encoder = NULL;
    }
}

void Synthesizer::OnMuxFinished() {
    release();
    if (m_mp4_muxer != NULL) {
        delete m_mp4_muxer;
    }
}
