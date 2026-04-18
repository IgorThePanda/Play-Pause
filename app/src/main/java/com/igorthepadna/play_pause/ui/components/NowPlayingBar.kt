package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.media3.common.Player
import java.util.Locale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBar(
    player: Player?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    showSearch: Boolean,
    onSortClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDrag: (Float) -> Unit = {},
    onDragStopped: (Float) -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentMediaItem by remember { mutableStateOf(player?.currentMediaItem) }
    var currentPosition by remember { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var duration by remember { mutableLongStateOf(player?.duration ?: 0L) }
    var isDragging by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchQuery)) }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Sync external searchQuery to internal textFieldValue
    LaunchedEffect(searchQuery) {
        if (searchQuery != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = searchQuery)
        }
    }

    // Auto-select text when search expands
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            delay(300) // Wait for expansion animation
            focusRequester.requestFocus()
            textFieldValue = textFieldValue.copy(
                selection = TextRange(0, textFieldValue.text.length)
            )
        }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
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

    LaunchedEffect(isPlaying, isDragging, player) {
        if (player == null || !isPlaying || isDragging) return@LaunchedEffect
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration
            delay(500)
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

    val cornerRadius by animateFloatAsState(
        targetValue = if (isPlaying) 12f else 24f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "cornerRadius"
    )

    val filters = remember { LibraryFilter.entries }
    val initialPage = remember { filters.indexOf(currentFilter).coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage) { filters.size }

    LaunchedEffect(currentFilter) {
        val targetPage = filters.indexOf(currentFilter)
        if (targetPage != -1 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newFilter = filters.getOrNull(pagerState.currentPage)
        if (newFilter != null && newFilter != currentFilter) {
            onFilterSelected(newFilter)
        }
    }

    val hasMedia = currentMediaItem != null

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .then(
                if (hasMedia) {
                    Modifier
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta -> onDrag(delta) },
                            onDragStopped = { velocity -> onDragStopped(velocity) }
                        )
                        .clickable { onClick() }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Playback Section (Top half - Progress bar here)
            AnimatedVisibility(
                visible = hasMedia,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(artworkColors.secondary.copy(alpha = 0.05f))
                        .drawBehind {
                            if (hasMedia) {
                                drawRect(
                                    color = artworkColors.secondary.copy(alpha = 0.15f),
                                    size = size.copy(width = size.width * progress)
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    player?.seekTo(currentPosition)
                                },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dragFactor = 150L 
                                    val newPos = (currentPosition + (dragAmount.x * dragFactor).toLong())
                                        .coerceIn(0L, duration)
                                    currentPosition = newPos
                                }
                            )
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentMediaItem?.mediaMetadata?.artworkUri)
                            .crossfade(true)
                            .size(160)
                            .build(),
                        contentDescription = "Artwork for ${currentMediaItem?.mediaMetadata?.title}",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    var showTimer by remember { mutableStateOf(false) }
                    val remainingTime = remember(currentPosition, duration) {
                        val remaining = duration - currentPosition
                        val minutes = (remaining / 1000) / 60
                        val seconds = (remaining / 1000) % 60
                        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    }

                    Surface(
                        shape = RoundedCornerShape(cornerRadius.dp),
                        color = artworkColors.secondary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(42.dp)
                            .combinedClickable(
                                onClick = { if (isPlaying) player?.pause() else player?.play() },
                                onLongClick = { showTimer = !showTimer }
                            ),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = showTimer,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "play_timer_switch"
                            ) { isTimer ->
                                if (isTimer) {
                                    Text(
                                        text = remainingTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColorFor(artworkColors.secondary.copy(alpha = 0.8f)),
                                        modifier = Modifier.clearAndSetSemantics { 
                                            contentDescription = "$remainingTime remaining" 
                                        }
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(28.dp),
                                        tint = contentColorFor(artworkColors.secondary.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (hasMedia) 8.dp else 0.dp))

            // Navigation & Search Pill (Clean background, NO progress)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
            ) {
                AnimatedContent(
                    targetState = showSearch && searchExpanded,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "search_expansion"
                ) { isExpanded ->
                    if (isExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    searchExpanded = false 
                                    onSearchQueryChange("") // Restore view by clearing search
                                }, 
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
                            }
                            
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    textFieldValue = it
                                    onSearchQueryChange(it.text)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (textFieldValue.text.isEmpty()) {
                                            Text(
                                                "Search here",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(42.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp)
                        ) {
                            IconButton(onClick = { searchExpanded = true }, modifier = Modifier.size(42.dp)) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search", modifier = Modifier.size(22.dp))
                            }
                            
                            VerticalDivider(
                                modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), 
                                thickness = 1.dp, 
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(horizontal = 70.dp), // Reduced from 90.dp to allow more space for the label
                                    pageSpacing = 0.dp,
                                    key = { filters[it].name }
                                ) { page ->
                                val filter = filters[page]
                                val isSelected = pagerState.currentPage == page
                                val activeColor = if (hasMedia) artworkColors.secondary else MaterialTheme.colorScheme.primary

                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = if (isSelected) 1f else 0.5f
                                                val scale = if (isSelected) 1f else 0.9f
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(CircleShape)
                                            .combinedClickable(
                                                onClick = { scope.launch { pagerState.animateScrollToPage(page) } },
                                                onDoubleClick = { onSortClick() },
                                                onLongClick = { onSortClick() }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            filter.icon, 
                                            contentDescription = filter.label, 
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = expandHorizontally() + fadeIn(),
                                            exit = shrinkHorizontally() + fadeOut()
                                        ) {
                                            Row {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    filter.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeColor,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            VerticalDivider(
                                modifier = Modifier.height(24.dp).padding(horizontal = 2.dp), 
                                thickness = 1.dp, 
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            
                            IconButton(onClick = onSettingsClick, modifier = Modifier.size(42.dp)) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
