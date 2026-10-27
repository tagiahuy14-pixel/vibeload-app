package com.vibeload.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.vibeload.app.VibeLoadApp
import com.vibeload.app.platform.update.UpdateDialogState
import com.vibeload.app.presentation.downloading.DownloadingViewModel
import com.vibeload.app.presentation.home.HomeViewModel
import com.vibeload.app.presentation.library.LibraryViewModel
import com.vibeload.app.presentation.navigation.AppNavigation
import com.vibeload.app.presentation.player.PlayerViewModel
import com.vibeload.app.presentation.settings.SettingsViewModel
import com.vibeload.app.presentation.theme.VibeLoadTheme

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

            // Automatic update check on app launch (background, non-blocking, rate-limited)
            LaunchedEffect(Unit) {
                container.appUpdateManager.checkAutoUpdate()
            }

            VibeLoadTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    AppNavigation(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        downloadingViewModel = downloadingViewModel,
                        libraryViewModel = libraryViewModel,
                        settingsViewModel = settingsViewModel,
                        playerViewModel = playerViewModel
                    )

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
                                    color = Color.White
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
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "Phiên bản mới: v${info.latestVersionName} (${info.latestVersionCode})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF38BDF8)
                                    )

                                    if (info.isMandatory) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ Đây là bản cập nhật bắt buộc theo chính sách phát hành của ứng dụng.",
                                                color = Color(0xFFFCA5A5),
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
                                        color = Color.White
                                    )
                                    Text(
                                        text = info.releaseNotesVi.ifBlank { "Cải thiện hiệu năng và sửa lỗi." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )

                                    if (prompt.isDownloading) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Đang tải APK: ${prompt.downloadProgress}%",
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        LinearProgressIndicator(
                                            progress = { prompt.downloadProgress / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF2563EB)
                                        )
                                    }

                                    if (prompt.errorMessage != null) {
                                        Text(
                                            text = prompt.errorMessage,
                                            color = Color(0xFFEF4444),
                                            fontSize = 12.sp
                                        )
                                        if (prompt.errorMessage.contains("quyền", ignoreCase = true) || prompt.errorMessage.contains("Cho phép", ignoreCase = true)) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { container.appUpdateManager.openInstallPermissionSettings() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Mở Cài đặt cấp quyền", color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { container.appUpdateManager.confirmUpdate() },
                                    enabled = !prompt.isDownloading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text(
                                        text = if (prompt.isDownloading) "Đang tải..." else "Cập nhật ngay",
                                        color = Color.White
                                    )
                                }
                            },
                            dismissButton = {
                                if (!info.isMandatory && !prompt.isDownloading) {
                                    TextButton(onClick = { container.appUpdateManager.postponeUpdate() }) {
                                        Text("Để sau", color = Color(0xFF94A3B8))
                                    }
                                }
                            },
                            containerColor = Color(0xFF131926)
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
