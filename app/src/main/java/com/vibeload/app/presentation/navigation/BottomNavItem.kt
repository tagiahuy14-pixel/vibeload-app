package com.vibeload.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Base64

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    data object Downloading : Screen("downloading", "Tải xuống", Icons.Default.FileDownload)
    data object Library : Screen("library", "Thư viện", Icons.Default.Folder)
    data object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)

    data object Player : Screen("player/{mediaUri}/{title}/{isAudio}", "Trình phát", Icons.Default.VideoLibrary) {
        fun createRoute(mediaUri: String, title: String, isAudio: Boolean): String {
            val encodedUri = Base64.getUrlEncoder().withoutPadding().encodeToString(mediaUri.toByteArray(Charsets.UTF_8))
            val encodedTitle = Base64.getUrlEncoder().withoutPadding().encodeToString(title.toByteArray(Charsets.UTF_8))
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
