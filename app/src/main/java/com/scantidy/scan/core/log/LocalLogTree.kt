package com.scantidy.scan.core.log

import android.content.Context
import android.util.Log
import com.scantidy.scan.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Release 模式：写本地日志文件，不联网，不上传。
 * 路径：files/logs/log-YYYYMMDD.log + crash-YYYYMMDD.log
 */
class LocalLogTree(private val appContext: Context) : Timber.Tree() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun logFile(prefix: String): File {
        val dir = File(appContext.filesDir, "logs").apply { mkdirs() }
        val date = fileDateFormat.format(Date())
        // 单日只保留一个文件，简单滚动（>2MB 切下一个）
        val f = File(dir, "$prefix-$date.log")
        if (f.exists() && f.length() > 2L * 1024 * 1024) {
            val rotated = File(dir, "$prefix-$date-${System.currentTimeMillis()}.log")
            f.renameTo(rotated)
            return rotated
        }
        return f
    }

    private fun append(prefix: String, priority: Int, tag: String?, message: String, t: Throwable? = null) {
        try {
            val ts = dateFormat.format(Date())
            val pname = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
            val sb = StringBuilder()
            sb.append("[$ts] [$pname")
            tag?.takeIf { it.isNotEmpty() }?.let { sb.append('/').append(it) }
            sb.append("] ").append(message).append('\n')
            t?.let {
                sb.append("Exception: ").append(it.javaClass.name).append(": ").append(it.message).append('\n')
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                sb.append(sw).append('\n')
            }
            FileOutputStream(logFile(prefix), true).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        } catch (e: Throwable) {
            // 日志本身失败时静默，避免递归
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (BuildConfig.DEBUG) {
            // Debug 模式也写本地一份（方便对比）
            append("log", priority, tag, message, t)
        } else {
            append("log", priority, tag, message, t)
        }
    }

    companion object {
        /**
         * 全局未捕获异常时调用
         */
        fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
            try {
                val dir = File(context.filesDir, "logs").apply { mkdirs() }
                val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                val file = File(dir, "crash-$date.log")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val content = """
                    === CRASH at $ts on thread "${thread.name}" ===
                    Exception: ${throwable.javaClass.name}: ${throwable.message ?: "(no message)"}
                    $sw
                    === END ===
                    
                    
                """.trimIndent()
                FileOutputStream(file, true).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            } catch (_: Throwable) {
                // swallow
            }
        }
    }
}
