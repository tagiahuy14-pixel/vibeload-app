package com.vibeload.app.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.data.local.entity.getFormattedDuration
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.presentation.common.VibeHaptic
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlayMedia: (HistoryEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var isGridView by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thư viện", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        VibeHaptic.tab(view)
                        isGridView = !isGridView
                    }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Chuyển sang danh sách" else "Chuyển sang dạng lưới"
                        )
                    }
                    SortMenu(
                        currentSort = uiState.sortOrder,
                        onSortSelected = { viewModel.onSortOrderChange(it) }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm video hoặc âm thanh...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Sub-tabs (Tất cả, Video, Âm thanh)
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Tất cả") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Video") }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Âm thanh") }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = { Text("Ảnh") }
                )
            }

            // Items List or Empty State
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) {
                                "Không tìm thấy tệp nào khớp với từ khóa"
                            } else {
                                "Chưa có tệp nào được tải về"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        GridMediaItem(
                            item = item,
                            onClick = {
                                VibeHaptic.click(view)
                                onPlayMedia(item)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        com.vibeload.app.presentation.home.VibeMediaCard(
                            item = item,
                            onPlay = { onPlayMedia(item) },
                            onOpenFolder = { viewModel.openContainingFolder(context, item) },
                            onDelete = { viewModel.requestDelete(item) },
                            onRename = { viewModel.requestRename(item) },
                            onShare = { viewModel.shareItem(context, item) },
                            onOpenWith = { viewModel.openWithExternalApp(context, item) }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    uiState.renamingItem?.let { item ->
        var newName by remember { mutableStateOf(item.fileName) }
        AlertDialog(
            onDismissRequest = { viewModel.cancelRename() },
            title = { Text("Đổi tên tệp") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Tên mới") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.confirmRename(item, newName.trim())
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRename() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    uiState.deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Xóa tệp?") },
            text = {
                Text("Bạn có chắc chắn muốn xóa tệp \"${item.fileName}\"? Thao tác này sẽ xóa tệp vĩnh viễn khỏi thiết bị.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa vĩnh viễn")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SortMenu(
    currentSort: LibrarySortOrder,
    onSortSelected: (LibrarySortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Sắp xếp")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Mới nhất") },
                onClick = {
                    onSortSelected(LibrarySortOrder.NEWEST_FIRST)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == LibrarySortOrder.NEWEST_FIRST) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Cũ nhất") },
                onClick = {
                    onSortSelected(LibrarySortOrder.OLDEST_FIRST)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == LibrarySortOrder.OLDEST_FIRST) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Tên (A-Z)") },
                onClick = {
                    onSortSelected(LibrarySortOrder.NAME_AZ)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == LibrarySortOrder.NAME_AZ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Dung lượng lớn nhất") },
                onClick = {
                    onSortSelected(LibrarySortOrder.FILE_SIZE_DESC)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == LibrarySortOrder.FILE_SIZE_DESC) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            )
        }
    }
}

@Composable
fun MediaItemCard(
    item: HistoryEntity,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(item.completedAt) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(item.completedAt))
    }

    val formattedSize = remember(item.fileSize) {
        val mb = item.fileSize / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB", mb)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Media Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (item.isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title.ifBlank { item.fileName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Play & Overflow Menu
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (item.isImage) Icons.Default.Visibility else Icons.Default.PlayCircle,
                    contentDescription = if (item.isImage) "Xem ảnh" else "Phát",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
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
                            onOpenExternal()
                        },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridMediaItem(
    item: HistoryEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.thumbnailUri.isNotEmpty()) {
                AsyncImage(
                    model = item.thumbnailUri,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isAudio) Icons.Default.MusicNote else Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Duration badge at bottom-right
            if (item.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.getFormattedDuration(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Type icon badge at top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isImage -> Icons.Default.Photo
                        item.isAudio -> Icons.Default.MusicNote
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
