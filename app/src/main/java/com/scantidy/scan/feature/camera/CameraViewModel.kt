package com.scantidy.scan.feature.camera

import androidx.lifecycle.ViewModel
import com.scantidy.scan.scan.OpenCVInitializer
import com.scantidy.scan.scan.filter.FilterMode
import com.scantidy.scan.scan.pipeline.DraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    val openCvInitializer: OpenCVInitializer,
    private val draftStore: DraftStore
) : ViewModel() {

    data class UiState(
        val filter: FilterMode = FilterMode.ENHANCED,
        val capturedPageUris: List<android.net.Uri> = emptyList(),
        val draftId: String = ""
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setFilter(mode: FilterMode) {
        _state.value = _state.value.copy(filter = mode)
    }

    fun addCapturedUri(uri: android.net.Uri) {
        _state.value = _state.value.copy(
            capturedPageUris = _state.value.capturedPageUris + uri
        )
    }

    fun removeLast() {
        val list = _state.value.capturedPageUris
        if (list.isNotEmpty()) {
            _state.value = _state.value.copy(capturedPageUris = list.dropLast(1))
        }
    }

    fun setDraftId(id: String) {
        _state.value = _state.value.copy(draftId = id)
    }

    fun clearAll() {
        _state.value = UiState()
    }

    /**
     * 继续到编辑页：把已拍 URI 存入 DraftStore，再回调
     */
    fun continueToEditor(onContinue: (draftId: String) -> Unit) {
        val s = _state.value
        if (s.capturedPageUris.isNotEmpty() && s.draftId.isNotEmpty()) {
            draftStore.save(s.draftId, s.capturedPageUris)
        }
        onContinue(s.draftId)
    }
}
