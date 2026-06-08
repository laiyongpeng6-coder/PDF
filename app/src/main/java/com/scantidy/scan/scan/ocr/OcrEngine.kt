package com.scantidy.scan.scan.ocr

/**
 * OCR 引擎要支持的语言
 */
enum class OcrLanguage(val tag: String, val displayName: String) {
    LATIN("Latin", "Latin"),
    CHINESE("Chinese", "中文")
    // 后续可加 JAPANESE("Japanese", "日本語") 等
}

/**
 * OCR 单个识别块
 */
data class OcrBlock(
    val text: String,
    val boundingBox: android.graphics.Rect? = null,
    val confidence: Float? = null
)

/**
 * 单张图的 OCR 完整结果
 */
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>
) {
    val isEmpty: Boolean get() = fullText.isBlank()
}

/**
 * OCR 引擎抽象
 *  - ML Kit 是默认实现
 *  - 测试时可注入 mock
 */
interface OcrEngine {
    suspend fun recognize(bitmap: android.graphics.Bitmap, languages: List<OcrLanguage>): OcrResult
}
