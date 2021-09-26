//
// Created by fgrid on 2021/9/18.
//

#include "base_track.h"
#include "../../../utils/logger.h"

BaseTrack::BaseTrack(JNIEnv *jniEnv, const char *source, ITrackCallback *i_track_callback) {
    this->m_source = source;
    this->m_i_track_callback = i_track_callback;

    // 初始化条件锁
    pthread_mutex_init(&m_state_mutex, NULL);
    pthread_cond_init(&m_state_cond, NULL);

    // 获取JVM虚拟机，为创建线程作准备
    jniEnv->GetJavaVM(&m_jvm_for_thread);
    // 新建解码线程
    CreateTrackThread(jniEnv);
}

BaseTrack::~BaseTrack() {
    LOGE(TAG, "~BaseTrack()")
    // 释放锁
    pthread_cond_destroy(&m_state_cond);
    pthread_mutex_destroy(&m_state_mutex);
}

void BaseTrack::CreateTrackThread(JNIEnv *env) {
    // 使用智能指针，线程结束时，自动删除本类指针
    std::shared_ptr<BaseTrack> that(this);
    std::thread th(RunTrackThread, that);
    th.detach();
}

void BaseTrack::RunTrackThread(std::shared_ptr<BaseTrack> that) {

}

void BaseTrack::Init(JNIEnv *env) {

}