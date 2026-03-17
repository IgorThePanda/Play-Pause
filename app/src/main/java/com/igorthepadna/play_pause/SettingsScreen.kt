package com.igorthepadna.play_pause

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Appearance", "About")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GeneralSettings(viewModel)
                1 -> AppearanceSettings(viewModel)
                2 -> AboutSettings()
            }
        }
    }
}

@Composable
fun GeneralSettings(viewModel: MainViewModel) {
    val gapless by viewModel.gaplessPlayback.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsCategory(title = "Playback")
        SettingsSwitchItem(
            title = "Gapless Playback",
            subtitle = "Remove silence between tracks",
            icon = Icons.Rounded.Timer,
            checked = gapless,
            onCheckedChange = { viewModel.setGaplessPlayback(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategory(title = "Library")
        SettingsClickableItem(
            title = "Rescan Library",
            subtitle = "Look for new music files",
            icon = Icons.Rounded.Refresh,
            onClick = { viewModel.loadSongs(refresh = true) }
        )
    }
}

@Composable
fun AppearanceSettings(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colorSchemeType by viewModel.colorSchemeType.collectAsStateWithLifecycle()
    val useArtworkAccent by viewModel.useArtworkAccent.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsCategory(title = "Theme Mode")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeModeButton(
                label = "Light",
                isSelected = themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeButton(
                label = "Dark",
                isSelected = themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeButton(
                label = "Auto",
                isSelected = themeMode == ThemeMode.AUTO,
                onClick = { viewModel.setThemeMode(ThemeMode.AUTO) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategory(title = "Color Scheme")
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorSchemeItem(
                label = "Material You (Dynamic)",
                isSelected = colorSchemeType == ColorSchemeType.DYNAMIC,
                color = MaterialTheme.colorScheme.primary,
                onClick = { viewModel.setColorSchemeType(ColorSchemeType.DYNAMIC) }
            )
            ColorSchemeItem(
                label = "Classic Purple",
                isSelected = colorSchemeType == ColorSchemeType.PURPLE,
                color = Color(0xFF6650a4),
                onClick = { viewModel.setColorSchemeType(ColorSchemeType.PURPLE) }
            )
            ColorSchemeItem(
                label = "Ocean Blue",
                isSelected = colorSchemeType == ColorSchemeType.BLUE,
                color = Color(0xFF0061A4),
                onClick = { viewModel.setColorSchemeType(ColorSchemeType.BLUE) }
            )
            ColorSchemeItem(
                label = "Forest Green",
                isSelected = colorSchemeType == ColorSchemeType.GREEN,
                color = Color(0xFF006D32),
                onClick = { viewModel.setColorSchemeType(ColorSchemeType.GREEN) }
            )
            ColorSchemeItem(
                label = "Sun Orange",
                isSelected = colorSchemeType == ColorSchemeType.ORANGE,
                color = Color(0xFF8B5000),
                onClick = { viewModel.setColorSchemeType(ColorSchemeType.ORANGE) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategory(title = "Player Background")
        SettingsSwitchItem(
            title = "Artwork Accent",
            subtitle = "Use colors from current song artwork",
            icon = Icons.Rounded.ColorLens,
            checked = useArtworkAccent,
            onCheckedChange = { viewModel.setUseArtworkAccent(it) }
        )
    }
}

@Composable
fun ThemeModeButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ColorSchemeItem(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AboutSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Play-Pause",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsClickableItem(
            title = "Developer",
            subtitle = "AI with the help of IgorThePanda",
            icon = Icons.Rounded.Person
        )
        SettingsClickableItem(
            title = "Source Code",
            subtitle = "GitHub",
            icon = Icons.Rounded.Code
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
