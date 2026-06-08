package com.scantidy.scan.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.data.model.Document
import com.scantidy.scan.data.repository.DocumentRepository
import com.scantidy.scan.pdf.render.PdfRenderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repo: DocumentRepository,
    private val renderEngine: PdfRenderEngine
) : ViewModel() {

    data class UiState(
        val document: Document? = null,
        val pageCount: Int = 0,
        val currentPage: Int = 0,
        val needsPassword: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isNightMode: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var currentDocId: String? = null

    fun init(docId: String) {
        if (currentDocId == docId) return
        currentDocId = docId
        viewModelScope.launch {
            val doc = repo.getById(docId)
            if (doc == null) {
                _state.update { it.copy(error = "document not found") }
                return@launch
            }
            _state.update { it.copy(document = doc, isLoading = true) }
            try {
                val file = java.io.File(doc.filePath)
                if (!file.exists()) {
                    _state.update { it.copy(isLoading = false, error = "file missing") }
                    return@launch
                }
                val pages = renderEngine.open(file)
                _state.update { it.copy(isLoading = false, pageCount = pages) }
            } catch (e: Throwable) {
                if (e.message?.contains("password", ignoreCase = true) == true ||
                    e is IllegalStateException && e.message?.contains("encrypted") == true) {
                    _state.update { it.copy(isLoading = false, needsPassword = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    fun setCurrentPage(p: Int) = _state.update { it.copy(currentPage = p) }

    fun toggleNightMode() = _state.update { it.copy(isNightMode = !it.isNightMode) }

    suspend fun renderPage(index: Int, scale: Float = 2f): android.graphics.Bitmap? = try {
        renderEngine.renderPage(index, scale)
    } catch (e: Throwable) {
        null
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val doc = _state.value.document ?: return@launch
            try {
                val file = java.io.File(doc.filePath)
                val pages = renderEngine.open(file, password)
                _state.update { it.copy(isLoading = false, pageCount = pages, needsPassword = false) }
            } catch (e: Throwable) {
                _state.update { it.copy(isLoading = false, error = "password wrong") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        kotlinx.coroutines.GlobalScope.launch { renderEngine.close() }
    }
}
