package com.scantidy.scan

import android.app.Application
import com.scantidy.scan.core.log.LocalLogTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Release 模式：写本地日志（不联网）
            Timber.plant(LocalLogTree(this))
        }
    }
}
