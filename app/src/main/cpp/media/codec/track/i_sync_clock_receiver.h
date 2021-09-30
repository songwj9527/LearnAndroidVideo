//
// Created by fgrid on 2021/9/28.
//

#ifndef OPENVIDEO_CODEC_I_SYNC_CLOCK_RECEIVER_H
#define OPENVIDEO_CODEC_I_SYNC_CLOCK_RECEIVER_H

#include <stdint.h>

class ISyncClockReceiver {
public:
    virtual int64_t GetSyncClock() = 0;
    virtual void SetSyncClock(int64_t clock) = 0;
};

#endif //OPENVIDEO_CODEC_I_SYNC_CLOCK_RECEIVER_H
