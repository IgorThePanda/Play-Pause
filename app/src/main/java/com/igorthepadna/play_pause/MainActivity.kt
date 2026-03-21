package com.igorthepadna.play_pause

import android.Manifest
import android.content.ComponentName
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.igorthepadna.play_pause.ui.components.FullScreenPlayer
import com.igorthepadna.play_pause.ui.components.NowPlayingBar
import com.igorthepadna.play_pause.ui.screens.LibraryScreen
import com.igorthepadna.play_pause.ui.theme.PlayPauseTheme
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
    val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()
    val isPlayerFullScreenState by viewModel.isPlayerFullScreen.collectAsStateWithLifecycle()
    
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    
    // Fix: BackHandler for settings
    BackHandler(enabled = isSettingsVisible) {
        isSettingsVisible = false
    }

    // Sort logic
    val tabSortSettingsMap by viewModel.tabSortSettings.collectAsStateWithLifecycle()
    val currentTabSettings = tabSortSettingsMap[currentFilter] ?: TabSortSettings()
    var showSortSheet by remember { mutableStateOf(false) }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
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
                        val extraPadding = if (isNowPlayingVisible) 156.dp else 88.dp
                        
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
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    LibraryScreen(
                        player = player, 
                        currentFilter = currentFilter,
                        hasPermission = hasPermission,
                        isRefreshing = isRefreshing,
                        onPermissionChanged = { hasPermission = it },
                        onPlaySongs = { songList, index -> viewModel.playSongs(songList, index) },
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
                        }
                    )

                    if (activeMediaItem != null && player != null) {
                        NowPlayingBar(
                            player = player,
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 72.dp)
                                .navigationBarsPadding()
                                .graphicsLayer { alpha = barAlpha }
                        )
                    }

                    // Floating Navigation Buttons (Expressive Design)
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        val filters = LibraryFilter.entries
                        val pagerState = rememberPagerState(
                            initialPage = filters.indexOf(currentFilter)
                        ) { filters.size }

                        LaunchedEffect(pagerState.currentPage) {
                            if (filters[pagerState.currentPage] != currentFilter) {
                                viewModel.setCurrentFilter(filters[pagerState.currentPage])
                            }
                        }
                        
                        LaunchedEffect(currentFilter) {
                            val targetPage = filters.indexOf(currentFilter)
                            if (pagerState.currentPage != targetPage) {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.height(64.dp),
                            contentPadding = PaddingValues(horizontal = maxWidth / 2 - 90.dp),
                            pageSpacing = 16.dp,
                            verticalAlignment = Alignment.CenterVertically
                        ) { page ->
                            val filter = filters[page]
                            val isSelected = pagerState.currentPage == page

                            val alpha by animateFloatAsState(if (isSelected) 1f else 0.7f, label = "alpha")
                            val scale by animateFloatAsState(if (isSelected) 1.1f else 0.95f, label = "scale")

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .width(180.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(page)
                                            }
                                        },
                                        onDoubleClick = {
                                            showSortSheet = true
                                        }
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        filter.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    AnimatedVisibility(visible = isSelected) {
                                        Text(
                                            text = filter.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Bar Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Removed Sort Button: Sort menu is now triggered via double-tap on nav bar items
                        
                        FilledIconButton(
                            onClick = { isSettingsVisible = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
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
                }
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
