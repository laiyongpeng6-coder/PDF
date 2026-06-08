package com.scantidy.scan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scantidy.scan.feature.camera.CameraScreen
import com.scantidy.scan.feature.home.HomeScreen
import com.scantidy.scan.feature.link.LinkInputScreen
import com.scantidy.scan.feature.link.LinkRenderScreen
import com.scantidy.scan.feature.pdfedit.PdfEditScreen
import com.scantidy.scan.feature.reader.ReaderScreen
import com.scantidy.scan.feature.settings.SettingsScreen
import com.scantidy.scan.feature.tools.ToolsScreen
import com.scantidy.scan.ui.nav.Destination
import com.scantidy.scan.ui.nav.SubRoute
import com.scantidy.scan.ui.nav.bottomTabs

/**
 * App 顶层 Composable：底部 Tab + 子页面 NavHost
 */
@Composable
fun ScanApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomTabs.map { it.destination.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.destination.route) {
                                        popUpTo(Destination.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.destination.titleRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            // 三个 Tab
            composable(Destination.Home.route) {
                HomeScreen(
                    onScanClick = { navController.navigate(SubRoute.Camera.route) },
                    onLinkClick = { navController.navigate(SubRoute.LinkInput.route) },
                    onOpenDocument = { id -> navController.navigate(SubRoute.Reader.build(id)) },
                    onEditDocument = { id -> navController.navigate(SubRoute.PdfEdit.build(id)) }
                )
            }
            composable(Destination.Tools.route) { ToolsScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }

            // 子页面
            composable(SubRoute.Camera.route) {
                CameraScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { draftId -> navController.navigate(SubRoute.Editor.build(draftId)) }
                )
            }
            composable(
                route = SubRoute.Editor.route,
                arguments = listOf(navArgument("draftId") { type = NavType.StringType })
            ) { back ->
                val draftId = back.arguments?.getString("draftId").orEmpty()
                com.scantidy.scan.feature.editor.EditorScreen(
                    draftId = draftId,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack(Destination.Home.route, inclusive = false)
                    }
                )
            }
            composable(SubRoute.LinkInput.route) {
                LinkInputScreen(
                    onBack = { navController.popBackStack() },
                    onGo = { url -> navController.navigate(SubRoute.LinkRender.build(url)) }
                )
            }
            composable(
                route = SubRoute.LinkRender.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { back ->
                val url = java.net.URLDecoder.decode(back.arguments?.getString("url").orEmpty(), "UTF-8")
                LinkRenderScreen(
                    url = url,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack(Destination.Home.route, inclusive = false)
                    }
                )
            }
            composable(
                route = SubRoute.Reader.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { back ->
                val docId = back.arguments?.getString("docId").orEmpty()
                ReaderScreen(
                    documentId = docId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(SubRoute.PdfEdit.build(docId)) }
                )
            }
            composable(
                route = SubRoute.PdfEdit.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { back ->
                val docId = back.arguments?.getString("docId").orEmpty()
                PdfEditScreen(
                    documentId = docId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
