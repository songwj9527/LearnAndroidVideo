package com.songwj.openvideo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 添加MultiDex分包
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}