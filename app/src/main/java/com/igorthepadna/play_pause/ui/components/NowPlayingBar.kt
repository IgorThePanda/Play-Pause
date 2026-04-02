package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import kotlinx.coroutines.delay

@Composable
fun NowPlayingBar(
    player: Player,
    onClick: () -> Unit,
    onDrag: (Float) -> Unit = {},
    onDragStopped: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var isDragging by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
                duration = player.duration
            }
            override fun onPlaybackStateChanged(state: Int) {
                duration = player.duration
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (!isPlaying || isDragging) return@LaunchedEffect
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration
            delay(500) // Reduced frequency to 2Hz for less UI pressure
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
        animationSpec = if (isDragging) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "progress_animation"
    )

    val artworkColors = rememberArtworkColors(
        artworkUri = currentMediaItem?.mediaMetadata?.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.primaryContainer,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 12.dp else 24.dp, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "cornerRadius"
    )

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(76.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDrag(delta)
                },
                onDragStopped = { velocity: Float ->
                    onDragStopped(velocity)
                }
            )
            .pointerInput(player) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        isDragging = true
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        if (duration > 0) {
                            currentPosition = (newProgress * duration).toLong()
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        if (duration > 0) {
                            currentPosition = (newProgress * duration).toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        player.seekTo(currentPosition)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(artworkColors.secondary.copy(alpha = 0.25f))
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentMediaItem?.mediaMetadata?.artworkUri)
                        .crossfade(true)
                        .size(160)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "Not Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    shape = RoundedCornerShape(cornerRadius),
                    color = artworkColors.secondary.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(32.dp),
                            tint = contentColorFor(artworkColors.secondary.copy(alpha = 0.8f))
                        )
                    }
                }
            }
        }
    }
}
