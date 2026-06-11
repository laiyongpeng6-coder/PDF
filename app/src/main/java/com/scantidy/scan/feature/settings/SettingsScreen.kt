package com.scantidy.scan.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scantidy.scan.BuildConfig
import com.scantidy.scan.R
import com.scantidy.scan.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onPremiumClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshCount() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 高级版入口 ---
            Button(
                onClick = onPremiumClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(Primary.value)
                )
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.premium_upgrade),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 外观
            SectionTitle(stringResource(R.string.settings_section_appearance))

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "system" to R.string.settings_language_system,
                    "zh" to R.string.settings_language_zh,
                    "en" to R.string.settings_language_en,
                    "ja" to R.string.settings_language_ja,
                    "ko" to R.string.settings_language_ko
                ).forEach { (tag, label) ->
                    FilterChip(
                        selected = state.appLanguage == tag,
                        onClick = { viewModel.setLanguage(tag) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "system" to R.string.settings_theme_system,
                    "light" to R.string.settings_theme_light,
                    "dark" to R.string.settings_theme_dark
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setTheme(mode) },
                        label = { Text(stringResource(label)) }
                    )
                }
            }

            HorizontalDivider()

            // OCR
            SectionTitle(stringResource(R.string.settings_section_ocr))
            Text(
                stringResource(R.string.settings_ocr_languages, state.ocrLanguages.size),
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "zh" to R.string.settings_ocr_lang_zh,
                    "en" to R.string.settings_ocr_lang_en,
                    "ja" to R.string.settings_ocr_lang_ja,
                    "ko" to R.string.settings_ocr_lang_ko
                ).forEach { (tag, label) ->
                    val selected = tag in state.ocrLanguages
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) viewModel.removeOcrLanguage(tag)
                            else viewModel.addOcrLanguage(tag)
                        },
                        label = { Text(stringResource(label)) }
                    )
                }
            }

            HorizontalDivider()

            // 数据
            SectionTitle(stringResource(R.string.settings_section_data))
            OutlinedButton(
                onClick = { viewModel.clearCache() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_clear_cache)) }
            OutlinedButton(
                onClick = { showClearDataDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_clear_data),
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            // 关于
            SectionTitle(stringResource(R.string.settings_section_about))
            Text(
                "${stringResource(R.string.settings_about_version)}: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.settings_about_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.settings_clear_data)) },
            text = { Text(stringResource(R.string.settings_clear_data_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }
                ) { Text(stringResource(R.string.settings_clear_data_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
