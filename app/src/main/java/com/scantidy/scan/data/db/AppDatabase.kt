package com.scantidy.scan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scantidy.scan.data.db.entity.DocumentEntity
import com.scantidy.scan.data.db.entity.DocumentFolderEntity
import com.scantidy.scan.data.db.entity.DocumentFts
import com.scantidy.scan.data.db.entity.DocumentTagEntity
import com.scantidy.scan.data.db.entity.LinkHistoryEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentFts::class,
        DocumentTagEntity::class,
        DocumentFolderEntity::class,
        LinkHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val NAME = "pdfscanner.db"
    }
}
