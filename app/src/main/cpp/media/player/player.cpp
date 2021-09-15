//
// Created by fgrid on 2021/7/20.
//

#include "player.h"

Player::Player(JNIEnv *jniEnv, jobject object) {
    this->jniEnv = jniEnv;
    this->jplayer = jniEnv->NewWeakGlobalRef(object);
}

Player::~Player() {
    // 此处不需要 delete 成员指针
    // 在BaseDecoder中的线程已经使用智能指针，会自动释放
    LOGE(TAG, "%s", "~Player");
    releaseSourceURL();
    releaseJPlayer();
}

/**
 * 释放java层player对象的弱应用
 */
void Player::releaseJPlayer() {
    if (jniEnv == NULL) {
        return;
    }
    if (jplayer != NULL) {
        if (!jniEnv->IsSameObject(jplayer, NULL)) {
            jniEnv->DeleteWeakGlobalRef(jplayer);
        }
        jplayer = NULL;
    }
    if (jsurface != NULL) {
        if (!jniEnv->IsSameObject(jsurface, NULL)) {
            jniEnv->DeleteGlobalRef(jsurface);
        }
        jsurface = NULL;
    }
}

/**
 * 释放source url相关资源
 */
void Player::releaseSourceURL() {
    if (jniEnv == NULL) {
        return;
    }
    // 释放转换参数
    if (jsourceURL != NULL && sourceURL != NULL) {
        if (!jniEnv->IsSameObject(jsourceURL, NULL)) {
            jniEnv->ReleaseStringUTFChars((jstring) jsourceURL, sourceURL);
            jniEnv->DeleteGlobalRef(jsourceURL);
        }
    }
    sourceURL = NULL;
    jsourceURL = NULL;
}

/**
 * 准备播放（初始化解码器等）
 * @param url
 */
void Player::setDataSource(jstring url) {
    if (jniEnv == NULL) {
        return;
    }
    releaseSourceURL();
    jsourceURL = jniEnv->NewGlobalRef(url);
    const char *urlChar = jniEnv->GetStringUTFChars(url, NULL);
    if (urlChar == NULL) {
        sourceURL = NULL;
        onError(jniEnv, JAVA_PATH_2_C_CHARS_FAILED, "传入的url转chars失败");
        return;
    }
    if (strlen(urlChar) == 0) {
        sourceURL = NULL;
        onError(jniEnv, MEDIA_SOURCE_URL_ERROR, "传入的url为空");
        return;
    }
    sourceURL = urlChar;
    LOGE(TAG, "setDataSource(): %s", sourceURL);
}

/**
 * 异常或错误调用方法
 * @param errorCode
 * @param errorMsg
 */
void Player::onError(JNIEnv *env, int errorCode, const char *errorMsg) {
    if (env == NULL) {
        env = jniEnv;
    }
    jstring jerrorMsg;
    if (errorMsg != NULL && strlen(errorMsg)) {
        jerrorMsg = env->NewStringUTF(errorMsg);
    }
    onPostEventToJava(env, CALLBACK_ERROR, errorCode, 0, jerrorMsg);
}

/**
 * 给Java层回调方法
 * @param event
 * @param what
 * @param args1
 * @param args2
 */
void Player::onPostEventToJava(JNIEnv *env, int event, int what, int args1, jobject args2) {
    LOGE(TAG, "onPostEventToJava 0: %d, %d, %d", event, what, args1);
    if (env == NULL) {
        env = jniEnv;
    }
    //检查是否发送异常：env->ExceptionOccurred()检查当前是否异常
    // 或(*env)->ExceptionCheck(env)
    jthrowable ex = NULL;
    jclass clazz = NULL;
    if (jplayer != NULL) {
        if (JNI_TRUE != env->IsSameObject(jplayer, NULL)) {
            clazz = env->GetObjectClass(jplayer);
        } else {
            return;
        }
    } else {
//        clazz = jniEnv->FindClass("com/songwj/openvideo/ffmpeg/NativePlayer");
        return;
    }
    LOGE(TAG, "onPostEventToJava %d:", 1);
    ex = env->ExceptionOccurred();
    if (ex != NULL && clazz == NULL) {
        //让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();

        jclass newExc = env->FindClass("java/lang/IllegalArgumentException");
        if (newExc == NULL) {
            return;
        }
        env->ThrowNew(newExc, "Throw exception from JNI: FindClass for NativePlayer failed");
        return;
    } else if (clazz == NULL) {
        return;
    } else if (ex != NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();
    }
    LOGE(TAG, "onPostEventToJava %d:", 2);
    ex = NULL;
    // 获取jmethodID
    jmethodID methodId = env->GetStaticMethodID(clazz, "postEventFromNative", "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    ex = env->ExceptionOccurred();
    // 判断异常是否发送
    if (ex != NULL && methodId == NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();

        jclass newExc = env->FindClass("java/lang/NoSuchMethodException");
        if (newExc == NULL) {
            return;
        }
        env->ThrowNew(newExc, "Throw exception from JNI: GetMethodID for postEventFromNative() failed");
    } else if (methodId != NULL) {
        LOGE(TAG, "onPostEventToJava %d:", 3);
        env->CallStaticVoidMethod(clazz, methodId, jplayer, event, what, args1, args2);
        LOGE(TAG, "onPostEventToJava %d:", 4);
    } else if (ex != NULL) {
        // 让java 继续运行, 输出关于这个异常的描述
        env->ExceptionDescribe();
        //清空JNI 产生的异常
        env->ExceptionClear();
    }
}