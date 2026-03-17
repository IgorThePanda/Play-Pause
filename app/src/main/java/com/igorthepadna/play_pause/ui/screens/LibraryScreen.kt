package com.igorthepadna.play_pause.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.data.SortOrder
import com.igorthepadna.play_pause.data.SortType
import com.igorthepadna.play_pause.ui.components.AlbumCard
import com.igorthepadna.play_pause.ui.components.SongItem
import com.igorthepadna.play_pause.ui.components.AlbumDetailView
import com.igorthepadna.play_pause.ui.components.SongDetailsContent
import com.igorthepadna.play_pause.ui.components.PlaylistSelectionSheet
import com.igorthepadna.play_pause.utils.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    player: Player?,
    currentFilter: LibraryFilter,
    songs: List<Song>,
    hasPermission: Boolean,
    isRefreshing: Boolean,
    onPermissionChanged: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    viewModel: MainViewModel? = null,
    onShowMessage: (String) -> Unit = {},
    sortType: SortType = SortType.TITLE,
    sortOrder: SortOrder = SortOrder.ASC
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSongForDetails by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    val playlists by viewModel?.playlists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    // Scroll-to-hide logic for top bar
    var isTopBarVisible by remember { mutableStateOf(true) }
    val lastScrollOffset = remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemScrollOffset, gridState.firstVisibleItemScrollOffset) {
        val currentOffset = if (currentFilter == LibraryFilter.ALBUMS) gridState.firstVisibleItemScrollOffset else listState.firstVisibleItemScrollOffset
        val index = if (currentFilter == LibraryFilter.ALBUMS) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        
        if (index > 0 || currentOffset > 100) {
            if (currentOffset > lastScrollOffset.intValue + 5) {
                isTopBarVisible = false
            } else if (currentOffset < lastScrollOffset.intValue - 5) {
                isTopBarVisible = true
            }
        } else {
            isTopBarVisible = true
        }
        lastScrollOffset.intValue = currentOffset
    }

    LaunchedEffect(currentFilter) {
        selectedAlbum = null
        isTopBarVisible = true
    }

    if (selectedAlbum != null) {
        BackHandler {
            selectedAlbum = null
        }
    }

    if (showDetailsSheet && selectedSongForDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = sheetState
        ) {
            SongDetailsContent(selectedSongForDetails!!)
        }
    }

    if (showPlaylistSheet && selectedSongForPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PlaylistSelectionSheet(
                song = selectedSongForPlaylist!!,
                playlists = playlists,
                onPlaylistSelected = { playlistId ->
                    viewModel?.addToPlaylist(playlistId, selectedSongForPlaylist!!.id)
                    onShowMessage("Added to playlist")
                    showPlaylistSheet = false
                },
                onCreatePlaylist = { name ->
                    viewModel?.createPlaylist(name)
                    onShowMessage("Playlist created: $name")
                },
                onFavoriteClick = {
                    viewModel?.toggleFavorite(selectedSongForPlaylist!!.id)
                    onShowMessage("Favorites updated")
                    showPlaylistSheet = false
                },
                onDismiss = { showPlaylistSheet = false }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        onPermissionChanged(permissions.values.all { it })
    }

    val filteredSongs = remember(songs, searchQuery, currentFilter, sortType, sortOrder) {
        var base = if (searchQuery.isEmpty()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }

        base = when (sortType) {
            SortType.TITLE -> if (sortOrder == SortOrder.ASC) base.sortedBy { it.title } else base.sortedByDescending { it.title }
            SortType.ARTIST -> if (sortOrder == SortOrder.ASC) base.sortedBy { it.artist } else base.sortedByDescending { it.artist }
            SortType.DURATION -> if (sortOrder == SortOrder.ASC) base.sortedBy { it.duration } else base.sortedByDescending { it.duration }
            SortType.RELEASE_DATE -> if (sortOrder == SortOrder.ASC) base.sortedBy { it.year } else base.sortedByDescending { it.year }
            SortType.DATE_ADDED -> if (sortOrder == SortOrder.ASC) base.sortedBy { it.dateAdded } else base.sortedByDescending { it.dateAdded }
        }

        base
    }

    var currentPlayingId by remember { mutableLongStateOf(player?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L) }
    
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {}
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentPlayingId = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    AnimatedContent(
        targetState = selectedAlbum,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "album_transition"
    ) { album ->
        if (album != null) {
            AlbumDetailView(
                album = album,
                player = player,
                onBack = { selectedAlbum = null },
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
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Space for top bar
                    AnimatedVisibility(
                        visible = isTopBarVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Spacer(modifier = Modifier.height(84.dp).statusBarsPadding())
                    }
                    if (!isTopBarVisible) {
                        Spacer(modifier = Modifier.height(16.dp).statusBarsPadding())
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        if (filteredSongs.isEmpty() && hasPermission && !isRefreshing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items found.")
                            }
                        } else if (!hasPermission) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Button(onClick = {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    permissionLauncher.launch(permissions)
                                }) {
                                    Text("Grant Permissions")
                                }
                            }
                        } else {
                            val bottomPadding = PaddingValues(bottom = 140.dp)
                            if (currentFilter == LibraryFilter.ALBUMS) {
                                val albums = remember(filteredSongs, sortType, sortOrder) {
                                    val grouped = filteredSongs.groupBy { it.albumId }.map { (albumId, songs) ->
                                        Album(
                                            id = albumId,
                                            title = songs.first().album,
                                            artist = songs.first().artist,
                                            artworkUri = songs.first().albumArtUri,
                                            songs = songs.sortedBy { it.trackNumber },
                                            year = songs.first().year
                                        )
                                    }
                                    
                                    when (sortType) {
                                        SortType.TITLE -> if (sortOrder == SortOrder.ASC) grouped.sortedBy { it.title } else grouped.sortedByDescending { it.title }
                                        SortType.ARTIST -> if (sortOrder == SortOrder.ASC) grouped.sortedBy { it.artist } else grouped.sortedByDescending { it.artist }
                                        SortType.RELEASE_DATE -> if (sortOrder == SortOrder.ASC) grouped.sortedBy { it.year } else grouped.sortedByDescending { it.year }
                                        else -> grouped.sortedBy { it.title }
                                    }
                                }

                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(2),
                                    contentPadding = bottomPadding,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScrollbar(gridState, padding = bottomPadding)
                                ) {
                                    items(albums, key = { it.id }) { album ->
                                        AlbumCard(
                                            album = album,
                                            onClick = { selectedAlbum = album }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = bottomPadding,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScrollbar(listState, padding = bottomPadding)
                                ) {
                                    items(filteredSongs, key = { it.id }) { song ->
                                        SongItem(
                                            song = song,
                                            isPlaying = song.id == currentPlayingId,
                                            onClick = {
                                                onPlaySongs(filteredSongs, filteredSongs.indexOf(song))
                                            },
                                            onDetailsClick = {
                                                selectedSongForDetails = song
                                                showDetailsSheet = true
                                            },
                                            onSwipePlayNext = {
                                                viewModel?.addPlayNext(song)
                                                onShowMessage("Added to Play Next: ${song.title}")
                                            },
                                            onSwipeAddToPlaylist = {
                                                selectedSongForPlaylist = song
                                                showPlaylistSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- FLOATING TOP BAR PILL ---
                AnimatedVisibility(
                    visible = isTopBarVisible,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search ${currentFilter.label.lowercase()}...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                )
                            }
                            
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                }
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.size(40.dp)) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", modifier = Modifier.size(22.dp))
                                }
                            }

                            IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
