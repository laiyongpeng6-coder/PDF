package com.scantidy.scan.feature.camera

import androidx.lifecycle.ViewModel
import com.scantidy.scan.scan.OpenCVInitializer
import com.scantidy.scan.scan.filter.FilterMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    val openCvInitializer: OpenCVInitializer
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
}
