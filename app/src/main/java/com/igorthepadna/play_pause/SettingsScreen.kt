package com.igorthepadna.play_pause

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.igorthepadna.play_pause.utils.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
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

            SettingsSection(title = "Library") {
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
            
            // Add some spacer at the bottom to allow scrolling past the floating bar
            Spacer(Modifier.height(100.dp))
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
