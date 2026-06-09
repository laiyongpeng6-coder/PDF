package com.scantidy.scan.pdf.convert

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF ↔ JPG 互转
 *  - PDF → JPG：每页一张图
 *  - JPG → PDF：见 PdfFromImages
 */
@Singleton
class PdfJpgConverter @Inject constructor() {

    /**
     * PDF 转 JPG（每页一张）
     * @param dpi 渲染分辨率（默认 150）
     */
    suspend fun pdfToJpg(
        input: File,
        outputDir: File,
        baseName: String = "page",
        dpi: Int = 150,
        format: String = "jpg"
    ): List<File> = withContext(Dispatchers.IO) {
        require(outputDir.exists() || outputDir.mkdirs()) { "output dir not accessible" }
        val outFiles = mutableListOf<File>()
        PdfCore.withDocument(input) { doc ->
            val renderer = PDFRenderer(doc)
            val count = doc.documentCatalog.pages.count
            for (i in 0 until count) {
                val page = doc.documentCatalog.pages[i] as PDPage
                val image: Bitmap = renderer.renderImageWithDPI(i, dpi.toFloat())
                val ext = if (format.equals("png", ignoreCase = true)) "png" else "jpg"
                val out = File(outputDir, "${baseName}_${i + 1}.$ext")
                FileOutputStream(out).use { fos ->
                    val compressFormat = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    image.compress(compressFormat, 90, fos)
                }
                image.recycle()
                outFiles.add(out)
            }
        }
        Timber.i("PDF→JPG: produced ${outFiles.size} files in ${outputDir.absolutePath}")
        outFiles
    }

    /**
     * 多图合并为 PDF
     * （功能跟 PdfFromImages 重叠；这里保留便捷入口）
     */
    suspend fun jpgToPdf(
        images: List<File>,
        output: File
    ) = withContext(Dispatchers.IO) {
        require(images.isNotEmpty()) { "no images" }
        PdfCore.withNewDocument { doc ->
            for (imageFile in images) {
                if (!imageFile.exists()) {
                    Timber.w("skip missing image: ${imageFile.absolutePath}")
                    continue
                }
                val image = BitmapFactory.decodeFile(imageFile.absolutePath) ?: continue
                val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, image)
                val page = com.tom_roush.pdfbox.pdmodel.PDPage(
                    com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                        pdImage.width.toFloat(),
                        pdImage.height.toFloat()
                    )
                )
                doc.addPage(page)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(pdImage, 0f, 0f, pdImage.width.toFloat(), pdImage.height.toFloat())
                }
                image.recycle()
            }
            PdfCore.save(doc, output)
        }
    }
}
