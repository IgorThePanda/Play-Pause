package com.igorthepadna.play_pause.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

@Composable
fun FullScreenPlayer(
    player: Player, 
    useArtworkAccent: Boolean,
    offsetY: Float,
    onDismiss: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val topPaddingPx = with(density) { 160.dp.toPx() }
    val peekingHeightPx = with(density) { 72.dp.toPx() }
    val closedValue = screenHeightPx - peekingHeightPx
    
    val scope = rememberCoroutineScope()
    
    val queueOffsetY = remember { Animatable(closedValue) }
    val isQueueVisible by remember { derivedStateOf { queueOffsetY.value < closedValue - 10f } }

    BackHandler(onBack = {
        if (isQueueVisible) {
            scope.launch {
                queueOffsetY.animateTo(closedValue, spring(stiffness = Spring.StiffnessLow))
            }
        } else {
            onDismiss()
        }
    })

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }

    var shuffleModeEnabled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }

    var wasPlayingBeforeDrag by remember { mutableStateOf(false) }
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }

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
        targetValue = if (isPlaying) 20.dp else 60.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "art_corner"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, (screenHeightPx + offsetY).roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDrag(delta)
                },
                onDragStopped = { velocity ->
                    onDragStopped(velocity)
                }
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

            if (buttonCenter != Offset.Zero) {
                Box(modifier = Modifier.fillMaxSize().zIndex(-1f)) {
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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle handle for dragging
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                )

                Spacer(modifier = Modifier.weight(0.5f))

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

                Spacer(modifier = Modifier.weight(3f))

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
                            Icon(qualityIcon, null, modifier = Modifier.size(14.dp), tint = artworkColors.secondary)
                            Text(bitrateStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = artworkColors.secondary)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.seekToPrevious() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    val playPauseCorner by animateDpAsState(
                        targetValue = if (isPlaying) 16.dp else 40.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "corner"
                    )
                    
                    val playPauseScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.0f else 1.1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                        label = "play_pause_scale"
                    )

                    Box(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val center = coordinates.positionInRoot() + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                            buttonCenter = center
                        }
                    ) {
                        Surface(
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            shape = RoundedCornerShape(playPauseCorner),
                            color = artworkColors.secondary,
                            modifier = Modifier.size(100.dp).graphicsLayer {
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
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    shape = CircleShape,
                    color = artworkColors.secondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) {
                            Icon(Icons.Rounded.Shuffle, null, tint = if (shuffleModeEnabled) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            player.repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }) {
                            Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (repeatMode != Player.REPEAT_MODE_OFF) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { }) { Icon(Icons.Rounded.Lyrics, null) }
                        IconButton(onClick = { }) { Icon(Icons.Rounded.Add, null) }
                        IconButton(onClick = { }) { Icon(Icons.Rounded.MoreHoriz, null) }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // PHYSICALLY BASED QUEUE SHEET
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, queueOffsetY.value.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                queueOffsetY.snapTo((queueOffsetY.value + delta).coerceIn(topPaddingPx, closedValue))
                            }
                        },
                        onDragStopped = { velocity ->
                            val midpoint = (closedValue + topPaddingPx) / 2
                            val targetValue = when {
                                velocity < -500f -> topPaddingPx
                                velocity > 500f -> closedValue
                                queueOffsetY.value < midpoint -> topPaddingPx
                                else -> closedValue
                            }
                            scope.launch {
                                queueOffsetY.animateTo(
                                    targetValue = targetValue,
                                    initialVelocity = velocity,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    ),
                color = artworkColors.secondary.copy(alpha = 0.15f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 12.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(peekingHeightPx.let { with(density) { it.toDp() } })
                            .clickable(remember { MutableInteractionSource() }, null) {
                                scope.launch {
                                    val target = if (isQueueVisible) closedValue else topPaddingPx
                                    queueOffsetY.animateTo(target, spring(stiffness = Spring.StiffnessLow))
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .width(48.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Queue", 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QueueContent(player)
                    }
                }
            }
        }
    }
}
