package com.scantidy.scan.pdf.signature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一笔签名：多个采样点
 */
data class SignatureStroke(
    val points: List<android.graphics.PointF>,
    val strokeWidth: Float = 6f
)

/**
 * 手写签名：透明 PNG → 叠加到 PDF 任意位置
 * v0.1 简化：不做数字证书签名（PKCS#7）
 */
@Singleton
class SignatureProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 把用户绘制的一笔签名渲染成透明 PNG，存入 files/signatures/
     */
    fun saveSignature(strokes: List<SignatureStroke>): File = run {
        if (strokes.isEmpty()) error("No strokes")

        // 计算包围盒
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (s in strokes) for (p in s.points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        val pad = 12f
        minX -= pad; minY -= pad; maxX += pad; maxY += pad
        val w = (maxX - minX).toInt().coerceAtLeast(64)
        val h = (maxY - minY).toInt().coerceAtLeast(64)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (stroke in strokes) {
            paint.strokeWidth = stroke.strokeWidth
            val path = Path()
            val pts = stroke.points
            if (pts.isEmpty()) continue
            path.moveTo(pts[0].x - minX, pts[0].y - minY)
            for (i in 1 until pts.size) {
                path.lineTo(pts[i].x - minX, pts[i].y - minY)
            }
            canvas.drawPath(path, paint)
        }

        val file = File(AppPaths.signaturesDir(context), "sig_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { os ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
        bmp.recycle()
        file
    }

    /**
     * 把签名 PNG 叠加到 PDF 指定页的指定位置
     * @param input 原 PDF
     * @param output 输出 PDF
     * @param signaturePng 签名图片
     * @param pageIndex 0-based
     * @param xPdf 落款左下角 X（PDF 单位）
     * @param yPdf 落款左下角 Y（PDF 单位）
     * @param widthPdf 落款宽度（PDF 单位）
     * @param heightPdf 落款高度（PDF 单位）
     */
    suspend fun addSignature(
        input: File,
        output: File,
        signaturePng: File,
        pageIndex: Int,
        xPdf: Float,
        yPdf: Float,
        widthPdf: Float,
        heightPdf: Float
    ) = withContext(Dispatchers.IO) {
        require(signaturePng.exists()) { "Signature PNG not found" }
        PdfCore.withDocument(input) { doc ->
            val pageList = doc.documentCatalog.pages
            if (pageIndex < 0 || pageIndex >= pageList.count) {
                error("Page index out of bounds: $pageIndex")
            }
            val page = pageList[pageIndex] as PDPage
            val image = android.graphics.BitmapFactory.decodeFile(signaturePng.absolutePath)
                ?: error("Cannot decode signature PNG")
            val pdImage = LosslessFactory.createFromImage(doc, image)

            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.drawImage(pdImage, xPdf, yPdf, widthPdf, heightPdf)
            }
            image.recycle()
            PdfCore.save(doc, output)
            Timber.i("Added signature to page $pageIndex: ${output.absolutePath}")
        }
    }
}
