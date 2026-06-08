package com.scantidy.scan.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * 文档主表
 * 所有数据完全本地；删除 App 一并消失
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "page_count")
    val pageCount: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    /** scan | link | import | convert | merge | split */
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,

    @ColumnInfo(name = "has_ocr")
    val hasOcr: Boolean = false,

    @ColumnInfo(name = "has_watermark")
    val hasWatermark: Boolean = false,

    @ColumnInfo(name = "has_annotation")
    val hasAnnotation: Boolean = false,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String? = null,

    /** 额外 JSON 字段（备用） */
    @ColumnInfo(name = "meta_json")
    val metaJson: String? = null
)

/**
 * 全文搜索虚拟表（FTS4）
 * 用 contentEntity 关联到 documents 表，按 name + ocr_text 搜索
 */
@Fts4(contentEntity = DocumentEntity::class)
@Entity(tableName = "documents_fts")
data class DocumentFts(
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String?
)

/**
 * 标签表（一对多）
 * 用 (documentId, tag) 复合主键
 */
@Entity(
    tableName = "document_tags",
    primaryKeys = ["document_id", "tag"]
)
data class DocumentTagEntity(
    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "tag")
    val tag: String
)

/**
 * 文件夹归属（一个文档可属于多个文件夹）
 */
@Entity(
    tableName = "document_folders",
    primaryKeys = ["document_id", "folder"]
)
data class DocumentFolderEntity(
    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "folder")
    val folder: String
)

/**
 * 链接 → PDF 的历史 URL
 * 用于 "最近链接" 列表
 */
@Entity(tableName = "link_history")
data class LinkHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,

    @ColumnInfo(name = "use_count")
    val useCount: Int = 1
)
