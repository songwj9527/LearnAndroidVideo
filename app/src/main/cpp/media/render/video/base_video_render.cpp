//
// Created by fgrid on 2021/7/20.
//

#include "base_video_render.h"

#include "../../player/player.h"
#include "../../decoder/video/video_decoder.h"
#include "../../encoder/encode_cache_frame.h"

BaseVideoRender::BaseVideoRender(bool for_synthesizer) : BaseRender(for_synthesizer) {
    m_rgb_frame = av_frame_alloc();
}

BaseVideoRender::~BaseVideoRender() {
    LOGE(TAG, "%s", "~BaseVideoRender 0");
    releaseReader();
    m_surface_ref = NULL;
    LOGE(TAG, "%s", "~BaseVideoRender 1");
}

/**
 * 渲染准备工作已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderPrepared方法）
 */
void BaseVideoRender::onPrepared(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderPrepared(env, MODULE_CODE_VIDEO);
    }
}

/**
 * 渲染线程异常时调用（子类实现此方法：最终会调用MediaPlayer的onRenderError方法）
 */
void BaseVideoRender::onError(JNIEnv *env,int code, const char *msg) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderError(env, MODULE_CODE_VIDEO, code, msg);
    }
}

/**
 * 渲染已完成时调用（子类实现此方法：最终会调用MediaPlayer的onRenderComplete方法）
 */
void BaseVideoRender::onComplete(JNIEnv *env) {
    if (mediaPlayer != NULL) {
        mediaPlayer->onRenderCompleted(env, MODULE_CODE_VIDEO);
    }
}


/**
 * 初始化渲染窗口
 * @param env
 */
void BaseVideoRender::initANativeWindow(JNIEnv *env) {
    releaseANativeWindow();
    if (decoder != NULL) {
        int video_height = 0, video_width = 0;
        video_height = ((VideoDecoder *) decoder)->getVideoHeight();
        video_width = ((VideoDecoder *) decoder)->getVideoWidth();

        LOGE(TAG, "%s", "initANativeWindow() 0");
        // 初始化窗口
        m_native_window = ANativeWindow_fromSurface(env, m_surface_ref);
        LOGE(TAG, "%s", "initANativeWindow() 1");

        // 绘制区域的宽高
        int windowWidth = ANativeWindow_getWidth(m_native_window);
        int windowHeight = ANativeWindow_getHeight(m_native_window);

        // 计算目标视频的宽高
        m_dst_w = windowWidth;
        m_dst_h = m_dst_w * video_height / video_width;
        if (m_dst_h > windowHeight) {
            m_dst_h = windowHeight;
            m_dst_w = windowHeight * video_width / video_height;
        }
        LOGE(TAG, "windowW: %d, windowH: %d, dstVideoW: %d, dstVideoH: %d",windowWidth, windowHeight, m_dst_w, m_dst_h)

        //设置宽高限制缓冲区中的像素数量
        ANativeWindow_setBuffersGeometry(m_native_window, windowWidth,windowHeight, WINDOW_FORMAT_RGBA_8888);
    }
}

/**
 * 初始化视频数据转换缓存
 */
void BaseVideoRender::initReaderBuffer() {
    LOGE(TAG, "%s", "initReaderBuffer() 0");
    if (m_buf_for_rgb_frame != NULL) {
        free(m_buf_for_rgb_frame);
        m_buf_for_rgb_frame = NULL;
    }
    if (decoder != NULL) {
        if (m_dst_w == -1) {
            m_dst_w = ((VideoDecoder *) decoder)->getVideoWidth();
        }
        if (m_dst_h == -1) {
            m_dst_w = ((VideoDecoder *) decoder)->getVideoHeight();
        }
        // 获取缓存大小
        int numBytes = av_image_get_buffer_size(DST_FORMAT, m_dst_w, m_dst_h, 1);
        // 分配内存
        m_buf_for_rgb_frame = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
        // 将内存分配给RgbFrame，并将内存格式化为三个通道后，分别保存其地址
        av_image_fill_arrays(m_rgb_frame->data, m_rgb_frame->linesize, m_buf_for_rgb_frame, DST_FORMAT, m_dst_w, m_dst_h, 1);
    }
    LOGE(TAG, "%s", "initReaderBuffer() 1");
}

/**
 * 初始化格式转换工具
 */
