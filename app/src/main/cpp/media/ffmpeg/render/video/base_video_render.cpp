//
// Created by fgrid on 2021/12/17.
//

#include "base_video_render.h"
#include "../../../../utils/logger.h"
#include "../../../media_codes.h"

BaseFFmpegVideoRender::BaseFFmpegVideoRender(
        JNIEnv *jniEnv,
        IFFmpegRenderCallback *iRenderCallback,
        int video_width,
        int video_height,
        AVPixelFormat avPixelFormat,
        AVRational codecTimeBase,
        AVRational streamTimeBase) : BaseFFmpegRender(jniEnv, iRenderCallback) {
    TAG = "BaseFFmpegVideoRender";
    this->video_width =video_width;
    this->video_height = video_height;
    this->m_av_pixel_format = avPixelFormat;
    this->m_codec_time_base.num = codecTimeBase.num;
    this->m_codec_time_base.den = codecTimeBase.den;
    this->m_stream_time_base.num = streamTimeBase.num;
    this->m_stream_time_base.den = streamTimeBase.den;
    this->m_rgb_frame = av_frame_alloc();
}

BaseFFmpegVideoRender::~BaseFFmpegVideoRender() {
    ReleaseReader();
    m_surface_ref = NULL;
    LOGE(TAG, "%s", "~BaseFFmpegVideoRender");
}

/**
 * 初始化渲染窗口
 * @param env
 */
void BaseFFmpegVideoRender::InitANativeWindow(JNIEnv *env) {
    ReleaseANativeWindow();
    // 初始化窗口
    m_native_window = ANativeWindow_fromSurface(env, m_surface_ref);
    if (m_native_window != NULL) {
//        //添加个引用，避免自动释放
//        ANativeWindow_acquire(m_native_window);

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
        LOGD(TAG, "windowW: %d, windowH: %d, dstVideoW: %d, dstVideoH: %d",windowWidth, windowHeight, m_dst_w, m_dst_h)

        //设置宽高限制缓冲区中的像素数量
        ANativeWindow_setBuffersGeometry(m_native_window, windowWidth, windowHeight, WINDOW_FORMAT_RGBA_8888);
    }
}

/**
 * 初始化视频数据转换缓存
 */
void BaseFFmpegVideoRender::InitReaderBuffer() {
    if (m_buf_for_rgb_frame != NULL) {
        free(m_buf_for_rgb_frame);
        m_buf_for_rgb_frame = NULL;
    }
    if (m_dst_w == -1) {
        m_dst_w = video_width;
    }
    if (m_dst_h == -1) {
        m_dst_w = video_height;
    }
    // 获取缓存大小
    int numBytes = av_image_get_buffer_size(DST_FORMAT, m_dst_w, m_dst_h, 1);
    // 分配内存
    m_buf_for_rgb_frame = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    // 将内存分配给RgbFrame，并将内存格式化为三个通道后，分别保存其地址
    av_image_fill_arrays(m_rgb_frame->data, m_rgb_frame->linesize, m_buf_for_rgb_frame, DST_FORMAT, m_dst_w, m_dst_h, 1);
}

/**
 * 初始化格式转换工具
 */
void BaseFFmpegVideoRender::InitSws() {
    if (m_sws_ctx != NULL) {
        sws_freeContext(m_sws_ctx);
        m_sws_ctx = NULL;
    }
    m_sws_ctx = sws_getContext(video_width, video_height, m_av_pixel_format,
                               m_dst_w, m_dst_h, DST_FORMAT,
                               SWS_FAST_BILINEAR, NULL, NULL, NULL);
}

/**
 * 新建读取视频帧、渲染线程等（opengl时需要新增opengl渲染线程，具体可在createOtherThread中自定义处理）
 */
void BaseFFmpegVideoRender::PrepareSyncAllThread(JNIEnv *env) {
    CreateDefaultThread();
    CreateOtherThread(env);
}

/**
 * 新建读取视频帧、渲染总线程
 */
void BaseFFmpegVideoRender::CreateDefaultThread() {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseFFmpegVideoRender> read_that(this);
    std::thread read_thread(RunDefaultThread, read_that);
    read_thread.detach();
}

