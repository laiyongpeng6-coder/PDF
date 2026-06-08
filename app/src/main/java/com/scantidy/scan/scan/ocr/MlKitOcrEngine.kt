package com.scantidy.scan.scan.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit 实现的 OCR
 *  - on-device Latin 优先
 *  - 如果用户启用中文，会同时跑 Latin + Chinese 然后合并
 *  - 完全离线（模型首次启动下载后即本地推理）
 */
@Singleton
class MlKitOcrEngine @Inject constructor() : OcrEngine {

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(bitmap: Bitmap, languages: List<OcrLanguage>): OcrResult {
        val effectiveLanguages = if (languages.isEmpty()) listOf(OcrLanguage.LATIN) else languages
        val parts = effectiveLanguages.map { lang ->
            recognizeWith(bitmap, lang)
        }
        val mergedText = parts.joinToString("\n") { it.fullText.trim() }.trim()
        val mergedBlocks = parts.flatMap { it.blocks }
        return OcrResult(mergedText, mergedBlocks)
    }

    private suspend fun recognizeWith(bitmap: Bitmap, lang: OcrLanguage): OcrResult {
        val recognizer = when (lang) {
            OcrLanguage.LATIN -> latinRecognizer
            OcrLanguage.CHINESE -> chineseRecognizer
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val blocks = result.textBlocks.map { block ->
                        OcrBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            confidence = block.lines.firstOrNull()?.confidence
                        )
                    }
                    cont.resume(OcrResult(result.text, blocks))
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }
}
