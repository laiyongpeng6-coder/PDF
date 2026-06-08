package com.scantidy.scan.feature.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.scantidy.scan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkRenderScreen(
    url: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: LinkRenderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(url) {
        viewModel.init(url)
    }

    LaunchedEffect(state.outputPdf) {
        if (state.outputPdf != null) {
            // 简化：保存完成直接返回
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.link_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(url, style = MaterialTheme.typography.bodyLarge)
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.link_loading), modifier = Modifier.padding(top = 8.dp))
                        LinearProgressIndicator(
                            progress = { (state.progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                    }
                }
            }
            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.link_internet_required), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
