package com.igorthepadna.play_pause.ui.components.playlists

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.ViewModeSettings
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.ui.components.AlbumCard
import com.igorthepadna.play_pause.ui.components.CategoryViewMode
import com.igorthepadna.play_pause.ui.components.CompactSongItem
import com.igorthepadna.play_pause.ui.components.SongItem
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    playlistSongs: List<Song>,
    currentPlayingId: Long,
    albumArtMap: Map<Long, android.net.Uri?>,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onSwipePlayNext: (Song) -> Unit,
    onSwipeAddToPlaylist: (Song) -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel? = null
) {
    val gridState = rememberLazyGridState()
    val categoryKey = "playlist_${playlist.id}"
    val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.DETAILED)
    
    val viewMode = settings.viewMode
    val columns = settings.columns

    val firstSongArtwork = remember(playlistSongs) { playlistSongs.firstOrNull()?.albumArtUri }
    val artworkColors = rememberArtworkColors(
        artworkUri = firstSongArtwork,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val bannerThresholdPx = with(LocalDensity.current) { 200.dp.toPx() }
    val showBanner by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > bannerThresholdPx
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val bottomPadding = PaddingValues(
            top = 16.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 140.dp
        )

        val effectiveColumns = if (viewMode == CategoryViewMode.GRID) columns else 1

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No songs in this playlist",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(effectiveColumns),
                contentPadding = bottomPadding,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScrollbar(gridState, padding = bottomPadding)
            ) {
                item(span = { GridItemSpan(effectiveColumns) }) {
                    PlaylistHighFidelityHeader(
                        playlist = playlist,
                        songCount = playlistSongs.size,
                        artworkUri = firstSongArtwork,
                        onBack = onBack,
                        onPlay = { onPlaySongs(playlistSongs, 0, false) },
                        onShuffle = { onPlaySongs(playlistSongs, 0, true) }
                    )
                }

                if (viewMode == CategoryViewMode.GRID) {
                    gridItems(playlistSongs, key = { it.id }) { song ->
                        val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                        AlbumCard(
                            album = Album(
                                id = song.albumId,
                                title = song.title,
                                artist = song.artist,
                                artworkUri = albumArt,
                                songs = listOf(song)
                            ),
                            onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song), null) },
                            modifier = Modifier.padding(4.dp),
                            columns = columns,
                            isPlaying = song.id == currentPlayingId
                        )
                    }
                } else {
                    item(span = { GridItemSpan(effectiveColumns) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            playlistSongs.forEach { song ->
                                val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                                Box(modifier = Modifier.padding(vertical = 2.dp)) {
                                    if (viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = song,
                                            isPlaying = song.id == currentPlayingId,
                                            onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song), null) },
                                            onDetailsClick = { onSongDetails(song) },
                                            onSwipePlayNext = { onSwipePlayNext(song) },
                                            onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) },
                                            onNavigateToArtist = onNavigateToArtist,
                                            showArtist = true,
                                            containerColor = if (song.id == currentPlayingId) null else Color.Transparent,
                                            artworkUri = albumArt
                                        )
                                    } else {
                                        SongItem(
                                            song = song,
                                            isPlaying = song.id == currentPlayingId,
                                            onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song), null) },
                                            onDetailsClick = { onSongDetails(song) },
                                            onSwipePlayNext = { onSwipePlayNext(song) },
                                            onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) },
                                            onNavigateToArtist = onNavigateToArtist,
                                            containerColor = if (song.id == currentPlayingId) null else Color.Transparent,
                                            artworkUri = albumArt
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Controls (View Mode / Columns) - Floating
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (viewMode == CategoryViewMode.GRID) {
                TextButton(
                    onClick = {
                        val newColumns = if (columns >= 4) 1 else columns + 1
                        viewModel?.updateViewModeSettings(categoryKey, settings.copy(columns = newColumns))
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("$columns", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            IconButton(onClick = {
                val newMode = when (viewMode) {
                    CategoryViewMode.DETAILED -> CategoryViewMode.COMPACT
                    CategoryViewMode.COMPACT -> CategoryViewMode.GRID
                    CategoryViewMode.GRID -> CategoryViewMode.DETAILED
                }
                viewModel?.updateViewModeSettings(categoryKey, settings.copy(viewMode = newMode))
            }) {
                Icon(
                    when (viewMode) {
                        CategoryViewMode.GRID -> Icons.Rounded.GridView
                        CategoryViewMode.DETAILED -> Icons.Rounded.ViewStream
                        CategoryViewMode.COMPACT -> Icons.Rounded.ViewHeadline
                    },
                    contentDescription = "View Mode",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            PlaylistPillBanner(
                playlist = playlist,
                artworkUri = firstSongArtwork,
                artworkColors = artworkColors,
                onBack = onBack
            )
        }
    }
}

@Composable
fun PlaylistHighFidelityHeader(
    playlist: Playlist,
    songCount: Int,
    artworkUri: android.net.Uri?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
        ) {
            // Main Artwork (Centered Rounded Rectangle to differentiate from Artist Circle)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 12.dp,
                shadowElevation = 20.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                if (artworkUri != null) {
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistPlay,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Glassmorphic Back Button (Top Left)
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
                    if (artworkUri != null) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(24.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Back",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }

            // Shuffle Button Pill (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { onShuffle() }
                    .shadow(12.dp, CircleShape)
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(24.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
                Icon(
                    Icons.Rounded.Shuffle,
                    null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
            
            // Play Button Pill (Bottom Start)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { onPlay() }
                    .shadow(12.dp, CircleShape)
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().blur(24.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
                Icon(
                    Icons.Rounded.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = playlist.name,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "$songCount Songs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PlaylistPillBanner(
    playlist: Playlist,
    artworkUri: android.net.Uri?,
    artworkColors: ArtworkColors,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .fillMaxWidth(),
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
            
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = playlist.name,
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
