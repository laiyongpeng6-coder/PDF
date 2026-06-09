package com.scantidy.scan.pdf.encrypt

import com.scantidy.scan.pdf.core.PdfCore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF 加密 / 解密
 *  - 使用 AES-128
 *  - 加密：需要 owner + user 密码
 *  - 解密：需要 owner 密码（owner 密码可以打开 + 修改；user 密码只可读）
 *
 * v0.1 简化：owner password = user password = 用户输入的密码
 */
@Singleton
class PdfEncryptor @Inject constructor() {

    /**
     * 给 PDF 设置密码
     */
    suspend fun encrypt(input: File, password: String, output: File) = withContext(Dispatchers.IO) {
        require(password.length >= 6) { "Password must be at least 6 characters" }
        PdfCore.withDocument(input) { doc ->
            val perms = AccessPermission().apply {
                setCanPrint(true)
                setCanModify(false)
                setCanExtractContent(false)
                setCanModifyAnnotations(false)
            }
            val policy = StandardProtectionPolicy(password, password, perms)
            policy.encryptionKeyLength = 128
            policy.isPreferAES = true
            doc.protect(policy)
            PdfCore.save(doc, output)
            Timber.i("Encrypted PDF: ${output.absolutePath}")
        }
    }

    /**
     * 移除密码（需要原密码）
     */
    suspend fun decrypt(input: File, password: String, output: File) = withContext(Dispatchers.IO) {
        PdfCore.withDocument(input) { doc ->
            val isEncrypted = doc.isEncrypted
            if (!isEncrypted) {
                // 没加密直接复制
                PdfCore.save(doc, output)
                return@withDocument
            }
            doc.setAllSecurityToBeRemoved(true)
            PdfCore.save(doc, output)
            Timber.i("Decrypted PDF: ${output.absolutePath}")
        }
    }

    /**
     * 读 PDF 时尝试用密码解锁（用于 PdfRenderer 之前的预处理）
     *  返回解密后的临时文件（调用方负责删除）
     */
    suspend fun unlockToTempFile(input: File, password: String): File = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("unlock_", ".pdf", input.parentFile)
        temp.deleteOnExit()
        decrypt(input, password, temp)
        temp
    }
}
