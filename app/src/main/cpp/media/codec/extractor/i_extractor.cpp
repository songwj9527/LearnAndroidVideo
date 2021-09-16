//
// Created by fgrid on 2021/9/15.
//

#include <string.h>

#include "i_extractor.h"
#include "../../utils/logger.h"

IExtractor::IExtractor() {
    memset((void *) m_mine, 0, sizeof(char) * 200);
}

IExtractor::~IExtractor() {
    LOGE(TAG, "~IExtractor()")
    Stop();
}

void IExtractor::Init() {
    if (m_extractor != NULL) {
        size_t track_count = AMediaExtractor_getTrackCount(m_extractor);
        for (int i = 0; i < track_count; i++) {
            AMediaFormat *format = AMediaExtractor_getTrackFormat(m_extractor, i);
            if (format != NULL) {
                memset((void *) m_mine, 0, sizeof(char) * 200);
                AMediaFormat_getString(format, "mine", reinterpret_cast<const char **>(&m_mine));
                if (strlen(m_mine) > 0) {
                    char *flag = GetMineTypeFlag();
                    if (strncmp(m_mine, flag, strlen(flag)) == 0) {
                        LOGE(TAG, "IsSoftwareCodec(): %s, %s, %s", IsSoftwareCodec(m_mine) ? "true" : "false", m_mine, flag)
                        m_track = i;
                        GetFormatDurationUs();
                        InitVirtual();
                        return;
                    }
                }
            }
            memset((void *) m_mine, 0, sizeof(char) * 200);
        }
    }
}

bool IExtractor::IsSoftwareCodec(const char *component_name) {
    if (!strncasecmp("OMX.google", component_name, 11)) {
        return true;
    }
    if (!strncasecmp("OMX.", component_name, 4)) {
        return false;
    }
    return true;
}

media_status_t IExtractor::SetDataSource(const char *source) {
    media_status_t status = AMEDIA_ERROR_BASE;
    Stop();
    m_extractor = AMediaExtractor_new();
    if (m_extractor != NULL) {
        status = AMediaExtractor_setDataSource(m_extractor, source);
        if (status == AMEDIA_OK) {
            Init();
        }
    }
    return status;
}

media_status_t IExtractor::SetDataSource(int fd, off64_t offset, off64_t length) {
    media_status_t status = AMEDIA_ERROR_BASE;
    Stop();
    m_extractor = AMediaExtractor_new();
    if (m_extractor != NULL) {
        status = AMediaExtractor_setDataSourceFd(m_extractor, fd, offset, length);
        if (status == AMEDIA_OK) {
            Init();
        }
    }
    return status;
}

AMediaFormat * IExtractor::GetMediaFormat() {
    if (m_extractor != NULL && m_track != -1) {
        return AMediaExtractor_getTrackFormat(m_extractor, m_track);
    }
    return NULL;
}

const char * IExtractor::GetFormatMineType() {
    if (m_extractor != NULL && m_track != -1 && strlen(m_mine) > 0) {
        return m_mine;
    }
    return NULL;
}

int64_t IExtractor::GetFormatDurationUs() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_duration < 0) {
            m_duration = -1;
            AMediaFormat *format = AMediaExtractor_getTrackFormat(m_extractor, m_track);
            if (format != NULL) {
                int64_t durationUs;
                if (AMediaFormat_getInt64(format, "durationUs", &durationUs)) {
                    m_duration = durationUs;
                    if (m_duration < 0) {
                        m_duration = -1;
                    }
                }
            }
        }
    }
    return (m_duration < 0) ? 0 : m_duration;
}

int64_t IExtractor::GetCurrentTimestamp() {
    if (m_extractor != NULL) {
        m_cur_sample_time = AMediaExtractor_getSampleTime(m_extractor);
    } else {
        m_cur_sample_time = -1;
    }
    return m_cur_sample_time;
}

int IExtractor::GetSampleFlag() {
    if (m_extractor != NULL) {
        m_cur_sample_flag = AMediaExtractor_getSampleFlags(m_extractor);
    } else {
        m_cur_sample_flag = -1;
    }
    return m_cur_sample_flag;
}

ssize_t IExtractor::ReadBuffer(uint8_t *buffer) {
    if (m_extractor != NULL && m_track != -1) {
        AMediaExtractor_selectTrack(m_extractor, m_track);
        ssize_t read_sample_count = AMediaExtractor_readSampleData(m_extractor, buffer, 0);
        if (read_sample_count < 0) {
            return 0;
        }
        // 记录当前帧的时间戳
        m_cur_sample_time = AMediaExtractor_getSampleTime(m_extractor);
        // 记录当前帧的标志
        m_cur_sample_flag = AMediaExtractor_getSampleFlags(m_extractor);
        //进入下一帧
        AMediaExtractor_advance(m_extractor);
        return read_sample_count;
    }
    return 0;
}

void IExtractor::Stop() {
    if (m_extractor != NULL) {
        AMediaExtractor_delete(m_extractor);
        m_extractor = NULL;
        m_track = -1;
        memset((void *) m_mine, 0, sizeof(char) * 200);
        m_duration = -1;
        m_cur_sample_time = -1;
        m_cur_sample_flag = -1;

        StopVirtual();
    }
}