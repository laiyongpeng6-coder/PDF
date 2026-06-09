package com.scantidy.scan.feature.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.core.time.TimeFormat
import com.scantidy.scan.data.model.Document
import com.scantidy.scan.data.repository.DocumentRepository
import com.scantidy.scan.pdf.merge.PdfFromImages
import com.scantidy.scan.scan.filter.FilterMode
import com.scantidy.scan.scan.ocr.OcrLanguage
import com.scantidy.scan.scan.pipeline.ScanPipeline
import com.scantidy.scan.scan.pipeline.ScanResult
import com.scantidy.scan.scan.pipeline.ScanSaveOptions
import com.scantidy.scan.scan.pipeline.DraftStore
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
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanPipeline: ScanPipeline,
    private val pdfFromImages: PdfFromImages,
    private val repo: DocumentRepository,
    private val draftStore: DraftStore
) : ViewModel() {

    data class UiState(
        val draftId: String = "",
        val isProcessing: Boolean = false,
        val isSaving: Boolean = false,
        val processed: ScanResult? = null,
        val fileName: String = "",
        val tagsInput: String = "",
        val enableOcr: Boolean = true,
        val enablePassword: Boolean = false,
        val password: String = "",
        val passwordConfirm: String = "",
        val enableWatermark: Boolean = false,
        val watermarkText: String = "",
        val filterMode: FilterMode = FilterMode.ENHANCED,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun init(draftId: String, capturedUris: List<Uri>) {
        if (_state.value.draftId == draftId && _state.value.processed != null) return
        // 优先用参数传入的 URIs，否则从 DraftStore 读取
        val uris = if (capturedUris.isNotEmpty()) capturedUris else draftStore.take(draftId)
        val now = System.currentTimeMillis()
        _state.value = UiState(
            draftId = draftId,
            fileName = "${context.getString(com.scantidy.scan.R.string.default_scan_name)}_${TimeFormat.forFileName(now)}"
        )
        if (uris.isNotEmpty()) {
            processUris(uris)
        }
    }

    fun setUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) processUris(uris)
    }

    fun setName(v: String) = _state.update { it.copy(fileName = v) }
    fun setTags(v: String) = _state.update { it.copy(tagsInput = v) }
    fun toggleOcr() = _state.update { it.copy(enableOcr = !it.enableOcr) }
    fun togglePassword() = _state.update { it.copy(enablePassword = !it.enablePassword) }
    fun setPassword(v: String) = _state.update { it.copy(password = v) }
    fun setPasswordConfirm(v: String) = _state.update { it.copy(passwordConfirm = v) }
    fun toggleWatermark() = _state.update { it.copy(enableWatermark = !it.enableWatermark) }
    fun setWatermarkText(v: String) = _state.update { it.copy(watermarkText = v) }
    fun setFilter(v: FilterMode) = _state.update { it.copy(filterMode = v) }
    fun dismissError() = _state.update { it.copy(error = null) }

    private fun processUris(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    scanPipeline.process(
                        uris = uris,
                        options = ScanSaveOptions(
                            filterMode = _state.value.filterMode,
                            ocrEnabled = _state.value.enableOcr,
                            ocrLanguages = listOf(OcrLanguage.LATIN, OcrLanguage.CHINESE),
                            enableTextLayer = _state.value.enableOcr
                        )
                    )
                }
                _state.update { it.copy(isProcessing = false, processed = result) }
            } catch (e: Throwable) {
                Timber.e(e, "process failed")
                _state.update { it.copy(isProcessing = false, error = e.message ?: "process failed") }
            }
        }
    }

    /**
     * 把处理结果存为 PDF + 写入元数据
     */
    fun save(
        onDone: () -> Unit
    ) {
        val current = _state.value
        val processed = current.processed ?: run {
            _state.update { it.copy(error = "No processed pages") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val outputPdf = withContext(Dispatchers.IO) {
                    val output = File(AppPaths.documentsDir(context), "${current.fileName}.pdf")
                    val inputs = scanPipeline.toPdfInput(processed)
                    pdfFromImages.create(
                        inputs = inputs,
                        output = output,
                        textLayerEnabled = current.enableOcr
                    )
                    output
                }

                // 写 metadata
                val doc = Document(
                    id = "doc_${System.currentTimeMillis()}",
                    name = "${current.fileName}.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = outputPdf.length(),
                    pageCount = processed.pages.size,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    source = Document.Source.SCAN,
                    isEncrypted = false,
                    hasOcr = current.enableOcr,
                    hasWatermark = false,
                    hasAnnotation = false,
                    thumbnailPath = processed.pages.firstOrNull()?.imageFile?.absolutePath.orEmpty(),
                    filePath = outputPdf.absolutePath,
                    ocrText = if (current.enableOcr) processed.fullOcrText else null,
                    tags = current.tagsInput.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
                )
                repo.save(doc)
                _state.update { it.copy(isSaving = false) }
                onDone()
            } catch (e: Throwable) {
                Timber.e(e, "save failed")
                _state.update { it.copy(isSaving = false, error = e.message ?: "save failed") }
            }
        }
    }
}
