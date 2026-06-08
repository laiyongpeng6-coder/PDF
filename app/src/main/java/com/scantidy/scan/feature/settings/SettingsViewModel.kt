package com.scantidy.scan.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.data.repository.DocumentRepository
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DocumentRepository
) : ViewModel() {

    data class UiState(
        val themeMode: String = "system",  // system / light / dark
        val appLanguage: String = "system",// system / zh / en / ja / ko
        val ocrLanguages: Set<String> = setOf("zh", "en"),
        val documentCount: Int = 0
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setTheme(mode: String) = _state.update { it.copy(themeMode = mode) }
    fun setLanguage(lang: String) = _state.update { it.copy(appLanguage = lang) }
    fun toggleOcrLang(lang: String) = _state.update {
        val newSet = it.ocrLanguages.toMutableSet()
        if (newSet.contains(lang)) newSet.remove(lang) else newSet.add(lang)
        it.copy(ocrLanguages = newSet)
    }

    fun refreshCount() {
        viewModelScope.launch {
            val c = repo.count()
            _state.update { it.copy(documentCount = c) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AppPaths.clearCache(context) }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppPaths.clearAll(context)
                repo.clearAll()
            }
        }
    }
}
