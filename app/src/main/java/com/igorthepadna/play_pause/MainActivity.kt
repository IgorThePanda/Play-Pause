package com.igorthepadna.play_pause

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.SortOrder
import com.igorthepadna.play_pause.data.SortType
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.ui.components.FullScreenLyrics
import com.igorthepadna.play_pause.ui.components.FullScreenPlayer
import com.igorthepadna.play_pause.ui.components.NowPlayingBar
import com.igorthepadna.play_pause.ui.components.PlaylistSelectionSheet
import com.igorthepadna.play_pause.ui.components.SongDetailsContent
import com.igorthepadna.play_pause.ui.screens.LibraryScreen
import com.igorthepadna.play_pause.ui.theme.PlayPauseTheme
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            val viewModel: MainViewModel = viewModel()
            
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val colorSchemeType by viewModel.colorSchemeType.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            val useDynamicColor = colorSchemeType == ColorSchemeType.DYNAMIC

            LaunchedEffect(controllerFuture) {
                controllerFuture?.addListener({
                    viewModel.setPlayer(controllerFuture?.get())
                }, MoreExecutors.directExecutor())
            }

            PlayPauseTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
                colorSchemeType = colorSchemeType
            ) {
                val player by viewModel.player.collectAsStateWithLifecycle()
                PlayPauseApp(viewModel, player, intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayPauseApp(viewModel: MainViewModel, player: Player?, intent: Intent) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()
    val isPlayerFullScreenState by viewModel.isPlayerFullScreen.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    var artistCategoryView by remember { mutableStateOf<String?>(null) }

    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    
    // Bottom Sheet States
    var selectedSongForDetails by remember { mutableStateOf<Song?>(null) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    val detailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playlistSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    // Colors for the current song (used in sheets)
    val detailsArtworkColors = rememberArtworkColors(
        artworkUri = selectedSongForDetails?.albumArtUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val playlistArtworkColors = rememberArtworkColors(
        artworkUri = selectedSongForPlaylist?.albumArtUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val isAnyOverlayVisible by remember {
        derivedStateOf {
            isSettingsVisible || showDetailsSheet || showPlaylistSheet || showSortSheet || isPlayerFullScreenState
        }
    }

    // Fix: BackHandler for settings and other UI states
    BackHandler(enabled = isAnyOverlayVisible) {
        if (isSettingsVisible) {
            isSettingsVisible = false
        } else if (showDetailsSheet) {
            showDetailsSheet = false
        } else if (showPlaylistSheet) {
            showPlaylistSheet = false
        } else if (showSortSheet) {
            showSortSheet = false
        } else if (isPlayerFullScreenState) {
            viewModel.setPlayerFullScreen(false)
        }
    }

    // Sort logic
    val tabSortSettingsMap by viewModel.tabSortSettings.collectAsStateWithLifecycle()
    val currentTabSettings = tabSortSettingsMap[currentFilter] ?: TabSortSettings()

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    val selectionStack by viewModel.selectionStack.collectAsStateWithLifecycle()

    val isSubMenuOpen by remember {
        derivedStateOf {
            selectionStack.isNotEmpty() || artistCategoryView != null
        }
    }
    
    var activeMediaItem by remember(player) { mutableStateOf(player?.currentMediaItem) }

    // --- PLAYER SHEET DRAG LOGIC ---
    val playerOffsetY = remember { Animatable(if (isPlayerFullScreenState) -screenHeightPx else 0f) }
    
    val transitionFraction by remember { derivedStateOf { (-playerOffsetY.value / screenHeightPx).coerceIn(0f, 1f) } }
    val barAlpha by remember { derivedStateOf { (1f - transitionFraction * 4f).coerceIn(0f, 1f) } }

    LaunchedEffect(isPlayerFullScreenState) {
        if (isPlayerFullScreenState) {
            playerOffsetY.animateTo(-screenHeightPx, spring(stiffness = Spring.StiffnessLow))
        } else {
            playerOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    LaunchedEffect(intent, player) {
        if (intent.getBooleanExtra("OPEN_PLAYER", false) && player != null) {
            viewModel.setPlayerFullScreen(true)
            intent.removeExtra("OPEN_PLAYER")
        }
    }

    var hasPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadSongs()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
    }
    
    // Silence warning for unused launcher
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            )
        }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                activeMediaItem = mediaItem
            }
            override fun onPlaybackStateChanged(state: Int) {
                activeMediaItem = player.currentMediaItem
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val isFullScreenLyricsVisible by viewModel.isFullScreenLyricsVisible.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSettingsVisible) {
            MainSettingsScreen(viewModel, onBack = { isSettingsVisible = false })
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { 
                    SnackbarHost(snackbarHostState) { data ->
                        val isNowPlayingVisible = activeMediaItem != null
                        val extraPadding = if (isNowPlayingVisible) 80.dp else 0.dp
                        
                        Snackbar(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = extraPadding)
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(28.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            actionContentColor = MaterialTheme.colorScheme.primary,
                            snackbarData = data
                        )
                    }
                },
                bottomBar = {
                    // Standard NavigationBar removed to re-add floating pill
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    LibraryScreen(
                        player = player, 
                        currentFilter = currentFilter,
                        hasPermission = hasPermission,
                        isRefreshing = isRefreshing,
                        onPermissionChanged = { hasPermission = it },
                        onPlaySongs = { songList, index, shuffle -> viewModel.playSongs(songList, index, shuffle ?: false) },
                        onSongDetails = { song ->
                            selectedSongForDetails = song
                            showDetailsSheet = true
                        },
                        onAddToPlaylist = { song ->
                            selectedSongForPlaylist = song
                            showPlaylistSheet = true
                        },
                        viewModel = viewModel,
                        tabSortSettings = currentTabSettings,
                        onShowMessage = { message ->
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onSettingsClick = { isSettingsVisible = true },
                        onSortClick = { showSortSheet = true }
                    )

                    NowPlayingBar(
                        player = player,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        currentFilter = currentFilter,
                        onFilterSelected = { viewModel.setCurrentFilter(it) },
                        showSearch = !isSubMenuOpen,
                        onSortClick = { showSortSheet = true },
                        onSettingsClick = { isSettingsVisible = true },
                        onClick = { viewModel.setPlayerFullScreen(true) },
                        onDrag = { delta ->
                            scope.launch {
                                playerOffsetY.snapTo((playerOffsetY.value + delta).coerceIn(-screenHeightPx, 0f))
                            }
                        },
                        onDragStopped = { velocity ->
                            val target = when {
                                velocity < -300f -> -screenHeightPx
                                velocity > 300f -> 0f
                                playerOffsetY.value < -screenHeightPx * 0.2f -> -screenHeightPx
                                else -> 0f
                            }
                            viewModel.setPlayerFullScreen(target != 0f)
                            scope.launch {
                                playerOffsetY.animateTo(target, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .graphicsLayer { alpha = barAlpha }
                    )
                }
            }
        }

        // Persistent "Updating Library" notification at the very top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = isRefreshing,
                enter = slideInVertically { -it * 2 } + fadeIn(tween(500)),
                exit = slideOutVertically { -it * 2 } + fadeOut(tween(500))
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Updating Library",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Full Screen Player
        if (activeMediaItem != null && player != null) {
            FullScreenPlayer(
                player = player,
                viewModel = viewModel,
                useArtworkAccent = useArtworkAccent,
                offsetY = playerOffsetY.value,
                onDismiss = { viewModel.setPlayerFullScreen(false) },
                onDrag = { delta ->
                    scope.launch {
                        playerOffsetY.snapTo((playerOffsetY.value + delta).coerceIn(-screenHeightPx, 0f))
                    }
                },
                onDragStopped = { velocity ->
                    val target = when {
                        velocity > 300f -> 0f
                        velocity < -300f -> -screenHeightPx
                        playerOffsetY.value > -screenHeightPx * 0.8f -> 0f
                        else -> -screenHeightPx
                    }
                    viewModel.setPlayerFullScreen(target != 0f)
                    scope.launch {
                        playerOffsetY.animateTo(target, spring(stiffness = Spring.StiffnessLow))
                    }
                },
                onMoreClick = { song ->
                    selectedSongForDetails = song
                    showDetailsSheet = true
                },
                onAddClick = { song ->
                    selectedSongForPlaylist = song
                    showPlaylistSheet = true
                },
                onNavigateToAlbum = { albumId ->
                    viewModel.clearSelections()
                    viewModel.setSelectedAlbumId(albumId)
                    viewModel.setPlayerFullScreen(false)
                },
                onNavigateToArtist = { artistName ->
                    viewModel.clearSelections()
                    viewModel.setSelectedArtistName(artistName)
                    viewModel.setPlayerFullScreen(false)
                }
            )
        }

        if (isFullScreenLyricsVisible && player != null) {
            FullScreenLyrics(
                player = player,
                viewModel = viewModel,
                onDismiss = { viewModel.setFullScreenLyricsVisible(false) }
            )
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            SortBottomSheetContent(
                currentFilter = currentFilter,
                settings = currentTabSettings,
                onUpdate = { newSettings ->
                    viewModel.updateSortSettings(currentFilter, newSettings)
                }
            )
        }
    }

    if (showDetailsSheet && selectedSongForDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            sheetState = detailsSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = detailsArtworkColors.secondary.copy(alpha = 0.04f).compositeOver(MaterialTheme.colorScheme.surface)
        ) {
            SongDetailsContent(
                song = selectedSongForDetails!!,
                artworkColors = detailsArtworkColors,
                onLyricClick = {
                    showDetailsSheet = false
                    viewModel.setFullScreenLyricsVisible(true, compactMode = true, songId = selectedSongForDetails?.id)
                },
                onFolderClick = { path ->
                    val folderPath = path.substringBeforeLast("/")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Folder Path", folderPath)
                    clipboard.setPrimaryClip(clip)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Path copied to clipboard")
                    }
                }
            )
        }
    }

    if (showPlaylistSheet && selectedSongForPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            sheetState = playlistSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = playlistArtworkColors.secondary.copy(alpha = 0.04f).compositeOver(MaterialTheme.colorScheme.surface)
        ) {
            PlaylistSelectionSheet(
                song = selectedSongForPlaylist!!,
                playlists = playlists,
                artworkColors = playlistArtworkColors,
                onDismiss = { showPlaylistSheet = false },
                onPlaylistSelected = { id ->
                    viewModel.addToPlaylist(id, selectedSongForPlaylist!!.id)
                    showPlaylistSheet = false
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name)
                    showPlaylistSheet = false
                },
                onFavoriteClick = { viewModel.toggleFavorite(selectedSongForPlaylist!!.id) }
            )
        }
    }
}

