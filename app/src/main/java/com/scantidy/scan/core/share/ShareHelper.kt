package com.scantidy.scan.core.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.data.model.Document
import java.io.File

/**
 * 系统分享：把 PDF 复制到 exports/ 临时目录，再用 FileProvider 暴露出去
 */
object ShareHelper {

    fun shareDocument(doc: Document, context: Context) {
        val src = File(doc.filePath)
        if (!src.exists()) return
        val export = File(AppPaths.exportsDir(context), src.name)
        src.copyTo(export, overwrite = true)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, export)
        val mime = when {
            doc.name.endsWith(".pdf", true) -> "application/pdf"
            doc.name.endsWith(".jpg", true) || doc.name.endsWith(".jpeg", true) -> "image/jpeg"
            doc.name.endsWith(".png", true) -> "image/png"
            else -> "*/*"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, doc.name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
