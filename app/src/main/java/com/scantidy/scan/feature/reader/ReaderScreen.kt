package com.scantidy.scan.feature.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scantidy.scan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var pageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordDialog by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(documentId) {
        viewModel.init(documentId)
    }
    LaunchedEffect(state.currentPage, state.pageCount) {
        if (state.pageCount > 0 && state.currentPage < state.pageCount) {
            pageBitmap = viewModel.renderPage(state.currentPage, scale = 2f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.document?.name ?: stringResource(R.string.reader_title),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { passwordDialog = true }) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    IconButton(onClick = { viewModel.toggleNightMode() }) {
                        if (state.isNightMode) {
                            Icon(Icons.Filled.LightMode, contentDescription = null)
                        } else {
                            Icon(Icons.Filled.DarkMode, contentDescription = null)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(if (state.isNightMode) Color.Black else MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                    }
                }
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.needsPassword -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.reader_password_required))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.reader_password_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                        TextButton(
                            onClick = { viewModel.unlockWithPassword(password) },
                            enabled = password.isNotBlank(),
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text(stringResource(R.string.reader_password_unlock)) }
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                pageBitmap != null -> {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    // 翻页控制
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp).align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                if (state.currentPage > 0) viewModel.setCurrentPage(state.currentPage - 1)
                            },
                            enabled = state.currentPage > 0
                        ) { Text("上一页") }
                        Text(
                            stringResource(R.string.reader_page, state.currentPage + 1, state.pageCount),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        TextButton(
                            onClick = {
                                if (state.currentPage < state.pageCount - 1) viewModel.setCurrentPage(state.currentPage + 1)
                            },
                            enabled = state.currentPage < state.pageCount - 1
                        ) { Text("下一页") }
                    }
                }
            }
        }
    }
}
