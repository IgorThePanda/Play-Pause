package com.igorthepadna.play_pause.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.SquigglySlider
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.formatDuration
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import kotlinx.coroutines.delay

@Composable
fun PlaybackShockWave(color: Color, isPlaying: Boolean) {
    val pauseWaveProgress = remember { Animatable(0f) }
    val playWaveProgress = remember { Animatable(0f) }
    
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            playWaveProgress.snapTo(0f)
            pauseWaveProgress.snapTo(0f)
            pauseWaveProgress.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        } else {
            pauseWaveProgress.snapTo(0f)
            playWaveProgress.snapTo(0f)
            playWaveProgress.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
    }

    Box(modifier = Modifier.wrapContentSize(unbounded = true), contentAlignment = Alignment.Center) {
        if (pauseWaveProgress.value > 0f && pauseWaveProgress.value < 1f) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        val scale = 5f - (pauseWaveProgress.value * 4f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - pauseWaveProgress.value) * 0.4f
                    }
                    .background(color, CircleShape)
            )
        }
        if (playWaveProgress.value > 0f && playWaveProgress.value < 1f) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        val scale = 1f + (playWaveProgress.value * 5f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - playWaveProgress.value) * 0.4f
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    player: Player, 
    useArtworkAccent: Boolean,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }

    var shuffleModeEnabled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }

    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var wasPlayingBeforeDrag by remember { mutableStateOf(false) }
    
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                duration = if (player.duration > 0) player.duration else 0L
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
                currentPosition = 0L
                duration = if (player.duration > 0) player.duration else 0L
            }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleModeEnabled = enabled
            }
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = if (player.currentPosition > 0) player.currentPosition else 0L
            if (duration <= 0 && player.duration > 0) {
                duration = player.duration
            }
            delay(500)
        }
    }

    val extractedColors = rememberArtworkColors(
        artworkUri = currentMediaItem?.mediaMetadata?.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val artworkColors = if (useArtworkAccent) {
        extractedColors
    } else {
        ArtworkColors(
            primary = MaterialTheme.colorScheme.surface,
            secondary = MaterialTheme.colorScheme.primary
        )
    }

    val bitrateStr = currentMediaItem?.mediaMetadata?.extras?.getString("bitrate") ?: "320 kbps"
    val bitrateValue = bitrateStr.filter { it.isDigit() }.toIntOrNull() ?: 320

    val qualityIcon = when {
        bitrateValue >= 1000 -> Icons.Rounded.Album
        bitrateValue >= 256 -> Icons.Rounded.HighQuality
        else -> Icons.Rounded.Sd
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "art_breathing"
    )
    
    val artCorner by animateDpAsState(
        targetValue = if (isPlaying) 28.dp else 80.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "art_corner"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle dynamic glow background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to artworkColors.primary.copy(alpha = if (useArtworkAccent) 0.1f else 0f),
                            1f to MaterialTheme.colorScheme.surface
                        )
                    )
            )

            // Wave background layer
            if (buttonCenter != Offset.Zero) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(-1f)
                ) {
                    Box(
                        modifier = Modifier.offset {
                            val waveHalfSize = with(density) { 50.dp.toPx() }
                            IntOffset(
                                (buttonCenter.x - waveHalfSize).toInt(),
                                (buttonCenter.y - waveHalfSize).toInt()
                            )
                        }
                    ) {
                        PlaybackShockWave(color = artworkColors.secondary, isPlaying = isPlaying)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", modifier = Modifier.size(32.dp))
                    }
                    
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue", modifier = Modifier.size(24.dp))
                    }
                }

                if (showQueueSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showQueueSheet = false },
                        sheetState = queueSheetState
                    ) {
                        QueueContent(player)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Artwork
                Surface(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth(0.9f)
                        .graphicsLayer {
                            scaleX = artScale
                            scaleY = artScale
                        }
                        .clip(RoundedCornerShape(artCorner)),
                    tonalElevation = 12.dp,
                    shadowElevation = 24.dp
                ) {
                    AsyncImage(
                        model = currentMediaItem?.mediaMetadata?.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )
                }

                Spacer(modifier = Modifier.weight(3f)) // Pushed typography significantly down as requested

                // Typography
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Text(
                        currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Title",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        lineHeight = 42.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = artworkColors.secondary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Slider and Timers
                Column(modifier = Modifier.fillMaxWidth()) {
                    SquigglySlider(
                        value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = {
                            val newPos = (it * duration).toLong()
                            player.seekTo(newPos)
                            currentPosition = newPos
                        },
                        isPlaying = isPlaying,
                        durationMillis = duration,
                        activeColor = artworkColors.secondary,
                        onDragStart = {
                            wasPlayingBeforeDrag = player.isPlaying
                            player.pause()
                        },
                        onDragEnd = {
                            if (wasPlayingBeforeDrag) {
                                player.play()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(artworkColors.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                qualityIcon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = artworkColors.secondary
                            )
                            Text(
                                text = bitrateStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = artworkColors.secondary
                            )
                        }

                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.seekToPrevious() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    val playPauseCorner by animateDpAsState(
                        targetValue = if (isPlaying) 20.dp else 48.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "corner"
                    )
                    
                    val playPauseScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.0f else 1.1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                        label = "play_pause_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val center = coordinates.positionInRoot() + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                            buttonCenter = center
                        }
                    ) {
                        Surface(
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            shape = RoundedCornerShape(playPauseCorner),
                            color = artworkColors.secondary,
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    scaleX = playPauseScale
                                    scaleY = playPauseScale
                                },
                            tonalElevation = 12.dp,
                            shadowElevation = 16.dp,
                            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(60.dp),
                                    tint = contentColorFor(artworkColors.secondary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(onClick = { player.seekToNext() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle, 
                            contentDescription = "Shuffle",
                            tint = if (shuffleModeEnabled) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            player.repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { /* Not implemented */ }) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics")
                    }

                    IconButton(onClick = { /* Not implemented */ }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add to playlist")
                    }

                    IconButton(onClick = { /* Not implemented */ }) {
                        Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
