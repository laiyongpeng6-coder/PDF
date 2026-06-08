package com.scantidy.scan.pdf.merge

import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF 合并 / 拆分 / 旋转 / 删页 / 调序 / 重新保存
 * 全部基于 PdfBox-Android 的 PDDocument
 */
@Singleton
class PdfMerger @Inject constructor() {

    /**
     * 把多个 PDF 合并为一个
     */
    suspend fun merge(inputs: List<File>, output: File) = withContext(Dispatchers.IO) {
        if (inputs.isEmpty()) error("inputs is empty")
        PdfCore.withNewDocument { dest ->
            for (input in inputs) {
                if (!input.exists()) {
                    Timber.w("skip non-existent: ${input.absolutePath}")
                    continue
                }
                PdfCore.withDocument(input) { src ->
                    copyAllPages(src, dest)
                }
            }
            PdfCore.save(dest, output)
        }
    }

    /**
     * 把一个 PDF 的指定页集合拆出来
     * @param indices 0-based 索引
     */
    suspend fun splitPages(input: File, indices: List<Int>, output: File) = withContext(Dispatchers.IO) {
        PdfCore.withDocument(input) { src ->
            PdfCore.withNewDocument { dest ->
                val pageList = src.documentCatalog.pages
                for (i in indices) {
                    if (i < 0 || i >= pageList.count) continue
                    val page = pageList[i] as PDPage
                    dest.addPage(dest.importPage(page))
                }
                PdfCore.save(dest, output)
            }
        }
    }

    /**
     * 把一个 PDF 的指定范围拆成多个文件
     * @param ranges 例如 [(0,2), (3,5)] 表示拆成 2 个文件
     */
    suspend fun splitByRanges(input: File, ranges: List<IntRange>, outputDir: File, baseName: String) = withContext(Dispatchers.IO) {
        require(ranges.isNotEmpty()) { "ranges is empty" }
        require(outputDir.exists() || outputDir.mkdirs()) { "output dir not accessible" }
        PdfCore.withDocument(input) { src ->
            val pageList = src.documentCatalog.pages
            ranges.forEachIndexed { idx, range ->
                val out = File(outputDir, "${baseName}_part${idx + 1}.pdf")
                PdfCore.withNewDocument { dest ->
                    for (i in range) {
                        if (i < 0 || i >= pageList.count) continue
                        dest.addPage(dest.importPage(pageList[i] as PDPage))
                    }
                    PdfCore.save(dest, out)
                }
            }
        }
    }

    /**
     * 把每个页拆成独立 PDF
     */
    suspend fun splitEachPage(input: File, outputDir: File, baseName: String) = withContext(Dispatchers.IO) {
        PdfCore.withDocument(input) { src ->
            val count = src.documentCatalog.pages.count
            for (i in 0 until count) {
                val out = File(outputDir, "${baseName}_p${i + 1}.pdf")
                PdfCore.withNewDocument { dest ->
                    dest.addPage(dest.importPage(src.documentCatalog.pages[i] as PDPage))
                    PdfCore.save(dest, out)
                }
            }
        }
    }

    /**
     * 旋转指定页（90 的倍数）
     */
    suspend fun rotatePages(input: File, indices: List<Int>, degrees: Int, output: File) = withContext(Dispatchers.IO) {
        require(degrees % 90 == 0) { "rotation must be multiple of 90" }
        PdfCore.withDocument(input) { src ->
            val pageList = src.documentCatalog.pages
            for (i in indices) {
                if (i < 0 || i >= pageList.count) continue
                val page = pageList[i] as PDPage
                page.rotation = (page.rotation + degrees) % 360
            }
            PdfCore.save(src, output)
        }
    }

    /**
     * 删除指定页
     */
    suspend fun deletePages(input: File, indices: List<Int>, output: File) = withContext(Dispatchers.IO) {
        PdfCore.withDocument(input) { src ->
            val pageList = src.documentCatalog.pages
            // 必须从大到小删，否则索引会偏移
            val sorted = indices.sortedDescending()
            for (i in sorted) {
                if (i < 0 || i >= pageList.count) continue
                src.removePage(pageList[i] as PDPage)
            }
            PdfCore.save(src, output)
        }
    }

    private fun copyAllPages(src: PDDocument, dest: PDDocument) {
        for (i in 0 until src.documentCatalog.pages.count) {
            val page = src.documentCatalog.pages[i] as PDPage
            dest.addPage(dest.importPage(page))
        }
    }
}
