package com.scantidy.scan.core.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    private val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun forDisplay(epoch: Long): String = displayFormat.format(Date(epoch))
    fun forFileName(epoch: Long): String = fileNameFormat.format(Date(epoch))
    fun forDay(epoch: Long): String = dayFormat.format(Date(epoch))

    /** 把字节数变成 "1.2 MB" */
    fun fileSize(bytes: Long): String {
        if (bytes < 0) return "—"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.getDefault(), "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.getDefault(), "%.2f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
