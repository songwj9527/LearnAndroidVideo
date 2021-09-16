//
// Created by fgrid on 2021/9/15.
//
#include <math.h>

#include "video_extractor.h"
#include "../../utils/logger.h"

VideoExtractor::VideoExtractor() {

}

VideoExtractor::~VideoExtractor() {
    LOGE(TAG, "~VideoExtractor()")
}

int32_t VideoExtractor::GetVideoWidth() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_video_width == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "width", &m_video_width);
                if (m_video_width < 0) {
                    m_video_width = -1;
                }
            }
        }
        return m_video_width;
    }
    return  0;
}

int32_t VideoExtractor::GetVideoHeight() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_video_height == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "height", &m_video_height);
                if (m_video_height < 0) {
                    m_video_height = -1;
                }
            }
        }
        return m_video_height;
    }
    return  0;
}

int32_t VideoExtractor::GetVideoRotation() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_video_rotation == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "rotation-degrees", &m_video_rotation);
                if (m_video_rotation < 0) {
                    m_video_rotation = -1;
                }
            }
        }
        return m_video_rotation;
    }
    return  0;
}

int32_t VideoExtractor::GetVideoFps() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_video_fps == -1) {
            AMediaFormat *format = GetMediaFormat();
            if (format != NULL) {
                AMediaFormat_getInt32(format, "frame-rate", &m_video_fps);
                if (m_video_fps < 0) {
                    m_video_fps = -1;
                }
            }
        }
        return m_video_fps;
    }
    return  0;
}

/**
 * 查找这个时间点对应的最接近的一帧。
 * seekTo指定位置播放不准（主要因为指定的位置可能不是I帧，所以最终效果可能是指定位置后，开始播放的位置在指定位置之前）
 * 该方法通过SEEK_TO_PREVIOUS_SYNC参数seek到指定位置的前一个I帧，然后向后推进，比较帧时间：
 * 这一帧的时间点如果和目标时间相差不到 一帧间隔 就算相近
 * @param timestamp （单位：microseconds微妙）
 * @param max_range  查找范围
 * @return
 */
int64_t VideoExtractor::GetValidSampleTime(int64_t timestamp, int max_range) {
    if (m_extractor != NULL && m_track != -1) {
        int32_t fps = GetVideoFps();
        if (fps <= 0) {
            return timestamp;
        }
        int64_t one_second = ((int64_t) 1000000L);
        // 根据fps得到一帧视频的时间（单位：微妙）
        int64_t per_time = (one_second / fps);
        AMediaExtractor_seekTo(m_extractor, timestamp, AMEDIAEXTRACTOR_SEEK_PREVIOUS_SYNC);
        int count = 0;
        int64_t current_sample_time = AMediaExtractor_getSampleTime(m_extractor);
        int64_t prev_sample_time = -1;
        int64_t next_sample_time = -1;
        while (count < max_range) {
            AMediaExtractor_advance(m_extractor);
            next_sample_time = AMediaExtractor_getSampleTime(m_extractor);
            if (next_sample_time != -1) {
                count++;
                prev_sample_time = current_sample_time;
                if ((current_sample_time > next_sample_time)) {
                    current_sample_time = next_sample_time;
                }
                int64_t cur_abs = abs(current_sample_time - timestamp);
                int64_t pre_abs = abs(prev_sample_time - timestamp);
                // 选取和目标时间差值最小的那个
                // 如果这个差值在 一帧间隔 内，即为成功; 或者前一个时间比现在的更接近，则无需再往下进行了
                if (cur_abs <= per_time || pre_abs < cur_abs) {
                    return current_sample_time;
                }
            } else {
                count = max_range;
            }
        }
        return (current_sample_time != -1) ? current_sample_time : timestamp;
    }
    return timestamp;
}

/**
 * Seek到指定位置，并返回实际帧的时间戳
 * @param position  seek位置（单位：microseconds微妙）
 * @return
 */
int64_t VideoExtractor::SeekTo(int64_t position) {
    /**
     * seekTo指定位置播放不准（主要因为指定的位置可能不是I帧，所以最终效果可能是指定位置后，开始播放的位置在指定位置之前）
     * 调用getValidSampleTime()来获取可以达到指定位置效果的位置值validTimestamp，
     * 再执行seekTo(validTimestamp, SEEK_TO_CLOSEST_SYNC)，来实现指定位置播放。
     */
    if (m_extractor != NULL) {
        int64_t valid_sample_time = GetValidSampleTime(position, 20);
        AMediaExtractor_seekTo(m_extractor, valid_sample_time, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    }
}