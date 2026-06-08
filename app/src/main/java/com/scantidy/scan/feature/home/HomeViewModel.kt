package com.scantidy.scan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.data.model.Document
import com.scantidy.scan.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val repo: DocumentRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sortByName = MutableStateFlow(false)
    val sortByName: StateFlow<Boolean> = _sortByName

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    val documents: StateFlow<List<Document>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) {
                if (_sortByName.value) {
                    repo.observeAllByName()
                } else {
                    repo.observeAll()
                }
            } else {
                repo.search(q)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<String>> = repo.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }

    fun toggleSortByName() {
        _sortByName.value = !_sortByName.value
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
        }
    }
}
