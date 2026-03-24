package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueContent(player: Player, artworkColors: ArtworkColors) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    
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
            override fun onPlaybackStateChanged(state: Int) {
                mediaItemCount = player.mediaItemCount
                currentIndex = player.currentMediaItemIndex
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val playbackOrder = remember(timeline, shuffleModeEnabled, mediaItemCount) {
        val list = mutableListOf<Int>()
        if (!timeline.isEmpty) {
            val firstIndex = timeline.getFirstWindowIndex(shuffleModeEnabled)
            var current = firstIndex
            while (current != C.INDEX_UNSET) {
                list.add(current)
                current = timeline.getNextWindowIndex(current, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
                if (list.size >= mediaItemCount) break
            }
        } else {
            for (i in 0 until mediaItemCount) list.add(i)
        }
        list
    }

    val listState = rememberLazyListState()
    val isInfinite = repeatMode == Player.REPEAT_MODE_ALL && playbackOrder.isNotEmpty()
    val totalDisplayCount = if (isInfinite) Int.MAX_VALUE else playbackOrder.size
    
    LaunchedEffect(currentIndex, isInfinite) {
        val basePos = playbackOrder.indexOf(currentIndex)
        if (basePos >= 0) {
            if (isInfinite) {
                val currentScrollIndex = listState.firstVisibleItemIndex
                val cycle = currentScrollIndex / playbackOrder.size
                val targetIndex = (cycle * playbackOrder.size) + basePos
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = bottomPadding,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState, padding = bottomPadding)
        ) {
            items(
                count = totalDisplayCount,
                key = { globalIndex -> 
                    val i = globalIndex % playbackOrder.size
                    val indexInPlayer = playbackOrder[i]
                    val item = player.getMediaItemAt(indexInPlayer)
                    "${item.mediaId}_${globalIndex}"
                }
            ) { globalIndex ->
                val i = globalIndex % playbackOrder.size
                val indexInPlayer = playbackOrder[i]
                val item = player.getMediaItemAt(indexInPlayer)
                
                val isCurrent = indexInPlayer == currentIndex
                val isPlayNext = item.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false

                val isFirstPlayNext = isPlayNext && (i == 0 || !(player.getMediaItemAt(playbackOrder[i-1]).mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false))

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

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            player.removeMediaItem(indexInPlayer)
                            true
                        } else false
                    },
                    positionalThreshold = { totalDistance -> 
                        val threshold = with(density) { 100.dp.toPx() }
                        maxOf(threshold, totalDistance * 0.25f)
                    }
                )

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
                            .padding(start = if (isPlayNext) 12.dp else 0.dp), // Indentation for sub-dir feel
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlayNext) {
                            // Vertical accent line for the "sub-dir" group
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .clip(CircleShape)
                                    .background(artworkColors.secondary.copy(alpha = 0.3f))
                            )
                            Spacer(Modifier.width(8.dp))
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
                                        model = item.mediaMetadata.artworkUri,
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
                if (isCurrent && !isInfinite && i < playbackOrder.size - 1) {
                    val nextIndex = playbackOrder[i+1]
                    val nextItem = player.getMediaItemAt(nextIndex)
                    val nextIsPlayNext = nextItem.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false
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
