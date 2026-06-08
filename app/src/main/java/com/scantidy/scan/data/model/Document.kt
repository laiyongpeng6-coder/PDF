package com.scantidy.scan.data.model

/**
 * 领域模型：Document
 *
 * 把 Entity + Tag + Folder 聚合成上层 UI 使用的模型。
 * Repository 负责组装，UI 只看这个。
 */
data class Document(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val source: Source,
    val isEncrypted: Boolean,
    val hasOcr: Boolean,
    val hasWatermark: Boolean,
    val hasAnnotation: Boolean,
    val thumbnailPath: String,
    val filePath: String,
    val ocrText: String? = null,
    val tags: List<String> = emptyList(),
    val folders: List<String> = emptyList()
) {
    enum class Source(val raw: String) {
        SCAN("scan"),
        LINK("link"),
        IMPORT("import"),
        CONVERT("convert"),
        MERGE("merge"),
        SPLIT("split");

        companion object {
            fun fromRaw(raw: String?): Source = entries.firstOrNull { it.raw == raw } ?: SCAN
        }
    }
}
