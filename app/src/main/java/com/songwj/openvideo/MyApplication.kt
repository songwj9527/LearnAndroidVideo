package com.songwj.openvideo

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import androidx.multidex.MultiDex
import com.tencent.bugly.crashreport.CrashReport

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
        if (isAppProcess(applicationContext)) {
            application = this

            CrashReport.initCrashReport(applicationContext, "14180e2931", false)
        }
    }

    /**
     * 是否为App主进程
     *
     * @param context
     * @return
     */
    fun isAppProcess(context: Context): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val processInfoList = am.runningAppProcesses
        val appProcessName: String = getPackageName(context)
        val myPid = Process.myPid()
        if (processInfoList == null || processInfoList.size == 0) {
            return true
        }
        for (processInfo in processInfoList) {
            if (processInfo.pid == myPid && appProcessName == processInfo.processName) {
                return true
            }
        }
        return false
    }

    /**
     * 获取应用包名
     *
     * @param context
     * @return
     */
    fun getPackageName(context: Context): String {
        return context.applicationContext.packageName
    }
}