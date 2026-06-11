package com.scantidy.scan.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶层导航目的地
 */
sealed class Destination(val route: String, val titleRes: Int) {
    data object Home : Destination("home", com.scantidy.scan.R.string.home_title)
    data object Tools : Destination("tools", com.scantidy.scan.R.string.tools_title)
    data object Settings : Destination("settings", com.scantidy.scan.R.string.settings_title)
}

data class BottomTab(
    val destination: Destination,
    val icon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Destination.Home, Icons.Filled.Folder),
    BottomTab(Destination.Tools, Icons.Filled.Tune),
    BottomTab(Destination.Settings, Icons.Filled.Settings)
)

/**
 * 子页面（不带 tab 栏）
 */
sealed class SubRoute(val route: String) {
    data object Camera : SubRoute("camera")
    data object Editor : SubRoute("editor/{draftId}") {
        fun build(draftId: String) = "editor/$draftId"
    }
    data object LinkInput : SubRoute("link-input")
    data object LinkRender : SubRoute("link-render/{url}") {
        fun build(url: String): String {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            return "link-render/$encoded"
        }
    }
    data object Reader : SubRoute("reader/{docId}") {
        fun build(docId: String) = "reader/$docId"
    }
    data object PdfEdit : SubRoute("pdf-edit/{docId}") {
        fun build(docId: String) = "pdf-edit/$docId"
    }
    data object Premium : SubRoute("premium")
}
