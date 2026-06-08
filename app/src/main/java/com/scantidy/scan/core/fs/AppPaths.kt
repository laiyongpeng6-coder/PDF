package com.scantidy.scan.core.fs

import android.content.Context
import java.io.File

/**
 * 应用私有目录工具
 * 一切用户数据 / 缓存 / 日志都收口在这里
 */
object AppPaths {

    /** 文档主目录：files/documents/{uuid}.pdf 等 */
    fun documentsDir(context: Context): File =
        File(context.filesDir, "documents").apply { mkdirs() }

    /** 缩略图：files/thumbnails/{uuid}.jpg */
    fun thumbnailsDir(context: Context): File =
        File(context.filesDir, "thumbnails").apply { mkdirs() }

    /** 手写签名：files/signatures/sig_{uuid}.png */
    fun signaturesDir(context: Context): File =
        File(context.filesDir, "signatures").apply { mkdirs() }

    /** 自定义水印图片：files/watermarks/wm_{uuid}.png */
    fun watermarksDir(context: Context): File =
        File(context.filesDir, "watermarks").apply { mkdirs() }

    /** 分享时复制一份到 exports/，避免原文件被系统回收 */
    fun exportsDir(context: Context): File =
        File(context.filesDir, "exports").apply { mkdirs() }

    /** 链接 → PDF 渲染的 WebView 缓存 */
    fun webCacheDir(context: Context): File =
        File(context.cacheDir, "webview").apply { mkdirs() }

    /** 临时目录（每次启动清空） */
    fun tempDir(context: Context): File =
        File(context.cacheDir, "tmp").apply { mkdirs() }

    /** 日志目录 */
    fun logsDir(context: Context): File =
        File(context.filesDir, "logs").apply { mkdirs() }

    /** 清空所有数据（清除文档、缩略图、签名、水印、日志、缓存） */
    fun clearAll(context: Context) {
        runCatching { documentsDir(context).deleteRecursively() }
        runCatching { thumbnailsDir(context).deleteRecursively() }
        runCatching { signaturesDir(context).deleteRecursively() }
        runCatching { watermarksDir(context).deleteRecursively() }
        runCatching { exportsDir(context).deleteRecursively() }
        runCatching { webCacheDir(context).deleteRecursively() }
        runCatching { tempDir(context).deleteRecursively() }
        // 日志保留（方便事后追查）
    }

    /** 只清缓存（exports / temp / web cache） */
    fun clearCache(context: Context) {
        runCatching { exportsDir(context).deleteRecursively() }
        runCatching { tempDir(context).deleteRecursively() }
        runCatching { webCacheDir(context).deleteRecursively() }
    }
}
