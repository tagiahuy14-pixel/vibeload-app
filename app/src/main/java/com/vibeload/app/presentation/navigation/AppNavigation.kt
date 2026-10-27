package com.vibeload.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.vibeload.app.domain.model.DownloadStatus
import com.vibeload.app.data.local.entity.isImage
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.vibeload.app.presentation.downloading.DownloadingScreen
import com.vibeload.app.presentation.downloading.DownloadingViewModel
import com.vibeload.app.presentation.home.HomeScreen
import com.vibeload.app.presentation.home.HomeViewModel
import com.vibeload.app.presentation.library.LibraryScreen
import com.vibeload.app.presentation.library.LibraryViewModel
import com.vibeload.app.presentation.player.PlayerScreen
import com.vibeload.app.presentation.player.PlayerViewModel
import com.vibeload.app.presentation.settings.SettingsScreen
import com.vibeload.app.presentation.settings.SettingsViewModel
import java.net.URLDecoder

@Composable
fun AppNavigation(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    downloadingViewModel: DownloadingViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeTask by downloadingViewModel.activeTask.collectAsState()
    val isDownloadActive = activeTask?.status?.isActive() == true

    val showBottomBar = currentRoute?.startsWith("player") != true

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0B0F19),
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = androidx.compose.ui.graphics.Color(0xFF60A5FA),
                                unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF64748B),
                                unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF64748B),
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFF1D4ED8)
                            ),
                            icon = {
                                if (screen == Screen.Downloading && isDownloadActive) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)) {
                                                Text("1", color = androidx.compose.ui.graphics.Color.White)
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    libraryViewModel = libraryViewModel,
                    onNavigateToDownloading = {
                        navController.navigate(Screen.Downloading.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPlayMedia = { item ->
                        if (item.isImage) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.mediaUriString), item.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, "Xem ảnh"))
                            } catch (_: Exception) { }
                        } else {
                            navController.navigate(
                                Screen.Player.createRoute(
                                    mediaUri = item.mediaUriString,
                                    title = item.title.ifBlank { item.fileName },
                                    isAudio = item.isAudio
                                )
                            )
                        }
                    }
                )
            }

            composable(Screen.Downloading.route) {
                DownloadingScreen(
                    viewModel = downloadingViewModel,
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onPlayMedia = { item ->
                        if (item.isImage) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.mediaUriString), item.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, "Xem ảnh"))
                            } catch (_: Exception) { }
                        } else {
                            navController.navigate(
                                Screen.Player.createRoute(
                                    mediaUri = item.mediaUriString,
                                    title = item.title.ifBlank { item.fileName },
                                    isAudio = item.isAudio
                                )
                            )
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("mediaUri") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("isAudio") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("mediaUri") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val isAudio = backStackEntry.arguments?.getBoolean("isAudio") ?: false

                val mediaUri = try {
                    String(java.util.Base64.getUrlDecoder().decode(encodedUri), Charsets.UTF_8)
                } catch (e: Exception) {
                    try { URLDecoder.decode(encodedUri, "UTF-8") } catch (_: Exception) { encodedUri }
                }

                val title = try {
                    String(java.util.Base64.getUrlDecoder().decode(encodedTitle), Charsets.UTF_8)
                } catch (e: Exception) {
                    try { URLDecoder.decode(encodedTitle, "UTF-8") } catch (_: Exception) { encodedTitle }
                }

                PlayerScreen(
                    viewModel = playerViewModel,
                    mediaUriString = mediaUri,
                    title = title,
                    isAudio = isAudio,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
