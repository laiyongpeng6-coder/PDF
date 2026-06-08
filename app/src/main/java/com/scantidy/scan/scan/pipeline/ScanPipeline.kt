package com.scantidy.scan.scan.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.scan.detector.DocumentDetector
import com.scantidy.scan.scan.detector.PerspectiveCorrector
import com.scantidy.scan.scan.filter.FilterMode
import com.scantidy.scan.scan.filter.ImageFilter
import com.scantidy.scan.scan.ocr.OcrEngine
import com.scantidy.scan.scan.ocr.OcrLanguage
import com.scantidy.scan.scan.ocr.OcrResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 扫描结果（一页）
 */
data class ScannedPage(
    val pageId: String,
    val imageFile: File,         // 矫正 + 滤镜后的图片
    val widthPx: Int,
    val heightPx: Int,
    val originalWidthPx: Int,
    val originalHeightPx: Int
)

/**
 * 扫描保存选项
 */
data class ScanSaveOptions(
    val filterMode: FilterMode = FilterMode.ENHANCED,
    val ocrEnabled: Boolean = true,
    val ocrLanguages: List<OcrLanguage> = listOf(OcrLanguage.LATIN, OcrLanguage.CHINESE),
    val enableTextLayer: Boolean = true
)

/**
 * 扫描结果（含 OCR 文本）
 */
data class ScanResult(
    val pages: List<ScannedPage>,
    val fullOcrText: String,
    val pageOcrTexts: List<String>   // 每页对应一段
)

/**
 * 扫描 + OCR 流水线
 *
 * 输入：原始图片 Uri 列表
 * 输出：ScannedPage（矫正+滤镜后的图）+ OCR 文本
 */
@Singleton
class ScanPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detector: DocumentDetector,
    private val corrector: PerspectiveCorrector,
    private val filter: ImageFilter,
    private val ocr: OcrEngine
) {

    /**
     * 处理一组图片
     */
    suspend fun process(uris: List<Uri>, options: ScanSaveOptions): ScanResult {
        val pages = uris.mapIndexed { idx, uri -> processOne(uri, idx, options) }
        val fullText = pages.mapIndexed { idx, page ->
            pageOcrText(page, options)
        }
        return ScanResult(
            pages = pages,
            fullOcrText = fullText.joinToString("\n\n"),
            pageOcrTexts = fullText
        )
    }

    private fun processOne(uri: Uri, index: Int, options: ScanSaveOptions): ScannedPage {
        val raw = loadBitmapFromUri(uri)
        val bgr = Mat()
        Utils.bitmapToMat(raw, bgr)

        val originalW = raw.width
        val originalH = raw.height

        val quad = detector.detect(bgr)
        val correctedBgr: Mat = if (quad != null) {
            corrector.correct(bgr, quad)
        } else {
            bgr.clone()
        }
        bgr.release()

        val filteredBgr = filter.apply(correctedBgr, options.filterMode)
        correctedBgr.release()

        val outBitmap = Bitmap.createBitmap(
            filteredBgr.cols(),
            filteredBgr.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(filteredBgr, outBitmap)
        filteredBgr.release()

        val pageId = "page_${System.currentTimeMillis()}_$index"
        val outFile = File(AppPaths.documentsDir(context), "$pageId.png")
        FileOutputStream(outFile).use { fos ->
            outBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        raw.recycle()
        // outBitmap 暂时不 recycle：要给 OCR 用；OCR 完成后由调用方 recycle

        return ScannedPage(
            pageId = pageId,
            imageFile = outFile,
            widthPx = outBitmap.width,
            heightPx = outBitmap.height,
            originalWidthPx = originalW,
            originalHeightPx = originalH
        )
    }

    /**
     * 对单页做 OCR（可选）
     * 因为 ML Kit 需要 Bitmap，这里在调用侧执行
     * 由 ScannedPage 取出路径 → 加载 → 识别 → 释放
     */
    private suspend fun pageOcrText(page: ScannedPage, options: ScanSaveOptions): String {
        if (!options.ocrEnabled) return ""
        return try {
            val bmp = BitmapFactory.decodeFile(page.imageFile.absolutePath) ?: return ""
            val result: OcrResult = ocr.recognize(bmp, options.ocrLanguages)
            bmp.recycle()
            result.fullText
        } catch (e: Throwable) {
            Timber.w(e, "OCR failed for ${page.pageId}")
            ""
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val rotation = readExifRotation(uri)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        } ?: error("Cannot decode bitmap from $uri")
        return if (rotation != 0) rotateBitmap(raw, rotation) else raw
    }

    private fun readExifRotation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) 0 else android.media.ExifInterface(input).rotationDegrees
            }
        } catch (e: Throwable) {
            0
        }
    }

    private fun rotateBitmap(src: Bitmap, degree: Int): Bitmap {
        if (degree % 360 == 0) return src
        val m = android.graphics.Matrix().apply { postRotate(degree.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (rotated != src) src.recycle()
        return rotated
    }

    /**
     * 给一组合并 PDF 的输入
     * @return PdfMergeInput
     */
    fun toPdfInput(scan: ScanResult): List<PdfPageInput> = scan.pages.mapIndexed { idx, p ->
        PdfPageInput(
            imagePath = p.imageFile.absolutePath,
            widthPx = p.widthPx,
            heightPx = p.heightPx,
            ocrText = scan.pageOcrTexts.getOrNull(idx)
        )
    }
}

/**
 * 给 PDF 生成器的输入
 */
data class PdfPageInput(
    val imagePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val ocrText: String? = null
)