/**
 * 读取视频帧、渲染总线程调用的方法
 * @param that DefaultVideoRender
 */
void BaseFFmpegVideoRender::RunDefaultThread(std::shared_ptr<BaseFFmpegVideoRender> that) {
    JNIEnv *env;
    //将线程附加到虚拟机，并获取env
    if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
        if (that->m_state != STOPPED && that->m_i_render_callback != NULL) {
            that->m_state = ERROR;
            that->m_i_render_callback->OnRenderError(NULL, MODULE_CODE_VIDEO, VIDEO_RENDER_UNPREPARED, "Fail to init default video render thread");
        }
        return;
    }
    that->jniEnv = env;

    if (that->m_surface_ref != NULL) {
        if (JNI_TRUE != env->IsSameObject(that->m_surface_ref, NULL)) {
            that->SetSurface(env, that->m_surface_ref);
        }
    }

    // 开始循环渲染
    that->LoopRender(env);
    // 结束循环渲染
    that->DoneRender();
    av_usleep(20*1000);

    //解除线程和jvm关联
    that->m_jvm_for_thread->DetachCurrentThread();
    LOGE(that->TAG, "%s", "render thread done.");
}

/**
 * 开始循环渲染
 */
void BaseFFmpegVideoRender::LoopRender(JNIEnv *env) {
    if (m_state == IDLE) {
        m_state = PREPARED;
        m_i_render_callback->OnRenderPrepared(env, MODULE_CODE_VIDEO);
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
    LOGE(TAG, "LoopRender(): Loop.");
    while (IsRunning()) {
        if (isFrameEOS && m_frame_queue.empty() && m_state != COMPLETED) {
            m_state = COMPLETED;
            m_i_render_callback->OnRenderCompleted(env, MODULE_CODE_VIDEO);
        }
        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
            ) {
            WaitState();
            LOGE(TAG, "LoopRender(): %s", "wake up.");
        }

        if (!IsRunning()) {
            break;
        }
        if (m_state == PREPARED // 设置解码器准备
            || m_state == PAUSED // 设置解码器暂停（一般当Activity处于后台时，设置此状态）
            || m_state == COMPLETED // 设置解码器已完成（解码器已全部解码时，设置此状态）
                ) {
            continue;
        }

        //从缓存中获取数据
        AVFrame *frame = PopFrame();
        LOGE(TAG, "PopFrame() Ok")
        if (frame == NULL) {
            continue;
        }
        // 可能需要等待，等待返回后可能已经不在播放了，所以再次判断isRunning
        if (!IsRunning()) {
            av_frame_free(&frame);
            break;
        }

        // 将视频帧数据转换成渲染数据
        ScaleFrame(frame);

        if (!IsRunning()) {
            av_frame_free(&frame);
            break;
        }
        // 获取基准时间
        sync_clock = m_i_render_callback->GetSyncClock();

        //获取pts时间
        if ((pts = av_frame_get_best_effort_timestamp(frame)) == AV_NOPTS_VALUE) {
            pts = 0;
        }
        play = pts * av_q2d(m_stream_time_base);
        // 纠正时间
        play = Synchronize(frame, play);
        av_frame_free(&frame);

        if (IsRunning()) {
            m_i_render_callback->UpdateSyncClock(play);
        }

        delay = play - last_play;
        if (delay <= 0 || delay > 1) {
            delay = last_delay;
        }
        last_delay = delay;
        last_play = play;

        // 7.083492, 7.083333 seek_target0 = 33723 seek_target1 = 1012
        // 同步时钟与视频的时间差
        diff = m_current_timestamp - sync_clock;
//        LOGD(TAG, "LoopRender(): %lf, %lf, %lf, %lf, %lf", sync_clock, play, m_current_timestamp, delay, diff);
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

        // 默认播放器播放过程中可能会崩溃：仅个人猜测是因为ANativeWindow渲染引起
        if (diff < 0) {
            if (!IsRunning()) {
                break;
            }
            LOGE(TAG, "Render() Ok")
            Render();
            if (actual_delay > 0) {
                av_usleep(actual_delay * 1000000.0 + 6000);
            }
        } else {
            if (actual_delay > 0) {
                av_usleep(actual_delay * 1000000.0 + 6000);
            }
            if (!IsRunning()) {
                break;
            }
            LOGE(TAG, "Render() Ok")
            Render();
        }
//        if (actual_delay > 0) {
//            actual_delay = actual_delay * 1000000.0 + 6000;
//        } else {
//            actual_delay = 6000;
//        }
//        if (diff < 0) {
//            if (!IsRunning()) {
//                break;
//            }
//            Render();
//            av_usleep(actual_delay);
//        } else {
//            av_usleep(actual_delay);
//            if (!IsRunning()) {
//                break;
//            }
//            Render();
//        }
    }
}

