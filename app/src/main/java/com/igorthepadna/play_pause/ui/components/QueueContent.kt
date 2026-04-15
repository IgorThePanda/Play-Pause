package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar

private data class QueueItem(
    val indexInPlayer: Int,
    val mediaItem: MediaItem,
    val isPlayNext: Boolean,
    val isFirstPlayNext: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueContent(player: Player, artworkColors: ArtworkColors) {
    val haptic = LocalHapticFeedback.current
    
    var shuffleModeEnabled by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var currentIndex by remember { mutableIntStateOf(player.currentMediaItemIndex) }
    var mediaItemCount by remember { mutableIntStateOf(player.mediaItemCount) }
    var timeline by remember { mutableStateOf(player.currentTimeline) }
    
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleModeEnabled = enabled
            }
            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
            }
            override fun onTimelineChanged(t: Timeline, reason: Int) {
                timeline = t
                mediaItemCount = player.mediaItemCount
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val queueItems = remember(timeline, shuffleModeEnabled, mediaItemCount) {
        val list = mutableListOf<QueueItem>()
        if (!timeline.isEmpty && mediaItemCount > 0) {
            val firstIndex = timeline.getFirstWindowIndex(shuffleModeEnabled)
            var current = firstIndex
            var lastWasPlayNext = false
            while (current != C.INDEX_UNSET) {
                if (current in 0 until player.mediaItemCount) {
                    val item = player.getMediaItemAt(current)
                    val isPlayNext = item.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false
                    list.add(
                        QueueItem(
                            indexInPlayer = current,
                            mediaItem = item,
                            isPlayNext = isPlayNext,
                            isFirstPlayNext = isPlayNext && !lastWasPlayNext
                        )
                    )
                    lastWasPlayNext = isPlayNext
                }
                current = timeline.getNextWindowIndex(current, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
                if (list.size >= mediaItemCount) break
            }
        } else if (mediaItemCount > 0) {
            for (i in 0 until mediaItemCount) {
                val item = player.getMediaItemAt(i)
                val isPlayNext = item.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false
                list.add(
                    QueueItem(
                        indexInPlayer = i,
                        mediaItem = item,
                        isPlayNext = isPlayNext,
                        isFirstPlayNext = isPlayNext && (i == 0 || !(player.getMediaItemAt(i - 1).mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false))
                    )
                )
            }
        }
        list
    }

    val listState = rememberLazyListState()
    val isInfinite = repeatMode == Player.REPEAT_MODE_ALL && queueItems.isNotEmpty()
    val totalDisplayCount = if (isInfinite) Int.MAX_VALUE else queueItems.size
    
    LaunchedEffect(currentIndex, isInfinite) {
        val basePos = queueItems.indexOfFirst { it.indexInPlayer == currentIndex }
        if (basePos >= 0) {
            if (isInfinite) {
                val currentScrollIndex = listState.firstVisibleItemIndex
                val cycle = currentScrollIndex / queueItems.size
                val targetIndex = (cycle * queueItems.size) + basePos
                listState.scrollToItem(targetIndex)
            } else {
                listState.animateScrollToItem(basePos)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        val bottomPadding = PaddingValues(bottom = 32.dp)
        
        LazyColumn(
            state = listState,
            contentPadding = bottomPadding,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState, padding = bottomPadding)
        ) {
            items(
                count = totalDisplayCount,
                key = { globalIndex -> 
                    val size = queueItems.size
                    if (size == 0) return@items "empty_$globalIndex"
                    val i = globalIndex % size
                    val qItem = queueItems.getOrNull(i) ?: return@items "invalid_$globalIndex"
                    "${qItem.mediaItem.mediaId}_${globalIndex}"
                }
            ) { globalIndex ->
                val size = queueItems.size
                if (size == 0) return@items
                val i = globalIndex % size
                val qItem = queueItems.getOrNull(i) ?: return@items
                
                val item = qItem.mediaItem
                val indexInPlayer = qItem.indexInPlayer
                val isCurrent = indexInPlayer == currentIndex
                val isPlayNext = qItem.isPlayNext
                val isFirstPlayNext = qItem.isFirstPlayNext
                val isLastPlayNext = remember(queueItems, i) {
                    val nextItem = queueItems.getOrNull(i + 1)
                    isPlayNext && (nextItem == null || !nextItem.isPlayNext)
                }

                if (isFirstPlayNext) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = artworkColors.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "PLAY NEXT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            fontWeight = FontWeight.Black,
                            color = artworkColors.secondary
                        )
                    }
                }

                val dismissState = rememberSwipeToDismissBoxState()
                
                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                    androidx.compose.runtime.LaunchedEffect(dismissState.currentValue) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (indexInPlayer in 0 until player.mediaItemCount) {
                            player.removeMediaItem(indexInPlayer)
                        }
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                else -> MaterialTheme.colorScheme.error
                            }, label = "swipe_bg"
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(color)
                                .padding(horizontal = 24.dp),
                            contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                val iconScale by animateFloatAsState(1.4f, label = "icon_scale")
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove",
                                    modifier = Modifier.scale(iconScale),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    modifier = Modifier.animateItem()
                ) {
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isCurrent) {
                            artworkColors.secondary.copy(alpha = 0.8f)
                        } else if (isPlayNext) {
                            artworkColors.secondary.copy(alpha = 0.12f)
                        } else {
                            artworkColors.secondary.copy(alpha = 0.05f)
                        },
                        label = "item_bg"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min) // Important for fillMaxHeight to work
                            .padding(start = if (isPlayNext) 24.dp else 0.dp), // Increased indentation for sub-dir feel
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlayNext) {
                            // Vertical accent line for the "sub-dir" group
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .padding(
                                        top = if (isFirstPlayNext) 8.dp else 0.dp,
                                        bottom = if (isLastPlayNext) 8.dp else 0.dp
                                    )
                                    .clip(RoundedCornerShape(
                                        topStart = if (isFirstPlayNext) 2.dp else 0.dp,
                                        topEnd = if (isFirstPlayNext) 2.dp else 0.dp,
                                        bottomStart = if (isLastPlayNext) 2.dp else 0.dp,
                                        bottomEnd = if (isLastPlayNext) 2.dp else 0.dp
                                    ))
                                    .background(artworkColors.secondary.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = backgroundColor
                        ) {
                            Box(modifier = Modifier.clickable { player.seekTo(indexInPlayer, 0L) }) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.mediaMetadata.artworkUri)
                                            .crossfade(true)
                                            .size(120) // Optimization: Loaded at small size for the queue
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(R.drawable.ic_launcher_foreground)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.mediaMetadata.title?.toString() ?: "Unknown",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isCurrent) contentColorFor(artworkColors.secondary) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            item.mediaMetadata.artist?.toString() ?: "Unknown",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCurrent) contentColorFor(artworkColors.secondary).copy(alpha = 0.8f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isCurrent) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = null,
                                            tint = contentColorFor(artworkColors.secondary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.DragHandle,
                                            contentDescription = "Reorder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dividers
                if (isCurrent && !isInfinite && i < size - 1) {
                    val nextQItem = queueItems.getOrNull(i + 1)
                    val nextIsPlayNext = nextQItem?.isPlayNext ?: false
                    if (!nextIsPlayNext) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp).animateItem(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
