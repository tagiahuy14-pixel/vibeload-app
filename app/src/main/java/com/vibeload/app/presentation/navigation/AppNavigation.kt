package com.vibeload.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vibeload.app.presentation.common.LiquidGlassTokens
import com.vibeload.app.presentation.common.VibeHaptic
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
import com.vibeload.app.presentation.theme.isAppDarkTheme
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
    val view = LocalView.current
    val isDark = isAppDarkTheme()

    val selectedIndex = remember(currentRoute) {
        bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 420f
        ),
        label = "liquidNavIndicator"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            spotColor = Color(0xFF0066FF).copy(alpha = if (isDark) 0.20f else 0.10f),
                            ambientColor = Color(0xFF10B981).copy(alpha = if (isDark) 0.12f else 0.06f)
                        )
                        .background(LiquidGlassTokens.glassSurface(isDark, if (isDark) 0.88f else 0.94f))
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.05f else 0.30f),
                                        Color.White.copy(alpha = if (isDark) 0.40f else 0.90f),
                                        Color(0xFF00C6FF).copy(alpha = if (isDark) 0.45f else 0.60f),
                                        Color.White.copy(alpha = if (isDark) 0.25f else 0.70f),
                                        Color.White.copy(alpha = if (isDark) 0.05f else 0.25f)
                                    )
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = strokeWidth
                            )
                        }
                        .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                        .height(68.dp)
                ) {
                    // Upper Glossy Sheen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(LiquidGlassTokens.glossySheenBrush(if (isDark) 0.08f else 0.20f))
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val count = bottomNavItems.size.coerceAtLeast(1)
                        val tabWidth = maxWidth / count
                        val capsuleWidth = tabWidth - 16.dp
                        val capsuleHeight = 52.dp

                        // Sliding Liquid Glass Capsule Indicator
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = tabWidth * animatedIndex + 8.dp,
                                    y = 8.dp
                                )
                                .width(capsuleWidth)
                                .height(capsuleHeight)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF0071E3).copy(alpha = 0.28f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isDark) {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0071E3).copy(alpha = 0.30f),
                                                Color(0xFF00C6FF).copy(alpha = 0.14f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0071E3).copy(alpha = 0.15f),
                                                Color(0xFF00C6FF).copy(alpha = 0.07f)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    1.dp,
                                    LiquidGlassTokens.specularBorder(isDark, if (isDark) 0.45f else 0.85f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            // Capsule Upper Specular Sheen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(LiquidGlassTokens.glossySheenBrush(if (isDark) 0.20f else 0.38f))
                            )
                        }

                        // Row of Clickable Tabs
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavItems.forEachIndexed { index, screen ->
                                val selected = selectedIndex == index
                                val itemColor by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "tabColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (currentRoute != screen.route) {
                                                VibeHaptic.tab(view)
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (screen == Screen.Downloading && isDownloadActive) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                        Text("1", color = Color.White)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title,
                                                    tint = itemColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title,
                                                tint = itemColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = itemColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
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
