package com.igorthepadna.play_pause

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPauseApp(viewModel: MainViewModel, player: Player?, intent: Intent) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isPlayerFullScreen by rememberSaveable { mutableStateOf(false) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var currentFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALBUMS) }
    
    // Sort logic
    var currentSortType by rememberSaveable { mutableStateOf(SortType.TITLE) }
    var currentSortOrder by rememberSaveable { mutableStateOf(SortOrder.ASC) }
    var showSortSheet by remember { mutableStateOf(false) }

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()
    
    var activeMediaItem by remember(player) { mutableStateOf(player?.currentMediaItem) }

    LaunchedEffect(intent, player) {
        if (intent.getBooleanExtra("OPEN_PLAYER", false) && player != null) {
            isPlayerFullScreen = true
            intent.removeExtra("OPEN_PLAYER")
        }
    }

    var hasPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
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

    if (isPlayerFullScreen && player != null) {
        FullScreenPlayer(
            player = player,
            useArtworkAccent = useArtworkAccent,
            onDismiss = { isPlayerFullScreen = false }
        )
    } else if (isSettingsVisible) {
        MainSettingsScreen(viewModel, onBack = { isSettingsVisible = false })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.padding(12.dp).navigationBarsPadding(),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                        snackbarData = data
                    )
                }
            }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                LibraryScreen(
                    player = player, 
                    currentFilter = currentFilter,
                    songs = songs,
                    hasPermission = hasPermission,
                    isRefreshing = isRefreshing,
                    onPermissionChanged = { hasPermission = it },
                    onRefresh = { viewModel.loadSongs(refresh = true) },
                    onOpenSettings = { isSettingsVisible = true },
                    onPlaySongs = { songList, index -> viewModel.playSongs(songList, index) },
                    viewModel = viewModel,
                    sortType = currentSortType,
                    sortOrder = currentSortOrder,
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

                // Now Playing Bar
                AnimatedVisibility(
                    visible = activeMediaItem != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                        .navigationBarsPadding()
                ) {
                    if (player != null) {
                        NowPlayingBar(
                            player = player,
                            onClick = { isPlayerFullScreen = true }
                        )
                    }
                }

                // Floating Navigation Buttons
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
                        currentFilter = filters[pagerState.currentPage]
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
                        val blurRadius by animateFloatAsState(if (isSelected) 0f else 8f, label = "blur")

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .width(180.dp)
                                .height(52.dp)
                                .pointerInput(page, isSelected) {
                                    detectTapGestures(
                                        onTap = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(page)
                                            }
                                        },
                                        onDoubleTap = {
                                            if (isSelected) {
                                                showSortSheet = true
                                            } else {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(page)
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f) 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                tonalElevation = if (isSelected) 8.dp else 2.dp,
                                shadowElevation = if (isSelected) 12.dp else 2.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = RenderEffect.createBlurEffect(blurRadius * 2, blurRadius * 2, Shader.TileMode.CLAMP).asComposeRenderEffect()
                                        }
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {}

                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Sort Options",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val sortingItems = listOf(
                    SortType.TITLE to "Title",
                    SortType.ARTIST to "Artist",
                    SortType.DURATION to "Duration",
                    SortType.RELEASE_DATE to "Release Date",
                    SortType.DATE_ADDED to "Recently Added"
                )

                sortingItems.forEach { (type, label) ->
                    val isTypeSelected = currentSortType == type
                    Surface(
                        onClick = { currentSortType = type },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isTypeSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isTypeSelected) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = null,
                                    tint = if (isTypeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isTypeSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            if (isTypeSelected) {
                                IconButton(
                                    onClick = {
                                        currentSortOrder = if (currentSortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentSortOrder == SortOrder.ASC) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = "Toggle Order",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    SettingsScreen(viewModel = viewModel, onBack = onBack)
}
