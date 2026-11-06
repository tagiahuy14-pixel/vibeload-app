package com.vibeload.app.presentation.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.media.RingtoneManager
import com.vibeload.app.platform.util.RingtoneHelper
import com.vibeload.app.platform.util.RingtoneResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    mediaUriString: String,
    title: String,
    isAudio: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaUriString) {
        val uri = Uri.parse(mediaUriString)
        viewModel.initPlayer(context, uri, title, isAudio)
    }

    DisposableEffect(mediaUriString) {
        onDispose {
            viewModel.pausePlayer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { title },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Media Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.errorMessage != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.retry(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thử lại", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (isAudio) {
                    // Audio Disc Animation / Icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = uiState.title.ifBlank { title },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val uri = Uri.parse(mediaUriString)
                                    when (val res = RingtoneHelper.setAsRingtone(context, uri, RingtoneManager.TYPE_RINGTONE)) {
                                        is RingtoneResult.Success -> Toast.makeText(context, "Đã cài làm nhạc chuông cuộc gọi! 🔔", Toast.LENGTH_SHORT).show()
                                        is RingtoneResult.PermissionRequired -> {
                                            Toast.makeText(context, "Vui lòng cho phép quyền 'Thay đổi cài đặt hệ thống'", Toast.LENGTH_SHORT).show()
                                            context.startActivity(res.intent)
                                        }
                                        is RingtoneResult.Error -> Toast.makeText(context, "Lỗi: ${res.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cài nhạc chuông", color = Color.White, fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val uri = Uri.parse(mediaUriString)
                                    when (val res = RingtoneHelper.setAsRingtone(context, uri, RingtoneManager.TYPE_ALARM)) {
                                        is RingtoneResult.Success -> Toast.makeText(context, "Đã cài làm chuông báo thức! ⏰", Toast.LENGTH_SHORT).show()
                                        is RingtoneResult.PermissionRequired -> {
                                            Toast.makeText(context, "Vui lòng cho phép quyền 'Thay đổi cài đặt hệ thống'", Toast.LENGTH_SHORT).show()
                                            context.startActivity(res.intent)
                                        }
                                        is RingtoneResult.Error -> Toast.makeText(context, "Lỗi: ${res.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đặt báo thức", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Video PlayerView
                    val player = viewModel.getPlayer()
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = false // Use custom Compose controls
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (uiState.isBuffering) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Controls Section
            PlayerControlsBar(
                isPlaying = uiState.isPlaying,
                currentPosMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                speed = uiState.playbackSpeed,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onRewind10s = { viewModel.seekBack10s() },
                onForward10s = { viewModel.seekForward10s() },
                onSpeedChange = { viewModel.setPlaybackSpeed(it) }
            )
        }
    }
}

@Composable
private fun PlayerControlsBar(
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    speed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind10s: () -> Unit,
    onForward10s: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }

    val formattedCurrent = remember(currentPosMs) { formatDurationMs(currentPosMs) }
    val formattedDuration = remember(durationMs) { formatDurationMs(durationMs) }

    val sliderPosition = if (durationMs > 0) {
        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1B1F))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Slider & Timers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedCurrent,
                color = Color.LightGray,
                style = MaterialTheme.typography.labelSmall
            )
            Slider(
                value = sliderPosition,
                onValueChange = { frac ->
                    onSeek((frac * durationMs).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Text(
                text = formattedDuration,
                color = Color.LightGray,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Button
            Box {
                TextButton(
                    onClick = { speedMenuExpanded = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${speed}x",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = speedMenuExpanded,
                    onDismissRequest = { speedMenuExpanded = false }
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s}x") },
                            onClick = {
                                onSpeedChange(s)
                                speedMenuExpanded = false
                            },
                            leadingIcon = {
                                if (speed == s) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }

            // Rewind 10s
            IconButton(onClick = onRewind10s) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Lùi 10 giây",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Forward 10s
            IconButton(onClick = onForward10s) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Tua tới 10 giây",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Dummy spacer to balance speed button
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

private fun formatDurationMs(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
