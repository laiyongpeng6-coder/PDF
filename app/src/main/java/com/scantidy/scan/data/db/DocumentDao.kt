package com.scantidy.scan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.scantidy.scan.data.db.entity.DocumentEntity
import com.scantidy.scan.data.db.entity.DocumentFolderEntity
import com.scantidy.scan.data.db.entity.DocumentTagEntity
import com.scantidy.scan.data.db.entity.LinkHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // -------- 单文档 CRUD --------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<DocumentTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolders(folders: List<DocumentFolderEntity>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM document_tags WHERE document_id = :id")
    suspend fun deleteTags(id: String)

    @Query("DELETE FROM document_folders WHERE document_id = :id")
    suspend fun deleteFolders(id: String)

    @Transaction
    suspend fun upsertWithMeta(
        document: DocumentEntity,
        tags: List<String>,
        folders: List<String>
    ) {
        upsert(document)
        upsertTags(tags.map { DocumentTagEntity(document.id, it) })
        upsertFolders(folders.map { DocumentFolderEntity(document.id, it) })
    }

    @Transaction
    suspend fun deleteWithMeta(id: String) {
        deleteTags(id)
        deleteFolders(id)
        delete(id)
    }

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: String): Flow<DocumentEntity?>

    // -------- 列表 / 搜索 --------

    @Query("SELECT * FROM documents ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY name COLLATE NOCASE ASC")
    fun observeAllByName(): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN documents_fts fts ON d.id = fts.rowid
        WHERE documents_fts MATCH :query
        ORDER BY d.updated_at DESC
        """
    )
    fun search(query: String): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN document_tags t ON d.id = t.document_id
        WHERE t.tag = :tag
        ORDER BY d.updated_at DESC
        """
    )
    fun observeByTag(tag: String): Flow<List<DocumentEntity>>

    @Query(
        """
        SELECT d.* FROM documents d
        INNER JOIN document_folders f ON d.id = f.document_id
        WHERE f.folder = :folder
        ORDER BY d.updated_at DESC
        """
    )
    fun observeByFolder(folder: String): Flow<List<DocumentEntity>>

    @Query("SELECT DISTINCT tag FROM document_tags ORDER BY tag")
    fun observeAllTags(): Flow<List<String>>

    @Query("SELECT DISTINCT folder FROM document_folders ORDER BY folder")
    fun observeAllFolders(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int

    // -------- 链接历史 --------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLink(link: LinkHistoryEntity)

    @Query("UPDATE link_history SET use_count = use_count + 1, last_used_at = :now WHERE url = :url")
    suspend fun bumpLinkUsage(url: String, now: Long)

    @Query("SELECT * FROM link_history ORDER BY last_used_at DESC LIMIT :limit")
    fun observeLinkHistory(limit: Int = 20): Flow<List<LinkHistoryEntity>>

    @Query("DELETE FROM link_history")
    suspend fun clearLinkHistory()

    /**
     * 记录或更新链接使用次数
     */
    @Transaction
    suspend fun recordLinkUsage(url: String, title: String?, now: Long) {
        val id = "link_${url.hashCode().toString()}"
        bumpLinkUsage(url, now)
        upsertLink(
            LinkHistoryEntity(
                id = id,
                url = url,
                title = title,
                lastUsedAt = now,
                useCount = 1
            )
        )
    }

    // -------- 全部清空 --------

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()

    @Query("DELETE FROM document_tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM document_folders")
    suspend fun clearAllFolders()

    @Transaction
    suspend fun clearAll() {
        clearAllTags()
        clearAllFolders()
        clearAllDocuments()
    }
}
