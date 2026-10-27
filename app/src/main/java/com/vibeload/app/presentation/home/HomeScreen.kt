package com.vibeload.app.presentation.home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    var formatCategoryTab by remember { mutableIntStateOf(0) } // 0: Video, 1: MP3, 2: Gốc
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
            .background(Color(0xFF090D16))
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
                        color = Color.White
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
                    text = "Tải video và âm thanh từ nhiều nền tảng",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Crown VIP Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131926))
                        .border(1.dp, Color(0xFF26324B), CircleShape)
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
                        .background(Color(0xFF131926))
                        .border(1.dp, Color(0xFF26324B), CircleShape)
                        .clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cài đặt",
                        tint = Color(0xFFCBD5E1),
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
                .background(Color(0xFF111622))
                .border(1.dp, Color(0xFF26324B), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = { viewModel.onUrlChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Dán liên kết video tại đây...",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color(0xFF38BDF8)
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
                            tint = Color(0xFF64748B),
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
                            tint = Color(0xFF38BDF8),
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
                    // Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131926)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26324B))
                    ) {
                        Column {
                            if (meta.thumbnail.isNotEmpty()) {
                                AsyncImage(
                                    model = meta.thumbnail,
                                    contentDescription = meta.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = meta.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (meta.isImageOnly) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val imageOpt = uiState.selectedFormat ?: meta.imageOptions.firstOrNull()
                                        val realCount = meta.imageOptions.count { it.formatId != "ALL_IMAGES" }
                                        val contentLabel = if (meta.platform.equals("tiktok", ignoreCase = true)) {
                                            if (realCount > 1) "Album ảnh TikTok ($realCount ảnh)" else "Ảnh TikTok"
                                        } else if (realCount > 1) {
                                            "Album ảnh ($realCount ảnh)"
                                        } else {
                                            "Ảnh (${imageOpt?.ext?.uppercase() ?: "JPG"})"
                                        }
                                        Text(
                                            text = "Loại: $contentLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                        if (imageOpt != null && imageOpt.filesize > 0) {
                                            val mb = imageOpt.filesize / (1024.0 * 1024.0)
                                            val sizeStr = if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%.0f KB", imageOpt.filesize / 1024.0)
                                            Text(
                                                text = "Kích thước: $sizeStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8)
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
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = meta.getFormattedDuration(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (meta.isImageOnly) {
                        Text(
                            text = "Định dạng ảnh",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            meta.imageOptions.forEach { opt ->
                                FormatChip(
                                    option = opt,
                                    isSelected = uiState.selectedFormat == opt,
                                    onClick = { viewModel.selectFormat(opt) }
                                )
                            }
                        }
                    } else {
                        // Format Selection Tabs
                        Text(
                            text = stringResource(R.string.home_format_selection_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        TabRow(
                            selectedTabIndex = formatCategoryTab,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            containerColor = Color(0xFF131926)
                        ) {
                            Tab(
                                selected = formatCategoryTab == 0,
                                onClick = { formatCategoryTab = 0 },
                                text = { Text("Video (MP4)", color = if (formatCategoryTab == 0) Color.White else Color(0xFF94A3B8)) },
                                icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (formatCategoryTab == 0) Color(0xFF38BDF8) else Color(0xFF64748B)) }
                            )
                            Tab(
                                selected = formatCategoryTab == 1,
                                onClick = { formatCategoryTab = 1 },
                                text = { Text("Âm thanh (MP3)", color = if (formatCategoryTab == 1) Color.White else Color(0xFF94A3B8)) },
                                icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (formatCategoryTab == 1) Color(0xFFA78BFA) else Color(0xFF64748B)) }
                            )
                            Tab(
                                selected = formatCategoryTab == 2,
                                onClick = { formatCategoryTab = 2 },
                                text = { Text("Gốc", color = if (formatCategoryTab == 2) Color.White else Color(0xFF94A3B8)) }
                            )
                        }

                        // Options chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (formatCategoryTab) {
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
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = uiState.customFilename,
                        onValueChange = { viewModel.onFilenameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF26324B),
                            focusedContainerColor = Color(0xFF131926),
                            unfocusedContainerColor = Color(0xFF131926)
                        )
                    )

                    // Download Button
                    Button(
                        onClick = {
                            viewModel.startDownload(onSuccess = onNavigateToDownloading)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedFormat?.formatId == "ALL_IMAGES") {
                                val c = meta.imageOptions.count { it.formatId != "ALL_IMAGES" }
                                "Tải tất cả ảnh ($c)"
                            } else if (meta.isImageOnly || uiState.selectedFormat?.isImage == true) {
                                "Tải ${uiState.selectedFormat?.label ?: "ảnh"}"
                            } else {
                                stringResource(R.string.home_download_button)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
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
            title = { Text("Đổi tên tệp", color = Color.White) },
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
            containerColor = Color(0xFF131926)
        )
    }

    // Delete Confirmation Dialog from Library
    libraryUiState.deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { libraryViewModel.cancelDelete() },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Xóa tệp?", color = Color.White) },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa tệp \"${item.fileName}\"? Tệp sẽ bị xóa khỏi bộ nhớ máy.",
                    color = Color(0xFFCBD5E1)
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
            containerColor = Color(0xFF131926)
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
            color = Color(0xFFCBD5E1),
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
    val bgColor = if (isSelected) Color.Transparent else Color(0xFF131926)
    val borderColor = if (isSelected) Color.Transparent else Color(0xFF26324B)

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
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (count != null) "$title $count" else title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
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
            .background(Color(0xFF131926))
            .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131926))
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
                        .background(Color(0xFF1E2434)),
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
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$formattedDate • $formattedSize",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
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
                            tint = Color(0xFF94A3B8)
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
    val bgColor = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF131926)
    val borderColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF26324B)
    val textColor = if (isSelected) Color.White else Color(0xFFCBD5E1)

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
