package com.igorthepadna.play_pause.ui.components

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.igorthepadna.play_pause.utils.verticalScrollbar

@Composable
fun QueueContent(player: Player) {
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

    // Logic for the visual queue
    // If Repeat Mode is ALL, we show a repeating list (simulated by a larger set)
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
    
    // We simulate infinite scrolling by using a very large count if Repeat Mode is ALL
    val isInfinite = repeatMode == Player.REPEAT_MODE_ALL && playbackOrder.isNotEmpty()
    val totalDisplayCount = if (isInfinite) Int.MAX_VALUE else playbackOrder.size
    val startIndex = if (isInfinite) (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % playbackOrder.size) else 0

    LaunchedEffect(currentIndex, isInfinite) {
        val basePos = playbackOrder.indexOf(currentIndex)
        if (basePos >= 0) {
            if (isInfinite) {
                // Find the closest instance of the current item to the center/current scroll
                val currentScrollIndex = listState.firstVisibleItemIndex
                val cycle = currentScrollIndex / playbackOrder.size
                val targetIndex = (cycle * playbackOrder.size) + basePos
                listState.scrollToItem(targetIndex)
            } else {
                listState.animateScrollToItem(basePos)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val bottomPadding = PaddingValues(bottom = 32.dp)
        
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp), 
            contentPadding = bottomPadding,
            modifier = Modifier
                .heightIn(max = 600.dp)
                .verticalScrollbar(listState, padding = bottomPadding)
        ) {
            items(totalDisplayCount) { globalIndex ->
                val i = globalIndex % playbackOrder.size
                val indexInPlayer = playbackOrder[i]
                val item = player.getMediaItemAt(indexInPlayer)
                
                // For infinite mode, we consider the "current" item to be the one matching player's current index
                val isCurrent = indexInPlayer == currentIndex
                val isPlayNext = item.mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false

                // Pill logic (only for finite lists or the first cycle of infinite)
                val isFirstPlayNext = !isInfinite && isPlayNext && (i == 0 || !(player.getMediaItemAt(playbackOrder[i-1]).mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false))
                val isLastPlayNext = !isInfinite && isPlayNext && (i == playbackOrder.size - 1 || !(player.getMediaItemAt(playbackOrder[i+1]).mediaMetadata.extras?.getBoolean("is_play_next", false) ?: false))

                if (isFirstPlayNext) {
                    Text(
                        text = "PLAY NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                val shape = when {
                    isFirstPlayNext && isLastPlayNext -> RoundedCornerShape(24.dp)
                    isFirstPlayNext -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    isLastPlayNext -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    isPlayNext && !isInfinite -> RoundedCornerShape(4.dp)
                    else -> RoundedCornerShape(16.dp)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        isPlayNext && !isInfinite -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        else -> Color.Transparent
                    },
                    tonalElevation = if (isCurrent) 4.dp else 0.dp
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
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "Unknown", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
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
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
