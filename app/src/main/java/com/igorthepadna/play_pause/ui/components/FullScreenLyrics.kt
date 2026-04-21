package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.igorthepadna.play_pause.MainViewModel
import androidx.compose.ui.graphics.Brush
import com.igorthepadna.play_pause.utils.ArtworkColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import com.igorthepadna.play_pause.data.LyricLine
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.igorthepadna.play_pause.utils.rememberArtworkColors

@Composable
fun FullScreenLyrics(
    player: Player,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentMediaItem = player.currentMediaItem
    val rawLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val parsedLyrics = remember(rawLyrics) { parseLrc(rawLyrics) }
    
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    val duration = player.duration.coerceAtLeast(0L)
    val isPlaying = player.isPlaying

    val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val allAlbums by viewModel.sortedAlbums.collectAsStateWithLifecycle()
    
    val currentSong = remember(songs, currentMediaItem) {
        songs.find { it.id.toString() == currentMediaItem?.mediaId }
    }
    
    val currentAlbum = remember(allAlbums, currentSong) {
        allAlbums.find { it.id == currentSong?.albumId }
    }

    val effectiveArtworkUri = remember(currentMediaItem, currentSong, currentAlbum) {
        currentAlbum?.artworkUri 
            ?: currentMediaItem?.mediaMetadata?.artworkUri
            ?: currentSong?.albumArtUri
    }

    val extractedColors = rememberArtworkColors(
        artworkUri = effectiveArtworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val artworkColors = if (useArtworkAccent) {
        extractedColors
    } else {
        ArtworkColors(
            primary = MaterialTheme.colorScheme.surface,
            secondary = MaterialTheme.colorScheme.primary,
            tertiary = MaterialTheme.colorScheme.primary
        )
    }
    
    val lyricFontSize by viewModel.lyricFontSize.collectAsStateWithLifecycle()
    val lyricInactiveAlpha by viewModel.lyricInactiveAlpha.collectAsStateWithLifecycle()
    val lyricActiveScale by viewModel.lyricActiveScale.collectAsStateWithLifecycle()
    val lyricLineSpacing by viewModel.lyricLineSpacing.collectAsStateWithLifecycle()
    val isCompactMode by viewModel.isCompactLyricsMode.collectAsStateWithLifecycle()

    val lyricsListState = rememberLazyListState()
    val currentLyricIndex = remember(currentPosition, parsedLyrics) {
        parsedLyrics.indexOfLast { it.timestamp <= currentPosition }.coerceAtLeast(0)
    }

    LaunchedEffect(isPlaying) {
        while (true) {
            currentPosition = player.currentPosition
            delay(100)
        }
    }

    LaunchedEffect(currentLyricIndex) {
        if (parsedLyrics.isNotEmpty()) {
            lyricsListState.animateScrollToItem(currentLyricIndex, scrollOffset = -200)
        }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to artworkColors.primary.copy(alpha = if (useArtworkAccent) 0.15f else 0f),
                            1f to MaterialTheme.colorScheme.surface
                        )
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
                ) {
                    // Header: Title and Artist
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Title",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Text(
                            text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown Artist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = artworkColors.secondary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Lyrics
                    Box(modifier = Modifier.weight(1f)) {
                        if (parsedLyrics.isNotEmpty()) {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(lyricLineSpacing.dp),
                                contentPadding = PaddingValues(vertical = 100.dp)
                            ) {
                                itemsIndexed(parsedLyrics) { index, lyric ->
                                    FullScreenLyricLineView(
                                        line = lyric,
                                        currentPosition = currentPosition,
                                        isActive = index == currentLyricIndex,
                                        artworkColors = artworkColors,
                                        fontSize = lyricFontSize,
                                        inactiveAlpha = lyricInactiveAlpha,
                                        activeScale = lyricActiveScale,
                                        lineSpacing = lyricLineSpacing,
                                        onSeek = { timestamp ->
                                            player.seekTo(timestamp)
                                            currentPosition = timestamp
                                        }
                                    )
                                }
                            }
                        } else if (!rawLyrics.isNullOrBlank()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.Start,
                                contentPadding = PaddingValues(vertical = 40.dp)
                            ) {
                                item {
                                    Text(
                                        text = rawLyrics!!,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = (lyricFontSize * 0.9f).sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = (lyricFontSize * 1.3).sp,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No lyrics available",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom: Playback Controls
                    if (!isCompactMode) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (player.hasPreviousMediaItem()) {
                                                player.seekToPreviousMediaItem()
                                            } else {
                                                player.seekTo(0)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(42.dp), tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                val morphProgress by animateFloatAsState(
                                    targetValue = if (isPlaying) 1f else 0f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                                    label = "morph"
                                )

                                val playPauseScale by animateFloatAsState(
                                    targetValue = if (isPlaying) 1.0f else 1.1f,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                                    label = "play_pause_scale"
                                )

                                Surface(
                                    onClick = { if (isPlaying) player.pause() else player.play() },
                                    shape = MorphingButtonShape(morphProgress),
                                    color = artworkColors.secondary,
                                    modifier = Modifier.size(80.dp).graphicsLayer {
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
                                            modifier = Modifier.size(48.dp),
                                            tint = contentColorFor(artworkColors.secondary)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(CircleShape)
                                        .clickable { player.seekToNext() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(42.dp), tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(artworkColors.secondary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = artworkColors.secondary)
                            }
                        }
                    } else {
                        // In compact mode, just a close button or a small indicator
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(artworkColors.secondary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = artworkColors.secondary)
                            }
                        }
                    }
                }

                // Right: Scroll Bar & Progression
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    VerticalSquigglySlider(
                        value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = { newValue: Float ->
                            val newPos = (newValue * duration).toLong()
                            player.seekTo(newPos)
                            currentPosition = newPos
                        },
                        isPlaying = isPlaying,
                        onDragStart = { player.pause() },
                        onDragEnd = { player.play() },
                        activeColor = artworkColors.secondary,
                        inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(64.dp)
                    )

                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullScreenLyricLineView(
    line: LyricLine,
    currentPosition: Long,
    isActive: Boolean,
    artworkColors: ArtworkColors,
    fontSize: Float,
    inactiveAlpha: Float,
    activeScale: Float,
    lineSpacing: Float,
    onSeek: (Long) -> Unit
) {
    val lineAlpha by animateFloatAsState(if (isActive) 1f else inactiveAlpha, label = "line_alpha")
    val lineScale by animateFloatAsState(if (isActive) activeScale else 1.0f, label = "line_scale")

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (lineSpacing / 4).dp)
            .graphicsLayer {
                alpha = lineAlpha
                scaleX = lineScale
                scaleY = lineScale
            }
    ) {
        if (!line.speaker.isNullOrBlank()) {
            val speakerColor = artworkColors.secondary.copy(alpha = 0.8f)
            Text(
                text = line.speaker.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = speakerColor,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        if (line.words.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                line.words.forEachIndexed { index, word ->
                    val nextWordStart = line.words.getOrNull(index + 1)?.timestamp ?: (word.timestamp + 500)
                    val isWordCurrentlyPlaying = currentPosition in word.timestamp until nextWordStart
                    val isWordPast = currentPosition >= word.timestamp
                    
                    val wordAlpha by animateFloatAsState(if (isWordCurrentlyPlaying) 1f else if (isWordPast) 0.8f else 0.4f, label = "word_alpha")
                    val wordScale by animateFloatAsState(if (isWordCurrentlyPlaying) 1.1f else 1.0f, label = "word_scale")

                    Text(
                        text = word.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSize.sp,
                            fontWeight = if (isWordCurrentlyPlaying) FontWeight.Black else FontWeight.Bold,
                            lineHeight = (fontSize * 1.4).sp,
                            letterSpacing = (-1).sp
                        ),
                        color = if (isWordCurrentlyPlaying) artworkColors.secondary else Color.White,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = wordScale
                                scaleY = wordScale
                                alpha = wordAlpha
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSeek(word.timestamp) }
                            )
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        } else {
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = (fontSize * 1.4).sp,
                    letterSpacing = (-1).sp
                ),
                color = if (isActive) artworkColors.secondary else Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeek(line.timestamp) }
                    )
            )
        }
    }
}

