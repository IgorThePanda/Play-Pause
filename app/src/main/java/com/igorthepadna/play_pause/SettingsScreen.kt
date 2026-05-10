package com.igorthepadna.play_pause

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.SortOrder
import com.igorthepadna.play_pause.data.SortType

enum class SettingsTab {
    MAIN, PLAYBACK, APPEARANCE, LYRICS_EDITOR, LIBRARY, BACKUP, PLAYLIST_EXPORT, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.MAIN) }

    BackHandler(enabled = true) {
        if (currentTab != SettingsTab.MAIN) {
            currentTab = SettingsTab.MAIN
        } else {
            onBack()
        }
    }

    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            if (targetState != SettingsTab.MAIN) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { tab ->
        when (tab) {
            SettingsTab.MAIN -> MainSettingsCategories(
                onNavigate = { currentTab = it },
                onBack = onBack
            )
            SettingsTab.PLAYBACK -> PlaybackSettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.MAIN }
            )
            SettingsTab.APPEARANCE -> AppearanceSettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.MAIN },
                onNavigateToLyrics = { currentTab = SettingsTab.LYRICS_EDITOR }
            )
            SettingsTab.LYRICS_EDITOR -> LyricSettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.APPEARANCE }
            )
            SettingsTab.LIBRARY -> LibrarySettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.MAIN }
            )
            SettingsTab.BACKUP -> BackupSettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.MAIN },
                onNavigateToPlaylistExport = { currentTab = SettingsTab.PLAYLIST_EXPORT }
            )
            SettingsTab.PLAYLIST_EXPORT -> PlaylistExportSelectionScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.BACKUP }
            )
            SettingsTab.ABOUT -> AboutScreen(
                onBack = { currentTab = SettingsTab.MAIN }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsCategories(
    onNavigate: (SettingsTab) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryCard(
                title = "Playback",
                subtitle = "Gapless, crossfade and audio engine",
                icon = Icons.Rounded.PlayCircle,
                onClick = { onNavigate(SettingsTab.PLAYBACK) }
            )
            CategoryCard(
                title = "Appearance",
                subtitle = "Themes, navbar order and lyrics editor",
                icon = Icons.Rounded.Palette,
                onClick = { onNavigate(SettingsTab.APPEARANCE) }
            )
            CategoryCard(
                title = "Library",
                subtitle = "Scanning, folder filters and artwork",
                icon = Icons.Rounded.LibraryMusic,
                onClick = { onNavigate(SettingsTab.LIBRARY) }
            )
            CategoryCard(
                title = "Backup & Restore",
                subtitle = "Export and import your playlists",
                icon = Icons.Rounded.SettingsBackupRestore,
                onClick = { onNavigate(SettingsTab.BACKUP) }
            )
            CategoryCard(
                title = "About",
                subtitle = "App version and developer information",
                icon = Icons.Rounded.Info,
                onClick = { onNavigate(SettingsTab.ABOUT) }
            )
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val gapless by viewModel.gaplessPlayback.collectAsStateWithLifecycle()
            val shuffleByDefault by viewModel.shuffleByDefault.collectAsStateWithLifecycle()
            val repeatByDefault by viewModel.repeatByDefault.collectAsStateWithLifecycle()
            val resetOnPrevious by viewModel.resetOnPrevious.collectAsStateWithLifecycle()
            val lyricsByDefault by viewModel.lyricsByDefault.collectAsStateWithLifecycle()

            SettingsSection(title = "Engine") {
                SettingsSwitchItem(
                    title = "Gapless Playback",
                    subtitle = "Eliminate silence between tracks",
                    icon = Icons.Rounded.MotionPhotosOn,
                    checked = gapless,
                    onCheckedChange = { viewModel.setGaplessPlayback(it) }
                )
            }

            SettingsSection(title = "Behaviors") {
                SettingsSwitchItem(
                    title = "Shuffle by default",
                    subtitle = "Automatically shuffle when playing a list",
                    icon = Icons.Rounded.Shuffle,
                    checked = shuffleByDefault,
                    onCheckedChange = { viewModel.setShuffleByDefault(it) }
                )
                SettingsSwitchItem(
                    title = "Repeat by default",
                    subtitle = "Keep playing the same list/song",
                    icon = Icons.Rounded.Repeat,
                    checked = repeatByDefault,
                    onCheckedChange = { viewModel.setRepeatByDefault(it) }
                )
                SettingsSwitchItem(
                    title = "Lyrics by default",
                    subtitle = "Open lyrics view when starting playback",
                    icon = Icons.Rounded.Lyrics,
                    checked = lyricsByDefault,
                    onCheckedChange = { viewModel.setLyricsByDefault(it) }
                )
                SettingsSwitchItem(
                    title = "Reset on Previous",
                    subtitle = "Restart song if past 3 seconds",
                    icon = Icons.Rounded.SkipPrevious,
                    checked = resetOnPrevious,
                    onCheckedChange = { viewModel.setResetOnPrevious(it) }
                )
            }
            
            val playNextBehavior by viewModel.playNextBehavior.collectAsStateWithLifecycle()
            SettingsSection(title = "Queue") {
                SettingsHeaderItem(title = "Play Next Position")
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionChip(
                        selected = playNextBehavior == MainViewModel.PlayNextBehavior.TOP,
                        label = "Top",
                        onClick = { viewModel.setPlayNextBehavior(MainViewModel.PlayNextBehavior.TOP) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        selected = playNextBehavior == MainViewModel.PlayNextBehavior.BOTTOM,
                        label = "Bottom",
                        onClick = { viewModel.setPlayNextBehavior(MainViewModel.PlayNextBehavior.BOTTOM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onNavigateToLyrics: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val colorSchemeType by viewModel.colorSchemeType.collectAsStateWithLifecycle()
            val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()
            val showBitrateInfo by viewModel.showBitrateInfo.collectAsStateWithLifecycle()
            val navBarAtTop by viewModel.navBarAtTop.collectAsStateWithLifecycle()

            SettingsSection(title = "Theming") {
                SettingsHeaderItem(title = "Theme Mode")
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOptionChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        label = "Light",
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        selected = themeMode == ThemeMode.DARK,
                        label = "Dark",
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        selected = themeMode == ThemeMode.AUTO,
                        label = "Auto",
                        onClick = { viewModel.setThemeMode(ThemeMode.AUTO) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                SettingsHeaderItem(title = "Color Scheme")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSchemeOptionChip(
                        selected = colorSchemeType == ColorSchemeType.DYNAMIC,
                        label = "Dynamic",
                        onClick = { viewModel.setColorSchemeType(ColorSchemeType.DYNAMIC) }
                    )
                    ColorSchemeOptionChip(
                        selected = colorSchemeType == ColorSchemeType.PURPLE,
                        label = "Purple",
                        onClick = { viewModel.setColorSchemeType(ColorSchemeType.PURPLE) }
                    )
                    ColorSchemeOptionChip(
                        selected = colorSchemeType == ColorSchemeType.BLUE,
                        label = "Blue",
                        onClick = { viewModel.setColorSchemeType(ColorSchemeType.BLUE) }
                    )
                    ColorSchemeOptionChip(
                        selected = colorSchemeType == ColorSchemeType.GREEN,
                        label = "Green",
                        onClick = { viewModel.setColorSchemeType(ColorSchemeType.GREEN) }
                    )
                    ColorSchemeOptionChip(
                        selected = colorSchemeType == ColorSchemeType.ORANGE,
                        label = "Orange",
                        onClick = { viewModel.setColorSchemeType(ColorSchemeType.ORANGE) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                SettingsSwitchItem(
                    title = "Artwork Accent",
                    subtitle = "Use colors from current song artwork",
                    icon = Icons.Rounded.Palette,
                    checked = useArtworkAccent,
                    onCheckedChange = { viewModel.setUseArtworkAccent(it) }
                )
                SettingsSwitchItem(
                    title = "Show Bitrate Info",
                    subtitle = "Show song quality in full-screen player",
                    icon = Icons.Rounded.HighQuality,
                    checked = showBitrateInfo,
                    onCheckedChange = { viewModel.setShowBitrateInfo(it) }
                )
            }

            SettingsSection(title = "Lyrics") {
                SettingsActionItem(
                    title = "Lyrics Editor",
                    subtitle = "Customize how lyrics appear",
                    icon = Icons.Rounded.Lyrics,
                    onClick = onNavigateToLyrics
                )
            }

            SettingsSection(title = "Navigation") {
                val navOrder by viewModel.navBarOrder.collectAsStateWithLifecycle()
                
                SettingsSwitchItem(
                    title = "Category Bar at Top",
                    subtitle = "Move the library categories to the top",
                    icon = Icons.Rounded.VerticalAlignTop,
                    checked = navBarAtTop,
                    onCheckedChange = { viewModel.setNavBarAtTop(it) }
                )

                Spacer(Modifier.height(12.dp))
                SettingsHeaderItem(title = "Library Categories Order")
                Text(
                    "Reorder by tapping the arrows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp)
                ) {
                    navOrder.forEachIndexed { index, filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                filter.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = navOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            viewModel.setNavBarOrder(newList)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, null)
                                }
                                IconButton(
                                    onClick = {
                                        if (index < navOrder.size - 1) {
                                            val newList = navOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            viewModel.setNavBarOrder(newList)
                                        }
                                    },
                                    enabled = index < navOrder.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, null)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsHeaderItem(title = "Hub Pages Order")
                val hubOrder by viewModel.hubOrder.collectAsStateWithLifecycle()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp)
                ) {
                    hubOrder.forEachIndexed { index, filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                filter.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Row {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = hubOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            viewModel.setHubOrder(newList)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, null)
                                }
                                IconButton(
                                    onClick = {
                                        if (index < hubOrder.size - 1) {
                                            val newList = hubOrder.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            viewModel.setHubOrder(newList)
                                        }
                                    },
                                    enabled = index < hubOrder.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val scanOnlyMusic by viewModel.scanOnlyMusicFolder.collectAsStateWithLifecycle()
            SettingsSection(title = "Scanning") {
                SettingsSwitchItem(
                    title = "Scan Only Music Folder",
                    subtitle = "Skip Downloads folder for faster scanning",
                    icon = Icons.Rounded.Folder,
                    checked = scanOnlyMusic,
                    onCheckedChange = { viewModel.setScanOnlyMusicFolder(it) }
                )

                SettingsActionItem(
                    title = "Rescan Media",
                    subtitle = "Search for new music files",
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.loadSongs(refresh = true) }
                )
            }
            
            SettingsSection(title = "Artwork") {
                SettingsActionItem(
                    title = "Fetch Artist Artwork",
                    subtitle = "Search local folders for artist images",
                    icon = Icons.Rounded.Image,
                    onClick = { viewModel.loadSongs(refresh = false) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onNavigateToPlaylistExport: () -> Unit) {
    val context = LocalContext.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.let { os -> viewModel.exportPlaylists(os) } }
    }
    val exportStatsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.let { os -> viewModel.exportStats(os) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.let { isStream -> viewModel.importPlaylists(isStream) } }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All History?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all your listening statistics and history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearStats()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "Playlist Backups") {
                SettingsActionItem(
                    title = "Export All Playlists",
                    subtitle = "Save all playlists to a single JSON file",
                    icon = Icons.Rounded.Upload,
                    onClick = { exportLauncher.launch("play_pause_playlists.json") }
                )
                SettingsActionItem(
                    title = "Export Playlists Separately",
                    subtitle = "Select playlists to save as individual files",
                    icon = Icons.Rounded.LibraryMusic,
                    onClick = onNavigateToPlaylistExport
                )
                SettingsActionItem(
                    title = "Import Playlists",
                    subtitle = "Restore playlists from JSON or M3U file",
                    icon = Icons.Rounded.Download,
                    onClick = { importLauncher.launch(arrayOf("application/json", "audio/mpegurl", "audio/x-mpegurl", "application/octet-stream")) }
                )
            }

            SettingsSection(title = "Statistics & History") {
                SettingsActionItem(
                    title = "Export Statistics",
                    subtitle = "Save your listening history to a JSON file",
                    icon = Icons.Rounded.BarChart,
                    onClick = { exportStatsLauncher.launch("play_pause_stats.json") }
                )
                SettingsActionItem(
                    title = "Clear All History",
                    subtitle = "Reset all your listening data",
                    icon = Icons.Rounded.DeleteSweep,
                    onClick = { showClearHistoryDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "Play-Pause", 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Text(
                "Version 1.0 (Beta)", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))
            
            SettingsSection(title = "Developers") {
                DeveloperItem(
                    name = "Google AI Model",
                    role = "App Creator & Architect",
                    icon = Icons.Rounded.AutoAwesome
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
                DeveloperItem(
                    name = "IgorThePanda",
                    role = "Lead Developer",
                    icon = Icons.Rounded.Person
                )
            }
            
            SettingsSection(title = "Credits") {
                SettingsActionItem(
                    title = "Material You 3 Expressive",
                    subtitle = "Design inspired by Google's M3 guide",
                    icon = Icons.Rounded.DesignServices,
                    onClick = {}
                )
                SettingsActionItem(
                    title = "Open Source Libraries",
                    subtitle = "Media3, Coil, Room, Serialization",
                    icon = Icons.Rounded.Code,
                    onClick = {}
                )
            }
            
            Text(
                "Made with ❤️ and Kotlin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val fontSize by viewModel.lyricFontSize.collectAsStateWithLifecycle()
    val inactiveAlpha by viewModel.lyricInactiveAlpha.collectAsStateWithLifecycle()
    val activeScale by viewModel.lyricActiveScale.collectAsStateWithLifecycle()
    val lineSpacing by viewModel.lyricLineSpacing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics Editor", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section
            Text(
                "Preview",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Inactive Sample
                    Text(
                        "This is an inactive line",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * 1.4).sp
                        ),
                        color = Color.White.copy(alpha = inactiveAlpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = (lineSpacing / 2).dp)
                    )
                    
                    // Active Sample
                    Text(
                        "This is the active line",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = (fontSize * 1.4).sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = activeScale
                                scaleY = activeScale
                            }
                            .padding(vertical = (lineSpacing / 2).dp)
                    )

                    // Inactive Sample
                    Text(
                        "Waiting for the beat...",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * 1.4).sp
                        ),
                        color = Color.White.copy(alpha = inactiveAlpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = (lineSpacing / 2).dp)
                    )
                }
            }

            SettingsSection(title = "Typography") {
                SettingsSliderItem(
                    title = "Font Size",
                    value = fontSize,
                    valueRange = 16f..48f,
                    icon = Icons.Rounded.TextFields,
                    onValueChange = { viewModel.setLyricFontSize(it) }
                )
                
                SettingsSliderItem(
                    title = "Line Spacing",
                    value = lineSpacing,
                    valueRange = 4f..48f,
                    icon = Icons.Rounded.FormatLineSpacing,
                    onValueChange = { viewModel.setLyricLineSpacing(it) }
                )
            }

            SettingsSection(title = "Animations") {
                SettingsSliderItem(
                    title = "Inactive Opacity",
                    value = inactiveAlpha,
                    valueRange = 0.05f..0.8f,
                    icon = Icons.Rounded.Opacity,
                    onValueChange = { viewModel.setLyricInactiveAlpha(it) }
                )

                SettingsSliderItem(
                    title = "Active Scale",
                    value = activeScale,
                    valueRange = 1f..1.5f,
                    icon = Icons.Rounded.ZoomIn,
                    onValueChange = { viewModel.setLyricActiveScale(it) }
                )
            }

            SettingsSection(title = "Interactions") {
                val showLyricsProgress by viewModel.showLyricsProgress.collectAsStateWithLifecycle()
                val lyricAlignmentCenter by viewModel.lyricAlignmentCenter.collectAsStateWithLifecycle()
                
                SettingsSwitchItem(
                    title = "Show Progress Bar",
                    subtitle = "Show the squiggly progress bar in fullscreen lyrics",
                    icon = Icons.Rounded.LinearScale,
                    checked = showLyricsProgress,
                    onCheckedChange = { viewModel.setShowLyricsProgress(it) }
                )
                SettingsSwitchItem(
                    title = "Center Lyrics",
                    subtitle = "Align lyrics text to the middle of the screen",
                    icon = Icons.Rounded.FormatAlignCenter,
                    checked = lyricAlignmentCenter,
                    onCheckedChange = { viewModel.setLyricAlignmentCenter(it) }
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun DeveloperItem(
    name: String,
    role: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (valueRange.endInclusive <= 2f) "%.2f".format(value) else value.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsHeaderItem(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun ThemeOptionChip(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

@Composable
fun ColorSchemeOptionChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistExportSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val selectedIds = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.exportPlaylistsToFolder(selectedIds.toList(), it, context.contentResolver)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Playlists", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { folderLauncher.launch(null) }) {
                            Text("Export (${selectedIds.size})", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No playlists to export", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Choose playlists to export as separate JSON files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                items(playlists) { playlist ->
                    val isSelected = selectedIds.contains(playlist.id)
                    Surface(
                        onClick = {
                            if (isSelected) selectedIds.remove(playlist.id)
                            else selectedIds.add(playlist.id)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${playlist.songs.size} songs", style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (it!!) selectedIds.add(playlist.id)
                                    else selectedIds.remove(playlist.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
