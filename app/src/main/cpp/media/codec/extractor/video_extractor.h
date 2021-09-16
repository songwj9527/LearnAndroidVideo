//
// Created by fgrid on 2021/9/15.
//

#ifndef OPENVIDEO_VIDEO_EXTRACTOR_H
#define OPENVIDEO_VIDEO_EXTRACTOR_H

#include "i_extractor.h"

class VideoExtractor : public IExtractor {
private:
    const char *TAG = "VideoExtractor";

    /**
     * 视频宽、高、旋转角度
     */
    int32_t m_video_width = -1;
    int32_t m_video_height = -1;
    int32_t m_video_rotation = -1;
    int32_t m_video_fps = -1;

    /**
     * 查找这个时间点对应的最接近的一帧。
     * seekTo指定位置播放不准（主要因为指定的位置可能不是I帧，所以最终效果可能是指定位置后，开始播放的位置在指定位置之前）
     * 该方法通过SEEK_TO_PREVIOUS_SYNC参数seek到指定位置的前一个I帧，然后向后推进，比较帧时间：
     * 这一帧的时间点如果和目标时间相差不到 一帧间隔 就算相近
     * @param timestamp （单位：microseconds微妙）
     * @param max_range  查找范围
     * @return
     */
    int64_t GetValidSampleTime(int64_t timestamp, int max_range);


protected:
    char * GetMineTypeFlag() override {
        return "video/";
    }

    void InitVirtual() override {
        GetVideoWidth();
        GetVideoHeight();
        GetVideoRotation();
        GetVideoFps();
    }

    void StopVirtual() override {
        m_video_width = -1;
        m_video_height = -1;
        m_video_rotation = -1;
        m_video_fps = -1;
    }

public:
    VideoExtractor();
    ~VideoExtractor();

    /**
     * 获取视频宽
     * @return
     */
    int32_t GetVideoWidth();

    /**
     * 获取视频高
     * @return
     */
    int32_t GetVideoHeight();

    /**
     * 获取视频旋转角度
     * @return
     */
    int32_t GetVideoRotation();

    /**
     * 获取视频显示频率fps
     * @return
     */
    int32_t GetVideoFps();

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     * @param position  seek位置（单位：microseconds微妙）
     * @return
     */
    int64_t SeekTo(int64_t position) override;
};

#endif //OPENVIDEO_VIDEO_EXTRACTOR_H