void BaseVideoRender::initSws() {
    LOGE(TAG, "%s", "initSws() 0");
    if (m_sws_ctx != NULL) {
        sws_freeContext(m_sws_ctx);
        m_sws_ctx = NULL;
    }
    if (decoder != NULL) {
        m_sws_ctx = sws_getContext(((VideoDecoder *) decoder)->getVideoWidth(), ((VideoDecoder *) decoder)->getVideoHeight(), decoder->getPixelFormat(),
                                   m_dst_w, m_dst_h, DST_FORMAT,
                                   SWS_FAST_BILINEAR, NULL, NULL, NULL);
    }
    LOGE(TAG, "%s", "initSws() 1");
}

/**
 * 新建读取视频帧、渲染线程等（opengl时需要新增opengl渲染线程，具体可在createOtherThread中自定义处理）
 */
void BaseVideoRender::prepareSyncAllThread(JNIEnv *env) {
    createDefaultThread();
    createOtherThread(env);
}

/**
 * 新建读取视频帧、渲染总线程
 */
void BaseVideoRender::createDefaultThread() {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseVideoRender> read_that(this);
    std::thread read_thread(runDefaultThread, read_that);
    read_thread.detach();
}

/**
 * 读取视频帧、渲染总线程调用的方法
 * @param that DefaultVideoRender
 */
void BaseVideoRender::runDefaultThread(std::shared_ptr<BaseVideoRender> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        that->onError(NULL, VIDEO_RENDER_UNPREPARED, "Fail to init default video render thread");
        return;
    }
    that->jniEnv = env;

    LOGE(that->TAG, "%s", "runDefaultThread()");
    if (that->m_surface_ref != NULL) {
        if (JNI_TRUE != env->IsSameObject(that->m_surface_ref, NULL)) {
            that->setSurface(that->jniEnv, that->m_surface_ref);
        }
    }

    // 开始循环渲染
    that->loopRender(env);
    // 结束循环渲染
    that->doneRender();

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
    LOGE(that->TAG, "%s", "thread done.");
}

/**
 * 开始循环渲染
 */
