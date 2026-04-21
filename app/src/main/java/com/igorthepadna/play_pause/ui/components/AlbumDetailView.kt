package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onNavigateToArtist: (String) -> Unit,
    onSongDetails: (Song) -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSwipePlayNext: (Song) -> Unit,
    onSwipeAddToPlaylist: (Song) -> Unit
) {
    val scrollState = rememberLazyListState()
    val artworkColors = rememberArtworkColors(
        artworkUri = album.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    var sortOrder by remember { mutableIntStateOf(0) } // 0: Default, 1: A-Z, 2: Duration
    val sortedSongs = remember(album.songs, sortOrder) {
        when (sortOrder) {
            1 -> album.songs.sortedBy { it.title }
            2 -> album.songs.sortedByDescending { it.duration }
            else -> album.songs
        }
    }

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

    val bannerThresholdPx = with(LocalDensity.current) { 500.dp.toPx() }
    val showBanner by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > bannerThresholdPx
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val bottomPadding = PaddingValues(bottom = 120.dp)
        
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(scrollState, color = artworkColors.secondary, padding = bottomPadding),
            contentPadding = bottomPadding
        ) {
            item {
                AlbumLargeHeader(
                    album = album,
                    artworkColors = artworkColors,
                    onBack = onBack,
                    onPlay = { onPlaySongs(sortedSongs, 0, false) },
                    onShuffle = { onPlaySongs(sortedSongs, 0, true) },
                    sortOrder = sortOrder,
                    onToggleSort = { sortOrder = (sortOrder + 1) % 3 },
                    onNavigateToArtist = onNavigateToArtist
                )
            }

            val hasMultipleDiscs = sortedSongs.any { it.discNumber > 1 }
            
            sortedSongs.groupBy { it.discNumber }.forEach { (discNumber, discSongs) ->
                item(key = "disc_container_$discNumber") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        if (hasMultipleDiscs && sortOrder == 0) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Album, null, tint = artworkColors.secondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "DISC $discNumber",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = artworkColors.secondary
                                )
                            }
                        }

                        discSongs.forEach { song ->
                            Box(modifier = Modifier.padding(vertical = 2.dp)) {
                                CompactSongItem(
                                    song = song,
                                    isPlaying = song.id == currentPlayingId,
                                    onClick = { onPlaySongs(sortedSongs, sortedSongs.indexOf(song), null) },
                                    onDetailsClick = { onSongDetails(song) },
                                    onSwipePlayNext = { onSwipePlayNext(song) },
                                    onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) },
                                    showArtist = song.artist != album.artist,
                                    shape = RoundedCornerShape(16.dp),
                                    artworkUri = album.artworkUri,
                                    containerColor = if (song.id == currentPlayingId) null else Color.Transparent
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp)
        ) {
            PillBanner(album = album, artworkColors = artworkColors, onBack = onBack)
        }
    }
}

@Composable
fun AlbumLargeHeader(
    album: Album,
    artworkColors: ArtworkColors,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    sortOrder: Int,
    onToggleSort: () -> Unit,
    onNavigateToArtist: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Spacer(Modifier.height(8.dp))
        
        // Artwork Container (Centered, slightly larger)
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
        ) {
            // Layer 1: Sharp Artwork (Background)
            AsyncImage(
                model = album.artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Layer 2: Pills with aligned background blur
            
            // Back Button Pill (Top Left) - Moved inside artwork
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onBack() }
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(20.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, 
                    "Back", 
                    tint = Color.White, 
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }

            // Song Count Pill (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .height(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(20.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }

                Row(
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${album.songs.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Info Content (Title & Artist) - Floating on artwork
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .height(IntrinsicSize.Min)
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(20.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onNavigateToArtist(album.artist) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Below artwork)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSort,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = when(sortOrder) {
                        1 -> Icons.Rounded.SortByAlpha
                        2 -> Icons.Rounded.Timer
                        else -> Icons.AutoMirrored.Rounded.Sort
                    },
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onShuffle,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = artworkColors.secondary.copy(alpha = 0.15f),
                    contentColor = artworkColors.secondary
                )
            ) {
                Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shuffle", fontWeight = FontWeight.ExtraBold)
            }

            FloatingActionButton(
                onClick = onPlay,
                containerColor = artworkColors.secondary,
                contentColor = contentColorFor(artworkColors.secondary),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(32.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PillBanner(
    album: Album,
    artworkColors: ArtworkColors,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).height(48.dp).fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            AsyncImage(
                model = album.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
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
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}
