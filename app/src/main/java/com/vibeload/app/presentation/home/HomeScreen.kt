package com.vibeload.app.presentation.home

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeload.app.R
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.presentation.common.VibeHaptic
import com.vibeload.app.presentation.library.LibrarySortOrder
import com.vibeload.app.presentation.library.LibraryViewModel
import com.vibeload.app.presentation.theme.AccentPurple
import java.text.SimpleDateFormat
import java.util.*

/**
 * Spring-physics press micro-interaction inspired by animate-ui.
 * Provides tactile feedback with smooth scale damping (scale ~0.95).
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceScale"
    )
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick() }
            )
        }
}

/**
 * Data model for supported platforms showcase.
 */
data class SupportedPlatform(
    val name: String,
    val iconResId: Int,
    val brandColor: Color,
    val containerBgColor: Color,
    val badge: String,
    val domains: List<String>,
    val supportedTypes: List<String>,
    val quickGuide: String
)

val SUPPORTED_PLATFORMS = listOf(
    SupportedPlatform(
        name = "TikTok",
        iconResId = R.drawable.ic_platform_tiktok,
        brandColor = Color(0xFF000000),
        containerBgColor = Color(0xFF000000), // Solid True Black box for high-contrast TikTok 3D icon
        badge = "Video • MP3 • Ảnh",
        domains = listOf("tiktok.com", "douyin.com"),
        supportedTypes = listOf(
            "🎬 Video Full HD 100% sạch logo watermark",
            "🎵 Tải trực tiếp link Âm thanh gốc (/music/)",
            "📸 Tải toàn bộ album ảnh (chọn lẻ từng ảnh hoặc tất cả)"
        ),
        quickGuide = "Mở TikTok -> Chia sẻ (Share) -> Sao chép liên kết -> Quay lại VibeLoad"
    ),
    SupportedPlatform(
        name = "YouTube",
        iconResId = R.drawable.ic_platform_youtube,
        brandColor = Color(0xFFFF0000),
        containerBgColor = Color(0xFFFEE2E2), // Soft Rose background for red play icon
        badge = "4K • Shorts • MP3",
        domains = listOf("youtube.com", "youtu.be"),
        supportedTypes = listOf(
            "🎬 Video 4K UHD, 2K, 1080p 60fps sắc nét",
            "⚡ Video ngắn YouTube Shorts chất lượng gốc",
            "🎵 Tách âm thanh MP3 từ video ca nhạc để nghe offline"
        ),
        quickGuide = "Mở YouTube -> Chia sẻ -> Sao chép liên kết -> Dán vào VibeLoad"
    ),
    SupportedPlatform(
        name = "SoundCloud",
        iconResId = R.drawable.ic_platform_soundcloud,
        brandColor = Color(0xFFFF5500),
        containerBgColor = Color(0xFFFFEDD5), // Soft Orange background for orange SoundCloud icon
        badge = "Nhạc MP3 320k",
        domains = listOf("soundcloud.com"),
        supportedTypes = listOf(
            "🎵 Tải bài hát MP3 chất lượng cao lên đến 320kbps",
            "🎧 Bản phối độc quyền, remix, DJ set & Podcast",
            "⚡ Tải trực tiếp không cần đăng nhập tài khoản VIP"
        ),
        quickGuide = "Mở SoundCloud -> Chia sẻ (Share) -> Copy Link -> Mở VibeLoad"
    ),
    SupportedPlatform(
        name = "Instagram",
        iconResId = R.drawable.ic_platform_instagram,
        brandColor = Color(0xFFE1306C),
        containerBgColor = Color(0xFFFCE7F3), // Soft Pink background for Instagram icon
        badge = "Reels • Ảnh • Story",
        domains = listOf("instagram.com"),
        supportedTypes = listOf(
            "⚡ Instagram Reels Full HD không mờ",
            "📸 Bài viết ảnh đơn & Album ảnh (Carousel)",
            "📱 Story & Video bài đăng công khai"
        ),
        quickGuide = "Mở Instagram -> Nhấn biểu tượng Chia sẻ -> Sao chép liên kết"
    ),
    SupportedPlatform(
        name = "Facebook",
        iconResId = R.drawable.ic_platform_facebook,
        brandColor = Color(0xFF1877F2),
        containerBgColor = Color(0xFFDBEAFE), // Soft Blue background for Facebook icon
        badge = "Video HD • Reels",
        domains = listOf("facebook.com", "fb.watch"),
        supportedTypes = listOf(
            "🎬 Video HD & 1080p từ Bảng tin & Watch",
            "⚡ Facebook Reels độ phân giải cao",
            "🎵 Tách file âm thanh MP3 từ video"
        ),
        quickGuide = "Mở Facebook -> Chia sẻ video -> Tùy chọn khác -> Sao chép"
    ),
    SupportedPlatform(
        name = "X (Twitter)",
        iconResId = R.drawable.ic_platform_x,
        brandColor = Color(0xFF0F1419),
        containerBgColor = Color(0xFF0F1419), // Deep Dark background for White X
        badge = "Video • GIF • Ảnh",
        domains = listOf("x.com", "twitter.com"),
        supportedTypes = listOf(
            "🎬 Video bài đăng chất lượng gốc 1080p",
            "🎞️ Ảnh động GIF không bị vỡ hạt",
            "📸 Hình ảnh độ phân giải cao"
        ),
        quickGuide = "Mở X -> Nhấn nút Chia sẻ dưới bài đăng -> Sao chép liên kết"
    ),
    SupportedPlatform(
        name = "Threads",
        iconResId = R.drawable.ic_platform_threads,
        brandColor = Color(0xFF8B5CF6),
        containerBgColor = Color(0xFF18181B), // Deep Dark background for White Threads @
        badge = "Video • Media",
        domains = listOf("threads.net"),
        supportedTypes = listOf(
            "🎬 Video chia sẻ trên Threads",
            "📸 Hình ảnh bài đăng chất lượng sắc nét"
        ),
        quickGuide = "Mở Threads -> Nhấn biểu tượng Chia sẻ -> Sao chép liên kết"
    ),
    SupportedPlatform(
        name = "100+ Khác",
        iconResId = R.drawable.ic_platform_more,
        brandColor = Color(0xFF6366F1),
        containerBgColor = Color(0xFFEDE9FE), // Soft Purple background for 100+ icon
        badge = "yt-dlp Core",
        domains = emptyList(),
        supportedTypes = listOf(
            "🌐 Bilibili, Douyin, Kuaishou, Pinterest",
            "📺 Reddit, Vimeo, Twitch, Dailymotion",
            "⚡ Hơn 100+ trang web video & âm thanh toàn cầu"
        ),
        quickGuide = "Sao chép bất kỳ liên kết video nào và dán vào VibeLoad"
    )
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val errorRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedPlatformForGuide by remember { mutableStateOf<SupportedPlatform?>(null) }

    // Dismiss keyboard when analysis starts
    LaunchedEffect(uiState.isAnalyzing) {
        if (uiState.isAnalyzing) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Auto-scroll down to options when analysis succeeds
    LaunchedEffect(uiState.metadata) {
        if (uiState.metadata != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
            kotlinx.coroutines.delay(300)
            scrollState.animateScrollTo(scrollState.maxValue)
            kotlinx.coroutines.delay(200)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Auto-scroll to error message if any
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
            kotlinx.coroutines.delay(200)
            errorRequester.bringIntoView()
        }
    }

    // Check clipboard for smart banner
    var clipboardCandidateUrl by remember { mutableStateOf<String?>(null) }
    var clipboardPlatformName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.urlInput) {
        if (uiState.urlInput.isBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.let {
                if (it.itemCount > 0) it.getItemAt(0).text?.toString()?.trim() else null
            }
            if (!text.isNullOrEmpty() && (text.startsWith("http://") || text.startsWith("https://") || text.contains(".com/") || text.contains(".net/") || text.contains(".be/"))) {
                clipboardCandidateUrl = text
                clipboardPlatformName = SUPPORTED_PLATFORMS.firstOrNull { p ->
                    p.domains.any { d -> text.contains(d, ignoreCase = true) }
                }?.name ?: "Web"
            } else {
                clipboardCandidateUrl = null
                clipboardPlatformName = null
            }
        } else {
            clipboardCandidateUrl = null
            clipboardPlatformName = null
        }
    }

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
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vibe",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Load",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = LocalTextStyle.current.copy(brush = gradientText)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Tải video, âm thanh và ảnh đa nền tảng 100% on-device",
                    fontSize = 12.sp,
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
                        .bounceClick {
                            Toast.makeText(context, "VibeLoad Pro — 100% On-Device • Không quảng cáo • Tải không giới hạn", Toast.LENGTH_SHORT).show()
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
                        .bounceClick { onNavigateToSettings() },
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

        // Smart Clipboard Detection Banner
        clipboardCandidateUrl?.let { url ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .bounceClick {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.onUrlChange(url)
                        viewModel.analyzeUrl()
                        clipboardCandidateUrl = null
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Phát hiện liên kết ${clipboardPlatformName ?: ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = url,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dán & Tải",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Link Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
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
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (uiState.urlInput.isNotBlank()) {
                                viewModel.analyzeUrl()
                            }
                        }
                    ),
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

        val view = LocalView.current
        val gradientQuick = remember {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
            )
        }

        // Action Buttons: "Phân tích" & "⚡ Tải nhanh (Bản nét nhất)"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button 1: "Phân tích"
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradientPrimary)
                    .bounceClick(enabled = !uiState.isAnalyzing && uiState.urlInput.isNotBlank()) {
                        VibeHaptic.click(view)
                        keyboardController?.hide()
                        focusManager.clearFocus()
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
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đang xử lý...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Phân tích",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Button 2: "⚡ Tải nhanh" (One-Tap Best Quality)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradientQuick)
                    .bounceClick(enabled = !uiState.isAnalyzing && uiState.urlInput.isNotBlank()) {
                        VibeHaptic.success(view)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.quickDownloadBestQuality(onNavigateToDownloading)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Tải nhanh",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "Bản nét nhất",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: NỀN TẢNG HỖ TRỢ (Supported Platforms Showcase)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nền tảng hỗ trợ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chạm để dán nhanh hoặc xem định dạng tải",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "100+ Trang",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Row Horizontal Scroll or Grid of Supported Platforms
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SUPPORTED_PLATFORMS.forEach { platform ->
                    ModernPlatformCard(
                        platform = platform,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.let {
                                if (it.itemCount > 0) it.getItemAt(0).text?.toString()?.trim() else null
                            }
                            val hasMatchingUrl = platform.domains.isNotEmpty() && !clipText.isNullOrEmpty() &&
                                    platform.domains.any { clipText.contains(it, ignoreCase = true) }

                            if (hasMatchingUrl) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.onUrlChange(clipText!!)
                                viewModel.analyzeUrl()
                                Toast.makeText(context, "Đã dán liên kết ${platform.name} từ bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedPlatformForGuide = platform
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // SECTION: BẠN CÓ THỂ TẢI NHỮNG GÌ? (Download Capabilities Bento Cards)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bạn có thể tải những gì?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "VibeLoad hỗ trợ xử lý 3 nhóm định dạng chính 100% trên điện thoại:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Bento Capability Cards Carousel (Uniform, Synchronized Dimensions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Video sắc nét
                CapabilityCard(
                    icon = Icons.Default.Videocam,
                    iconGradient = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
                    title = "Video Không Logo",
                    subtitle = "TikTok • Reels • Shorts",
                    description = "Tải video sạch logo watermark. Tự động ghép âm thanh và hình ảnh độ phân giải cao nhất.",
                    badges = listOf("Không Watermark", "360p - 4K UHD")
                )

                // Card 2: Âm thanh & MP3
                CapabilityCard(
                    icon = Icons.Default.MusicNote,
                    iconGradient = listOf(Color(0xFFFF5500), Color(0xFFF59E0B)),
                    title = "Âm Thanh & MP3 320k",
                    subtitle = "SoundCloud • TikTok Music",
                    description = "Tải trực tiếp nhạc SoundCloud 320kbps, liên kết âm thanh gốc TikTok hoặc tách MP3 từ video.",
                    badges = listOf("SoundCloud 320k", "TikTok Gốc & MP3")
                )

                // Card 3: Album ảnh đa tấm
                CapabilityCard(
                    icon = Icons.Default.PhotoLibrary,
                    iconGradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    title = "Album Ảnh Đa Tấm",
                    subtitle = "TikTok Slide • Instagram",
                    description = "Quét trọn bộ album ảnh TikTok và bài viết Instagram. Tùy chọn tick tải từng ảnh hoặc tất cả.",
                    badges = listOf("TikTok Slide", "Instagram Album")
                )
            }
        }

        // Error message if any
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let { errorText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .bringIntoViewRequester(errorRequester),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                        .padding(top = 20.dp)
                        .bringIntoViewRequester(bringIntoViewRequester),
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
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            // Large preview image box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (meta.isImageOnly) 240.dp else 190.dp)
                                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
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
                                                .bounceClick {
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
                                    .bounceClick {
                                        val mp3Opt = meta.audioOptions.firstOrNull { !it.isOriginalAudio && it.bitrateKbps == 128 } ?: meta.audioOptions.firstOrNull()
                                        if (mp3Opt != null) {
                                            viewModel.selectFormat(mp3Opt)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
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
                                            durationSeconds = meta.durationSeconds,
                                            onClick = {
                                                VibeHaptic.tab(view)
                                                viewModel.selectFormat(opt)
                                            }
                                        )
                                    }
                                }
                                1 -> {
                                    meta.audioOptions.filter { !it.isOriginalAudio }.forEach { opt ->
                                        FormatChip(
                                            option = opt,
                                            isSelected = uiState.selectedFormat == opt,
                                            durationSeconds = meta.durationSeconds,
                                            onClick = {
                                                VibeHaptic.tab(view)
                                                viewModel.selectFormat(opt)
                                            }
                                        )
                                    }
                                }
                                2 -> {
                                    meta.audioOptions.filter { it.isOriginalAudio }.forEach { opt ->
                                        FormatChip(
                                            option = opt,
                                            isSelected = uiState.selectedFormat == opt,
                                            durationSeconds = meta.durationSeconds,
                                            onClick = {
                                                VibeHaptic.tab(view)
                                                viewModel.selectFormat(opt)
                                            }
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
        // Wrapped with horizontalScroll & defaultMinSize to guarantee NO SQUISHING on any screen ratio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
                color = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(vertical = 36.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Chưa có tệp nào được tải về",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dán liên kết phía trên để bắt đầu tải xuống video, âm thanh hoặc ảnh",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
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

    // Platform Guide Dialog
    selectedPlatformForGuide?.let { platform ->
        PlatformGuideDialog(
            platform = platform,
            onDismiss = { selectedPlatformForGuide = null },
            onPasteFromClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipText = clipboard?.primaryClip?.let {
                    if (it.itemCount > 0) it.getItemAt(0).text?.toString()?.trim() else null
                }
                if (!clipText.isNullOrEmpty()) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.onUrlChange(clipText)
                    viewModel.analyzeUrl()
                } else {
                    Toast.makeText(context, "Bộ nhớ tạm đang trống!", Toast.LENGTH_SHORT).show()
                }
                selectedPlatformForGuide = null
            }
        )
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

/**
 * Modern Platform Card with tactile bounce, authentic brand icon & capability badge.
 */
@Composable
fun ModernPlatformCard(
    platform: SupportedPlatform,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(138.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(platform.containerBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = platform.iconResId),
                    contentDescription = platform.name,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = platform.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = platform.badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Capability Bento Card showcasing download features (Video, Audio, Album).
 * Synchronized with uniform dimensions, balanced typography, and bottom-pinned badges.
 */
@Composable
fun CapabilityCard(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    description: String,
    badges: List<String>
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(188.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Icon + Title & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(iconGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iconGradient.first(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Description Body (Guaranteed 2 lines of identical height)
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Badges Row (Uniform pinned bottom row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                badges.take(2).forEach { badge ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog guiding users on how to copy links from a specific platform & showing all supported media formats.
 */
@Composable
fun PlatformGuideDialog(
    platform: SupportedPlatform,
    onDismiss: () -> Unit,
    onPasteFromClipboard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(platform.containerBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = platform.iconResId),
                        contentDescription = platform.name,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = platform.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hướng dẫn tải & tính năng hỗ trợ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Section: Supported Features
                Text(
                    text = "Định dạng hỗ trợ:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                platform.supportedTypes.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section: Quick 3-Step Guide
                Text(
                    text = "Cách tải nhanh:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = platform.quickGuide,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPasteFromClipboard,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dán từ bộ nhớ tạm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun FilterPill(
    title: String,
    count: Int?,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val selectedBgColor = MaterialTheme.colorScheme.primary
    val unselectedBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val selectedContentColor = Color.White
    val unselectedContentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)

    val targetBgColor = if (isSelected) selectedBgColor else unselectedBgColor
    val targetContentColor = if (isSelected) selectedContentColor else unselectedContentColor
    val targetBorderColor = if (isSelected) selectedBgColor else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    val animBgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(200), label = "pillBg")
    val animContentColor by animateColorAsState(targetValue = targetContentColor, animationSpec = tween(200), label = "pillContent")
    val animBorderColor by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(200), label = "pillBorder")

    Box(
        modifier = Modifier
            .height(38.dp)
            .defaultMinSize(minWidth = 92.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(animBgColor)
            .border(1.dp, animBorderColor, RoundedCornerShape(19.dp))
            .bounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animContentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (count != null) "$title $count" else title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = animContentColor,
                maxLines = 1,
                softWrap = false
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
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    } else if (item.isAudio) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

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
                        color = MaterialTheme.colorScheme.onSurface,
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
                // Play / View Button (Tonal Primary Pill)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .bounceClick { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isImage) Icons.Default.Visibility else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isImage) "Xem ảnh" else "Phát",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Open Folder Button (Tonal SurfaceVariant Pill)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick { onOpenFolder() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mở thư mục",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Delete Button (Tonal Error Pill)
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .bounceClick { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Xóa",
                            color = MaterialTheme.colorScheme.error,
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
    durationSeconds: Long = 0L,
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
            .bounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = option.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 14.sp
            )
            val sizeText = if (option.filesize > 0L) {
                option.getFormattedSize()
            } else {
                option.getEstimatedSize(durationSeconds)
            }
            if (sizeText.isNotEmpty()) {
                Text(
                    text = sizeText,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}
