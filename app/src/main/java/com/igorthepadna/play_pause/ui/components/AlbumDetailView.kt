package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar

@Composable
fun AlbumDetailView(
    album: Album,
    player: Player?,
    onBack: () -> Unit,
    onSongDetails: (Song) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onSwipePlayNext: (Song) -> Unit,
    onSwipeAddToPlaylist: (Song) -> Unit
) {
    val scrollState = rememberLazyListState()
    val headerHeight = 520.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }

    val scrollOffset = remember { derivedStateOf {
        if (scrollState.firstVisibleItemIndex == 0) {
            scrollState.firstVisibleItemScrollOffset.toFloat()
        } else {
            headerHeightPx
        }
    } }

    val collapseFraction = remember { derivedStateOf {
        (scrollOffset.value / headerHeightPx).coerceIn(0f, 1f)
    } }

    val artworkColors = rememberArtworkColors(
        artworkUri = album.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    // Dynamic current playing tracking
    var currentPlayingId by remember { mutableLongStateOf(player?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L) }
    
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentPlayingId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val squircleShape = RoundedCornerShape(24.dp)
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        
        // Expressive Background: Vibrant artwork glow that follows the scroll
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -scrollOffset.value * 0.5f }
                .background(
                    Brush.verticalGradient(
                        0f to artworkColors.primary.copy(alpha = 0.4f),
                        0.4f to MaterialTheme.colorScheme.surface
                    )
                )
        )

        val bottomPadding = PaddingValues(bottom = 160.dp)
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(scrollState, color = artworkColors.secondary, padding = bottomPadding),
            contentPadding = bottomPadding
        ) {
            item {
                Spacer(modifier = Modifier.height(headerHeight))
            }

            items(album.songs, key = { it.id }) { song ->
                val isThisPlaying = song.id == currentPlayingId
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    SongItem(
                        song = song,
                        isPlaying = isThisPlaying,
                        onClick = { onPlaySongs(album.songs, album.songs.indexOf(song)) },
                        onDetailsClick = { onSongDetails(song) },
                        onSwipePlayNext = { onSwipePlayNext(song) },
                        onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) }
                    )
                }
            }
        }

        // --- REINVENTED COLLAPSIBLE HEADER ---
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val collapsedHeaderHeight = 100.dp + topPadding
        val currentHeaderHeight = lerp(headerHeight, collapsedHeaderHeight, collapseFraction.value)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight),
            color = if (collapseFraction.value > 0.9f) {
                artworkColors.primary.copy(alpha = 0.98f)
            } else {
                Color.Transparent
            },
            tonalElevation = if (collapseFraction.value > 0.9f) 8.dp else 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 1. DYNAMIC ARTWORK: Shrinks and moves into the header "frame"
                val artSize = lerp(260.dp, 44.dp, collapseFraction.value)
                val artX = lerp(0.dp, 64.dp, collapseFraction.value) // Moves to right of back button
                val artY = lerp(100.dp, topPadding + 14.dp, collapseFraction.value)
                
                Surface(
                    modifier = Modifier
                        .padding(start = artX, top = artY)
                        .size(artSize)
                        // Dynamic alignment swap
                        .align(if (collapseFraction.value < 0.5f) Alignment.TopCenter else Alignment.TopStart)
                        .graphicsLayer {
                            // Subtly rotate back to 0 as it enters the header frame
                            rotationZ = -4f * (1f - collapseFraction.value)
                        },
                    shape = RoundedCornerShape(lerp(32.dp, 8.dp, collapseFraction.value)),
                    tonalElevation = 16.dp,
                    shadowElevation = 24.dp
                ) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 2. NAVIGATION: Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color.Black.copy(alpha = 0.1f * (1f - collapseFraction.value)),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, 
                        contentDescription = "Back",
                        tint = if (collapseFraction.value > 0.8f) contentColorFor(artworkColors.primary) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3. COLLAPSED INFO: Title & Artist sliding in next to the tiny art
                if (collapseFraction.value > 0.8f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .statusBarsPadding()
                            .padding(start = 124.dp, end = 16.dp)
                            .graphicsLayer { alpha = (collapseFraction.value - 0.8f) * 5f }
                    ) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColorFor(artworkColors.primary)
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = artworkColors.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. EXPANDED INFO: Huge typography and strange buttons
                if (collapseFraction.value < 0.7f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 380.dp)
                            .graphicsLayer { 
                                alpha = 1f - (collapseFraction.value * 3f).coerceIn(0f, 1f)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp,
                                letterSpacing = (-1.5).sp,
                                lineHeight = 40.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 32.dp),
                            maxLines = 2
                        )
                        
                        Text(
                            text = album.artist.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = artworkColors.secondary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Strange shapes: Circle Play, Squircle Shuffle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onPlaySongs(album.songs, 0) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = artworkColors.secondary,
                                    contentColor = contentColorFor(artworkColors.secondary)
                                ),
                                modifier = Modifier.size(68.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(36.dp))
                            }

                            Surface(
                                onClick = { onPlaySongs(album.songs.shuffled(), 0) },
                                shape = squircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(2.dp, artworkColors.secondary),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = artworkColors.secondary, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
