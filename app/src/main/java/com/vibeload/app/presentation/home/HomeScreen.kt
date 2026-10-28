package com.vibeload.app.presentation.home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeload.app.R
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.presentation.library.LibrarySortOrder
import com.vibeload.app.presentation.library.LibraryViewModel
import com.vibeload.app.presentation.theme.AccentPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    libraryViewModel: LibraryViewModel,
    onNavigateToDownloading: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPlayMedia: (HistoryEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val downloadedItems by libraryViewModel.items.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    val gradientPrimary = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
        )
    }

    val gradientText = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFFA78BFA))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tải ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Media",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = LocalTextStyle.current.copy(brush = gradientText)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tải video, âm thanh và ảnh đa nền tảng",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Crown VIP Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable {
                            Toast.makeText(context, "VibeLoad Pro — 100% On-Device & Miễn phí", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings Gear Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cài đặt",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Link Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = { viewModel.onUrlChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Dán liên kết video, âm thanh hoặc ảnh tại đây...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                if (uiState.urlInput.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.onUrlChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.pasteFromClipboard(context) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Dán",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Action Button: "Phân tích liên kết"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(gradientPrimary)
                .clickable(enabled = !uiState.isAnalyzing && uiState.urlInput.isNotBlank()) {
                    viewModel.analyzeUrl()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đang phân tích...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phân tích liên kết",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Platform Quick Shortcuts Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PlatformIcon(
                label = "YouTube",
                bgColor = Color(0xFFDC2626),
                icon = Icons.Default.PlayArrow,
                iconTint = Color.White,
                onClick = { handlePlatformClick("youtube.com", viewModel, context) }
            )
            PlatformIcon(
                label = "TikTok",
                bgColor = Color(0xFF0F172A),
                icon = Icons.Default.MusicNote,
                iconTint = Color(0xFF06B6D4),
                onClick = { handlePlatformClick("tiktok.com", viewModel, context) }
            )
            PlatformIcon(
                label = "Facebook",
                bgColor = Color(0xFF1877F2),
                icon = Icons.Default.ThumbUp,
                iconTint = Color.White,
                onClick = { handlePlatformClick("facebook.com", viewModel, context) }
            )
            PlatformIcon(
                label = "Instagram",
                bgColor = Color(0xFFD946EF),
                icon = Icons.Default.CameraAlt,
                iconTint = Color.White,
                onClick = { handlePlatformClick("instagram.com", viewModel, context) }
            )
            PlatformIcon(
                label = "X",
                bgColor = Color(0xFF1E293B),
                icon = Icons.Default.Close,
                iconTint = Color.White,
                onClick = { handlePlatformClick("x.com", viewModel, context) }
            )
            PlatformIcon(
                label = "Thêm",
                bgColor = Color(0xFF1E2434),
                icon = Icons.Default.MoreHoriz,
                iconTint = Color(0xFF94A3B8),
                onClick = {
                    Toast.makeText(context, "Hỗ trợ hơn 100+ trang web thông qua yt-dlp", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Error message if any
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let { errorText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }
        }

        // Analyzed Metadata & Format Selection Sheet
        AnimatedVisibility(visible = uiState.metadata != null) {
            uiState.metadata?.let { meta ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val realImages = meta.imageOptions.filter { it.formatId != "ALL_IMAGES" }
                    val hasMultipleImages = meta.isImageOnly && realImages.size > 1
                    val previewIndex = if (hasMultipleImages) uiState.previewImageIndex.coerceIn(0, (realImages.size - 1).coerceAtLeast(0)) else 0
                    val currentPreviewUrl = if (hasMultipleImages) {
                        realImages.getOrNull(previewIndex)?.formatId ?: meta.thumbnail
                    } else {
                        meta.thumbnail
                    }

                    // Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            // Large preview image box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (meta.isImageOnly) 240.dp else 180.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentPreviewUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = currentPreviewUrl,
                                        contentDescription = meta.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = if (meta.isImageOnly) ContentScale.Fit else ContentScale.Crop
                                    )
                                }

                                if (hasMultipleImages) {
                                    // Prev button
                                    if (previewIndex > 0) {
                                        IconButton(
                                            onClick = { viewModel.setPreviewImageIndex(previewIndex - 1) },
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Ảnh trước",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    // Next button
                                    if (previewIndex < realImages.size - 1) {
                                        IconButton(
                                            onClick = { viewModel.setPreviewImageIndex(previewIndex + 1) },
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Ảnh tiếp theo",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    // Counter pill badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${previewIndex + 1} / ${realImages.size}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = meta.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (meta.isImageOnly) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val realCount = realImages.size
                                        val contentLabel = if (meta.platform.equals("tiktok", ignoreCase = true)) {
                                            if (realCount > 1) "Album ảnh TikTok ($realCount ảnh)" else "Ảnh TikTok"
                                        } else if (realCount > 1) {
                                            "Album ảnh ($realCount ảnh)"
                                        } else {
                                            "Ảnh (${uiState.selectedFormat?.ext?.uppercase() ?: "JPG"})"
                                        }
                                        Text(
                                            text = "Loại: $contentLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (hasMultipleImages) {
                                            Text(
                                                text = "Đang xem ảnh ${previewIndex + 1}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Tác giả: ${meta.uploader}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = meta.getFormattedDuration(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (meta.isImageOnly) {
                        if (hasMultipleImages) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chọn ảnh muốn tải (${uiState.selectedImageIndices.size}/${realImages.size} ảnh)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { viewModel.selectAllImages() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Chọn tất cả", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        TextButton(
                                            onClick = { viewModel.deselectAllImages() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Bỏ chọn", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Horizontal thumbnail gallery
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(realImages) { idx, opt ->
                                        val isChecked = uiState.selectedImageIndices.contains(idx)
                                        val isCurrentPreview = previewIndex == idx

                                        Card(
                                            modifier = Modifier
                                                .width(86.dp)
                                                .clickable {
                                                    viewModel.setPreviewImageIndex(idx)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                width = if (isCurrentPreview) 2.dp else 1.dp,
                                                color = if (isCurrentPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(86.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = opt.formatId,
                                                        contentDescription = "Ảnh ${idx + 1}",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )

                                                    // Checkbox in corner
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isChecked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f))
                                                            .border(1.dp, Color.White, CircleShape)
                                                            .clickable {
                                                                viewModel.toggleSelectImage(idx)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isChecked) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = "Đã chọn",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "Ảnh ${idx + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrentPreview) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrentPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Background music option if available in photo post
                        if (meta.audioOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val isMusicSelected = uiState.selectedFormat?.isImage == false
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val mp3Opt = meta.audioOptions.firstOrNull { !it.isOriginalAudio && it.bitrateKbps == 128 } ?: meta.audioOptions.firstOrNull()
                                        if (mp3Opt != null) {
                                            viewModel.selectFormat(mp3Opt)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isMusicSelected) AccentPurple else MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentPurple)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Nhạc nền bài đăng (MP3)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tải riêng tệp âm thanh MP3 đính kèm của bài viết",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = isMusicSelected,
                                        onClick = {
                                            val mp3Opt = meta.audioOptions.firstOrNull { !it.isOriginalAudio && it.bitrateKbps == 128 } ?: meta.audioOptions.firstOrNull()
                                            if (mp3Opt != null) {
                                                viewModel.selectFormat(mp3Opt)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Format Selection Tabs
                        Text(
                            text = stringResource(R.string.home_format_selection_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TabRow(
                            selectedTabIndex = uiState.formatCategoryTab,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Tab(
                                selected = uiState.formatCategoryTab == 0,
                                onClick = { viewModel.setFormatCategoryTab(0) },
                                text = { Text("Video (MP4)", color = if (uiState.formatCategoryTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (uiState.formatCategoryTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            Tab(
                                selected = uiState.formatCategoryTab == 1,
                                onClick = { viewModel.setFormatCategoryTab(1) },
                                text = { Text("Âm thanh (MP3)", color = if (uiState.formatCategoryTab == 1) AccentPurple else MaterialTheme.colorScheme.onSurfaceVariant) },
                                icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (uiState.formatCategoryTab == 1) AccentPurple else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            Tab(
                                selected = uiState.formatCategoryTab == 2,
                                onClick = { viewModel.setFormatCategoryTab(2) },
                                text = { Text("Gốc", color = if (uiState.formatCategoryTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }

                        // Options chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (uiState.formatCategoryTab) {
                                0 -> {
                                    meta.videoOptions.forEach { opt ->
                                        FormatChip(
                                            option = opt,
                                            isSelected = uiState.selectedFormat == opt,
                                            onClick = { viewModel.selectFormat(opt) }
                                        )
                                    }
                                }
                                1 -> {
                                    meta.audioOptions.filter { !it.isOriginalAudio }.forEach { opt ->
                                        FormatChip(
                                            option = opt,
                                            isSelected = uiState.selectedFormat == opt,
                                            onClick = { viewModel.selectFormat(opt) }
                                        )
                                    }
                                }
                                2 -> {
                                    meta.audioOptions.filter { it.isOriginalAudio }.forEach { opt ->
                                        FormatChip(
                                            option = opt,
                                            isSelected = uiState.selectedFormat == opt,
                                            onClick = { viewModel.selectFormat(opt) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filename Editor
                    Text(
                        text = stringResource(R.string.home_filename_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = uiState.customFilename,
                        onValueChange = { viewModel.onFilenameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Download Button
                    val isAudioSelected = uiState.selectedFormat?.isVideo == false && uiState.selectedFormat?.isImage == false
                    val isImageDownload = meta.isImageOnly && !isAudioSelected
                    val canDownload = if (isImageDownload && hasMultipleImages) {
                        uiState.selectedImageIndices.isNotEmpty()
                    } else {
                        true
                    }

                    Button(
                        onClick = {
                            viewModel.startDownload(onSuccess = onNavigateToDownloading)
                        },
                        enabled = canDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isAudioSelected -> "Tải âm thanh MP3 (${uiState.selectedFormat?.bitrateKbps ?: 128} kbps)"
                                isImageDownload && hasMultipleImages -> {
                                    val selCount = uiState.selectedImageIndices.size
                                    if (selCount == 0) {
                                        "Chọn ít nhất 1 ảnh để tải"
                                    } else if (selCount == realImages.size) {
                                        "Tải tất cả (${realImages.size} ảnh)"
                                    } else {
                                        "Tải $selCount ảnh đã chọn"
                                    }
                                }
                                meta.isImageOnly || uiState.selectedFormat?.isImage == true -> "Tải ${uiState.selectedFormat?.label ?: "ảnh"}"
                                else -> stringResource(R.string.home_download_button)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (canDownload) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Filter Pills: "Tất cả (count)", "Video", "Âm thanh", "Ảnh"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalCount = downloadedItems.size
            FilterPill(
                title = "Tất cả",
                count = totalCount,
                icon = Icons.Default.Layers,
                isSelected = libraryUiState.selectedTab == 0,
                onClick = { libraryViewModel.selectTab(0) }
            )
            FilterPill(
                title = "Video",
                count = null,
                icon = Icons.Default.PlayArrow,
                isSelected = libraryUiState.selectedTab == 1,
                onClick = { libraryViewModel.selectTab(1) }
            )
            FilterPill(
                title = "Âm thanh",
                count = null,
                icon = Icons.Default.MusicNote,
                isSelected = libraryUiState.selectedTab == 2,
                onClick = { libraryViewModel.selectTab(2) }
            )
            FilterPill(
                title = "Ảnh",
                count = null,
                icon = Icons.Default.Image,
                isSelected = libraryUiState.selectedTab == 3,
                onClick = { libraryViewModel.selectTab(3) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Header: "Tệp đã tải về"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tệp đã tải về",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sortLabel = when (libraryUiState.sortOrder) {
                            LibrarySortOrder.NEWEST_FIRST -> "Mới nhất"
                            LibrarySortOrder.OLDEST_FIRST -> "Cũ nhất"
                            LibrarySortOrder.NAME_AZ -> "Tên (A-Z)"
                            LibrarySortOrder.FILE_SIZE_DESC -> "Dung lượng"
                        }
                        Text(
                            text = sortLabel,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mới nhất") },
                            onClick = {
                                libraryViewModel.onSortOrderChange(LibrarySortOrder.NEWEST_FIRST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cũ nhất") },
                            onClick = {
                                libraryViewModel.onSortOrderChange(LibrarySortOrder.OLDEST_FIRST)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tên (A-Z)") },
                            onClick = {
                                libraryViewModel.onSortOrderChange(LibrarySortOrder.NAME_AZ)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dung lượng lớn nhất") },
                            onClick = {
                                libraryViewModel.onSortOrderChange(LibrarySortOrder.FILE_SIZE_DESC)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Kiểu danh sách",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Downloaded Items Cards List
        if (downloadedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa có tệp nào được tải về",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                downloadedItems.forEach { item ->
                    VibeMediaCard(
                        item = item,
                        onPlay = { onPlayMedia(item) },
                        onOpenFolder = { libraryViewModel.openContainingFolder(context, item) },
                        onDelete = { libraryViewModel.requestDelete(item) },
                        onRename = { libraryViewModel.requestRename(item) },
                        onShare = { libraryViewModel.shareItem(context, item) },
                        onOpenWith = { libraryViewModel.openWithExternalApp(context, item) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Rename Dialog from Library
    libraryUiState.renamingItem?.let { item ->
        var newName by remember { mutableStateOf(item.fileName) }
        AlertDialog(
            onDismissRequest = { libraryViewModel.cancelRename() },
            title = { Text("Đổi tên tệp", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Tên mới") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            libraryViewModel.confirmRename(item, newName.trim())
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { libraryViewModel.cancelRename() }) {
                    Text("Hủy")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Delete Confirmation Dialog from Library
    libraryUiState.deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { libraryViewModel.cancelDelete() },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Xóa tệp?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa tệp \"${item.fileName}\"? Tệp sẽ bị xóa khỏi bộ nhớ máy.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { libraryViewModel.confirmDelete(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Xóa vĩnh viễn")
                }
            },
            dismissButton = {
                TextButton(onClick = { libraryViewModel.cancelDelete() }) {
                    Text("Hủy")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun PlatformIcon(
    label: String,
    bgColor: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FilterPill(
    title: String,
    count: Int?,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED)))
    val bgColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) Modifier.background(gradient) else Modifier.background(bgColor)
            )
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (count != null) "$title $count" else title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VibeMediaCard(
    item: HistoryEntity,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(item.completedAt) {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(item.completedAt))
    }

    val formattedSize = remember(item.fileSize) {
        val mb = item.fileSize / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB", mb)
    }

    val durationString = remember(item.durationSeconds) {
        if (item.durationSeconds > 0) {
            val mins = item.durationSeconds / 60
            val secs = item.durationSeconds % 60
            String.format(Locale.US, "%02d:%02d", mins, secs)
        } else {
            ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: thumbnail + info + menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Thumbnail
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnailUri.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.thumbnailUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (item.isImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0D9488), Color(0xFF0284C7), Color(0xFF2563EB))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else if (!item.isAudio) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        // Audio purple-to-blue gradient cover
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF06B6D4))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Duration Badge (bottom right)
                    if (durationString.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = durationString,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { item.fileName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val typeLabel = when {
                        item.isImage -> "${item.fileName.substringAfterLast(".").lowercase()} • ${item.fileName}"
                        item.isAudio -> "mp3 • ${item.fileName}"
                        else -> item.fileName
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$formattedDate • $formattedSize",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3-dots more menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (item.isImage) "Xem ảnh" else "Phát") },
                            onClick = {
                                menuExpanded = false
                                onPlay()
                            },
                            leadingIcon = { Icon(if (item.isImage) Icons.Default.Visibility else Icons.Default.PlayArrow, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Đổi tên") },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Chia sẻ") },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Mở bằng ứng dụng khác") },
                            onClick = {
                                menuExpanded = false
                                onOpenWith()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Xóa", color = Color(0xFFEF4444)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Buttons Row: "Phát" / "Xem ảnh", "Mở thư mục", "Xóa"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / View Button (Blue Pill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E3A8A))
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isImage) Icons.Default.Visibility else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isImage) "Xem ảnh" else "Phát",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Open Folder Button (Dark Blue Pill)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onOpenFolder() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mở thư mục",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Delete Button (Red Pill)
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF450A0A))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Xóa",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormatChip(
    option: FormatOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = option.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 14.sp
            )
            if (option.filesize > 0) {
                Text(
                    text = option.getFormattedSize(),
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun handlePlatformClick(domain: String, viewModel: HomeViewModel, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    val clipText = clipboard?.primaryClip?.let {
        if (it.itemCount > 0) it.getItemAt(0).text?.toString()?.trim() else null
    }

    if (!clipText.isNullOrEmpty() && clipText.contains(domain, ignoreCase = true)) {
        viewModel.onUrlChange(clipText)
        viewModel.analyzeUrl()
    } else {
        Toast.makeText(context, "Dán liên kết từ $domain vào ô nhập để tải", Toast.LENGTH_SHORT).show()
    }
}
