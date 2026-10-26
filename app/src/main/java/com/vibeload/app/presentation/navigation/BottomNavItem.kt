package com.vibeload.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    data object Downloading : Screen("downloading", "Đang tải", Icons.Default.ArrowDownward)
    data object Library : Screen("library", "Thư viện", Icons.Default.VideoLibrary)
    data object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)

    data object Player : Screen("player/{mediaUri}/{title}/{isAudio}", "Trình phát", Icons.Default.VideoLibrary) {
        fun createRoute(mediaUri: String, title: String, isAudio: Boolean): String {
            val encodedUri = java.net.URLEncoder.encode(mediaUri, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "player/$encodedUri/$encodedTitle/$isAudio"
        }
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Downloading,
    Screen.Library,
    Screen.Settings
)
