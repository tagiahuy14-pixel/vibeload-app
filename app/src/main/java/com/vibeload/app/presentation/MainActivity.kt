package com.vibeload.app.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.vibeload.app.VibeLoadApp
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.platform.coordinator.DownloadCompletedEvent
import com.vibeload.app.platform.update.UpdateDialogState
import com.vibeload.app.presentation.common.VibeHaptic
import com.vibeload.app.presentation.downloading.DownloadingViewModel
import com.vibeload.app.presentation.home.HomeViewModel
import com.vibeload.app.presentation.library.LibraryViewModel
import com.vibeload.app.presentation.navigation.AppNavigation
import com.vibeload.app.presentation.navigation.Screen
import com.vibeload.app.presentation.player.PlayerViewModel
import com.vibeload.app.presentation.settings.SettingsViewModel
import com.vibeload.app.presentation.theme.VibeLoadTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val container by lazy {
        (application as VibeLoadApp).container
    }

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    downloadEngine = container.downloadEngine,
                    downloadCoordinator = container.downloadCoordinator,
                    storageRepository = container.storageRepository
                ) as T
            }
        }
    }

    private val downloadingViewModel: DownloadingViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DownloadingViewModel(
                    downloadCoordinator = container.downloadCoordinator
                ) as T
            }
        }
    }

    private val libraryViewModel: LibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(
                    historyRepository = container.historyRepository,
                    storageRepository = container.storageRepository
                ) as T
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    storageRepository = container.storageRepository,
                    appUpdateManager = container.appUpdateManager
                ) as T
            }
        }
    }

    private val playerViewModel: PlayerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on Android 13+
        requestNotificationPermission()

        // Handle initial intent if app launched with shared link
        handleIncomingIntent(intent)

        setContent {
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            val updateDialogState by container.appUpdateManager.dialogState.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDarkTheme = when (settingsUiState.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    if (window != null) {
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !isDarkTheme
                        insetsController.isAppearanceLightNavigationBars = !isDarkTheme
                        @Suppress("DEPRECATION")
                        window.statusBarColor = if (isDarkTheme) {
                            android.graphics.Color.parseColor("#090D16")
                        } else {
                            android.graphics.Color.parseColor("#F8FAFC")
                        }
                        @Suppress("DEPRECATION")
                        window.navigationBarColor = if (isDarkTheme) {
                            android.graphics.Color.parseColor("#131926")
                        } else {
                            android.graphics.Color.parseColor("#FFFFFF")
                        }
                    }
                }
            }

            // Automatic update check on app launch (background, non-blocking, rate-limited)
            LaunchedEffect(Unit) {
                container.appUpdateManager.checkAutoUpdate()
            }

            var completedBannerEvent by remember { mutableStateOf<DownloadCompletedEvent?>(null) }

            // In-app download completion banner listener with success haptic
            LaunchedEffect(Unit) {
                container.downloadCoordinator.completedEvent.collect { event ->
                    VibeHaptic.success(view)
                    completedBannerEvent = event
                    delay(5000)
                    if (completedBannerEvent == event) {
                        completedBannerEvent = null
                    }
                }
            }

            VibeLoadTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settingsUiState.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            downloadingViewModel = downloadingViewModel,
                            libraryViewModel = libraryViewModel,
                            settingsViewModel = settingsViewModel,
                            playerViewModel = playerViewModel
                        )

                        // In-App Interactive Floating Banner when download finishes
                        AnimatedVisibility(
                            visible = completedBannerEvent != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
                                .zIndex(100f)
                        ) {
                            completedBannerEvent?.let { event ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Đã tải xong tệp!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${event.fileName} (${event.fileSizeFormatted})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                val item = event.historyEntity
                                                completedBannerEvent = null
                                                if (item.isImage) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(android.net.Uri.parse(item.mediaUriString), item.mimeType)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        startActivity(Intent.createChooser(intent, "Xem ảnh"))
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
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Xem ngay", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Automatic Update Dialog
                    if (updateDialogState is UpdateDialogState.Prompt) {
                        val prompt = updateDialogState as UpdateDialogState.Prompt
                        val info = prompt.info

                        AlertDialog(
                            onDismissRequest = {
                                if (!info.isMandatory && !prompt.isDownloading) {
                                    container.appUpdateManager.postponeUpdate()
                                }
                            },
                            title = {
                                Text(
                                    text = "Đã có phiên bản VibeLoad mới!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Phiên bản hiện tại: v${info.currentVersionName} (${info.currentVersionCode})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                    Text(
                                        text = "Phiên bản mới: v${info.latestVersionName} (${info.latestVersionCode})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (info.isMandatory) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ Đây là bản cập nhật bắt buộc theo chính sách phát hành của ứng dụng.",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Nội dung thay đổi:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                        ),
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = info.releaseNotesVi.ifBlank { "Cải thiện hiệu năng và sửa lỗi." },
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                            color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }

                                    if (prompt.isDownloading) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Đang tải APK: ${prompt.downloadProgress}%",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        LinearProgressIndicator(
                                            progress = { prompt.downloadProgress / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    if (prompt.errorMessage != null) {
                                        Text(
                                            text = prompt.errorMessage,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp
                                        )
                                        if (prompt.errorMessage.contains("quyền", ignoreCase = true) || prompt.errorMessage.contains("Cho phép", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { container.appUpdateManager.openInstallPermissionSettings() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Mở Cài đặt cấp quyền", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { container.appUpdateManager.confirmUpdate() },
                                    enabled = !prompt.isDownloading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Text(
                                        text = if (prompt.isDownloading) "Đang tải..." else "Cập nhật ngay"
                                    )
                                }
                            },
                            dismissButton = {
                                if (!info.isMandatory && !prompt.isDownloading) {
                                    TextButton(onClick = { container.appUpdateManager.postponeUpdate() }) {
                                        Text("Để sau", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verify if app has been updated upon returning from PackageInstaller
        container.appUpdateManager.verifyInstalledVersion()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                homeViewModel.handleSharedUrl(sharedText)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
