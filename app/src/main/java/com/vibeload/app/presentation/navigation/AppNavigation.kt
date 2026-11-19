package com.vibeload.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val coroutineScope = rememberCoroutineScope()
                val count = bottomNavItems.size.coerceAtLeast(1)
                val navPosition = remember { Animatable(selectedIndex.toFloat()) }
                var isDraggingNav by remember { mutableStateOf(false) }

                val currentRouteState by rememberUpdatedState(currentRoute)
                val currentSelectedIndex by rememberUpdatedState(selectedIndex)

                val onNavigateToTab by rememberUpdatedState { targetIndex: Int ->
                    val targetScreen = bottomNavItems.getOrNull(targetIndex) ?: return@rememberUpdatedState
                    val activeRoute = currentRouteState ?: navController.currentDestination?.route
                    if (activeRoute != targetScreen.route) {
                        navController.navigate(targetScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                LaunchedEffect(selectedIndex) {
                    if (!isDraggingNav && navPosition.targetValue != selectedIndex.toFloat()) {
                        navPosition.animateTo(
                            targetValue = selectedIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 420f
                            )
                        )
                    }
                }

                val dockShape = RoundedCornerShape(999.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = dockShape,
                                spotColor = Color(0xFF0071E3).copy(alpha = if (isDark) 0.35f else 0.15f),
                                ambientColor = Color(0xFF00C6FF).copy(alpha = if (isDark) 0.18f else 0.08f)
                            )
                            .clip(dockShape)
                            .background(LiquidGlassTokens.glassSurface(isDark, if (isDark) 0.82f else 0.92f))
                            .border(1.dp, LiquidGlassTokens.specularBorder(isDark), dockShape)
                    ) {
                        // Upper Glossy Sheen along the floating dock
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(topStart = 999.dp, topEnd = 999.dp))
                                .background(LiquidGlassTokens.glossySheenBrush(if (isDark) 0.10f else 0.25f))
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .pointerInput(count) {
                                    val touchSlop = viewConfiguration.touchSlop
                                    val tabWidthPx = size.width.toFloat() / count
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startX = down.position.x
                                        var activeIndex = (startX / tabWidthPx).toInt().coerceIn(0, count - 1)
                                        var hasDragged = false
                                        var completedGesture = false

                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    change.consume()
                                                    completedGesture = true
                                                    break
                                                }
                                                val currentX = change.position.x
                                                val deltaX = abs(currentX - startX)

                                                if (!hasDragged && deltaX > touchSlop) {
                                                    hasDragged = true
                                                    isDraggingNav = true
                                                }

                                                if (hasDragged) {
                                                    val currentFraction = ((currentX - tabWidthPx / 2f) / tabWidthPx).coerceIn(0f, (count - 1).toFloat())
                                                    coroutineScope.launch {
                                                        navPosition.snapTo(currentFraction)
                                                    }

                                                    val hoveredIndex = currentFraction.roundToInt().coerceIn(0, count - 1)
                                                    if (hoveredIndex != activeIndex) {
                                                        activeIndex = hoveredIndex
                                                        VibeHaptic.tab(view)
                                                    }
                                                    change.consume()
                                                }
                                            }
                                        } finally {
                                            isDraggingNav = false
                                            if (completedGesture) {
                                                val finalIndex = if (hasDragged) {
                                                    navPosition.value.roundToInt().coerceIn(0, count - 1)
                                                } else {
                                                    (startX / tabWidthPx).toInt().coerceIn(0, count - 1)
                                                }

                                                if (!hasDragged) {
                                                    VibeHaptic.tab(view)
                                                }

                                                onNavigateToTab(finalIndex)

                                                coroutineScope.launch {
                                                    navPosition.animateTo(
                                                        targetValue = finalIndex.toFloat(),
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = 420f
                                                        )
                                                    )
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    navPosition.animateTo(
                                                        targetValue = currentSelectedIndex.toFloat(),
                                                        animationSpec = spring(
                                                            dampingRatio = 0.82f,
                                                            stiffness = 420f
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            val tabWidth = maxWidth / count
                            val capsuleWidth = tabWidth - 8.dp
                            val capsuleHeight = 48.dp
                            val capsuleShape = RoundedCornerShape(999.dp)

                            // Sliding Liquid Glass Capsule Indicator
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = tabWidth * navPosition.value + 4.dp,
                                        y = 4.dp
                                    )
                                    .width(capsuleWidth)
                                    .height(capsuleHeight)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = capsuleShape,
                                        spotColor = Color(0xFF0071E3).copy(alpha = if (isDark) 0.40f else 0.22f)
                                    )
                                    .clip(capsuleShape)
                                    .background(
                                        if (isDark) {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF0071E3).copy(alpha = 0.38f),
                                                    Color(0xFF00C6FF).copy(alpha = 0.18f)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF0071E3).copy(alpha = 0.18f),
                                                    Color(0xFF00C6FF).copy(alpha = 0.08f)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        LiquidGlassTokens.specularBorder(isDark, if (isDark) 0.60f else 0.90f),
                                        capsuleShape
                                    )
                            ) {
                                // Capsule Upper Specular Sheen
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(topStart = 999.dp, topEnd = 999.dp))
                                        .background(LiquidGlassTokens.glossySheenBrush(if (isDark) 0.22f else 0.40f))
                                )
                            }

                            // Row of Nav Tabs
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentActiveIndex = if (isDraggingNav) navPosition.value.roundToInt().coerceIn(0, count - 1) else selectedIndex

                                bottomNavItems.forEachIndexed { index, screen ->
                                    val selected = currentActiveIndex == index
                                    val itemColor by animateColorAsState(
                                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "tabColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
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
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title,
                                                    tint = itemColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

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
        },
        containerColor = Color.Transparent
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