@Composable
fun VerticalSquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = LocalDensity.current
    val maxAmplitudePx = with(density) { 6.dp.toPx() }
    val thumbRadiusIdle = with(density) { 10.dp.toPx() }
    val thumbRadiusDragging = with(density) { 14.dp.toPx() }
    
    var isDragging by remember { mutableStateOf(false) }
    
    val thumbRadius by animateFloatAsState(
        targetValue = if (isDragging) thumbRadiusDragging else thumbRadiusIdle,
        label = "ThumbRadius"
    )
    
    var phase by remember { mutableStateOf(0f) }
    
    val currentAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) maxAmplitudePx else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SquiggleAmplitude"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameNanos { it }
            while (true) {
                val currentTime = withFrameNanos { it }
                val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                phase += deltaTime * 5f 
                lastTime = currentTime
                yield()
            }
        }
    }

    val segmentLength = 6f
    val frequency = 1 / 15f 
    val envelopeDistance = with(density) { 24.dp.toPx() }
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.y / size.height).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onDragStart()
                        onValueChange((offset.y / size.height).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        onValueChange((change.position.y / size.height).coerceIn(0f, 1f))
                        change.consume()
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val activeHeight = height * value
        val strokeWidthPx = 6.dp.toPx()

        // Inactive line (bottom part)
        if (activeHeight < height) {
            drawLine(
                color = inactiveColor,
                start = Offset(centerX, activeHeight),
                end = Offset(centerX, height),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // Active squiggly line (top part)
        if (activeHeight > 0) {
            path.reset()
            path.moveTo(centerX, 0f)
            
            var y = 0f
            val targetY = activeHeight
            while (y < targetY) {
                val startEnvelope = (y / envelopeDistance).coerceIn(0f, 1f)
                val endEnvelope = ((targetY - y) / envelopeDistance).coerceIn(0f, 1f)
                val combinedEnvelope = startEnvelope * endEnvelope
                
                val x = centerX + currentAmplitude * sin(y * frequency + phase) * combinedEnvelope
                path.lineTo(x, y)
                y += segmentLength
            }
            
            path.lineTo(centerX, targetY)

            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // Thumb
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(centerX, activeHeight)
        )
    }
}
