package com.scantidy.scan.pdf.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import com.scantidy.scan.pdf.encrypt.PdfEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 包装系统 PdfRenderer
 *  - 单例 + 互斥：PdfRenderer 不是线程安全的
 *  - 提供异步、按页加载
 *  - 加密 PDF：先解密到临时文件
 */
@Singleton
class PdfRenderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptor: PdfEncryptor
) {

    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null
    private var currentFile: File? = null
    private var needsRecycle: Boolean = false

    /**
     * 打开一个 PDF
     * @return 页数
     */
    suspend fun open(file: File, password: String? = null): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeLocked()
            val source = if (isPdfEncrypted(file)) {
                if (password.isNullOrBlank()) {
                    throw IllegalStateException("PDF is encrypted and no password provided")
                }
                needsRecycle = true
                encryptor.unlockToTempFile(file, password)
            } else {
                needsRecycle = false
                file
            }
            currentFile = source
            val r = PdfRenderer(android.os.ParcelFileDescriptor.open(source, android.os.ParcelFileDescriptor.MODE_READ_ONLY))
            renderer = r
            r.pageCount
        }
    }

    /**
     * 渲染一页
     */
    suspend fun renderPage(index: Int, scale: Float = 2f): Bitmap = withContext(Dispatchers.IO) {
        mutex.withLock {
            val r = renderer ?: error("PdfRenderer not open")
            if (index < 0 || index >= r.pageCount) error("Page index out of range: $index")
            r.openPage(index).use { page ->
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }

    fun pageCount(): Int = renderer?.pageCount ?: 0

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock { closeLocked() }
    }

    private fun closeLocked() {
        runCatching { renderer?.close() }
        renderer = null
        if (needsRecycle) {
            currentFile?.takeIf { it.exists() }?.delete()
            needsRecycle = false
        }
        currentFile = null
    }

    private fun isPdfEncrypted(file: File): Boolean {
        return try {
            file.inputStream().use { ins ->
                val buf = ByteArray(1024)
                val n = ins.read(buf)
                val head = String(buf, 0, n, Charsets.ISO_8859_1)
                // 简单判断：有 /Encrypt 字典
                head.contains("/Encrypt")
            }
        } catch (e: Throwable) {
            Timber.w(e, "isPdfEncrypted check failed")
            false
        }
    }
}
