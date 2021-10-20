package com.songwj.openvideo

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

class MyApplication : Application() {

    public companion object {
        private var application: MyApplication? = null

        public fun getInstance(): MyApplication? {
            return application
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // 添加MultiDex分包
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (application == null) {
            application = this
        }
    }
}