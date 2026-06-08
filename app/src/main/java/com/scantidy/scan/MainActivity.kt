package com.scantidy.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.scantidy.scan.core.log.LocalLogTree
import com.scantidy.scan.ui.LocalAppLocale
import com.scantidy.scan.ui.ScanApp
import com.scantidy.scan.ui.theme.PDFScannerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 启动时附加一个全局未捕获异常处理器
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            LocalLogTree.writeCrash(applicationContext, t, e)
            defaultHandler?.uncaughtException(t, e)
        }
        setContent {
            PDFScannerTheme {
                CompositionLocalProvider(
                    LocalAppLocale provides Locale.getDefault()
                ) {
                    ScanApp()
                }
            }
        }
    }
}