void BaseVideoRender::loopRender(JNIEnv *env) {
    pthread_mutex_lock(&m_state_mutex);
    if (m_state == STOPPED) {
        return;
    } else {
        m_state = PREPARED;
    }
    pthread_mutex_unlock(&m_state_mutex);
    onPrepared(env);
    if (m_for_synthesizer && m_i_render_state_cb != NULL) {
        m_i_render_state_cb->RenderPrepare(this);
    }

    double  last_play = 0,  //上一帧的播放时间
    play = 0,              //当前帧的播放时间
    last_delay = 0,    // 上一次播放视频的两帧视频间隔时间
    delay = 0,        //两帧视频间隔时间
    sync_clock = 0, //实际播放时间
    diff = 0,   //音频帧与视频帧相差时间
    sync_threshold = 0,
            start_time,  //从第一帧开始的绝对时间
    pts = 0, // pts时间
    actual_delay = 0//真正需要延迟时间
    ;//两帧间隔合理间隔时间
    start_time = av_gettime() / 1000000.0;
    while (isRunning()) {
        if (decoder != NULL) {
            bool isComplete = false;
//            pthread_mutex_lock(&m_state_mutex);
            if (decoder->isCompleted() && m_render_frame_queue.empty() && m_state != COMPLETED) {
                m_state = COMPLETED;
                isComplete = true;
            }
//            pthread_mutex_unlock(&m_state_mutex);
            if (isComplete) {
                onComplete(env);
                if (m_for_synthesizer && m_i_render_state_cb != NULL) {
                    m_i_render_state_cb->RenderFinish(this);
                }
            }
        }
        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
            || m_state == SEEKING // 设置解码器正在拉取进度（设置解码器指定位置解码时，设置此状态）
                ) {
            waitRenderFrame(0);
            LOGE(TAG, "loopRender(): %s", "wake up.");
        }
        LOGE(TAG, "isRunning(): %d", m_state);
        if (!isRunning()) {
            break;
        }
        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
            || m_state == SEEKING // 设置解码器正在拉取进度（设置解码器指定位置解码时，设置此状态）
                ) {
            continue;
        }

        LOGE(TAG, "loopRender(): %s", "queue pop 0.");
        //从缓存中获取数据
        RenderFrame *frame = renderFramePop();
        LOGE(TAG, "loopRender(): %s", "queue pop 1.");
        if (frame == NULL) {
            continue;
        }
        // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
        if (!isRunning()) {
            decodeFramePush(frame->m_frame);
            delete frame;
            break;
        }

        // 将视频帧数据转换成渲染数据
        scaleFrame(frame->m_frame);
        if (m_for_synthesizer && m_i_render_state_cb != NULL) {
            if (m_dst_h > 0) {
                uint8_t *data = static_cast<uint8_t *>(malloc(
                        sizeof(uint8_t) * m_rgb_frame->linesize[0] * m_dst_h));
                memcpy(data, m_rgb_frame->data[0], m_rgb_frame->linesize[0] * m_dst_h);
                EncodeCacheFrame *encodeFrame = new EncodeCacheFrame(
                        data,
                        m_rgb_frame->linesize[0],
                        frame->m_frame->pts,
                        decoder->getStreamTimeBase(),
                        NULL);
                m_i_render_state_cb->RenderOneFrame(this, encodeFrame);
            }
//            EncodeFrame *encodeFrame = new EncodeFrame(
//                    frame->m_frame->data[0],
//                    frame->m_frame->linesize[0],
//                    frame->m_frame->pts,
//                    decoder->getStreamTimeBase(),
//                    NULL);
//            m_i_render_state_cb->RenderOneFrame(this, encodeFrame);
        }

        if (mediaPlayer != NULL) {
            // 获取基准时间
            sync_clock = mediaPlayer->getSyncClock();
//            sync_clock = av_gettime() / 1000000.0 - start_time;
        }

        //获取pts时间
        if ((pts = av_frame_get_best_effort_timestamp(frame->m_frame)) == AV_NOPTS_VALUE) {
            pts = 0;
        }
        if (decoder != NULL) {
            play = pts * av_q2d(decoder->getStreamTimeBase());
        } else {
            play = 0;
        }

        LOGE(TAG, "loopRender(): synchronize");
        // 纠正时间
        play = synchronize(frame->m_frame, play);
        decodeFramePush(frame->m_frame);
        delete frame;
        if (decoder != NULL) {
            decoder->wake();
        }

        delay = play - last_play;
        if (delay <= 0 || delay > 1) {
            delay = last_delay;
        }
        last_delay = delay;
        last_play = play;

        // 7.083492, 7.083333 seek_target0 = 33723 seek_target1 = 1012
        // 同步时钟与视频的时间差
        diff = current_render_clock - sync_clock;
        LOGE(TAG, "loopRender(): %lf, %lf, %lf, %lf, %lf", sync_clock, play, current_render_clock, delay, diff);
        // 可能造成丢帧
        if (diff < -10) {
            continue;
        }

        // 在合理范围外  才会延迟  加快
        sync_threshold = (delay > 0.01 ? 0.01 : delay);
        if (fabs(diff) <= 10) {
            if (diff <= -sync_threshold) {
                delay = 0;
            } else if (diff >= sync_threshold) {
                delay = 2 * delay;
            }
        }
        if (delay > 0) {
            actual_delay = delay;
            if (actual_delay < 0.01) {
                actual_delay = 0.01;
            }
        } else {
            actual_delay = 0;
        }
        LOGE(TAG, "loopRender(): av_usleep %lf", actual_delay);

        if (diff < 0) {
            LOGE(TAG, "loopRender(): render 0");
            // 默认播放器播放过程中可能会崩溃：仅个人猜测是因为渲染太过频；，OpenGL则不会，原因可能是GL渲染是独立与CPU之外
            render();
            LOGE(TAG, "loopRender(): render 1");

            if (actual_delay > 0) {
                // 延时相应时间再渲染到窗口上
                av_usleep(actual_delay * 1000000.0 + 6000);
            }
        } else {
            if (actual_delay > 0) {
                // 延时相应时间再渲染到窗口上
                av_usleep(actual_delay * 1000000.0 + 6000);
                LOGE(TAG, "isRunning(): %d", m_state);
                if (!isRunning()) {
                    break;
                }
            }

            LOGE(TAG, "loopRender(): render 0");
            // 默认播放器播放过程中可能会崩溃：仅个人猜测是因为渲染太过频；，OpenGL则不会，原因可能是GL渲染是独立与CPU之外
            render();
            LOGE(TAG, "loopRender(): render 1");
        }
    }
}

/**
 * 将视频帧数据转换成渲染数据
 */
