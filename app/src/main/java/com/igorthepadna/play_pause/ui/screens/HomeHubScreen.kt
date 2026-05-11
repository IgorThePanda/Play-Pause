package com.igorthepadna.play_pause.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.*
import com.igorthepadna.play_pause.ui.components.AlbumCard
import com.igorthepadna.play_pause.ui.components.ArtistCard
import com.igorthepadna.play_pause.ui.components.UniversalSongItem
import com.igorthepadna.play_pause.ui.screens.StatsSummaryView

@Composable
fun HomeHubScreen(
    viewModel: MainViewModel,
    currentHubFilter: HubFilter,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    val pinnedItems by viewModel.pinnedItems.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.allArtists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val albumArtMap by viewModel.albumArtMap.collectAsStateWithLifecycle(emptyMap())

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val weeklyTopArtist by viewModel.weeklyTopArtist.collectAsStateWithLifecycle()
    val weeklyTopTrack by viewModel.weeklyTopTrack.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val hubOrder by viewModel.hubOrder.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentHubFilter,
        transitionSpec = {
            val oldIdx = hubOrder.indexOf(initialState)
            val newIdx = hubOrder.indexOf(targetState)
            if (newIdx > oldIdx) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "hub_page_transition"
    ) { filter ->
        when (filter) {
            HubFilter.NEWS -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarPadding + 80.dp,
                        bottom = navigationBarPadding + 120.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HubHeader("Your News")
                    }

                    if (weeklyTopArtist != null) {
                        item {
                            NewsCard(
                                title = "Top Artist",
                                subtitle = weeklyTopArtist!!.artistName,
                                icon = Icons.Rounded.Person,
                                onClick = { viewModel.setSelectedArtistName(weeklyTopArtist!!.artistName) }
                            )
                        }
                    }

                    if (weeklyTopTrack != null) {
                        item {
                            val song = songs.find { it.id == weeklyTopTrack!!.songId }
                            NewsCard(
                                title = "Top Track",
                                subtitle = song?.title ?: "Unknown",
                                icon = Icons.Rounded.MusicNote,
                                onClick = { song?.let { onPlaySongs(listOf(it), 0, null) } }
                            )
                        }
                    }

                    if (recentlyAdded.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            HubHeader("Recently Added")
                        }
                        items(recentlyAdded, key = { "recent_${it.id}" }) { song ->
                            UniversalSongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onPlaySongs(listOf(song), 0, null) },
                                onDetailsClick = { onSongDetails(song) },
                                onSwipePlayNext = { viewModel.addPlayNext(song) },
                                onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                                shape = RoundedCornerShape(24.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        }
                    }
                }
            }
            HubFilter.STATS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    var showPlayTimeDetail by remember { mutableStateOf(false) }

                    if (showPlayTimeDetail) {
                        PlayTimeDetailView(
                            viewModel = viewModel, 
                            onBack = { showPlayTimeDetail = false },
                            contentPadding = PaddingValues(
                                top = statusBarPadding + 80.dp,
                                bottom = navigationBarPadding + 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                        )
                    } else {
                        StatsSummaryView(
                            viewModel = viewModel,
                            onPlayTimeClick = { showPlayTimeDetail = true },
                            contentPadding = PaddingValues(
                                top = statusBarPadding + 80.dp,
                                bottom = navigationBarPadding + 120.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                        )
                    }
                }
            }
            HubFilter.HOME -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarPadding + 80.dp,
                        bottom = navigationBarPadding + 120.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HubHeader("Pinned Board")
                    }

                    if (pinnedItems.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            EmptyPinnedState()
                        }
                    } else {
                        items(pinnedItems) { pin ->
                            PinnedItemCard(
                                pin = pin,
                                songs = songs,
                                albums = albums,
                                artists = artists,
                                playlists = playlists,
                                albumArtMap = albumArtMap,
                                onPlaySongs = onPlaySongs,
                                onSongDetails = onSongDetails,
                                onAddToPlaylist = onAddToPlaylist,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        ),
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun NewsCard(
    title: String, 
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (icon != null) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyPinnedState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.PushPin,
            null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Nothing pinned yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Long-press or open details to pin items here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PinnedItemCard(
    pin: PinnedItem,
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    albumArtMap: Map<Long, android.net.Uri?>,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    viewModel: MainViewModel
) {
    when (pin.type) {
        PinnedType.SONG -> {
            val song = songs.find { it.id.toString() == pin.mediaId }
            if (song != null) {
                UniversalSongItem(
                    song = song,
                    isPlaying = false,
                    onClick = { onPlaySongs(listOf(song), 0, null) },
                    onDetailsClick = { onSongDetails(song) },
                    onSwipePlayNext = { viewModel.addPlayNext(song) },
                    onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
        PinnedType.ALBUM -> {
            val album = albums.find { it.id.toString() == pin.mediaId }
            if (album != null) {
                AlbumCard(
                    album = album,
                    onClick = { 
                        viewModel.setSelectedAlbumId(album.id)
                    },
                    onPlayClick = { onPlaySongs(album.songs, 0, null) },
                    columns = 2
                )
            }
        }
        PinnedType.ARTIST -> {
            val artist = artists.find { it.name == pin.mediaId }
            if (artist != null) {
                ArtistCard(
                    artist = artist,
                    onClick = { 
                        viewModel.setSelectedArtistName(artist.name)
                    },
                    columns = 2
                )
            }
        }
        PinnedType.PLAYLIST -> {
            val playlist = playlists.find { it.id == pin.mediaId }
            if (playlist != null) {
                com.igorthepadna.play_pause.ui.components.playlists.PlaylistCard(
                    playlist = playlist,
                    onClick = { 
                        viewModel.setSelectedPlaylistId(playlist.id)
                    },
                    albumArtMap = albumArtMap
                )
            }
        }
    }
}
