package com.vibeload.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Giao diện
            SettingsSection(title = "Giao diện") {
                ThemeOptionRow(
                    title = "Theo hệ thống",
                    selected = uiState.themeMode == "SYSTEM",
                    onClick = { viewModel.setThemeMode("SYSTEM") }
                )
                ThemeOptionRow(
                    title = "Chế độ Sáng",
                    selected = uiState.themeMode == "LIGHT",
                    onClick = { viewModel.setThemeMode("LIGHT") }
                )
                ThemeOptionRow(
                    title = "Chế độ Tối",
                    selected = uiState.themeMode == "DARK",
                    onClick = { viewModel.setThemeMode("DARK") }
                )
            }

            // Section 2: Vị trí lưu trữ
            SettingsSection(title = "Lưu trữ Scoped Storage") {
                SettingsInfoRow(
                    icon = Icons.Default.Movie,
                    label = "Thư mục Video",
                    value = "Movies/VibeLoad"
                )
                SettingsInfoRow(
                    icon = Icons.Default.Audiotrack,
                    label = "Thư mục Âm thanh",
                    value = "Music/VibeLoad"
                )
                SettingsInfoRow(
                    icon = Icons.Default.Storage,
                    label = "Dung lượng bộ nhớ khả dụng",
                    value = uiState.availableStorageFormatted
                )
            }

            // Section 3: Bảo trì & Dữ liệu
            SettingsSection(title = "Bảo trì & Dữ liệu") {
                SettingsActionRow(
                    icon = Icons.Default.CleaningServices,
                    label = "Dọn dẹp bộ nhớ tạm (Cache)",
                    description = "Xóa các tệp tải dở hoặc tạm thời không còn sử dụng",
                    onClick = { viewModel.clearCache() }
                )
                SettingsActionRow(
                    icon = Icons.Default.DeleteSweep,
                    label = "Xóa toàn bộ lịch sử",
                    description = "Xóa danh sách lịch sử tải (không xóa tệp trên máy)",
                    isDestructive = true,
                    onClick = { viewModel.requestClearHistory() }
                )
            }

            // Section 4: Hệ thống & Cập nhật
            SettingsSection(title = "Thông tin & Cập nhật") {
                SettingsInfoRow(
                    icon = Icons.Default.Info,
                    label = "Phiên bản ứng dụng",
                    value = "v${uiState.appVersion}"
                )
                SettingsInfoRow(
                    icon = Icons.Default.Terminal,
                    label = "Engine lõi",
                    value = uiState.engineVersion
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.checkForUpdates() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCheckingUpdate,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang kiểm tra...")
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kiểm tra bản cập nhật")
                    }
                }
            }

            // Section 5: Giới thiệu
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VibeLoad v1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ứng dụng tải video & âm thanh trực tiếp trên thiết bị (100% On-Device, Serverless). Tôn trọng quyền riêng tư, không máy chủ trung gian, không thu thập dữ liệu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Clear History Dialog
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearHistoryDialog() },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Xóa toàn bộ lịch sử?") },
            text = {
                Text("Lịch sử các tệp đã tải sẽ bị xóa khỏi cơ sở dữ liệu ứng dụng. Các tệp đã tải trong bộ nhớ máy (Movies, Music) vẫn được giữ nguyên.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmClearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearHistoryDialog() }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
