package com.igorthepadna.play_pause.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.SquigglySlider
import com.igorthepadna.play_pause.data.LyricLine
import com.igorthepadna.play_pause.data.LyricWord
import com.igorthepadna.play_pause.data.Song
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

private fun parseLrc(lrcContent: String?): List<LyricLine> {
    if (lrcContent.isNullOrBlank()) return emptyList()
    val lines = mutableListOf<LyricLine>()
    val lineRegex = Regex("\\[(\\d+):(\\d+)([:.]\\d+)?\\](.*)")
    val wordTimeRegex = Regex("<(\\d+):(\\d+)([:.]\\d+)?>")
    
    lrcContent.lines().forEach { line ->
        lineRegex.find(line)?.let { match ->
            val min = match.groupValues[1].toLong()
            val secPart = match.groupValues[2]
            val sec = secPart.toLong()
            val msPart = match.groupValues[3]
            var lineMs = 0L
            if (msPart.isNotEmpty()) {
                val numericPart = msPart.substring(1)
                lineMs = if (msPart.startsWith(".")) {
                    numericPart.padEnd(3, '0').take(3).toLong()
                } else {
                    numericPart.toLong() * 10
                }
            }
            val lineTimestamp = (min * 60 * 1000) + (sec * 1000) + lineMs
            var content = match.groupValues[4].trim()
            
            // Handle speaker tag [speaker:Name]
            var speaker: String? = null
            val speakerTagRegex = Regex("\\[speaker:(.*?)\\]")
            speakerTagRegex.find(content)?.let { sMatch ->
                speaker = sMatch.groupValues[1]
                content = content.replace(sMatch.value, "").trim()
            }
            
            if (speaker == null && content.contains(": ")) {
                val potentialSpeaker = content.substringBefore(": ")
                if (potentialSpeaker.length < 20 && !potentialSpeaker.contains("<")) {
                    speaker = potentialSpeaker
                    content = content.substringAfter(": ").trim()
                }
            }

            val words = mutableListOf<LyricWord>()
            val wordMatches = wordTimeRegex.findAll(content).toList()
            val plainTexts = content.split(wordTimeRegex)
            
            if (wordMatches.isNotEmpty()) {
                val initialText = plainTexts.getOrNull(0)?.trim() ?: ""
                if (initialText.isNotEmpty()) {
                    words.add(LyricWord(lineTimestamp, initialText))
                }
                
                wordMatches.forEachIndexed { index, wMatch ->
                    val wMin = wMatch.groupValues[1].toLong()
                    val wSec = wMatch.groupValues[2].toLong()
                    val wMsPart = wMatch.groupValues[3]
                    var wMs = 0L
                    if (wMsPart.isNotEmpty()) {
                        val numericPart = wMsPart.substring(1)
                        wMs = if (wMsPart.startsWith(".")) {
                            numericPart.padEnd(3, '0').take(3).toLong()
                        } else {
                            numericPart.toLong() * 10
                        }
                    }
                    val wTimestamp = (wMin * 60 * 1000) + (wSec * 1000) + wMs
                    val wordText = plainTexts.getOrNull(index + 1)?.trim() ?: ""
                    if (wordText.isNotEmpty()) {
                        words.add(LyricWord(wTimestamp, wordText))
                    }
                }
                content = words.joinToString(" ") { it.text }
            }

            if (content.isNotEmpty() || words.isNotEmpty()) {
                lines.add(LyricLine(lineTimestamp, content, speaker, words))
            }
        }
    }
    return lines.sortedBy { it.timestamp }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricLineView(
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (lineSpacing / 2).dp)
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
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                line.words.forEachIndexed { index, word ->
                    val nextWordStart = line.words.getOrNull(index + 1)?.timestamp ?: Long.MAX_VALUE
                    val isWordCurrentlyPlaying = currentPosition in word.timestamp until nextWordStart
                    val isWordPast = currentPosition >= word.timestamp
                    
                    val wordAlpha by animateFloatAsState(if (isWordCurrentlyPlaying) 1f else if (isWordPast) 0.8f else 0.4f, label = "word_alpha")
                    val wordScale by animateFloatAsState(if (isWordCurrentlyPlaying) 1.15f else 1.0f, label = "word_scale")

                    Text(
                        text = word.text,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSize.sp,
                            fontWeight = if (isWordCurrentlyPlaying) FontWeight.Black else FontWeight.Bold,
                            lineHeight = (fontSize * 1.4).sp,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.White,
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
                            .padding(horizontal = 4.dp)
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
                color = Color.White,
                textAlign = TextAlign.Center,
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
fun FullScreenPlayer(
    player: Player, 
    viewModel: MainViewModel,
    useArtworkAccent: Boolean,
    offsetY: Float,
    onDismiss: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onMoreClick: (Song) -> Unit,
    onAddClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val topPaddingPx = with(density) { 160.dp.toPx() }
    val peekingHeightPx = with(density) { 48.dp.toPx() } // Aggressively lowered to move queue to the very bottom
    val closedValue = screenHeightPx - peekingHeightPx
    
    val scope = rememberCoroutineScope()
    
    val queueOffsetY = remember { Animatable(closedValue) }
    val isQueueVisible by remember { derivedStateOf { queueOffsetY.value < closedValue - 10f } }

    var isLyricsVisible by remember { mutableStateOf(false) }
    val rawLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val parsedLyrics = remember(rawLyrics) { parseLrc(rawLyrics) }
    
    // Lyric Editor States
    val lyricFontSize by viewModel.lyricFontSize.collectAsStateWithLifecycle()
    val lyricInactiveAlpha by viewModel.lyricInactiveAlpha.collectAsStateWithLifecycle()
    val lyricActiveScale by viewModel.lyricActiveScale.collectAsStateWithLifecycle()
    val lyricLineSpacing by viewModel.lyricLineSpacing.collectAsStateWithLifecycle()

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }
    var currentMediaItem by remember { mutableStateOf(player.currentMediaItem) }

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentSong = remember(songs, currentMediaItem) {
        songs.find { it.id.toString() == currentMediaItem?.mediaId }
    }

    val currentLyricIndex by remember(parsedLyrics, currentPosition) {
        derivedStateOf {
            val index = parsedLyrics.indexOfLast { it.timestamp <= currentPosition }
            if (index == -1 && parsedLyrics.isNotEmpty()) 0 else index
        }
    }

    val lyricsListState = rememberLazyListState()
    
    LaunchedEffect(currentLyricIndex) {
        if (isLyricsVisible && currentLyricIndex != -1) {
            lyricsListState.animateScrollToItem(currentLyricIndex)
        }
    }

    BackHandler(onBack = {
        if (isLyricsVisible) {
            isLyricsVisible = false
        } else if (isQueueVisible) {
            scope.launch {
                queueOffsetY.animateTo(closedValue, spring(stiffness = Spring.StiffnessLow))
            }
        } else {
            onDismiss()
        }
    })

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
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = if (player.currentPosition > 0) player.currentPosition else 0L
            delay(50) 
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

    var buttonCenter by remember { mutableStateOf(Offset.Zero) }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying && !isLyricsVisible) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "art_breathing"
    )
    
    val artCorner by animateDpAsState(
        targetValue = if (isPlaying && !isLyricsVisible) 20.dp else 60.dp,
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
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp), // Fixed padding issue
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle handle for dragging
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
                )

                Spacer(modifier = Modifier.weight(0.1f)) // Moves everything up aggressively

                // CONTENT AREA (Artwork or Synced Lyrics)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth(0.8f),
                    contentAlignment = Alignment.Center
                ) {
                    // Combined Container for Artwork, Blur, and Lyrics
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                            }
                            .clip(RoundedCornerShape(artCorner)),
                        tonalElevation = 12.dp,
                        shadowElevation = 24.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = currentMediaItem?.mediaMetadata?.artworkUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(if (isLyricsVisible) 30.dp else 0.dp),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ic_launcher_foreground)
                            )
                            
                            // EXPRESSIVE LYRICS OVERLAY (Clipped to Container)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLyricsVisible,
                                enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f, animationSpec = spring(stiffness = Spring.StiffnessLow)),
                                exit = fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.2f, animationSpec = tween(400))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (parsedLyrics.isNotEmpty()) {
                                        LazyColumn(
                                            state = lyricsListState,
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(lyricLineSpacing.dp, Alignment.CenterVertically),
                                            contentPadding = PaddingValues(vertical = 100.dp)
                                        ) {
                                            itemsIndexed(parsedLyrics) { index, lyric ->
                                                LyricLineView(
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
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            item {
                                                Text(
                                                    text = rawLyrics!!,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontSize = (lyricFontSize * 0.8f).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        lineHeight = (lyricFontSize * 1.1).sp
                                                    ),
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .clickable { isLyricsVisible = false }
                                                )
                                            }
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.6f))
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                "Lyrics unavailable",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(2f)) // Pushes title and controls further down

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

                Spacer(modifier = Modifier.height(12.dp))

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
                            player.pause()
                        },
                        onDragEnd = {
                            player.play()
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
                            val bitrateStr = currentMediaItem?.mediaMetadata?.extras?.getString("bitrate") ?: "320 kbps"
                            val bitrateValue = bitrateStr.filter { it.isDigit() }.toIntOrNull() ?: 320
                            val qualityIcon = when {
                                bitrateValue >= 1000 -> Icons.Rounded.Album
                                bitrateValue >= 256 -> Icons.Rounded.HighQuality
                                else -> Icons.Rounded.Sd
                            }
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player.seekToPrevious() }, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(42.dp))
                    }

                    Spacer(modifier = Modifier.width(20.dp))

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

                    Box(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val center = coordinates.positionInRoot() + Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                            buttonCenter = center
                        }
                    ) {
                        Surface(
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            shape = MorphingButtonShape(morphProgress),
                            color = artworkColors.secondary,
                            modifier = Modifier.size(80.dp).graphicsLayer { // Even smaller button for compact screens
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
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(onClick = { player.seekToNext() }, modifier = Modifier.size(60.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(42.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = CircleShape,
                    color = artworkColors.secondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var shuffleModeEnabledLocal by remember { mutableStateOf(player.shuffleModeEnabled) }
                        var repeatModeLocal by remember { mutableIntStateOf(player.repeatMode) }
                        
                        DisposableEffect(player) {
                            val listener = object : Player.Listener {
                                override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleModeEnabledLocal = enabled }
                                override fun onRepeatModeChanged(mode: Int) { repeatModeLocal = mode }
                            }
                            player.addListener(listener)
                            onDispose { player.removeListener(listener) }
                        }

                        IconButton(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) {
                            Icon(Icons.Rounded.Shuffle, null, tint = if (shuffleModeEnabledLocal) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            player.repeatMode = when (repeatModeLocal) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }) {
                            Icon(if (repeatModeLocal == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (repeatModeLocal != Player.REPEAT_MODE_OFF) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { 
                            isLyricsVisible = !isLyricsVisible
                            if (isLyricsVisible) {
                                viewModel.loadLyricsForCurrentSong()
                            }
                        }) {
                            Icon(
                                Icons.Rounded.Lyrics, 
                                null,
                                tint = if (isLyricsVisible) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                        IconButton(onClick = { currentSong?.let { onAddClick(it) } }) { Icon(Icons.Rounded.Add, null) }
                        IconButton(onClick = { currentSong?.let { onMoreClick(it) } }) { Icon(Icons.Rounded.MoreHoriz, null) }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp)) // Extra safety space at the bottom
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
                                    .padding(top = 10.dp)
                                    .width(48.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Queue", 
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        QueueContent(player, artworkColors)
                    }
                }
            }
        }
    }
}
