//
// Created by fgrid on 2021/9/29.
//

#ifndef OPENVIDEO_CODEC_DEFAULT_CODEC_PLAYER_H
#define OPENVIDEO_CODEC_DEFAULT_CODEC_PLAYER_H

#include "codec_player.h"

class DefaultCodecPlayer : public CodecPlayer {
private:
    const char *TAG = "DefaultCodecPlayer";
public:
    DefaultCodecPlayer(JNIEnv *jniEnv, jobject object);
    ~DefaultCodecPlayer();

    void prepareSync() override;
};
#endif //OPENVIDEO_DEFAULT_CODEC_PLAYER_H
