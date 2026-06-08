package com.scantidy.scan.feature.pdfedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scantidy.scan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditScreen(
    documentId: String,
    onBack: () -> Unit,
    viewModel: PdfEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var passwordDialog by remember { mutableStateOf<Mode?>(null) }
    var watermarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) { viewModel.init(documentId) }
    state.message?.let { msg ->
        LaunchedEffect(msg) {
            snackbar.showSnackbar(msg)
            viewModel.let { /* 一次性提示，不清空避免重复 */ }
        }
    }
    state.error?.let { err ->
        LaunchedEffect(err) {
            snackbar.showSnackbar(err)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pdfedit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.document?.let { doc ->
                Text(doc.name, style = MaterialTheme.typography.titleMedium)
                Text("${doc.pageCount} ${stringResource(R.string.page_unit)}", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = { viewModel.rotateAll(90) },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_rotate) + " 90°") }

            OutlinedButton(
                onClick = { viewModel.splitAll() },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_split) + "（每页一个文件）") }

            OutlinedButton(
                onClick = { viewModel.toJpg() },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_to_jpg)) }

            OutlinedButton(
                onClick = { passwordDialog = Mode.ENCRYPT },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_encrypt)) }

            OutlinedButton(
                onClick = { passwordDialog = Mode.DECRYPT },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_decrypt)) }

            OutlinedButton(
                onClick = { watermarkDialog = true },
                enabled = !state.isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.pdfedit_watermark)) }
        }
    }

    passwordDialog?.let { mode ->
        PasswordDialog(
            mode = mode,
            onDismiss = { passwordDialog = null },
            onConfirm = { pwd ->
                when (mode) {
                    Mode.ENCRYPT -> viewModel.encrypt(pwd)
                    Mode.DECRYPT -> viewModel.decrypt(pwd)
                }
                passwordDialog = null
            }
        )
    }

    if (watermarkDialog) {
        WatermarkDialog(
            onDismiss = { watermarkDialog = false },
            onConfirm = { text ->
                viewModel.addWatermark(text)
                watermarkDialog = false
            }
        )
    }
}

private enum class Mode { ENCRYPT, DECRYPT }

@Composable
private fun PasswordDialog(mode: Mode, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (mode) {
                    Mode.ENCRYPT -> stringResource(R.string.pdfedit_encrypt)
                    Mode.DECRYPT -> stringResource(R.string.pdfedit_decrypt)
                }
            )
        },
        text = {
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it },
                label = { Text(stringResource(R.string.editor_password_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pwd) },
                enabled = pwd.length >= 6
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun WatermarkDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("PDF 极扫") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_watermark_text)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
