package com.scantidy.scan.feature.link

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.core.time.TimeFormat
import com.scantidy.scan.data.model.Document
import com.scantidy.scan.data.repository.DocumentRepository
import com.scantidy.scan.web.renderer.WebPageRenderer
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
class LinkRenderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renderer: WebPageRenderer,
    private val repo: DocumentRepository
) : ViewModel() {

    data class UiState(
        val url: String = "",
        val isLoading: Boolean = false,
        val progress: Int = 0,
        val error: String? = null,
        val outputPdf: File? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun init(url: String) {
        if (_state.value.url == url) return
        _state.value = UiState(url = url)
        checkInternetAndRender()
    }

    fun hasInternetPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkInternetAndRender() {
        if (!hasInternetPermission()) {
            _state.update { it.copy(error = "INTERNET permission required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val out = withContext(Dispatchers.IO) {
                    val outputDir = AppPaths.documentsDir(context)
                    val file = File(outputDir, "Link_${TimeFormat.forFileName(System.currentTimeMillis())}.pdf")
                    renderer.renderUrlToPdf(_state.value.url, file)
                    file
                }
                _state.update { it.copy(isLoading = false, outputPdf = out, progress = 100) }
                saveAsDocument(out)
                recordLink()
            } catch (e: Throwable) {
                Timber.e(e, "render failed")
                _state.update { it.copy(isLoading = false, error = e.message ?: "render failed") }
            }
        }
    }

    private fun saveAsDocument(file: File) {
        viewModelScope.launch {
            val doc = Document(
                id = "doc_${System.currentTimeMillis()}",
                name = file.name,
                mimeType = "application/pdf",
                sizeBytes = file.length(),
                pageCount = 1,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                source = Document.Source.LINK,
                isEncrypted = false,
                hasOcr = false,
                hasWatermark = false,
                hasAnnotation = false,
                thumbnailPath = file.absolutePath,    // v0.1 简化：缩略图 = pdf 文件
                filePath = file.absolutePath
            )
            repo.save(doc)
        }
    }

    private fun recordLink() {
        viewModelScope.launch {
            repo.recordLinkUsage(_state.value.url, null)
        }
    }
}
