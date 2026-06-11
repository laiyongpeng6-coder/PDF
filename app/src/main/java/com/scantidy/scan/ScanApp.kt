package com.scantidy.scan

import android.app.Application
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.scantidy.scan.core.analytics.AnalyticsTracker
import com.scantidy.scan.core.log.LocalLogTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ScanApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase 初始化
        AnalyticsTracker.init()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Release: 启用 Firebase Crashlytics + 本地日志
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
            Timber.plant(LocalLogTree(this))
        }
    }
}
