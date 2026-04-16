package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors
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

    val bannerThresholdPx = with(LocalDensity.current) { 280.dp.toPx() }
    val showBanner by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > bannerThresholdPx
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        
        // Expressive Background Gradient with Artwork Colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to artworkColors.primary.copy(alpha = 0.35f),
                        0.5f to MaterialTheme.colorScheme.surface
                    )
                )
        )

        val bottomPadding = PaddingValues(bottom = 120.dp)
        val hasMultipleDiscs = remember(album.songs) { album.songs.any { it.discNumber > 1 } }
        
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(scrollState, color = artworkColors.secondary, padding = bottomPadding),
            contentPadding = bottomPadding
        ) {
            item {
                AlbumHeader(
                    album = album,
                    artworkColors = artworkColors,
                    onBack = onBack,
                    onPlay = { onPlaySongs(album.songs, 0) },
                    onShuffle = { onPlaySongs(album.songs.shuffled(), 0) }
                )
            }

            album.songs.groupBy { it.discNumber }.forEach { (discNumber, discSongs) ->
                if (hasMultipleDiscs) {
                    item(key = "disc_$discNumber") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Album,
                                        contentDescription = null,
                                        tint = artworkColors.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "DISC $discNumber",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 2.sp,
                                        fontSize = 10.sp
                                    ),
                                    fontWeight = FontWeight.Black,
                                    color = artworkColors.secondary
                                )
                            }
                            // Bridge line to the first item
                            Box(
                                modifier = Modifier
                                    .padding(start = 11.dp)
                                    .width(2.dp)
                                    .height(8.dp)
                                    .background(artworkColors.secondary.copy(alpha = 0.25f))
                            )
                        }
                    }
                }

                items(
                    items = discSongs,
                    key = { it.id },
                    contentType = { "song_item" }
                ) { song ->
                    val isFirst = discSongs.firstOrNull()?.id == song.id
                    val isLast = discSongs.lastOrNull()?.id == song.id
                    
                    val shape = when {
                        isFirst && isLast -> RoundedCornerShape(12.dp)
                        isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        isLast -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(start = if (hasMultipleDiscs) 8.dp else 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasMultipleDiscs) {
                            // Vertical accent line - Seamless connection
                            Box(
                                modifier = Modifier
                                    .padding(start = 11.dp)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(artworkColors.secondary.copy(alpha = 0.25f))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            CompactSongItem(
                                song = song,
                                isPlaying = song.id == currentPlayingId,
                                onClick = { onPlaySongs(album.songs, album.songs.indexOf(song)) },
                                onDetailsClick = { onSongDetails(song) },
                                onSwipePlayNext = { onSwipePlayNext(song) },
                                onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) },
                                showArtist = song.artist != album.artist,
                                shape = shape
                            )
                        }
                    }
                }
            }
        }

        // Pill Banner that appears on scroll
        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            PillBanner(
                album = album,
                artworkColors = artworkColors,
                onBack = onBack
            )
        }
    }
}

@Composable
fun AlbumHeader(
    album: Album,
    artworkColors: ArtworkColors,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 16.dp).offset(x = (-12).dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedCard(
                modifier = Modifier.size(130.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
            ) {
                AsyncImage(
                    model = album.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        lineHeight = 32.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = artworkColors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = artworkColors.secondary,
                    contentColor = contentColorFor(artworkColors.secondary)
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Play All", fontWeight = FontWeight.ExtraBold)
            }

            FilledTonalButton(
                onClick = onShuffle,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, null)
                Spacer(Modifier.width(8.dp))
                Text("Shuffle", fontWeight = FontWeight.ExtraBold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            thickness = 1.dp
        )
    }
}

@Composable
fun PillBanner(
    album: Album,
    artworkColors: ArtworkColors,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .fillMaxWidth(),
        shape = CircleShape, // Pill Shape
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, 
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            AsyncImage(
                model = album.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Space for the Settings icon that lives in MainActivity
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}