/**
 * 将视频帧数据转换成渲染数据
 */
void BaseFFmpegVideoRender::ScaleFrame(AVFrame *frame) {
    if (!IsRunning() || frame == NULL || m_sws_ctx == NULL || m_rgb_frame == NULL) {
        return;
    }
//    pthread_mutex_lock(&m_state_mutex);
//    int ret = sws_scale(m_sws_ctx, frame->data, frame->linesize, 0,
//                        video_height, m_rgb_frame->data, m_rgb_frame->linesize);
//    LOGD(TAG, "sws_scale(): %d", ret);
    sws_scale(m_sws_ctx, frame->data, frame->linesize, 0,
                        video_height, m_rgb_frame->data, m_rgb_frame->linesize);
//    pthread_mutex_unlock(&m_state_mutex);
}

/**
 * 释放视频数据转换相关资源
 */
void BaseFFmpegVideoRender::ReleaseReader() {
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
 * 音视频时间校准
 * @param frame
 * @param play
 * @return
 */
double BaseFFmpegVideoRender::Synchronize(AVFrame *frame, double render_clock) {
    //clock是当前播放的时间位置
    if (render_clock != 0)
        m_current_timestamp = render_clock;
    else //pst为0 则先把pts设为上一帧时间
        render_clock = m_current_timestamp;
    //可能有pts为0 则主动增加clock
    //frame->repeat_pict = 当解码时，这张图片需要要延迟多少
    //需要求出扩展延时：
    //extra_delay = repeat_pict / (2*fps) 显示这样图片需要延迟这么久来显示
    double repeat_pict = frame->repeat_pict;
    //使用AvCodecContext的而不是stream的
    double frame_delay = av_q2d(m_codec_time_base);
    //如果time_base是1,25 把1s分成25份，则fps为25
    //fps = 1/(1/25)
    double fps = 1 / frame_delay;
    //pts 加上 这个延迟 是显示时间
    double extra_delay = repeat_pict / (2 * fps);
    double delay = extra_delay + frame_delay;
//    LOGI("extra_delay:%f",extra_delay);
    m_current_timestamp += delay;
    return m_current_timestamp;
}

void BaseFFmpegVideoRender::Start() {
    LOGD(TAG, "%s", "Start()");
    if (m_state == PREPARED || m_state == PAUSED) {
        m_state = RUNNING;
        WakeUpState();
        OnStartRun();
    }
}

void BaseFFmpegVideoRender::Pause() {
    LOGD(TAG, "%s", "Pause()");
    if (m_state == RUNNING) {
        m_state = PAUSED;
        WakeUpState();
        OnStartRun();
    }
}

void BaseFFmpegVideoRender::Resume() {
    LOGD(TAG, "%s", "Resume()");
    if (m_state == PAUSED) {
        m_state = RUNNING;
        WakeUpState();
        OnResumeRun();
    }
}

void BaseFFmpegVideoRender::Stop() {
    LOGD(TAG, "%s", "Stop()");
    if (m_state != STOPPED) {
        m_state = STOPPED;
    }
    OnStopRun();
    WakeUpState();
    WakeUpFrameQueue();
}

void BaseFFmpegVideoRender::SeekTo(int64_t timestamp) {
    if (m_state != STOPPED && m_state != ERROR) {
        m_state = PAUSED;
        isFrameEOS = false;
        ClearFrameQueue();
    }
}