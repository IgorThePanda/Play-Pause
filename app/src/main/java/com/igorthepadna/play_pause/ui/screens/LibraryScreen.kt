package com.igorthepadna.play_pause.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.igorthepadna.play_pause.ui.screens.StatsScreen
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
import com.igorthepadna.play_pause.data.GridSizeMode
import com.igorthepadna.play_pause.ViewModeSettings
import com.igorthepadna.play_pause.ui.components.GenreCard
import com.igorthepadna.play_pause.ui.components.CategoryViewMode
import com.igorthepadna.play_pause.ui.components.UniversalSongItem
import com.igorthepadna.play_pause.utils.calculateGridColumns
import com.igorthepadna.play_pause.utils.verticalScrollbar
import com.igorthepadna.play_pause.utils.ScrollbarLabel

@Composable
private fun PermissionBox(
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
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
}

@Composable
private fun EmptyStateBox(searchQuery: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(if (searchQuery.isEmpty()) "No items found." else "No matches found.")
    }
}

@Composable
private fun LibraryTopBarActions(
    currentFilter: LibraryFilter,
    settings: ViewModeSettings,
    onUpdateViewModeSettings: (ViewModeSettings) -> Unit
) {
    val isUnifiedList = currentFilter == LibraryFilter.SONGS || 
                        currentFilter == LibraryFilter.GENRES

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 20.dp, end = 24.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isUnifiedList) {
            if (settings.viewMode == CategoryViewMode.GRID) {
                FilledIconButton(
                    onClick = {
                        val nextMode = when (settings.gridSizeMode) {
                            GridSizeMode.AUTO -> GridSizeMode.SMALL
                            GridSizeMode.SMALL -> GridSizeMode.MEDIUM
                            GridSizeMode.MEDIUM -> GridSizeMode.LARGE
                            GridSizeMode.LARGE -> GridSizeMode.AUTO
                        }
                        onUpdateViewModeSettings(settings.copy(gridSizeMode = nextMode))
                    },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                    )
                ) {
                    Text(settings.gridSizeMode.toString(), fontWeight = FontWeight.Black, fontSize = 14.sp)
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
                    onUpdateViewModeSettings(settings.copy(viewMode = newMode))
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
    }
}

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
    val pinnedItems by viewModel?.pinnedItems?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> onPermissionChanged(permissions.values.all { it }) }

    BackHandler(enabled = selectedDetail != null) {
        viewModel?.popSelection()
    }

    DetailViewSwitcher(
        currentFilter = currentFilter,
        selectedDetail = selectedDetail,
        currentPlayingId = currentPlayingId,
        currentPlayingSong = currentPlayingSong,
        albumArtMap = albumArtMap,
        viewModeSettings = viewModeSettings,
        selectedPlaylistSongs = selectedPlaylistSongs,
        selectedGenreSongs = selectedGenreSongs,
        genres = genres,
        filteredSongs = filteredSongs,
        sortedAlbums = sortedAlbums,
        sortedArtists = sortedArtists,
        playlists = playlists,
        hasPermission = hasPermission,
        isRefreshing = isRefreshing,
        searchQuery = searchQuery,
        listState = listState,
        gridState = gridState,
        tabSortSettings = tabSortSettings,
        pinnedItems = pinnedItems,
        onPlaySongs = onPlaySongs,
        onSongDetails = onSongDetails,
        onAddToPlaylist = onAddToPlaylist,
        onShowMessage = onShowMessage,
        viewModel = viewModel,
        permissionLauncher = permissionLauncher
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailViewSwitcher(
    currentFilter: LibraryFilter,
    selectedDetail: Any?,
    currentPlayingId: Long,
    currentPlayingSong: Song?,
    albumArtMap: Map<Long, android.net.Uri?>,
    viewModeSettings: Map<String, ViewModeSettings>,
    selectedPlaylistSongs: List<Song>,
    selectedGenreSongs: List<Song>,
    genres: List<String>,
    filteredSongs: List<Song>,
    sortedAlbums: List<Album>,
    sortedArtists: List<Artist>,
    playlists: List<Playlist>,
    hasPermission: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    tabSortSettings: TabSortSettings,
    pinnedItems: List<com.igorthepadna.play_pause.data.PinnedItem> = emptyList(),
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: MainViewModel?,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
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
            is Album -> {
                val isPinned = remember(pinnedItems, detailItem) {
                    pinnedItems.any { it.type == com.igorthepadna.play_pause.data.PinnedType.ALBUM && it.mediaId == detailItem.id.toString() }
                }
                AlbumDetailView(
                    album = detailItem,
                    currentPlayingId = currentPlayingId,
                    isPinned = isPinned,
                    onPinClick = { viewModel?.togglePin(com.igorthepadna.play_pause.data.PinnedType.ALBUM, detailItem.id.toString()) },
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
            }
            is Artist -> {
                val isPinned = remember(pinnedItems, detailItem) {
                    pinnedItems.any { it.type == com.igorthepadna.play_pause.data.PinnedType.ARTIST && it.mediaId == detailItem.name }
                }
                ArtistDetailView(
                    artist = detailItem,
                    currentPlayingId = currentPlayingId,
                    currentPlayingSong = currentPlayingSong,
                    albumArtMap = albumArtMap,
                    isPinned = isPinned,
                    onPinClick = { viewModel?.togglePin(com.igorthepadna.play_pause.data.PinnedType.ARTIST, detailItem.name) },
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
            }
            is Pair<*, *> -> {
                val artist = (detailItem.first as? Artist)
                val category = (detailItem.second as? String)
                if (artist != null && category != null) {
                    val (ownAlbums, featuredAlbums) = remember(artist.albums, artist.name) {
                        artist.albums.partition { it.artist == artist.name }
                    }
                    val (mainAlbums, singles) = remember(ownAlbums) {
                        ownAlbums.partition { it.songs.size > 2 }
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
                    val effectiveColumns = calculateGridColumns(settings.gridSizeMode)

                    CategoryDetailView(
                        title = category,
                        artist = artist,
                        mainAlbums = mainAlbums,
                        singles = singles,
                        unreleasedSongs = unreleasedSongs,
                        featuredSongs = artist.featuredSongs,
                        appearsOnAlbums = featuredAlbums,
                        viewMode = settings.viewMode,
                        gridSizeMode = settings.gridSizeMode,
                        onViewModeChange = { newMode ->
                            viewModel?.updateViewModeSettings(categoryKey, settings.copy(viewMode = newMode))
                        },
                        onGridSizeModeChange = { nextMode ->
                            viewModel?.updateViewModeSettings(categoryKey, settings.copy(gridSizeMode = nextMode))
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
                        albumArtMap = albumArtMap,
                        currentPlayingId = currentPlayingId,
                        currentPlayingSong = currentPlayingSong
                    )
                }
            }
            "STATS" -> {
                if (viewModel != null) {
                    StatsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.popSelection() }
                    )
                }
            }
            is Playlist -> {
                val isPinned = remember(pinnedItems, detailItem) {
                    pinnedItems.any { it.type == com.igorthepadna.play_pause.data.PinnedType.PLAYLIST && it.mediaId == detailItem.id }
                }
                PlaylistDetailView(
                    playlist = detailItem,
                    playlistSongs = selectedPlaylistSongs,
                    currentPlayingId = currentPlayingId,
                    albumArtMap = albumArtMap,
                    isPinned = isPinned,
                    onPinClick = { viewModel?.togglePin(com.igorthepadna.play_pause.data.PinnedType.PLAYLIST, detailItem.id) },
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
                onEditCover = { viewModel?.setCoverEditingPlaylistId(detailItem.id) },
                onAddSongs = { viewModel?.setShowSongSelectionForPlaylist(detailItem.id) },
                onInfoClick = { viewModel?.setSelectedPlaylistInfoId(detailItem.id) },
                viewModel = viewModel
                )
            }
            is com.igorthepadna.play_pause.MainViewModel.Selection.PlaylistInfo -> {
                val playlist = remember(playlists) { playlists.find { it.id == detailItem.id } }
                if (playlist != null) {
                    com.igorthepadna.play_pause.ui.components.playlists.PlaylistInfoView(
                        playlist = playlist,
                        playlistSongs = selectedPlaylistSongs,
                        albumArtMap = albumArtMap,
                        onBack = { viewModel?.popSelection() },
                        onEditCover = { viewModel?.setCoverEditingPlaylistId(playlist.id) },
                        onUpdateName = { viewModel?.updatePlaylistName(playlist.id, it) },
                        onDeletePlaylist = { viewModel?.deletePlaylist(playlist.id) },
                        onRemoveSong = { viewModel?.removeFromPlaylist(playlist.id, it) },
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
                }
            }
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
            else -> MainLibraryContent(
                filter = filter,
                hasPermission = hasPermission,
                isRefreshing = isRefreshing,
                searchQuery = searchQuery,
                filteredSongs = filteredSongs,
                sortedAlbums = sortedAlbums,
                sortedArtists = sortedArtists,
                playlists = playlists,
                genres = genres,
                currentPlayingSong = currentPlayingSong,
                currentPlayingId = currentPlayingId,
                albumArtMap = albumArtMap,
                viewModeSettings = viewModeSettings,
                gridState = gridState,
                listState = listState,
                tabSortSettings = tabSortSettings,
                onPlaySongs = onPlaySongs,
                onSongDetails = onSongDetails,
                onAddToPlaylist = onAddToPlaylist,
                onShowMessage = onShowMessage,
                viewModel = viewModel,
                permissionLauncher = permissionLauncher
            )
        }
    }
}

@Composable
private fun MainLibraryContent(
    filter: LibraryFilter,
    hasPermission: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    filteredSongs: List<Song>,
    sortedAlbums: List<Album>,
    sortedArtists: List<Artist>,
    playlists: List<Playlist>,
    genres: List<String>,
    currentPlayingSong: Song?,
    currentPlayingId: Long,
    albumArtMap: Map<Long, android.net.Uri?>,
    viewModeSettings: Map<String, ViewModeSettings>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    tabSortSettings: TabSortSettings,
    onPlaySongs: (List<Song>, Int, Boolean?) -> Unit,
    onSongDetails: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: MainViewModel?,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listTopPadding = 84.dp + statusBarPadding
        val bottomPadding = if (currentPlayingSong != null) 176.dp + navigationBarPadding else 112.dp + navigationBarPadding
        val contentPadding = PaddingValues(top = listTopPadding, bottom = bottomPadding, start = 16.dp, end = 16.dp)
        val scrollbarPadding = PaddingValues(bottom = bottomPadding)

        if (filteredSongs.isEmpty() && hasPermission && !isRefreshing) {
            EmptyStateBox(searchQuery)
        } else if (!hasPermission) {
            PermissionBox(permissionLauncher)
        } else {
            when (filter) {
                LibraryFilter.ALBUMS -> Box(modifier = Modifier.fillMaxSize()) {
                    val categoryKey = "library_albums"
                    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.GRID)
                    val effectiveColumns = calculateGridColumns(if (settings.viewMode == CategoryViewMode.GRID) settings.gridSizeMode else GridSizeMode.LARGE)

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
                                UniversalSongItem(
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
                            } else {
                                UniversalSongItem(
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
                    val effectiveColumns = calculateGridColumns(if (settings.viewMode == CategoryViewMode.GRID) settings.gridSizeMode else GridSizeMode.LARGE)

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
                                val representativeSong = (artist.songs + artist.featuredSongs).firstOrNull()
                                if (representativeSong != null) {
                                    UniversalSongItem(
                                        song = representativeSong,
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
                            } else {
                                val representativeSong = (artist.songs + artist.featuredSongs).firstOrNull()
                                if (representativeSong != null) {
                                    UniversalSongItem(
                                        song = representativeSong,
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
                    val effectiveColumns = calculateGridColumns(if (settings.viewMode == CategoryViewMode.GRID) settings.gridSizeMode else GridSizeMode.LARGE)

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
                                com.igorthepadna.play_pause.ui.components.playlists.PlaylistCard(
                                    playlist = playlist,
                                    onClick = onPlaylistClick,
                                    albumArtMap = albumArtMap
                                )
                            } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                UniversalSongItem(
                                    song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                    isPlaying = isPlaying,
                                    onClick = onPlaylistClick,
                                    onDetailsClick = {},
                                    onSwipePlayNext = {},
                                    onSwipeAddToPlaylist = {},
                                    label = playlist.name,
                                    secondaryLabel = "${playlist.songs.size} Songs",
                                    artworkUri = null,
                                    leadingContent = {
                                        com.igorthepadna.play_pause.ui.components.playlists.PlaylistCoverImage(
                                            coverUri = playlist.coverUri,
                                            songCovers = playlist.songs.map { albumArtMap[it] },
                                            iconSize = 24.dp
                                        )
                                    }
                                )
                            } else {
                                UniversalSongItem(
                                    song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                    isPlaying = isPlaying,
                                    onClick = onPlaylistClick,
                                    onDetailsClick = {},
                                    onSwipePlayNext = {},
                                    onSwipeAddToPlaylist = {},
                                    label = playlist.name,
                                    secondaryLabel = "${playlist.songs.size} Songs",
                                    artworkUri = null,
                                    leadingContent = {
                                        com.igorthepadna.play_pause.ui.components.playlists.PlaylistCoverImage(
                                            coverUri = playlist.coverUri,
                                            songCovers = playlist.songs.map { albumArtMap[it] },
                                            iconSize = 32.dp
                                        )
                                    }
                                )
                            }
                        }
                        item {
                            val onCreatePlaylist: () -> Unit = remember { { viewModel?.setShowCreatePlaylistDialog(true) } }
                            if (settings.viewMode == CategoryViewMode.GRID) {
                                com.igorthepadna.play_pause.ui.components.playlists.CreatePlaylistCard(onClick = onCreatePlaylist)
                            } else if (settings.viewMode == CategoryViewMode.COMPACT) {
                                UniversalSongItem(
                                    song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                    isPlaying = false,
                                    onClick = onCreatePlaylist,
                                    onDetailsClick = {},
                                    onSwipePlayNext = {},
                                    onSwipeAddToPlaylist = {},
                                    label = "Create playlist",
                                    secondaryLabel = "Tap to start",
                                    artworkUri = null,
                                    leadingIcon = Icons.Rounded.Add
                                )
                            } else {
                                UniversalSongItem(
                                    song = Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                    isPlaying = false,
                                    onClick = onCreatePlaylist,
                                    onDetailsClick = {},
                                    onSwipePlayNext = {},
                                    onSwipeAddToPlaylist = {},
                                    label = "Create playlist",
                                    secondaryLabel = "Tap to start",
                                    artworkUri = null,
                                    leadingIcon = Icons.Rounded.Add
                                )
                            }
                        }
                    }
                }
                LibraryFilter.GENRES -> Box(modifier = Modifier.fillMaxSize()) {
                    val categoryKey = "library_genres"
                    val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(viewMode = CategoryViewMode.GRID)
                    val effectiveColumns = calculateGridColumns(if (settings.viewMode == CategoryViewMode.GRID) settings.gridSizeMode else GridSizeMode.LARGE)

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
                                UniversalSongItem(
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
                                UniversalSongItem(
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
                    val effectiveColumns = calculateGridColumns(settings.gridSizeMode)
                    
                    if (settings.viewMode == CategoryViewMode.GRID) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(effectiveColumns),
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
                                    columns = effectiveColumns,
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
                                    UniversalSongItem(
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
                                    UniversalSongItem(
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
        val categoryKey = when(filter) {
            LibraryFilter.ALBUMS -> "library_albums"
            LibraryFilter.ARTISTS -> "library_artists"
            LibraryFilter.GENRES -> "library_genres"
            LibraryFilter.PLAYLISTS -> "library_playlists"
            else -> "library_songs"
        }
        val settings = viewModeSettings[categoryKey] ?: ViewModeSettings(
            viewMode = if (filter == LibraryFilter.ALBUMS || filter == LibraryFilter.ARTISTS || filter == LibraryFilter.GENRES) CategoryViewMode.GRID else CategoryViewMode.DETAILED
        )

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            LibraryTopBarActions(
                currentFilter = filter,
                settings = settings,
                onUpdateViewModeSettings = { newSettings ->
                    viewModel?.updateViewModeSettings(categoryKey, newSettings)
                }
            )
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
