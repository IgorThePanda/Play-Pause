package com.igorthepadna.play_pause.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.TabSortSettings
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Artist
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.data.SortType
import com.igorthepadna.play_pause.ui.components.AlbumCard
import com.igorthepadna.play_pause.ui.components.SongItem
import com.igorthepadna.play_pause.ui.components.AlbumDetailView
import com.igorthepadna.play_pause.ui.components.ArtistCard
import com.igorthepadna.play_pause.ui.components.ArtistDetailView
import com.igorthepadna.play_pause.ui.components.CategoryDetailView
import com.igorthepadna.play_pause.ui.components.GenreDetailView
import com.igorthepadna.play_pause.ui.components.playlists.PlaylistView
import com.igorthepadna.play_pause.ui.components.playlists.PlaylistDetailView
import com.igorthepadna.play_pause.data.Playlist
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.unit.sp
import com.igorthepadna.play_pause.ui.components.GenreCard
import com.igorthepadna.play_pause.ui.components.CategoryViewMode
import com.igorthepadna.play_pause.ViewModeSettings
import com.igorthepadna.play_pause.ui.components.CompactSongItem
import com.igorthepadna.play_pause.utils.verticalScrollbar
import com.igorthepadna.play_pause.utils.ScrollbarLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    player: Player?,
    currentFilter: LibraryFilter,
    hasPermission: Boolean,
    isRefreshing: Boolean,
    onPermissionChanged: (Boolean) -> Unit,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    viewModel: MainViewModel? = null,
    onShowMessage: (String) -> Unit = {},
    tabSortSettings: TabSortSettings = TabSortSettings(),
    onSettingsClick: () -> Unit = {},
    onSortClick: () -> Unit = {}
) {
    val searchQuery by viewModel?.searchQuery?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("") }
    val filteredSongs by viewModel?.filteredSongs?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val sortedAlbums by viewModel?.sortedAlbums?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val sortedArtists by viewModel?.sortedArtists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val playlists by viewModel?.playlists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    // Read the current playing ID but don't perform the comparison here
    val currentPlayingId by viewModel?.currentPlayingId?.collectAsStateWithLifecycle(-1L) ?: remember { mutableLongStateOf(-1L) }
    
    val selectedDetail by viewModel?.selectedDetail?.collectAsStateWithLifecycle(null) ?: remember { mutableStateOf(null) }
    val selectedPlaylistWithMetadata by viewModel?.selectedPlaylist?.collectAsStateWithLifecycle(null) ?: remember { mutableStateOf(null) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    
    val selectedPlaylistSongs = remember(selectedPlaylistWithMetadata, filteredSongs) {
        selectedPlaylistWithMetadata?.songs?.mapNotNull { songId ->
            filteredSongs.find { it.id == songId }
        } ?: emptyList()
    }

    val selectedGenreSongs by viewModel?.selectedGenreSongs?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val albumArtMap by viewModel?.albumArtMap?.collectAsStateWithLifecycle(emptyMap()) ?: remember { mutableStateOf(emptyMap()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> onPermissionChanged(permissions.values.all { it }) }

    BackHandler(enabled = selectedDetail != null) {
        viewModel?.popSelection()
    }

    // Automatically close sub-menus when the navigation tab (filter) changes
    LaunchedEffect(currentFilter) {
        viewModel?.clearSelections()
    }

    AnimatedContent(
        targetState = selectedDetail,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    .apply { targetContentZIndex = 1f }
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    .apply { targetContentZIndex = -1f }
            }
        },
        label = "library_transition"
    ) { detailItem ->
        when (detailItem) {
            is Album -> AlbumDetailView(
                album = detailItem,
                player = player,
                onBack = { viewModel?.popSelection() },
                onNavigateToArtist = { artistName ->
                    viewModel?.setSelectedArtistName(artistName)
                },
                onSongDetails = onSongDetails,
                onPlaySongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = onAddToPlaylist
            )
            is Artist -> ArtistDetailView(
                artist = detailItem,
                onBack = { viewModel?.popSelection() },
                onAlbumClick = { viewModel?.setSelectedAlbumId(it.id) },
                onPlayArtist = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onSongClick = { song ->
                    onPlaySongs(detailItem.songs, detailItem.songs.indexOf(song), null)
                },
                onSongDetailsClick = onSongDetails,
                onPlayAllSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onPlaySpecificSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onExpandCategory = { category ->
                    viewModel?.setSelectedArtistCategory(detailItem.name, category)
                },
                viewModel = viewModel
            )
            is Pair<*, *> -> {
                val artist = (detailItem.first as? Artist)
                val category = (detailItem.second as? String)
                if (artist != null && category != null) {
                    val (mainAlbums, singles) = remember(artist.albums) {
                        artist.albums.partition { it.songs.size > 2 }
                    }
                    val unreleasedSongs = remember(artist.songs) {
                        artist.songs.filter { song ->
                            val folderName = song.path.substringBeforeLast('/').substringAfterLast('/')
                            folderName.startsWith("XXXX", ignoreCase = true)
                        }
                    }
                    val categoryKey = "artist_${artist.name}_$category"
                    val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(
                        viewMode = if (category == "Albums" || category == "Singles & EPs") CategoryViewMode.GRID
                        else CategoryViewMode.DETAILED
                    )

                    CategoryDetailView(
                        title = category,
                        artist = artist,
                        mainAlbums = mainAlbums,
                        singles = singles,
                        unreleasedSongs = unreleasedSongs,
                        viewMode = settings.viewMode,
                        columns = settings.columns,
                        onViewModeChange = { newMode ->
                            viewModel?.updateViewModeSettings(categoryKey, settings.copy(viewMode = newMode))
                        },
                        onColumnsChange = { newColumns ->
                            viewModel?.updateViewModeSettings(categoryKey, settings.copy(columns = newColumns))
                        },
                        onBack = { viewModel?.popSelection() },
                        onAlbumClick = { viewModel?.setSelectedAlbumId(it.id) },
                        onSongClick = { onPlaySongs(listOf(it), 0, null) },
                        onSongDetailsClick = onSongDetails,
                        onPlayAllSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                        onPlaySpecificSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                        albumArtMap = albumArtMap
                    )
                }
            }
            is Playlist -> PlaylistDetailView(
                playlist = detailItem,
                playlistSongs = selectedPlaylistSongs,
                currentPlayingId = currentPlayingId,
                onBack = { viewModel?.popSelection() },
                onPlaySongs = onPlaySongs,
                onSongDetails = onSongDetails,
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = onAddToPlaylist,
                viewModel = viewModel
            )
            is String -> GenreDetailView(
                genre = detailItem,
                songs = selectedGenreSongs,
                currentPlayingId = currentPlayingId,
                onBack = { viewModel?.popSelection() },
                onSongClick = { song -> onPlaySongs(selectedGenreSongs, selectedGenreSongs.indexOf(song), null) },
                onSongDetailsClick = onSongDetails,
                onPlaySongs = onPlaySongs,
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = onAddToPlaylist,
                viewModel = viewModel
            )
            else -> Box(modifier = Modifier.fillMaxSize()) {
                val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val listTopPadding = 84.dp + statusBarPadding
                val bottomPadding = 140.dp
                val contentPadding = PaddingValues(top = listTopPadding, bottom = bottomPadding, start = 16.dp, end = 16.dp)
                val scrollbarPadding = PaddingValues(bottom = bottomPadding)

                if (filteredSongs.isEmpty() && hasPermission && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isEmpty()) "No items found." else "No matches found.")
                    }
                } else if (!hasPermission) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissions)
                        }) { Text("Grant Permissions") }
                    }
                } else {
                    when (currentFilter) {
                        LibraryFilter.ALBUMS -> Box(modifier = Modifier.fillMaxSize()) {
                            val categoryKey = "library_albums"
                            val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                            val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.GRID)
                            val effectiveColumns = if (settings.viewMode == CategoryViewMode.GRID) settings.columns else 1

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(effectiveColumns),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                            ) {
                                items(
                                    items = sortedAlbums, 
                                    key = { it.id },
                                    contentType = { "album_card" }
                                ) { album ->
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        AlbumCard(
                                            album = album, 
                                            onClick = { viewModel?.setSelectedAlbumId(album.id) },
                                            onPlayClick = { onPlaySongs(album.songs, 0, null) },
                                            columns = effectiveColumns
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = album.songs.first(),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedAlbumId(album.id) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = album.title,
                                            secondaryLabel = album.artist,
                                            artworkUri = album.artworkUri,
                                            onPlayClick = { onPlaySongs(album.songs, 0, null) }
                                        )
                                    } else {
                                        SongItem(
                                            song = album.songs.first(),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedAlbumId(album.id) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = album.title,
                                            secondaryLabel = album.artist,
                                            artworkUri = album.artworkUri
                                        )
                                    }
                                }
                            }
                            ScrollbarLabel(
                                state = gridState,
                                padding = scrollbarPadding,
                                labelProvider = { index ->
                                    val album = sortedAlbums.getOrNull(index) ?: return@ScrollbarLabel ""
                                    when(tabSortSettings.sortType) {
                                        SortType.TITLE -> album.title.firstOrNull()?.uppercase()?.toString() ?: ""
                                        SortType.ARTIST -> album.artist.firstOrNull()?.uppercase()?.toString() ?: ""
                                        else -> ""
                                    }
                                }
                            )
                        }
                        LibraryFilter.ARTISTS -> Box(modifier = Modifier.fillMaxSize()) {
                            val categoryKey = "library_artists"
                            val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                            val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.GRID)
                            val effectiveColumns = if (settings.viewMode == CategoryViewMode.GRID) settings.columns else 1

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(effectiveColumns),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                            ) {
                                items(
                                    items = sortedArtists, 
                                    key = { it.name },
                                    contentType = { "artist_card" }
                                ) { artist ->
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        ArtistCard(
                                            artist = artist, 
                                            onClick = { viewModel?.setSelectedArtistName(artist.name) },
                                            columns = effectiveColumns
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = artist.songs.first(),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedArtistName(artist.name) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = artist.name,
                                            secondaryLabel = "${artist.albumCount} Albums • ${artist.trackCount} Songs",
                                            artworkUri = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri
                                        )
                                    } else {
                                        SongItem(
                                            song = artist.songs.first(),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedArtistName(artist.name) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = artist.name,
                                            secondaryLabel = "${artist.albumCount} Albums • ${artist.trackCount} Songs",
                                            artworkUri = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri
                                        )
                                    }
                                }
                            }
                            ScrollbarLabel(
                                state = gridState,
                                padding = scrollbarPadding,
                                labelProvider = { index ->
                                    val artist = sortedArtists.getOrNull(index) ?: return@ScrollbarLabel ""
                                    artist.name.firstOrNull()?.uppercase()?.toString() ?: ""
                                }
                            )
                        }
                        LibraryFilter.PLAYLISTS -> Box(modifier = Modifier.fillMaxSize()) {
                            val categoryKey = "library_playlists"
                            val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                            val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.DETAILED)
                            val effectiveColumns = if (settings.viewMode == CategoryViewMode.GRID) settings.columns else 1

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(effectiveColumns),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                            ) {
                                items(playlists) { playlist ->
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        AlbumCard(
                                            album = Album(
                                                id = 0,
                                                title = playlist.name,
                                                artist = "${playlist.songs.size} Songs",
                                                artworkUri = null,
                                                songs = emptyList()
                                            ),
                                            onClick = { viewModel?.setSelectedPlaylistId(playlist.id) }
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedPlaylistId(playlist.id) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = playlist.name,
                                            secondaryLabel = "${playlist.songs.size} Songs",
                                            artworkUri = null
                                        )
                                    } else {
                                        SongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedPlaylistId(playlist.id) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = playlist.name,
                                            secondaryLabel = "${playlist.songs.size} Songs",
                                            artworkUri = null
                                        )
                                    }
                                }
                            }
                        }
                        LibraryFilter.GENRES -> Box(modifier = Modifier.fillMaxSize()) {
                            val genres = remember(filteredSongs) {
                                filteredSongs.mapNotNull { it.genre }.distinct().sorted()
                            }
                            val categoryKey = "library_genres"
                            val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                            val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.GRID)
                            val effectiveColumns = if (settings.viewMode == CategoryViewMode.GRID) settings.columns else 1

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(effectiveColumns),
                                contentPadding = contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                            ) {
                                items(genres) { genre ->
                                    val count = filteredSongs.count { it.genre == genre }
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        GenreCard(
                                            genre = genre,
                                            songCount = count,
                                            onClick = { viewModel?.setSelectedGenreName(genre) }
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedGenreName(genre) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = genre,
                                            secondaryLabel = "$count Songs",
                                            artworkUri = null
                                        )
                                    } else {
                                        SongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = false,
                                            onClick = { viewModel?.setSelectedGenreName(genre) },
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = genre,
                                            secondaryLabel = "$count Songs",
                                            artworkUri = null
                                        )
                                    }
                                }
                            }
                        }
                        else -> Box(modifier = Modifier.fillMaxSize()) {
                            val categoryKey = "library_songs"
                            val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                            val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.DETAILED)
                            
                            if (settings.viewMode == CategoryViewMode.GRID) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(settings.columns),
                                    contentPadding = contentPadding,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                    modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                                ) {
                                    items(filteredSongs) { song ->
                                        val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                                        AlbumCard(
                                            album = Album(
                                                id = song.albumId,
                                                title = song.title,
                                                artist = song.artist,
                                                artworkUri = albumArt,
                                                songs = listOf(song)
                                            ),
                                            onClick = { onPlaySongs(filteredSongs, filteredSongs.indexOf(song), null) }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = contentPadding,
                                    modifier = Modifier.fillMaxSize().verticalScrollbar(listState, padding = scrollbarPadding)
                                ) {
                                    itemsIndexed(
                                        items = filteredSongs, 
                                        key = { _, song -> song.id },
                                        contentType = { _, _ -> "song_item" }
                                    ) { index, song ->
                                        val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                                        if (settings.viewMode == CategoryViewMode.COMPACT) {
                                            CompactSongItem(
                                                song = song,
                                                isPlaying = song.id == currentPlayingId,
                                                onClick = { onPlaySongs(filteredSongs, index, null) },
                                                onDetailsClick = { onSongDetails(song) },
                                                onSwipePlayNext = {
                                                    viewModel?.addPlayNext(song)
                                                    onShowMessage("Added to Play Next")
                                                },
                                                onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                                                artworkUri = albumArt
                                            )
                                        } else {
                                            SongItem(
                                                song = song,
                                                isPlaying = song.id == currentPlayingId,
                                                onClick = { onPlaySongs(filteredSongs, index, null) },
                                                onDetailsClick = { onSongDetails(song) },
                                                onSwipePlayNext = {
                                                    viewModel?.addPlayNext(song)
                                                    onShowMessage("Added to Play Next")
                                                },
                                                onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                                                artworkUri = albumArt
                                            )
                                        }
                                    }
                                }
                            }
                            ScrollbarLabel(
                                state = listState,
                                padding = scrollbarPadding,
                                labelProvider = { index ->
                                    val song = filteredSongs.getOrNull(index) ?: return@ScrollbarLabel ""
                                    when(tabSortSettings.sortType) {
                                        SortType.TITLE -> song.title.firstOrNull()?.uppercase()?.toString() ?: ""
                                        SortType.ARTIST -> song.artist.firstOrNull()?.uppercase()?.toString() ?: ""
                                        else -> ""
                                    }
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarPadding + 32.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to MaterialTheme.colorScheme.background,
                                0.6f to MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Top Bar Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 20.dp, end = 24.dp)
                        .align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categoryKey = when(currentFilter) {
                        LibraryFilter.ALBUMS -> "library_albums"
                        LibraryFilter.ARTISTS -> "library_artists"
                        LibraryFilter.GENRES -> "library_genres"
                        LibraryFilter.PLAYLISTS -> "library_playlists"
                        else -> "library_songs"
                    }
                    val viewModeSettings by (viewModel?.viewModeSettings?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
                    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(
                        viewMode = if (currentFilter == LibraryFilter.ALBUMS || currentFilter == LibraryFilter.ARTISTS || currentFilter == LibraryFilter.GENRES) CategoryViewMode.GRID else CategoryViewMode.DETAILED
                    )

                    if (settings.viewMode == CategoryViewMode.GRID) {
                        FilledIconButton(
                            onClick = {
                                val newColumns = if (settings.columns >= 4) 1 else settings.columns + 1
                                viewModel?.updateViewModeSettings(categoryKey, settings.copy(columns = newColumns))
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                            )
                        ) {
                            Text("${settings.columns}", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = {
                            val newMode = when (settings.viewMode) {
                                CategoryViewMode.DETAILED -> CategoryViewMode.COMPACT
                                CategoryViewMode.COMPACT -> if (currentFilter == LibraryFilter.SONGS) CategoryViewMode.DETAILED else CategoryViewMode.GRID
                                CategoryViewMode.GRID -> CategoryViewMode.DETAILED
                            }
                            viewModel?.updateViewModeSettings(categoryKey, settings.copy(viewMode = newMode))
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            when (settings.viewMode) {
                                CategoryViewMode.GRID -> Icons.Rounded.GridView
                                CategoryViewMode.DETAILED -> Icons.Rounded.ViewStream
                                CategoryViewMode.COMPACT -> Icons.Rounded.ViewHeadline
                            },
                            contentDescription = "View Mode",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.4f to MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                )

            }
        }
    }
}
