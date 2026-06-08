package com.scantidy.scan.feature.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.data.db.entity.LinkHistoryEntity
import com.scantidy.scan.data.repository.DocumentRepository
import com.scantidy.scan.web.url.UrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LinkInputViewModel @Inject constructor(
    val urlValidator: UrlValidator,
    private val repo: DocumentRepository
) : ViewModel() {

    data class UiState(
        val input: String = "",
        val errorRes: Int? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val history: StateFlow<List<LinkHistoryEntity>> = repo.observeLinkHistory(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInput(v: String) = _state.update { it.copy(input = v, errorRes = null) }

    fun isValid(): Boolean = urlValidator.isValid(_state.value.input)

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearLinkHistory()
        }
    }
}
