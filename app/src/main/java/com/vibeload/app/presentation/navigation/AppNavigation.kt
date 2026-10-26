package com.vibeload.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.vibeload.app.domain.model.DownloadStatus
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Downloading && isDownloadActive) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("1")
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
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
                    onNavigateToDownloading = {
                        navController.navigate(Screen.Downloading.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                        navController.navigate(
                            Screen.Player.createRoute(
                                mediaUri = item.mediaUriString,
                                title = item.title.ifBlank { item.fileName },
                                isAudio = item.isAudio
                            )
                        )
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
                    URLDecoder.decode(encodedUri, "UTF-8")
                } catch (e: Exception) {
                    encodedUri
                }

                val title = try {
                    URLDecoder.decode(encodedTitle, "UTF-8")
                } catch (e: Exception) {
                    encodedTitle
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
