//
// Created by fgrid on 2021/8/5.
//

#ifndef OPENVIDEO_BASE_ENCODER_H
#define OPENVIDEO_BASE_ENCODER_H

#include "i_encoder.h"
#include "../muxer/mp4_muxer.h"
#include "../../utils/logger.h"

#include <thread>
#include <queue>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include <libavutil/frame.h>
};

class BaseEncoder : public IEncoder {
private:

    // 编码格式 ID
    AVCodecID m_codec_id;

    // 线程依附的JVM环境
    JavaVM *m_jvm_for_thread = NULL;
    // 线程等待锁变量
    pthread_mutex_t m_mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t m_cond = PTHREAD_COND_INITIALIZER;

    // 编码器
    AVCodec *m_codec = NULL;

    // 编码上下文
    AVCodecContext *m_codec_ctx = NULL;

    // 编码数据包
    AVPacket *m_encoded_pkt = NULL;

    // 写入Mp4的输入流索引
    int m_encode_stream_index = 0;

    // 原数据时间基
    AVRational m_src_time_base;

    // 缓冲队列
    std::queue<EncodeFrame *> m_src_frames;

    // 操作数据锁
    std::mutex m_frames_lock;

    // 状态回调
    IEncodeStateCb *m_state_cb = NULL;

    /**
     * 初始化
     * @return
     */
    bool Init();

    /**
     * 循环拉去已经编码的数据，直到没有数据或者编码完毕
     * @return true 编码结束；false 编码未完成
     */
    bool DrainEncode();

    /**
     * 编码一帧数据
     * @return 错误信息
     */
    int EncodeOneFrame();

    // 新建编码线程
    void CreateEncodeThread();

    // 解码静态方法，给线程调用
    static void Encode(std::shared_ptr<BaseEncoder> that);

    // 打开编码器
    void OpenEncoder();

    // 循环编码
    void LoopEncode();

    // 释放资源
    void DoRelease();

    // 休眠等待
    void Wait() {
        pthread_mutex_lock(&m_mutex);
        pthread_cond_wait(&m_cond, &m_mutex);
        pthread_mutex_unlock(&m_mutex);
    }

    // 唤醒
    void SendSignal() {
        pthread_mutex_lock(&m_mutex);
        pthread_cond_signal(&m_cond);
        pthread_mutex_unlock(&m_mutex);
    }

protected:
    const char * TAG = "BaseEncoder";

    // Mp4 封装器
    Mp4Muxer *m_muxer = NULL;

    // 初始化编码参数（上下文）
    virtual void InitContext(AVCodecContext *codec_ctx) = 0;

    // 配置Mp4 混淆通道信息
    virtual int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) = 0;

    // 处理一帧数据
    virtual AVFrame* DealFrame(EncodeFrame *encode_frame) = 0;

    // 释放资源
    virtual void Release() = 0;

public:
    BaseEncoder(JNIEnv *env, Mp4Muxer *muxer, AVCodecID codec_id);

    // 压入一帧待编码数据（由外部调用）
    void PushFrame(EncodeFrame *encode_frame) override ;

    // 判断是否缓冲数据过多，用于控制缓冲队列大小
    bool TooMuchData() override {
        return m_src_frames.size() > 100;
    }

    // 设置编码状态监听器
    void SetStateReceiver(IEncodeStateCb *cb) override {
        this->m_state_cb = cb;
    }
};

#endif //OPENVIDEO_BASE_ENCODER_H
