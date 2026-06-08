package com.scantidy.scan.scan

import android.content.Context
import org.opencv.android.OpenCVLoader
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenCV 单例加载器
 * 第一次使用任何图像处理功能前必须 ensureLoaded()
 */
@Singleton
class OpenCVInitializer @Inject constructor() {

    @Volatile
    private var loaded: Boolean = false

    @Synchronized
    fun ensureLoaded(context: Context): Boolean {
        if (loaded) return true
        val ok = OpenCVLoader.initLocal()
        Timber.i("OpenCV initLocal = $ok")
        if (ok) loaded = true
        return ok
    }

    fun isLoaded(): Boolean = loaded
}
