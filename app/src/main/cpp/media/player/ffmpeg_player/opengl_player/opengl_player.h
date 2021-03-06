//
// Created by fgrid on 2021/7/20.
//

#ifndef OPENVIDEO_OPENGL_PLAYER_H
#define OPENVIDEO_OPENGL_PLAYER_H

#include "../ffmpeg_player.h"

class OpenGLPlayer : public FFmpegPlayer {
private:
    const char *TAG = "OpenGLPlayer";
    bool defaultDraw = true;

public:

    OpenGLPlayer(JNIEnv *jniEnv, jobject object, bool defaultDraw);
    ~OpenGLPlayer();

    void prepareSync() override;
};
#endif //OPENVIDEO_OPENGL_PLAYER_H
