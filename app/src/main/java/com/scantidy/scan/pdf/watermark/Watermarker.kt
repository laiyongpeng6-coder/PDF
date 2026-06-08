package com.scantidy.scan.pdf.watermark

import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.font.Standard14Fonts
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import timber.log.Timber
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

/**
 * 水印参数
 */
data class WatermarkConfig(
    val text: String = "PDF 极扫",
    val fontSize: Float = 48f,
    val color: Int = 0x66_80_80_80.toInt(),       // ARGB，半透明灰
    val rotation: Float = 30f,                     // 角度
    val opacity: Float = 0.20f,                    // 0..1
    val tile: Boolean = true,                      // 平铺 vs 居中单条
    val imageFile: File? = null                    // 图片水印（与 text 二选一）
)

/**
 * PDF 水印
 *  - 文字水印：使用系统 Helvetica 字体（v0.1 不嵌入中文）
 *  - 图片水印：PNG 透明图
 */
@Singleton
class Watermarker @Inject constructor() {

    suspend fun add(input: File, config: WatermarkConfig, output: File) = withContext(Dispatchers.IO) {
        PdfCore.withDocument(input) { doc ->
            val pageList = doc.documentCatalog.pages
            for (i in 0 until pageList.count) {
                val page = pageList[i] as PDPage
                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    if (config.imageFile != null && config.imageFile.exists()) {
                        drawImageWatermark(doc, cs, page, config)
                    } else {
                        drawTextWatermark(cs, page, config)
                    }
                }
            }
            PdfCore.save(doc, output)
            Timber.i("Watermarked PDF: ${output.absolutePath}")
        }
    }

    private fun drawTextWatermark(cs: PDPageContentStream, page: PDPage, config: WatermarkConfig) {
        val box = page.mediaBox
        val pageW = box.width
        val pageH = box.height

        val gs = PDExtendedGraphicsState()
        gs.nonStrokingAlphaConstant = config.opacity
        cs.setGraphicsStateParameters(gs)

        val font: PDFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val fontSize = config.fontSize
        val text = config.text
        val textWidth = font.getStringWidth(text) / 1000f * fontSize
        val textHeight = fontSize

        val r = (config.rotation.toDouble() * Math.PI / 180.0)
        val cosR = cos(r).toFloat()
        val sinR = sin(r).toFloat()

        // 颜色按 ARGB
        val a = ((config.color shr 24) and 0xFF) / 255f
        val rC = ((config.color shr 16) and 0xFF) / 255f
        val gC = ((config.color shr 8) and 0xFF) / 255f
        val bC = (config.color and 0xFF) / 255f
        cs.setNonStrokingColor(rC, gC, bC)

        if (config.tile) {
            val stepX = textWidth * 1.6f
            val stepY = textHeight * 4f
            var y = 0f
            while (y < pageH) {
                var x = 0f
                while (x < pageW) {
                    drawRotatedText(cs, text, font, fontSize, x, y, cosR, sinR)
                    x += stepX
                }
                y += stepY
            }
        } else {
            val cx = (pageW - textWidth) / 2f
            val cy = (pageH - textHeight) / 2f
            drawRotatedText(cs, text, font, fontSize, cx, cy, cosR, sinR)
        }
    }

    private fun drawRotatedText(
        cs: PDPageContentStream,
        text: String,
        font: PDFont,
        fontSize: Float,
        x: Float, y: Float,
        cosR: Float, sinR: Float
    ) {
        // PDFBox: cs.transform 写矩阵，但需要保存/恢复
        // 用 beginText + setTextMatrix 写旋转
        cs.saveGraphicsState()
        cs.beginText()
        // text matrix = [a, b, c, d, e, f] = [cos, sin, -sin, cos, x, y]
        // PDF text origin 是 baseline
        cs.setTextMatrix(org.apache.pdfbox.util.Matrix(
            cosR, sinR, -sinR, cosR, x, y
        ))
        cs.setFont(font, fontSize)
        cs.showText(text)
        cs.endText()
        cs.restoreGraphicsState()
    }

    private fun drawImageWatermark(
        doc: PDDocument,
        cs: PDPageContentStream,
        page: PDPage,
        config: WatermarkConfig
    ) {
        val imageFile = config.imageFile ?: return
        val image: BufferedImage = ImageIO.read(imageFile) ?: return
        val pdImage: PDImageXObject = LosslessFactory.createFromImage(doc, image)
        val box = page.mediaBox
        val pageW = box.width
        val pageH = box.height

        val gs = PDExtendedGraphicsState()
        gs.nonStrokingAlphaConstant = config.opacity
        cs.setGraphicsStateParameters(gs)

        val targetW = pageW * 0.3f
        val targetH = targetW * (pdImage.height.toFloat() / pdImage.width.toFloat())
        val cx = (pageW - targetW) / 2f
        val cy = (pageH - targetH) / 2f
        cs.drawImage(pdImage, cx, cy, targetW, targetH)
    }
}
