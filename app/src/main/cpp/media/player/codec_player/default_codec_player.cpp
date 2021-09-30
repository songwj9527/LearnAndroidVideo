//
// Created by fgrid on 2021/9/29.
//

#include "default_codec_player.h"
#include "../../codec/track/default_video_track.h"

DefaultCodecPlayer::DefaultCodecPlayer(JNIEnv *jniEnv, jobject object) : CodecPlayer(jniEnv, object){}

DefaultCodecPlayer::~DefaultCodecPlayer() {

}

void DefaultCodecPlayer::prepareSync() {
    release();
    if (jniEnv == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    if (sourceURL == NULL) {
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    audio_track = new AudioTrack(jniEnv, sourceURL, this);
    is_video_prepared = true;
    is_video_completed = true;
    is_video_seek_completed  =true;
//    video_track = new DefaultVideoTrack(jniEnv, sourceURL, this);
    LOGE(TAG, "%s", "prepareSync()");
}