package com.scantidy.scan.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.scantidy.scan.R
import com.scantidy.scan.core.fs.AppPaths
import com.scantidy.scan.scan.filter.FilterMode
import timber.log.Timber
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onContinue: (draftId: String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.openCvInitializer.ensureLoaded(context)
        if (state.draftId.isEmpty()) {
            viewModel.setDraftId("draft_${System.currentTimeMillis()}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            if (hasPermission) {
                CameraPreviewWithCapture(
                    onCaptured = { file ->
                        // 关键：用 FileProvider 暴露给上层（这里直接转 Uri）
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        viewModel.addCapturedUri(uri)
                    }
                )
            } else {
                PermissionPlaceholder(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }

            // 滤镜 / 场景切换
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 滤镜
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.filter == mode,
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

                // 控制条
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.removeLast() },
                        enabled = state.capturedPageUris.isNotEmpty(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "撤销")
                    }

                    Text(
                        text = if (state.capturedPageUris.isEmpty()) stringResource(R.string.camera_hint)
                               else "${state.capturedPageUris.size} ${stringResource(R.string.page_unit)}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = { viewModel.continueToEditor(onContinue) },
                        enabled = state.capturedPageUris.isNotEmpty(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.camera_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithCapture(
    onCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    } catch (e: Throwable) {
                        Timber.e(e, "Camera bind failed")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // 大快门按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        ) {
            IconButton(
                onClick = {
                    val file = File(AppPaths.tempDir(context), "capture_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onCaptured(file)
                            }
                            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                Timber.e(exception, "capture failed")
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                // 留空：外圈是快门视觉
            }
        }
    }
}

@Composable
private fun PermissionPlaceholder(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.permission_camera_message),
                color = Color.White
            )
            androidx.compose.material3.TextButton(onClick = onRequest) {
                Text(stringResource(R.string.permission_grant), color = Color.White)
            }
        }
    }
}
