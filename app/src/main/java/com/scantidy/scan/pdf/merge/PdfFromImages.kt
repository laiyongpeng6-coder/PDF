package com.scantidy.scan.pdf.merge

import android.graphics.BitmapFactory
import com.scantidy.scan.pdf.core.PdfCore
import com.scantidy.scan.scan.pipeline.PdfPageInput
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 把图片序列合并为 PDF
 * 可选写入 OCR 文本层（让 PDF 可搜索）
 */
@Singleton
class PdfFromImages @Inject constructor() {

    /**
     * @param inputs 页面输入
     * @param output 输出 PDF
     * @param pageWidthMm / pageHeightMm 页面尺寸（毫米），默认 A4
     * @param marginMm 页边距
     * @param textLayerEnabled 是否写入 OCR 文本层
     */
    suspend fun create(
        inputs: List<PdfPageInput>,
        output: File,
        pageWidthMm: Float = 210f,
        pageHeightMm: Float = 297f,
        marginMm: Float = 0f,
        textLayerEnabled: Boolean = true
    ) = withContext(Dispatchers.IO) {
        require(inputs.isNotEmpty()) { "inputs is empty" }
        PdfCore.withNewDocument { doc ->
            val pageW = mmToPt(pageWidthMm)
            val pageH = mmToPt(pageHeightMm)
            val margin = mmToPt(marginMm)
            val usableW = pageW - 2 * margin
            val usableH = pageH - 2 * margin

            for (input in inputs) {
                val page = PDPage(PDRectangle(pageW, pageH))
                doc.addPage(page)

                val imageFile = File(input.imagePath)
                if (!imageFile.exists()) {
                    Timber.w("Image not found, skip: ${input.imagePath}")
                    continue
                }
                val rawImage = BitmapFactory.decodeFile(imageFile.absolutePath) ?: continue

                val pdImage: PDImageXObject = LosslessFactory.createFromImage(doc, rawImage)
                val (drawW, drawH) = fitInto(pdImage.width, pdImage.height, usableW, usableH)
                val offsetX = margin + (usableW - drawW) / 2f
                val offsetY = margin + (usableH - drawH) / 2f

                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(pdImage, offsetX, offsetY, drawW, drawH)
                }

                if (textLayerEnabled && !input.ocrText.isNullOrBlank()) {
                    writeTextLayer(doc, page, input.ocrText, pageW, pageH, margin, drawW, drawH, offsetX, offsetY)
                }
                rawImage.recycle()
            }
            PdfCore.save(doc, output)
        }
    }

    private fun writeTextLayer(
        doc: PDDocument,
        page: PDPage,
        text: String,
        pageW: Float, pageH: Float,
        margin: Float, drawW: Float, drawH: Float,
        offsetX: Float, offsetY: Float
    ) {
        // 简单实现：把文本按行写入，字体小到不影响外观
        // 真实项目应该按 OCR bounding box 还原，这里取平均行
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        val font: PDFont = PDType1Font.HELVETICA
        val fontSize = (drawH * 0.012f).coerceIn(4f, 10f)
        val lineHeight = fontSize * 1.3f

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.beginText()
            cs.setFont(font, fontSize)
            // 文本透明放在最上层 → 影响可见性，故不透明
            cs.setNonStrokingColor(0f, 0f, 0f)
            // 起点：左上角 (margin, pageH - margin - fontSize)
            val startY = pageH - margin - fontSize
            val maxCharsPerLine = (drawW * 0.6f / fontSize).toInt().coerceAtLeast(10)
            cs.newLineAtOffset(offsetX, startY)
            var currentLine = 0
            for (line in lines) {
                if (currentLine >= 4) break   // 简化：每页只放 4 行（防止覆盖图）
                val truncated = if (line.length > maxCharsPerLine) line.substring(0, maxCharsPerLine) else line
                cs.showText(truncated)
                cs.newLineAtOffset(0f, -lineHeight)
                currentLine++
            }
            cs.endText()
        }
    }

    private fun fitInto(imgW: Int, imgH: Int, maxW: Float, maxH: Float): Pair<Float, Float> {
        if (imgW <= 0 || imgH <= 0) return maxW to maxH
        val scale = minOf(maxW / imgW, maxH / imgH)
        return imgW * scale to imgH * scale
    }

    private fun mmToPt(mm: Float): Float = mm * 2.83465f
}
