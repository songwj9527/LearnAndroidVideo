//
// Created by fgrid on 2021/9/15.
//

#include <string.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <malloc.h>

#include "base_extractor.h"
#include "../../utils/logger.h"

BaseExtractor::BaseExtractor() {

}

BaseExtractor::~BaseExtractor() {
    LOGE(TAG, "~BaseExtractor()")
    Stop();
}

void BaseExtractor::Init() {
    LOGE(TAG, "Init()")
    if (m_extractor != NULL) {
        size_t track_count = AMediaExtractor_getTrackCount(m_extractor);
        for (int i = 0; i < track_count; i++) {
            AMediaFormat *format = AMediaExtractor_getTrackFormat(m_extractor, i);
            if (format != NULL) {
                bool get_mine = AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, m_mine_out);
                if (get_mine) {
                    int length = strlen(m_mine_out[0]);
                    if (length > 0) {
                        const char *flag = GetMineTypeFlag();
//                    char judge[20] = {0};
//                    memset(judge, 0, sizeof(char) * 20);
//                    memcpy(judge, m_mine, strlen(flag));
                        int result = strncmp(m_mine_out[0], flag, strlen(flag));
                        if (result == 0) {
                            m_mine = static_cast<char *>(malloc(sizeof(char) * (length +1)));
                            strcpy(m_mine, m_mine_out[0]);
                            LOGE(TAG, "IsSoftwareCodec(): %s, %s, %s", IsSoftwareCodec(m_mine) ? "true" : "false", m_mine, flag)
                            m_track = i;
                            m_format = format;
                            GetFormatDurationUs();
                            GetMaxInputSize();
                            InitVirtual();
                            media_status_t select = AMediaExtractor_selectTrack(m_extractor, m_track);
                            LOGE(TAG, "select: %d", select)
//                            media_status_t seek = AMediaExtractor_seekTo(m_extractor, 0, AMEDIAEXTRACTOR_SEEK_PREVIOUS_SYNC);
//                            LOGE(TAG, "seek to set(): %d", seek)
//                            bool advance = AMediaExtractor_advance(m_extractor);
//                            LOGE(TAG, "advance(): %d", advance)
                            return;
                        }
                    }
                }
            }
        }
    }
}

bool BaseExtractor::IsSoftwareCodec(const char *component_name) {
    if (!strncasecmp("OMX.google", component_name, 11)) {
        return true;
    }
    if (!strncasecmp("OMX.", component_name, 4)) {
        return false;
    }
    return true;
}

media_status_t BaseExtractor::SetDataSource(const char *source) {
    media_status_t status = AMEDIA_ERROR_BASE;
    Stop();
    char http[6] = {0};
    char https[7] = {0};
    memset(http, 0, 6);
    memset(https, 0, 7);
    int length = strlen(source);
    if (length > 5) {
        memcpy(http, source, 5);
    } else {
        strcpy(http, source);
    }
    if (length > 6) {
        memcpy(https, source, 6);
    } else {
        strcpy(https, source);
    }
    if (strcasecmp("http:", http) == 0 || strcasecmp("https:", https) == 0) {
        LOGE(TAG, "SetDataSourceHttp()")
        m_extractor = AMediaExtractor_new();
        if (m_extractor != NULL) {
            status = AMediaExtractor_setDataSource(m_extractor, source);
            LOGE(TAG, "SetDataSourceHttp() %d", status)
            if (status == AMEDIA_OK) {
                Init();
            } else {
                AMediaExtractor_delete(m_extractor);
                m_extractor = NULL;
            }
        }
        return status;
    }
    LOGE(TAG, "SetDataSourceFd()")
    m_media_fd = open(source, O_RDWR);
    if (m_media_fd > 0) {
        struct stat info;
        fstat(m_media_fd, &info);
        m_extractor = AMediaExtractor_new();
        if (m_extractor != NULL) {
            status = AMediaExtractor_setDataSourceFd(m_extractor, m_media_fd, 0, info.st_size);
            LOGE(TAG, "SetDataSourceFd(): %d", status)
            if (status == AMEDIA_OK) {
                Init();
            } else {
                close(m_media_fd);
                m_media_fd = -1;
                AMediaExtractor_delete(m_extractor);
                m_extractor = NULL;
            }
        } else {
            close(m_media_fd);
            m_media_fd = -1;
        }
        return status;
    }
    return status;
}

media_status_t BaseExtractor::SetDataSource(int fd, off64_t offset, off64_t length) {
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

AMediaFormat * BaseExtractor::GetMediaFormat() {
    if (m_extractor != NULL && m_track != -1) {
        return m_format;
//        return AMediaExtractor_getTrackFormat(m_extractor, m_track);
    }
    return NULL;
}

int32_t BaseExtractor::GetMaxInputSize() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_max_input_size < 0) {
            m_max_input_size = -1;
            if (m_format != NULL) {
                int64_t max_input_size;
                if (AMediaFormat_getInt64(m_format, AMEDIAFORMAT_KEY_MAX_INPUT_SIZE, &max_input_size)) {
                    m_max_input_size = max_input_size;
                    if (m_max_input_size < 0) {
                        m_max_input_size = -1;
                        return 0;
                    } else {
                        return m_max_input_size;
                    }
                }
            }
        }
    }
    return 0;
}

const char * BaseExtractor::GetFormatMineType() {
    if (m_extractor != NULL && m_track != -1 && strlen(m_mine) > 0) {
        return m_mine;
    }
    return NULL;
}

int64_t BaseExtractor::GetFormatDurationUs() {
    if (m_extractor != NULL && m_track != -1) {
        if (m_duration < 0) {
            m_duration = -1;
            if (m_format != NULL) {
                int64_t durationUs;
                if (AMediaFormat_getInt64(m_format, AMEDIAFORMAT_KEY_DURATION, &durationUs)) {
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

int64_t BaseExtractor::GetCurrentTimestampUs() {
    if (m_extractor != NULL) {
        m_cur_sample_time = AMediaExtractor_getSampleTime(m_extractor);
    } else {
        m_cur_sample_time = -1;
    }
    return m_cur_sample_time;
}

int BaseExtractor::GetSampleFlag() {
    if (m_extractor != NULL) {
        m_cur_sample_flag = AMediaExtractor_getSampleFlags(m_extractor);
    } else {
        m_cur_sample_flag = -1;
    }
    return m_cur_sample_flag;
}

ssize_t BaseExtractor::ReadBuffer(uint8_t *buffer) {
    if (m_extractor != NULL && m_track != -1) {
        media_status_t select = AMediaExtractor_selectTrack(m_extractor, m_track);
        LOGE(TAG, "select: %d", select)
        ssize_t read_sample_count = AMediaExtractor_readSampleData(m_extractor, buffer, 0);
        LOGE(TAG, "ReadBuffer() %d", read_sample_count)
        if (read_sample_count < 0) {
            return -1;
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

void BaseExtractor::Stop() {
    LOGE(TAG, "Stop()")
    if (m_extractor != NULL) {
        AMediaExtractor_delete(m_extractor);
        m_extractor = NULL;
        if (m_media_fd > 0) {
            close(m_media_fd);
            m_media_fd = -1;
        }
        if (m_mine != NULL) {
            free(m_mine);
        }
        m_track = -1;
        m_format = NULL;
        m_duration = -1;
        m_cur_sample_time = -1;
        m_cur_sample_flag = -1;
        StopVirtual();
    }
}