//
// Created by fgrid on 1/25/21.
//

#ifndef OPENVIDEO_MEDIA_PLAYER_H
#define OPENVIDEO_MEDIA_PLAYER_H

#include "../player.h"


class MediaPlayer : public Player {
private:
    const char *TAG = "MediaPlayer";

public:

    MediaPlayer(JNIEnv *jniEnv, jobject object);
    ~MediaPlayer();

    void prepareSync() override;
};
#endif //OPENVIDEO_MEDIA_PLAYER_H
