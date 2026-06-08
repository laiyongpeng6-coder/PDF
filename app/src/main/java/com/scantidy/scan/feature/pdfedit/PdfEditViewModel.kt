package com.scantidy.scan.feature.pdfedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.data.model.Document
import com.scantidy.scan.data.repository.DocumentRepository
import com.scantidy.scan.pdf.convert.PdfJpgConverter
import com.scantidy.scan.pdf.encrypt.PdfEncryptor
import com.scantidy.scan.pdf.merge.PdfMerger
import com.scantidy.scan.pdf.watermark.WatermarkConfig
import com.scantidy.scan.pdf.watermark.Watermarker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DocumentRepository,
    private val merger: PdfMerger,
    private val encryptor: PdfEncryptor,
    private val watermarker: Watermarker,
    private val jpgConverter: PdfJpgConverter
) : ViewModel() {

    data class UiState(
        val document: Document? = null,
        val isWorking: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun init(documentId: String) {
        if (_state.value.document?.id == documentId) return
        viewModelScope.launch {
            val doc = repo.getById(documentId)
            _state.update { it.copy(document = doc) }
        }
    }

    fun rotateAll(degrees: Int) = doWork { doc ->
        val src = File(doc.filePath)
        val out = File(src.parentFile, "${doc.nameWithoutExtension}_rotated.pdf")
        merger.rotatePages(src, (0 until doc.pageCount).toList(), degrees, out)
        out
    }

    fun deletePages(indices: List<Int>) = doWork { doc ->
        val src = File(doc.filePath)
        val out = File(src.parentFile, "${doc.nameWithoutExtension}_trimmed.pdf")
        merger.deletePages(src, indices, out)
        out
    }

    fun splitAll() = doWork { doc ->
        val src = File(doc.filePath)
        val outDir = File(src.parentFile, "${doc.nameWithoutExtension}_pages")
        merger.splitEachPage(src, outDir, doc.nameWithoutExtension)
        null
    }

    fun toJpg() = doWork { doc ->
        val src = File(doc.filePath)
        val outDir = File(src.parentFile, "${doc.nameWithoutExtension}_jpg")
        jpgConverter.pdfToJpg(src, outDir, doc.nameWithoutExtension)
        null
    }

    fun encrypt(password: String) = doWork { doc ->
        val src = File(doc.filePath)
        val out = File(src.parentFile, "${doc.nameWithoutExtension}_locked.pdf")
        encryptor.encrypt(src, password, out)
        out
    }

    fun decrypt(password: String) = doWork { doc ->
        val src = File(doc.filePath)
        val out = File(src.parentFile, "${doc.nameWithoutExtension}_unlocked.pdf")
        encryptor.decrypt(src, password, out)
        out
    }

    fun addWatermark(text: String) = doWork { doc ->
        val src = File(doc.filePath)
        val out = File(src.parentFile, "${doc.nameWithoutExtension}_wm.pdf")
        watermarker.add(src, WatermarkConfig(text = text), out)
        out
    }

    private fun doWork(block: suspend (Document) -> File?) {
        val doc = _state.value.document ?: return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null, message = null) }
            try {
                val outFile = withContext(Dispatchers.IO) { block(doc) }
                _state.update {
                    it.copy(
                        isWorking = false,
                        message = if (outFile != null) "Saved: ${outFile.name}" else "Done"
                    )
                }
            } catch (e: Throwable) {
                Timber.e(e, "work failed")
                _state.update { it.copy(isWorking = false, error = e.message ?: "failed") }
            }
        }
    }

    private val Document.nameWithoutExtension: String
        get() = name.removeSuffix(".pdf").removeSuffix(".PDF")
}
