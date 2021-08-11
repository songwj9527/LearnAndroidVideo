//
// Created by fgrid on 2021/8/6.
//

#ifndef OPENVIDEO_SYNTHESIZER_H
#define OPENVIDEO_SYNTHESIZER_H

#include "../player/player.h"
#include "../encoder/audio/audio_encoder.h"
#include "../encoder/video/video_encoder.h"
#include "../render/i_render_state_cb.h"
#include "../render/video/opengl/opengl_pixel_receiver.h"

class Synthesizer : public Player, public IRenderStateCb, public IMuxerCb,
                    public IEncodeStateCb, public OpenGLPixelReceiver {
private:
    const char *TAG = "Synthesizer";

//    VideoDecoder *m_video_decoder = NULL;
//    AudioDecoder *m_audio_decoder = NULL;
//    OpenSLRender *m_sl_render = NULL;
//    OpenGLRender *m_gl_render = NULL;

    // 编码后的文件
    jobject     jdstURL = NULL; // java层传入的视频流地址的弱引用
    const char  *dstURL = NULL; // c层创建的视频流地址

    Mp4Muxer *m_mp4_muxer = NULL;

    VideoEncoder *m_v_encoder = NULL;
    AudioEncoder *m_a_encoder = NULL;

    EncodeCacheFrame *m_cur_v_frame = NULL;
    EncodeCacheFrame *m_cur_a_frame = NULL;

public:
    Synthesizer(JNIEnv *env, jobject object);
    ~Synthesizer();

    void setSourceUrls(jstring src_path, jstring dst_path);
    void releaseDstURL();

    void prepareSync() override;

    void Start();

    void RenderPrepare(BaseRender *render) override;
    void RenderRunning(BaseRender *render) override;
    bool RenderOneFrame(BaseRender *render, EncodeCacheFrame *frame) override;
    void RenderPause(BaseRender *render) override;
    void RenderStop(BaseRender *render) override;
    void RenderFinish(BaseRender *render) override;

    void ReceivePixel(uint8_t *rgba) override;

    void EncodeStart(BaseEncoder *encoder) override;
    void EncodeSend(BaseEncoder *encoder) override;
    void EncodeFrame(BaseEncoder *encoder, void *data) override;
    void EncodeProgress(BaseEncoder *encoder, long time) override;
    void EncodeFinish(BaseEncoder *encoder) override;

    void OnMuxFinished() override;
};

#endif //OPENVIDEO_SYNTHESIZER_H
