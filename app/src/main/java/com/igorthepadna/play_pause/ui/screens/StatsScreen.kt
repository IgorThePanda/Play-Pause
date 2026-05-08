package com.igorthepadna.play_pause.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.db.DailyPlayTime
import com.igorthepadna.play_pause.data.db.TopArtist
import com.igorthepadna.play_pause.data.db.TopTrack
import com.igorthepadna.play_pause.ui.components.ArtistSubtitle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showPlayTimeDetail by remember { mutableStateOf(false) }

    if (showPlayTimeDetail) {
        PlayTimeDetailView(viewModel = viewModel, onBack = { showPlayTimeDetail = false })
    } else {
        StatsSummaryView(
            viewModel = viewModel,
            onBack = onBack,
            onPlayTimeClick = { showPlayTimeDetail = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSummaryView(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayTimeClick: () -> Unit
) {
    val totalPlayCount by viewModel.getTotalPlayCount().collectAsState(initial = 0L)
    val topTracks by viewModel.getTopTracks(5).collectAsState(initial = emptyList())
    val topArtists by viewModel.getTopArtists(5).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Play Time Overview Card
            item {
                StatsOverviewCard(
                    totalPlayCount = totalPlayCount,
                    onClick = onPlayTimeClick
                )
            }

            // Top Tracks Section
            item {
                SectionHeader("Top Tracks")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    if (topTracks.isEmpty()) {
                        Text(
                            "Not enough data yet",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topTracks.forEach { track ->
                            val song = viewModel.songs.collectAsState().value.find { it.id == track.songId }
                            if (song != null) {
                                ListItem(
                                    headlineContent = { Text(song.title, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text(song.artist) },
                                    trailingContent = { 
                                        Text(
                                            "${track.playCount} plays",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        ) 
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }

            // Top Artists Section
            item {
                SectionHeader("Top Artists")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    if (topArtists.isEmpty()) {
                        Text(
                            "Not enough data yet",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topArtists.forEach { artist ->
                            ListItem(
                                headlineContent = { 
                                    ArtistSubtitle(
                                        artistText = artist.artistName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        mainColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingContent = { 
                                    Text(
                                        "${artist.playCount} plays",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    ) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayTimeDetailView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val dailyStats by viewModel.getDailyPlayCounts(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Play Time Detail", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar() // Note: You might need to import or implement this
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Calendar View Header
            Text(
                "Last 30 Days Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            // Simple Calendar-like Heatmap
            HeatmapGrid(dailyStats)

            // Daily Averages
            WeekdayAveragesChart(dailyStats)
        }
    }
}

@Composable
fun StatsOverviewCard(
    totalPlayCount: Long,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timeline, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Total Listens", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = totalPlayCount.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Click for detailed activity insights",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun HeatmapGrid(stats: List<DailyPlayTime>) {
    val maxCount = stats.maxOfOrNull { it.totalPlayCount } ?: 1
    
    // Create a map for easy lookup
    val statsMap = stats.associateBy { 
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.date
        cal.get(Calendar.DAY_OF_YEAR)
    }

    val cal = Calendar.getInstance()
    val todayOfYear = cal.get(Calendar.DAY_OF_YEAR)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Simplified 7x4 or 7x5 grid for the last 30 days
            for (week in 0 until 5) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (day in 0 until 7) {
                        val daysAgo = (4 - week) * 7 + (6 - day)
                        if (daysAgo < 30) {
                            val checkCal = Calendar.getInstance()
                            checkCal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                            val playStat = statsMap[checkCal.get(Calendar.DAY_OF_YEAR)]
                            
                            val alpha = if (playStat != null) {
                                (playStat.totalPlayCount.toFloat() / maxCount).coerceIn(0.1f, 1f)
                            } else 0.05f
                            
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                            )
                        } else {
                            Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth())
                        }
                    }
                }
            }
        }
        Text(
            "Less \u2192 More Playtime",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeekdayAveragesChart(stats: List<DailyPlayTime>) {
    val weekdayCounts = IntArray(7)
    val weekdaySums = IntArray(7)
    
    stats.forEach { stat ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = stat.date
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        weekdaySums[dayOfWeek] += stat.totalPlayCount
        weekdayCounts[dayOfWeek]++
    }
    
    val averages = FloatArray(7) { i ->
        if (weekdayCounts[i] > 0) weekdaySums[i].toFloat() / weekdayCounts[i] else 0f
    }
    
    val maxAvg = averages.maxOrNull() ?: 1f
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column {
        Text(
            "Average Activity by Day",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            averages.forEachIndexed { index, avg ->
                val heightPercent = if (maxAvg > 0) avg / maxAvg else 0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightPercent.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(labels[index], style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// Extension to handle vertical scroll
@Composable
fun Modifier.verticalScrollbar() = this.then(
    Modifier.verticalScroll(rememberScrollState())
)
