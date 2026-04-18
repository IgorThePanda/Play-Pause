package com.igorthepadna.play_pause.ui.components.playlists

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.ViewModeSettings
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.ui.components.AlbumCard
import com.igorthepadna.play_pause.ui.components.CategoryViewMode
import com.igorthepadna.play_pause.ui.components.CompactSongItem
import com.igorthepadna.play_pause.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    playlistSongs: List<Song>,
    currentPlayingId: Long,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onSongDetails: (Song) -> Unit,
    onSwipePlayNext: (Song) -> Unit,
    onSwipeAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel? = null
) {
    val categoryKey = "playlist_${playlist.id}"
    val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.DETAILED)
    
    val viewMode = settings.viewMode
    val columns = settings.columns

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(playlist.name, fontWeight = FontWeight.Black)
                        Text(
                            "${playlistSongs.size} Songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                                contentDescription = "View Mode"
                            )
                        }

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

                        Spacer(Modifier.width(4.dp))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        val effectiveColumns = if (viewMode == CategoryViewMode.GRID) columns else 1

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(effectiveColumns),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = playlistSongs,
                    key = { it.id }
                ) { song ->
                    when (viewMode) {
                        CategoryViewMode.GRID -> {
                            AlbumCard(
                                album = Album(
                                    id = song.albumId,
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUri = song.albumArtUri,
                                    songs = listOf(song)
                                ),
                                onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song)) },
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        CategoryViewMode.COMPACT -> {
                            CompactSongItem(
                                song = song,
                                isPlaying = song.id == currentPlayingId,
                                onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song)) },
                                onDetailsClick = { onSongDetails(song) },
                                onSwipePlayNext = { onSwipePlayNext(song) },
                                onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) },
                                showArtist = true
                            )
                        }
                        CategoryViewMode.DETAILED -> {
                            SongItem(
                                song = song,
                                isPlaying = song.id == currentPlayingId,
                                onClick = { onPlaySongs(playlistSongs, playlistSongs.indexOf(song)) },
                                onDetailsClick = { onSongDetails(song) },
                                onSwipePlayNext = { onSwipePlayNext(song) },
                                onSwipeAddToPlaylist = { onSwipeAddToPlaylist(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}
