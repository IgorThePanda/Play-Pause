package com.igorthepadna.play_pause

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.igorthepadna.play_pause.utils.verticalScrollbar

enum class SettingsTab {
    MAIN, LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(SettingsTab.MAIN) }

    BackHandler(enabled = currentTab != SettingsTab.MAIN) {
        currentTab = SettingsTab.MAIN
    }

    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            if (targetState == SettingsTab.LYRICS) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { tab ->
        when (tab) {
            SettingsTab.MAIN -> MainSettingsContent(
                viewModel = viewModel,
                onBack = onBack,
                onNavigateToLyrics = { currentTab = SettingsTab.LYRICS }
            )
            SettingsTab.LYRICS -> LyricSettingsScreen(
                viewModel = viewModel,
                onBack = { currentTab = SettingsTab.MAIN }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsContent(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToLyrics: () -> Unit
) {
    val scrollState = rememberScrollState()
    val paddingValues = PaddingValues(16.dp)
    
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
                .verticalScrollbar(scrollState, padding = paddingValues)
                .verticalScroll(scrollState)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "Playback") {
                val gaplessPlayback by viewModel.gaplessPlayback.collectAsStateWithLifecycle()
                
                SettingsSwitchItem(
                    title = "Gapless Playback",
                    subtitle = "Remove silence between tracks",
                    icon = Icons.Rounded.MusicNote,
                    checked = gaplessPlayback,
                    onCheckedChange = { viewModel.setGaplessPlayback(it) }
                )
            }

            SettingsSection(title = "Customization") {
                SettingsActionItem(
                    title = "Lyrics Editor",
                    subtitle = "Customize how lyrics appear",
                    icon = Icons.Rounded.Lyrics,
                    onClick = onNavigateToLyrics
                )
            }

            SettingsSection(title = "Library") {
                val scanOnlyMusic by viewModel.scanOnlyMusicFolder.collectAsStateWithLifecycle()
                
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
                
                SettingsActionItem(
                    title = "Fetch Artist Artwork",
                    subtitle = "Search local folders for artist images",
                    icon = Icons.Rounded.Image,
                    onClick = { viewModel.loadSongs(refresh = false) }
                )
            }

            SettingsSection(title = "Backup & Restore") {
                val context = LocalContext.current
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.let { os ->
                            viewModel.exportPlaylists(os)
                        }
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        context.contentResolver.openInputStream(it)?.let { isStream ->
                            viewModel.importPlaylists(isStream)
                        }
                    }
                }

                SettingsActionItem(
                    title = "Export Playlists",
                    subtitle = "Save playlists to a JSON file",
                    icon = Icons.Rounded.Upload,
                    onClick = { exportLauncher.launch("play_pause_playlists.json") }
                )

                SettingsActionItem(
                    title = "Import Playlists",
                    subtitle = "Restore playlists from JSON or M3U file",
                    icon = Icons.Rounded.Download,
                    onClick = { importLauncher.launch(arrayOf("application/json", "audio/mpegurl", "audio/x-mpegurl", "application/octet-stream")) }
                )
            }

            SettingsSection(title = "Appearance") {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val colorSchemeType by viewModel.colorSchemeType.collectAsStateWithLifecycle()
                val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()

                SettingsHeaderItem(title = "Theme Mode")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Spacer(Modifier.height(8.dp))
                SettingsHeaderItem(title = "Color Scheme")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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

                Spacer(Modifier.height(8.dp))
                SettingsSwitchItem(
                    title = "Artwork Accent",
                    subtitle = "Use colors from current song artwork",
                    icon = Icons.Rounded.Palette,
                    checked = useArtworkAccent,
                    onCheckedChange = { viewModel.setUseArtworkAccent(it) }
                )
            }
            
            Spacer(Modifier.height(100.dp))
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
                SettingsSwitchItem(
                    title = "Show Progress Bar",
                    subtitle = "Show the squiggly progress bar in fullscreen lyrics",
                    icon = Icons.Rounded.LinearScale,
                    checked = showLyricsProgress,
                    onCheckedChange = { viewModel.setShowLyricsProgress(it) }
                )
            }
            
            Spacer(Modifier.height(40.dp))
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
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