void BaseVideoRender::scaleFrame(AVFrame *frame) {
    LOGE(TAG, "loopRender(): %s", "scale 0.");
    if (!isRunning() || frame == NULL || decoder == NULL || m_sws_ctx == NULL) {
        return;
    }
    int height = 0;
//    pthread_mutex_lock(&m_state_mutex);
    height = ((VideoDecoder *) decoder)->getVideoHeight();
    LOGE(TAG, "loopRender(): %s", "scale 1.");
    int ret = sws_scale(m_sws_ctx, frame->data, frame->linesize, 0,
                        height, m_rgb_frame->data, m_rgb_frame->linesize);
    LOGE(TAG, "loopRender(): scale 2(%d).", ret);
//    pthread_mutex_unlock(&m_state_mutex);
}

/**
 * 释放视频数据转换相关资源
 */
void BaseVideoRender::releaseReader() {
    if (m_rgb_frame != NULL) {
        av_frame_unref(m_rgb_frame);
        av_frame_free(&m_rgb_frame);
        m_rgb_frame = NULL;
    }
    if (m_buf_for_rgb_frame != NULL) {
        free(m_buf_for_rgb_frame);
        m_buf_for_rgb_frame = NULL;
    }
    if (m_sws_ctx != NULL) {
        sws_freeContext(m_sws_ctx);
        m_sws_ctx = NULL;
    }
}

/**
 * 初始化Render
 * @param env
 */
void BaseVideoRender::prepareSync(JNIEnv *env, Player *mediaPlayer, BaseDecoder *decoder) {
    this->mediaPlayer = mediaPlayer;
    this->decoder = decoder;
    // 获取JVM虚拟机，为创建线程作准备
    env->GetJavaVM(&m_jvm_for_thread);
    // 新建读取视频帧、渲染线程
    prepareSyncAllThread(env);
    LOGE(TAG, "%s", "prepareSync()");
}

/**
 * 判断渲染线程是否可以继续渲染
 * @return
 */
bool BaseVideoRender::isRunning() {
    return !(m_state == IDLE || m_state == STOPPED || m_state == ERROR);
}

/**
 * 音视频时间校准
 * @param frame
 * @param play
 * @return
 */
double BaseVideoRender::synchronize(AVFrame *frame, double render_clock) {
    //clock是当前播放的时间位置
    if (render_clock != 0)
        current_render_clock = render_clock;
    else //pst为0 则先把pts设为上一帧时间
        render_clock = current_render_clock;
    //可能有pts为0 则主动增加clock
    //frame->repeat_pict = 当解码时，这张图片需要要延迟多少
    //需要求出扩展延时：
    //extra_delay = repeat_pict / (2*fps) 显示这样图片需要延迟这么久来显示
    double repeat_pict = frame->repeat_pict;
    //使用AvCodecContext的而不是stream的
    double frame_delay = av_q2d(decoder->getCodecTimeBase());
    //如果time_base是1,25 把1s分成25份，则fps为25
    //fps = 1/(1/25)
    double fps = 1 / frame_delay;
    //pts 加上 这个延迟 是显示时间
    double extra_delay = repeat_pict / (2 * fps);
    double delay = extra_delay + frame_delay;
//    LOGI("extra_delay:%f",extra_delay);
    current_render_clock += delay;
    return render_clock;
}

/**
 * 开始渲染
 */
void BaseVideoRender::start() {
    if (m_state != RUNNING && m_state != STOPPED) {
        m_state = RUNNING;
        LOGE(TAG, "%s", "start()");
        sendRenderFrameSignal();
        onStartRun();
    }
}

/**
 * 暂停渲染
 */
void BaseVideoRender::pause() {
    if (m_state == RUNNING ) {
        m_state = PAUSED;
        LOGE(TAG, "%s", "pause()");
        sendRenderFrameSignal();
        onPauseRun();
    }
}

/**
 * 继续渲染
 */
void BaseVideoRender::resume() {
    if (m_state == PAUSED) {
        m_state = RUNNING;
        sendRenderFrameSignal();
        onResumeRun();
        LOGE(TAG, "%s", "resume()");
    }
}

/**
 * 停止渲染
 */
void BaseVideoRender::stop() {
    LOGE(TAG, "%s", "stop()");
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    onStopRun();
    sendRenderFrameSignal();
}

/**
 * 释放渲染相关资源
 */
void BaseVideoRender::release() {
    LOGE(TAG, "%s", "release()");
    stop();
}