package com.vibeload.app.presentation.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val isAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = true,
    val errorMessage: String? = null
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var currentMediaUri: Uri? = null
    private var progressJob: Job? = null

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initPlayer(context: Context, mediaUri: Uri, title: String, isAudio: Boolean) {
        if (currentMediaUri == mediaUri && exoPlayer != null && _uiState.value.errorMessage == null) {
            return
        }

        releasePlayer()
        currentMediaUri = mediaUri

        _uiState.update {
            it.copy(
                title = title,
                isAudio = isAudio,
                errorMessage = null,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = 0L,
                isPlaying = false
            )
        }

        // Validate URI accessibility before feeding to ExoPlayer
        var accessible = true
        try {
            if (mediaUri.scheme == "content") {
                context.contentResolver.openInputStream(mediaUri)?.use { } ?: run { accessible = false }
            } else if (mediaUri.scheme == "file") {
                val f = java.io.File(mediaUri.path ?: "")
                if (!f.exists() || f.length() == 0L) accessible = false
            }
        } catch (e: Exception) {
            accessible = false
        }

        if (!accessible) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = "Không thể phát tệp phương tiện. Tệp có thể đã bị di chuyển hoặc bị hỏng"
                )
            }
            return
        }

        try {
            val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    val mediaItem = MediaItem.fromUri(mediaUri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true

                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _uiState.update { it.copy(isPlaying = isPlaying) }
                            if (isPlaying) {
                                startProgressUpdates()
                            } else {
                                progressJob?.cancel()
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    _uiState.update { it.copy(isBuffering = true) }
                                }
                                Player.STATE_READY -> {
                                    val dur = duration.coerceAtLeast(0L)
                                    _uiState.update {
                                        it.copy(
                                            isBuffering = false,
                                            durationMs = dur
                                        )
                                    }
                                }
                                Player.STATE_ENDED -> {
                                    _uiState.update { it.copy(isPlaying = false) }
                                    progressJob?.cancel()
                                }
                                Player.STATE_IDLE -> {
                                    _uiState.update { it.copy(isBuffering = false) }
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            _uiState.update {
                                it.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    errorMessage = "Không thể phát tệp phương tiện. Tệp có thể đã bị di chuyển hoặc bị hỏng (${error.localizedMessage})"
                                )
                            }
                        }
                    })
                }

            exoPlayer = player
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = "Lỗi khởi tạo trình phát: ${e.localizedMessage}"
                )
            }
        }
    }

    fun pausePlayer() {
        exoPlayer?.pause()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun releasePlayer() {
        progressJob?.cancel()
        progressJob = null
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) { }
        exoPlayer = null
        currentMediaUri = null
    }

    fun retry(context: Context) {
        val uri = currentMediaUri ?: return
        val state = _uiState.value
        initPlayer(context, uri, state.title, state.isAudio)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = if (dur > 0) dur else it.durationMs
                        )
                    }
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekForward10s() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + 10_000L).coerceAtMost(player.duration)
            player.seekTo(target)
            _uiState.update { it.copy(currentPositionMs = target) }
        }
    }

    fun seekBack10s() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
            player.seekTo(target)
            _uiState.update { it.copy(currentPositionMs = target) }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
