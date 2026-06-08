package com.scantidy.scan.pdf.annotation

import android.content.Context
import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 标注序列化
 *  - 持久化到 metadata 文件：files/annotations/{docId}.json
 *  - 渲染到 PDF：在原 PDF 之上叠加颜色
 */
@Singleton
class AnnotationStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun fileFor(documentId: String): File {
        val dir = File(context.filesDir, "annotations").apply { mkdirs() }
        return File(dir, "$documentId.json")
    }

    suspend fun load(documentId: String): AnnotationSet = withContext(Dispatchers.IO) {
        val f = fileFor(documentId)
        if (!f.exists()) return@withContext AnnotationSet(documentId, emptyList())
        val text = f.readText(Charsets.UTF_8)
        if (text.isBlank()) return@withContext AnnotationSet(documentId, emptyList())
        try {
            val root = JSONObject(text)
            val arr = root.optJSONArray("annotations") ?: JSONArray()
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val type = AnnotationType.valueOf(obj.optString("type", "HIGHLIGHT"))
                val page = obj.optInt("page")
                val x = obj.optDouble("x").toFloat()
                val y = obj.optDouble("y").toFloat()
                val w = obj.optDouble("w").toFloat()
                val h = obj.optDouble("h").toFloat()
                val color = obj.optInt("color", 0xFFFFEB3B.toInt())
                val alpha = obj.optDouble("alpha", 0.4).toFloat()
                val stroke = obj.optDouble("sw", 0.005).toFloat()
                val pathArr = obj.optJSONArray("path")
                val path = if (pathArr != null) (0 until pathArr.length()).map { j ->
                    val o = pathArr.getJSONObject(j)
                    o.optDouble("x").toFloat() to o.optDouble("y").toFloat()
                } else emptyList()
                Annotation(type, page, x, y, w, h, color, alpha, stroke, path)
            }
            AnnotationSet(documentId, list)
        } catch (e: Throwable) {
            Timber.w(e, "parse annotation failed")
            AnnotationSet(documentId, emptyList())
        }
    }

    suspend fun save(set: AnnotationSet) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("documentId", set.documentId)
        val arr = JSONArray()
        for (a in set.annotations) {
            val o = JSONObject()
            o.put("id", UUID.randomUUID().toString())
            o.put("type", a.type.name)
            o.put("page", a.pageIndex)
            o.put("x", a.x.toDouble())
            o.put("y", a.y.toDouble())
            o.put("w", a.w.toDouble())
            o.put("h", a.h.toDouble())
            o.put("color", a.color)
            o.put("alpha", a.alpha.toDouble())
            o.put("sw", a.strokeWidth.toDouble())
            if (a.scribblePath.isNotEmpty()) {
                val pArr = JSONArray()
                for ((x, y) in a.scribblePath) {
                    val p = JSONObject()
                    p.put("x", x.toDouble())
                    p.put("y", y.toDouble())
                    pArr.put(p)
                }
                o.put("path", pArr)
            }
            arr.put(o)
        }
        root.put("annotations", arr)
        fileFor(set.documentId).writeText(root.toString(), Charsets.UTF_8)
    }

    /**
     * 把标注叠加到 PDF 之上
     *  - 高亮：在页面上画半透明矩形
     *  - 涂鸦：按归一化坐标画折线
     */
    suspend fun applyToPdf(input: File, set: AnnotationSet, output: File) = withContext(Dispatchers.IO) {
        if (set.annotations.isEmpty()) {
            // 没有标注直接复制
            input.copyTo(output, overwrite = true)
            return@withContext
        }
        PdfCore.withDocument(input) { doc ->
            for (a in set.annotations) {
                applyOne(doc, a)
            }
            PdfCore.save(doc, output)
        }
    }

    private fun applyOne(doc: PDDocument, a: Annotation) {
        val pageList = doc.documentCatalog.pages
        if (a.pageIndex < 0 || a.pageIndex >= pageList.count) return
        val page = pageList[a.pageIndex] as PDPage
        val box: PDRectangle = page.mediaBox
        val pageW = box.width
        val pageH = box.height

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            val gs = PDExtendedGraphicsState()
            gs.nonStrokingAlphaConstant = a.alpha
            cs.setGraphicsStateParameters(gs)

            val aC = (a.color shr 24) and 0xFF
            val rC = (a.color shr 16) and 0xFF
            val gC = (a.color shr 8) and 0xFF
            val bC = a.color and 0xFF
            cs.setNonStrokingColor(rC / 255f, gC / 255f, bC / 255f)
            @Suppress("UNUSED_VARIABLE") val alphaByte = aC

            when (a.type) {
                AnnotationType.HIGHLIGHT -> {
                    val x = a.x * pageW
                    val y = a.y * pageH
                    val w = a.w * pageW
                    val h = a.h * pageH
                    cs.addRect(x, y, w, h)
                    cs.fill()
                }
                AnnotationType.SCRIBBLE -> {
                    if (a.scribblePath.size < 2) return
                    val strokePt = a.strokeWidth * pageW
                    cs.setStrokingColor(rC / 255f, gC / 255f, bC / 255f)
                    cs.setLineWidth(strokePt)
                    cs.moveTo(a.scribblePath[0].first * pageW, a.scribblePath[0].second * pageH)
                    for (i in 1 until a.scribblePath.size) {
                        cs.lineTo(a.scribblePath[i].first * pageW, a.scribblePath[i].second * pageH)
                    }
                    cs.stroke()
                }
            }
        }
    }
}
