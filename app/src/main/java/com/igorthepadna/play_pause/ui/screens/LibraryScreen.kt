package com.igorthepadna.play_pause.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.TabSortSettings
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Artist
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.MusicRepository
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
    val genres by viewModel?.genres?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val albumArtMap by viewModel?.albumArtMap?.collectAsStateWithLifecycle(emptyMap()) ?: remember { mutableStateOf(emptyMap()) }
    val currentPlayingId by viewModel?.currentPlayingId?.collectAsStateWithLifecycle(-1L) ?: remember { mutableLongStateOf(-1L) }
    val currentPlayingSong by viewModel?.currentPlayingSong?.collectAsStateWithLifecycle(null) ?: remember { mutableStateOf(null) }
    val viewModeSettings by viewModel?.viewModeSettings?.collectAsStateWithLifecycle(emptyMap()) ?: remember { mutableStateOf(emptyMap()) }

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
        targetState = currentFilter to selectedDetail,
        transitionSpec = {
            val oldDetail = initialState.second
            val newDetail = targetState.second
            val oldFilter = initialState.first
            val newFilter = targetState.first

            if (newDetail != null && oldDetail == null) {
                // Navigating INTO a detail view
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    .apply { targetContentZIndex = 1f }
            } else if (newDetail == null && oldDetail != null) {
                // Navigating BACK from a detail view
                (slideInHorizontally { width -> -width / 2 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    .apply { targetContentZIndex = -1f }
            } else if (oldFilter != newFilter && newDetail == null) {
                // Switching categories at the top level
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            } else {
                // Internal detail change or other
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "library_transition"
    ) { (filter, detailItem) ->
        when (detailItem) {
            is Album -> AlbumDetailView(
                album = detailItem,
                currentPlayingId = currentPlayingId,
                onBack = { viewModel?.popSelection() },
                onNavigateToArtist = { artistName ->
                    val artists = MusicRepository.splitArtists(artistName)
                    if (artists.size > 1) {
                        viewModel?.showArtistSelection(artists)
                    } else {
                        viewModel?.setSelectedArtistName(artistName)
                    }
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
                currentPlayingId = currentPlayingId,
                currentPlayingSong = currentPlayingSong,
                albumArtMap = albumArtMap,
                onBack = { viewModel?.popSelection() },
                onAlbumClick = { viewModel?.setSelectedAlbumId(it.id) },
                onPlayArtist = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onSongClick = { song ->
                    onPlaySongs(detailItem.songs, detailItem.songs.indexOf(song), null)
                },
                onSongDetailsClick = onSongDetails,
                onPlayAllSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onPlaySpecificSongs = { songs, index, shuffle -> onPlaySongs(songs, index, shuffle) },
                onNavigateToArtist = { artistName ->
                    val artists = MusicRepository.splitArtists(artistName)
                    if (artists.size > 1) {
                        viewModel?.showArtistSelection(artists)
                    } else {
                        viewModel?.setSelectedArtistName(artistName)
                    }
                },
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
                    val unreleasedSongs = remember(artist.songs, artist.featuredSongs) {
                        (artist.songs + artist.featuredSongs).filter { song ->
                            val folderName = song.path.substringBeforeLast('/').substringAfterLast('/')
                            folderName.startsWith("XXXX", ignoreCase = true)
                        }
                    }
                    val categoryKey = "artist_${artist.name}_$category"
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
                        featuredSongs = artist.featuredSongs,
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
                        onNavigateToArtist = { artistName ->
                            val artists = MusicRepository.splitArtists(artistName)
                            if (artists.size > 1) {
                                viewModel?.showArtistSelection(artists)
                            } else {
                                viewModel?.setSelectedArtistName(artistName)
                            }
                        },
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
                albumArtMap = albumArtMap,
                onBack = { viewModel?.popSelection() },
                onPlaySongs = onPlaySongs,
                onSongDetails = onSongDetails,
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = onAddToPlaylist,
                onNavigateToArtist = { artistName ->
                    val artists = MusicRepository.splitArtists(artistName)
                    if (artists.size > 1) {
                        viewModel?.showArtistSelection(artists)
                    } else {
                        viewModel?.setSelectedArtistName(artistName)
                    }
                },
                viewModel = viewModel
            )
            is String -> GenreDetailView(
                genre = detailItem,
                songs = selectedGenreSongs,
                currentPlayingId = currentPlayingId,
                albumArtMap = albumArtMap,
                onBack = { viewModel?.popSelection() },
                onSongClick = { song -> onPlaySongs(selectedGenreSongs, selectedGenreSongs.indexOf(song), null) },
                onSongDetailsClick = onSongDetails,
                onPlaySongs = onPlaySongs,
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = onAddToPlaylist,
                onNavigateToArtist = { artistName ->
                    val artists = MusicRepository.splitArtists(artistName)
                    if (artists.size > 1) {
                        viewModel?.showArtistSelection(artists)
                    } else {
                        viewModel?.setSelectedArtistName(artistName)
                    }
                },
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
                    when (filter) {
                        LibraryFilter.ALBUMS -> Box(modifier = Modifier.fillMaxSize()) {
                            val categoryKey = "library_albums"
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
                                    val isPlaying = album.id == currentPlayingSong?.albumId
                                    val albumArt = albumArtMap[album.id] ?: album.artworkUri
                                    val displayAlbum = remember(album, albumArt) {
                                        if (albumArt != album.artworkUri) album.copy(artworkUri = albumArt) else album
                                    }
                                    
                                    val onAlbumClick: () -> Unit = remember(album.id) { { viewModel?.setSelectedAlbumId(album.id) } }
                                    val onAlbumPlay: () -> Unit = remember(album.id, album.songs) { { onPlaySongs(album.songs, 0, null) } }
                                    val onArtistNav: (String) -> Unit = remember { 
                                        { artistName: String ->
                                            val artists = MusicRepository.splitArtists(artistName)
                                            if (artists.size > 1) {
                                                viewModel?.showArtistSelection(artists)
                                            } else {
                                                viewModel?.setSelectedArtistName(artistName)
                                            }
                                        }
                                    }

                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        AlbumCard(
                                            album = displayAlbum, 
                                            onClick = onAlbumClick,
                                            onPlayClick = onAlbumPlay,
                                            columns = effectiveColumns,
                                            isPlaying = isPlaying,
                                            onNavigateToArtist = onArtistNav
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = displayAlbum.songs.first(),
                                            isPlaying = isPlaying,
                                            onClick = onAlbumClick,
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = displayAlbum.title,
                                            secondaryLabel = displayAlbum.artist,
                                            artworkUri = displayAlbum.artworkUri,
                                            onPlayClick = onAlbumPlay,
                                            onNavigateToArtist = onArtistNav
                                        )
                                    } else {
                                        SongItem(
                                            song = displayAlbum.songs.first(),
                                            isPlaying = isPlaying,
                                            onClick = onAlbumClick,
                                            onDetailsClick = {},
                                            onSwipePlayNext = {},
                                            onSwipeAddToPlaylist = {},
                                            label = displayAlbum.title,
                                            secondaryLabel = displayAlbum.artist,
                                            artworkUri = displayAlbum.artworkUri,
                                            onNavigateToArtist = onArtistNav
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
                                    val isPlaying = remember(currentPlayingSong, artist.name) {
                                        currentPlayingSong?.let { 
                                            MusicRepository.splitArtists(it.artist).contains(artist.name)
                                        } ?: false
                                    }
                                    val onArtistClick: () -> Unit = remember(artist.name) { { viewModel?.setSelectedArtistName(artist.name) } }
                                    
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        ArtistCard(
                                            artist = artist, 
                                            onClick = onArtistClick,
                                            columns = effectiveColumns,
                                            isPlaying = isPlaying
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = artist.songs.first(),
                                            isPlaying = isPlaying,
                                            onClick = onArtistClick,
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
                                            isPlaying = isPlaying,
                                            onClick = onArtistClick,
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
                                    val isPlaying = remember(playlist.songs, currentPlayingId) {
                                        playlist.songs.contains(currentPlayingId)
                                    }
                                    val onPlaylistClick: () -> Unit = remember(playlist.id) { { viewModel?.setSelectedPlaylistId(playlist.id) } }
                                    
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        AlbumCard(
                                            album = Album(
                                                id = 0,
                                                title = playlist.name,
                                                artist = "${playlist.songs.size} Songs",
                                                artworkUri = null,
                                                songs = emptyList()
                                            ),
                                            onClick = onPlaylistClick,
                                            isPlaying = isPlaying
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = isPlaying,
                                            onClick = onPlaylistClick,
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
                                            isPlaying = isPlaying,
                                            onClick = onPlaylistClick,
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
                            val categoryKey = "library_genres"
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
                                    val isPlaying = currentPlayingSong?.genre == genre
                                    val count = remember(filteredSongs, genre) { filteredSongs.count { it.genre == genre } }
                                    val onGenreClick: () -> Unit = remember(genre) { { viewModel?.setSelectedGenreName(genre) } }
                                    
                                    if (settings.viewMode == CategoryViewMode.GRID) {
                                        GenreCard(
                                            genre = genre,
                                            songCount = count,
                                            onClick = onGenreClick,
                                            isPlaying = isPlaying
                                        )
                                    } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                        CompactSongItem(
                                            song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                            isPlaying = isPlaying,
                                            onClick = onGenreClick,
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
                                            isPlaying = isPlaying,
                                            onClick = onGenreClick,
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
                                    gridItemsIndexed(
                                        items = filteredSongs,
                                        key = { _: Int, song: Song -> song.id },
                                        contentType = { _: Int, _: Song -> "song_album_card" }
                                    ) { index, song ->
                                        val isPlaying = song.id == currentPlayingId
                                        val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                                        val onSongClick: () -> Unit = remember(song.id, index) { { onPlaySongs(filteredSongs, index, null) } }
                                        val onArtistNav: (String) -> Unit = remember { 
                                            { artistName: String ->
                                                val artists = MusicRepository.splitArtists(artistName)
                                                if (artists.size > 1) {
                                                    viewModel?.showArtistSelection(artists)
                                                } else {
                                                    viewModel?.setSelectedArtistName(artistName)
                                                }
                                            }
                                        }
                                        AlbumCard(
                                            title = song.title,
                                            artist = song.artist,
                                            artworkUri = albumArt,
                                            onClick = onSongClick,
                                            columns = settings.columns,
                                            isPlaying = isPlaying,
                                            songCount = 1,
                                            allCovers = emptyList(),
                                            hasFolderCover = true,
                                            onNavigateToArtist = onArtistNav
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
                                    lazyItemsIndexed(
                                        items = filteredSongs, 
                                        key = { _: Int, song: Song -> song.id },
                                        contentType = { _: Int, _: Song -> "song_item" }
                                    ) { index, song ->
                                        val isPlaying = song.id == currentPlayingId
                                        val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                                        val onSongClick: () -> Unit = remember(song.id, index) { { onPlaySongs(filteredSongs, index, null) } }
                                        val onSongDetailsInternal: () -> Unit = remember(song.id) { { onSongDetails(song) } }
                                        val onArtistNav: (String) -> Unit = remember { 
                                            { artistName: String ->
                                                val artists = MusicRepository.splitArtists(artistName)
                                                if (artists.size > 1) {
                                                    viewModel?.showArtistSelection(artists)
                                                } else {
                                                    viewModel?.setSelectedArtistName(artistName)
                                                }
                                            }
                                        }

                                        if (settings.viewMode == CategoryViewMode.COMPACT) {
                                            CompactSongItem(
                                                song = song,
                                                isPlaying = isPlaying,
                                                onClick = onSongClick,
                                                onDetailsClick = onSongDetailsInternal,
                                                onSwipePlayNext = {
                                                    viewModel?.addPlayNext(song)
                                                    onShowMessage("Added to Play Next")
                                                },
                                                onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                                                artworkUri = albumArt,
                                                onNavigateToArtist = onArtistNav
                                            )
                                        } else {
                                            SongItem(
                                                song = song,
                                                isPlaying = isPlaying,
                                                onClick = onSongClick,
                                                onDetailsClick = onSongDetailsInternal,
                                                onSwipePlayNext = {
                                                    viewModel?.addPlayNext(song)
                                                    onShowMessage("Added to Play Next")
                                                },
                                                onSwipeAddToPlaylist = { onAddToPlaylist(song) },
                                                artworkUri = albumArt,
                                                onNavigateToArtist = onArtistNav
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
                            Text(settings.columns.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp)
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
