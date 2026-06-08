package com.scantidy.scan.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberimport androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scantidy.scan.R
import com.scantidy.scan.scan.filter.FilterMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    draftId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(draftId) {
        viewModel.init(draftId, emptyList())
    }

    // 从相册选图
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
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
            // 多页缩略图
            val pages = state.processed?.pages.orEmpty()
            if (pages.isNotEmpty()) {
                Text(
                    "${stringResource(R.string.editor_title)} · ${pages.size} ${stringResource(R.string.page_unit)}",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pages, key = { it.pageId }) { p ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(p.imageFile.absolutePath)).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            } else if (state.isProcessing) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(R.string.editor_processing, 1, 1),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // 还没有图 → 引导用户选
                OutlinedButton(
                    onClick = {
                        pickImages.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(
                        text = stringResource(R.string.tools_pick_images),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // 滤镜
            Text("滤镜", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.filterMode == mode,
                        onClick = { viewModel.setFilter(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    FilterMode.ORIGINAL -> "原图"
                                    FilterMode.ENHANCED -> "增强"
                                    FilterMode.GRAYSCALE -> "灰度"
                                    FilterMode.BLACK_WHITE -> "黑白"
                                }
                            )
                        }
                    )
                }
            }

            // 文件名
            OutlinedTextField(
                value = state.fileName,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.editor_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 标签
            OutlinedTextField(
                value = state.tagsInput,
                onValueChange = viewModel::setTags,
                label = { Text(stringResource(R.string.editor_tags_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // OCR 开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_ocr), modifier = Modifier.weight(1f))
                Switch(checked = state.enableOcr, onCheckedChange = { viewModel.toggleOcr() })
            }

            // 加密
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_encrypt), modifier = Modifier.weight(1f))
                Switch(checked = state.enablePassword, onCheckedChange = { viewModel.togglePassword() })
            }
            if (state.enablePassword) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text(stringResource(R.string.editor_password_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.passwordConfirm,
                    onValueChange = viewModel::setPasswordConfirm,
                    label = { Text(stringResource(R.string.editor_password_confirm_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 水印
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_watermark), modifier = Modifier.weight(1f))
                Switch(checked = state.enableWatermark, onCheckedChange = { viewModel.toggleWatermark() })
            }
            if (state.enableWatermark) {
                OutlinedTextField(
                    value = state.watermarkText,
                    onValueChange = viewModel::setWatermarkText,
                    label = { Text(stringResource(R.string.editor_watermark_text)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 保存
            Button(
                onClick = { viewModel.save(onDone = onSaved) },
                enabled = pages.isNotEmpty() && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(stringResource(R.string.editor_save_pdf))
            }
        }
    }

    state.error?.let { msg ->
        LaunchedEffect(msg) {
            snackbar.showSnackbar(msg)
            viewModel.dismissError()
        }
    }
}
