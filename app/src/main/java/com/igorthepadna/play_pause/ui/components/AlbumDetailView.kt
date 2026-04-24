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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar
import androidx.media3.common.Player

@Composable
fun AlbumDetailView(
    album: Album,
    currentPlayingId: Long,
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
                                    onNavigateToArtist = onNavigateToArtist,
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
            PillBanner(album = album, artworkColors = artworkColors, onBack = onBack, onNavigateToArtist = onNavigateToArtist)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Back Button Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                ) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.2f)))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.align(Alignment.Center),
                        tint = Color.White
                    )
                }

                // Info Pill (Bottom)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = album.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(50.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
                    
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        val artists = remember(album.artist) { MusicRepository.splitArtists(album.artist) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            artists.forEachIndexed { index, artistName ->
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onNavigateToArtist(artistName) }
                                )
                                if (index < artists.size - 1) {
                                    Text(
                                        text = " & ",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = artworkColors.secondary.copy(alpha = 0.1f),
                    contentColor = artworkColors.secondary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", fontWeight = FontWeight.Black)
                }
            }

            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = artworkColors.secondary,
                    contentColor = contentColorFor(artworkColors.secondary)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun PillBanner(
    album: Album,
    artworkColors: ArtworkColors,
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
            
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                AsyncImage(
                    model = album.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val artists = remember(album.artist) { MusicRepository.splitArtists(album.artist) }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    artists.forEachIndexed { index, artistName ->
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onNavigateToArtist(artistName) }
                        )
                        if (index < artists.size - 1) {
                            Text(
                                text = " & ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            
            IconButton(
                onClick = { /* Add to playlist */ },
                modifier = Modifier.background(artworkColors.secondary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Rounded.Add, null, tint = artworkColors.secondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
