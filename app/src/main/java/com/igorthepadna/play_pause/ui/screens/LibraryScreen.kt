package com.igorthepadna.play_pause.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
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
import com.igorthepadna.play_pause.ui.components.PlaylistSelectionSheet
import com.igorthepadna.play_pause.ui.components.SongDetailsContent
import com.igorthepadna.play_pause.ui.components.ArtistCard
import com.igorthepadna.play_pause.ui.components.ArtistDetailView
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
    onPlaySongs: (List<Song>, Int) -> Unit,
    viewModel: MainViewModel? = null,
    onShowMessage: (String) -> Unit = {},
    tabSortSettings: TabSortSettings = TabSortSettings()
) {
    val searchQuery by viewModel?.searchQuery?.collectAsStateWithLifecycle() ?: remember { mutableStateOf("") }
    val filteredSongs by viewModel?.filteredSongs?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val sortedAlbums by viewModel?.sortedAlbums?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val sortedArtists by viewModel?.sortedArtists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val playlists by viewModel?.playlists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val currentPlayingId by viewModel?.currentPlayingId?.collectAsStateWithLifecycle(-1L) ?: remember { mutableLongStateOf(-1L) }
    
    val savedAlbumId by viewModel?.selectedAlbumId?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val savedArtistName by viewModel?.selectedArtistName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    var selectedSongForDetails by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    val selectedAlbum = remember(savedAlbumId, sortedAlbums) { sortedAlbums.find { it.id == savedAlbumId } }
    val selectedArtist = remember(savedArtistName, sortedArtists) { sortedArtists.find { it.name == savedArtistName } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> onPermissionChanged(permissions.values.all { it }) }

    BackHandler(enabled = selectedAlbum != null || selectedArtist != null) {
        if (selectedAlbum != null) viewModel?.setSelectedAlbumId(null)
        else if (selectedArtist != null) viewModel?.setSelectedArtistName(null)
    }

    AnimatedContent(
        targetState = selectedAlbum ?: selectedArtist,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "library_transition"
    ) { detailItem ->
        when (detailItem) {
            is Album -> AlbumDetailView(
                album = detailItem,
                player = player,
                onBack = { viewModel?.setSelectedAlbumId(null) },
                onSongDetails = { song ->
                    selectedSongForDetails = song
                    showDetailsSheet = true
                },
                onPlaySongs = onPlaySongs,
                onSwipePlayNext = { song ->
                    viewModel?.addPlayNext(song)
                    onShowMessage("Added to Play Next: ${song.title}")
                },
                onSwipeAddToPlaylist = { song ->
                    selectedSongForPlaylist = song
                    showPlaylistSheet = true
                }
            )
            is Artist -> ArtistDetailView(
                artist = detailItem,
                onBack = { viewModel?.setSelectedArtistName(null) },
                onAlbumClick = { viewModel?.setSelectedAlbumId(it.id) },
                onPlayArtist = { onPlaySongs(detailItem.songs, 0) }
            )
            else -> Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(84.dp).statusBarsPadding())

                    // Scanning / Refreshing Indicator
                    AnimatedVisibility(
                        visible = isRefreshing,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Updating Library",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Scanning for new music...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
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
                            val bottomPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp)
                            val scrollbarPadding = PaddingValues(bottom = 140.dp)
                            
                            when (currentFilter) {
                                LibraryFilter.ALBUMS -> Box(modifier = Modifier.fillMaxSize()) {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Fixed(2),
                                        contentPadding = bottomPadding,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp),
                                        modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                                    ) {
                                        items(sortedAlbums, key = { it.id }) { album ->
                                            AlbumCard(album = album, onClick = { viewModel?.setSelectedAlbumId(album.id) })
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
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Fixed(2),
                                        contentPadding = bottomPadding,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(24.dp),
                                        modifier = Modifier.fillMaxSize().verticalScrollbar(gridState, padding = scrollbarPadding)
                                    ) {
                                        items(sortedArtists, key = { it.name }) { artist ->
                                            ArtistCard(artist = artist, onClick = { viewModel?.setSelectedArtistName(artist.name) })
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
                                else -> Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = listState,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = bottomPadding,
                                        modifier = Modifier.fillMaxSize().verticalScrollbar(listState, padding = scrollbarPadding)
                                    ) {
                                        itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                                            SongItem(
                                                song = song,
                                                isPlaying = song.id == currentPlayingId,
                                                onClick = { onPlaySongs(filteredSongs, index) },
                                                onDetailsClick = {
                                                    selectedSongForDetails = song
                                                    showDetailsSheet = true
                                                },
                                                onSwipePlayNext = {
                                                    viewModel?.addPlayNext(song)
                                                    onShowMessage("Added to Play Next")
                                                },
                                                onSwipeAddToPlaylist = {
                                                    selectedSongForPlaylist = song
                                                    showPlaylistSheet = true
                                                }
                                            )
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
                    }
                }
            }
        }
    }

    if (showDetailsSheet && selectedSongForDetails != null) {
        ModalBottomSheet(onDismissRequest = { showDetailsSheet = false }, sheetState = sheetState) {
            SongDetailsContent(song = selectedSongForDetails!!)
        }
    }

    if (showPlaylistSheet && selectedSongForPlaylist != null) {
        PlaylistSelectionSheet(
            song = selectedSongForPlaylist!!,
            playlists = playlists,
            onDismiss = { showPlaylistSheet = false },
            onPlaylistSelected = { id ->
                viewModel?.addToPlaylist(id, selectedSongForPlaylist!!.id)
                showPlaylistSheet = false
            },
            onCreatePlaylist = { name ->
                viewModel?.createPlaylist(name)
                showPlaylistSheet = false
            },
            onFavoriteClick = { viewModel?.toggleFavorite(selectedSongForPlaylist!!.id) }
        )
    }
}