@Composable
fun SortBottomSheetContent(
    currentFilter: LibraryFilter,
    settings: TabSortSettings,
    onUpdate: (TabSortSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Sort ${currentFilter.label}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val sortTypes = when (currentFilter) {
            LibraryFilter.ALBUMS -> listOf(SortType.TITLE, SortType.ARTIST, SortType.RELEASE_DATE)
            LibraryFilter.ARTISTS -> listOf(SortType.TITLE, SortType.ALBUM_COUNT, SortType.TRACK_COUNT)
            else -> listOf(SortType.TITLE, SortType.ARTIST, SortType.RELEASE_DATE, SortType.DATE_ADDED)
        }

        Text("Sort by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        // Fix: Use Row with horizontal scroll instead of FlowRow to prevent NoSuchMethodError
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortTypes.forEach { type ->
                FilterChip(
                    selected = settings.sortType == type,
                    onClick = { onUpdate(settings.copy(sortType = type)) },
                    label = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (settings.sortType == type) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Order", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = settings.sortOrder == order,
                    onClick = { onUpdate(settings.copy(sortOrder = order)) },
                    label = { Text(if (order == SortOrder.ASC) "Ascending" else "Descending") },
                    leadingIcon = {
                        Icon(
                            if (order == SortOrder.ASC) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (currentFilter == LibraryFilter.ARTISTS) {
            Spacer(Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUpdate(settings.copy(showOnlyAlbumArtists = !settings.showOnlyAlbumArtists)) }
            ) {
                Checkbox(
                    checked = settings.showOnlyAlbumArtists,
                    onCheckedChange = { onUpdate(settings.copy(showOnlyAlbumArtists = it)) }
                )
                Text("Show only album artists", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
