package com.scantidy.scan.pdf.core

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * PdfBox 工具：打开/创建/保存 PDF
 *  - 全部 IO 在 Dispatchers.IO
 *  - 加载/保存使用 try-with-resources 模式
 */
object PdfCore {

    suspend fun <T> withDocument(file: File, block: suspend (PDDocument) -> T): T = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { doc ->
            block(doc)
        }
    }

    suspend fun <T> withNewDocument(block: suspend (PDDocument) -> T): T = withContext(Dispatchers.IO) {
        PDDocument().use { doc ->
            block(doc)
        }
    }

    suspend fun save(doc: PDDocument, file: File) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        doc.save(file)
        Timber.i("PDF saved: ${file.absolutePath} (${file.length()} bytes)")
    }

    /**
     * 添加空白页（A4）
     */
    fun addBlankPage(doc: PDDocument, width: Float = PDRectangle.A4.width, height: Float = PDRectangle.A4.height): PDPage {
        val page = PDPage(PDRectangle(width, height))
        doc.addPage(page)
        return page
    }
}
