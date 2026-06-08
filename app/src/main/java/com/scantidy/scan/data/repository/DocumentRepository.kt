package com.scantidy.scan.data.repository

import com.scantidy.scan.data.db.DocumentDao
import com.scantidy.scan.data.db.entity.DocumentEntity
import com.scantidy.scan.data.db.entity.LinkHistoryEntity
import com.scantidy.scan.data.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文档仓库
 * UI 层只看这个；不直接接触 Entity / Dao
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val dao: DocumentDao
) {

    // ---------- 列表 / 搜索 ----------

    fun observeAll(): Flow<List<Document>> =
        dao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun observeAllByName(): Flow<List<Document>> =
        dao.observeAllByName()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun search(query: String): Flow<List<Document>> {
        val q = formatFtsQuery(query)
        if (q.isBlank()) return observeAll()
        return dao.search(q)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    fun observeByTag(tag: String): Flow<List<Document>> =
        dao.observeByTag(tag)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun observeAllTags(): Flow<List<String>> = dao.observeAllTags().flowOn(Dispatchers.IO)
    fun observeAllFolders(): Flow<List<String>> = dao.observeAllFolders().flowOn(Dispatchers.IO)

    // ---------- 单条 ----------

    fun observeById(id: String): Flow<Document?> =
        dao.observeById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    suspend fun getById(id: String): Document? = dao.getById(id)?.toDomain()

    // ---------- 写入 ----------

    suspend fun save(doc: Document) {
        dao.upsertWithMeta(
            document = doc.toEntity(),
            tags = doc.tags.distinct(),
            folders = doc.folders.distinct()
        )
    }

    suspend fun delete(id: String) {
        dao.deleteWithMeta(id)
    }

    suspend fun count(): Int = dao.count()

    suspend fun clearAll() {
        dao.clearAll()
    }

    // ---------- 链接历史 ----------

    fun observeLinkHistory(limit: Int = 20): Flow<List<LinkHistoryEntity>> =
        dao.observeLinkHistory(limit).flowOn(Dispatchers.IO)

    suspend fun recordLinkUsage(url: String, title: String? = null) {
        dao.recordLinkUsage(url, title, System.currentTimeMillis())
    }

    suspend fun clearLinkHistory() = dao.clearLinkHistory()

    // ---------- 工具 ----------

    /**
     * 把 FTS 查询字符串安全化
     *  - 不允许裸引号/星号
     *  - 用 * 前缀匹配
     */
    private fun formatFtsQuery(input: String): String {
        val cleaned = input.trim().replace("\"", " ").replace("*", " ")
        if (cleaned.isBlank()) return ""
        return cleaned.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { "$it*" }
    }

    private fun DocumentEntity.toDomain(): Document {
        val file = File(filePath)
        return Document(
            id = id,
            name = name,
            mimeType = mimeType,
            sizeBytes = if (file.exists()) file.length() else sizeBytes,
            pageCount = pageCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            source = Document.Source.fromRaw(source),
            isEncrypted = isEncrypted,
            hasOcr = hasOcr,
            hasWatermark = hasWatermark,
            hasAnnotation = hasAnnotation,
            thumbnailPath = thumbnailPath,
            filePath = filePath,
            ocrText = ocrText
        )
    }

    private fun Document.toEntity(): DocumentEntity = DocumentEntity(
        id = id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        pageCount = pageCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        source = source.raw,
        isEncrypted = isEncrypted,
        hasOcr = hasOcr,
        hasWatermark = hasWatermark,
        hasAnnotation = hasAnnotation,
        thumbnailPath = thumbnailPath,
        filePath = filePath,
        ocrText = ocrText,
        metaJson = null
    )
}
