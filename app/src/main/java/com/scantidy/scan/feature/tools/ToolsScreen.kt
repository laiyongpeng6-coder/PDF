package com.scantidy.scan.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scantidy.scan.R

private data class ToolItem(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onScan: () -> Unit = {},
    onLink: () -> Unit = {}
) {
    val items = listOf(
        ToolItem(R.string.home_fab_scan, R.string.camera_title, Icons.Filled.CameraAlt, onScan),
        ToolItem(R.string.home_fab_link, R.string.link_title, Icons.Filled.Link, onLink),
        ToolItem(R.string.tools_merge, R.string.tools_merge, Icons.Filled.Description, {}),
        ToolItem(R.string.tools_split, R.string.tools_split, Icons.Filled.Folder, {}),
        ToolItem(R.string.tools_jpg_to_pdf, R.string.tools_jpg_to_pdf, Icons.Filled.Image, {}),
        ToolItem(R.string.tools_pdf_to_jpg, R.string.tools_pdf_to_jpg, Icons.Filled.PhotoSizeSelectLarge, {}),
        ToolItem(R.string.tools_encrypt, R.string.tools_encrypt, Icons.Filled.Lock, {}),
        ToolItem(R.string.tools_decrypt, R.string.tools_decrypt, Icons.Filled.LockOpen, {}),
        ToolItem(R.string.tools_watermark, R.string.tools_watermark, Icons.Filled.WaterDrop, {})
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tools_title)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { tool ->
                ToolCard(tool)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ToolCard(tool: ToolItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = tool.onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                tool.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(tool.titleRes),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
