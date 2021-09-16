//
// Created by fgrid on 2021/9/15.
//

#ifndef OPENVIDEO_I_EXTRACTOR_H
#define OPENVIDEO_I_EXTRACTOR_H

#include <media/NdkMediaExtractor.h>

class IExtractor {
private:
    const char *TAG = "IExtractor";

    /**
     * 初始化音视频信道、获取时长等参数
     * @return
     */
    void Init();


protected:
    /**音视频分离器*/
    AMediaExtractor *m_extractor = NULL;

    /**音视频解码器类型**/
    const char m_mine[200] = {0};

    /**音视频频通道索引**/
    int m_track = -1;

    /**音视频总时长**/
    int64_t m_duration = -1;

    /**当前帧时间戳*/
    int64_t m_cur_sample_time = -1;

    /**当前帧标志*/
    int m_cur_sample_flag = 0;

    /**
     * 获取编解码器类型校验标识：音频，"audio/"；视频，"video/"
     * @return
     */
    virtual char *GetMineTypeFlag() = 0;

    /**
     * 是否为软编解码器
     * @return
     */
    bool IsSoftwareCodec(const char *component_name);

    /**
     * Init时，子类做自己独有的Init操作
     */
    virtual void InitVirtual() = 0;

    /**
     * Stop时，子类做自己独有的清理工作
     */
    virtual void StopVirtual() = 0;


public:
    IExtractor();
    ~IExtractor();

    /**
     * 加载视频源
     * @param source    视频源地址
     * @return
     */
    media_status_t SetDataSource(const char *source);

    /**
     * 加载视频源
     * @param fd        视频源文件描述符
     * @param offset    源文件起始加载位置
     * @param length    源文件加载长度
     * @return
     */
    media_status_t SetDataSource(int fd, off64_t offset, off64_t length);

    /**
     * 音视频解码器类型
     * @return
     */
    const char *GetFormatMineType();

    /**
     * 获取音视频总时长（单位：microseconds微妙）
     * @return
     */
    int64_t GetFormatDurationUs();

    /**
     * 获取音视频对应的格式参数
     * @return
     */
    AMediaFormat *GetMediaFormat();

    /**
     * 获取当前帧时间
     * @return
     */
    int64_t GetCurrentTimestamp();

    /**
     * 获取当前帧标志
     * @return
     */
    int GetSampleFlag();

    /**
     * 读取音视频数据
     * @param buffer
     * @param capacity
     * @return
     */
    ssize_t ReadBuffer(uint8_t *buffer);

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     * @param position  seek位置（单位：microseconds微妙）
     * @return
     */
    virtual int64_t SeekTo(int64_t position) = 0;

    /**
     * 停止读取数据
     */
    void Stop();
};

#endif //OPENVIDEO_I_EXTRACTOR_H
