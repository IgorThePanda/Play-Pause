package com.igorthepadna.play_pause.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showPlayTimeDetail by remember { mutableStateOf(false) }

    if (showPlayTimeDetail) {
        PlayTimeDetailView(
            viewModel = viewModel, 
            onBack = { showPlayTimeDetail = false },
            contentPadding = contentPadding
        )
    } else {
        StatsSummaryView(
            viewModel = viewModel,
            onBack = onBack,
            onPlayTimeClick = { showPlayTimeDetail = true },
            contentPadding = contentPadding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSummaryView(
    viewModel: MainViewModel,
    onBack: (() -> Unit)? = null,
    onPlayTimeClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val totalPlayCount by viewModel.getTotalPlayCount().collectAsState(initial = 0L)
    val topTracks by viewModel.getTopTracks(5).collectAsState(initial = emptyList())
    val topArtists by viewModel.getTopArtists(5).collectAsState(initial = emptyList())

    val content = @Composable { padding: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = contentPadding,
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
                                com.igorthepadna.play_pause.ui.components.UniversalSongItem(
                                    song = song,
                                    isPlaying = false,
                                    onClick = { viewModel.playSongs(listOf(song), 0, null) },
                                    onDetailsClick = { },
                                    onSwipePlayNext = { },
                                    onSwipeAddToPlaylist = { },
                                    label = song.title,
                                    secondaryLabel = "${track.playCount} plays",
                                    containerColor = Color.Transparent
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
                            val artistData = viewModel.allArtists.collectAsState().value.find { it.name == artist.artistName }
                            com.igorthepadna.play_pause.ui.components.UniversalSongItem(
                                song = com.igorthepadna.play_pause.data.Song(0, "", "", "", 0, android.net.Uri.EMPTY, null, "", 0, "", 0, 0, 1, null, 0),
                                isPlaying = false,
                                onClick = { viewModel.setSelectedArtistName(artist.artistName) },
                                onDetailsClick = { },
                                onSwipePlayNext = { },
                                onSwipeAddToPlaylist = { },
                                label = artist.artistName,
                                secondaryLabel = "${artist.playCount} plays",
                                artworkUri = artistData?.thumbnailUri,
                                containerColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        }
    }

    if (onBack != null) {
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
            content(padding)
        }
    } else {
        content(PaddingValues(0.dp))
    }
}

@Composable
fun PlayTimeDetailView(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
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
                .verticalScrollbar() 
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Timer,
                null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Total Plays",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = totalPlayCount.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "View Activity Breakdown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HeatmapGrid(dailyStats: List<DailyPlayTime>) {
    val maxCount = remember(dailyStats) { dailyStats.maxOfOrNull { it.totalPlayCount } ?: 1 }
    
    // Last 30 days
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    val endDay = calendar.timeInMillis
    val startDay = endDay - (29L * 24 * 60 * 60 * 1000)

    val statsMap = dailyStats.associateBy { it.date }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Simple 7x5 grid (approx)
        val chunkedDays = (0 until 30).chunked(7)
        
        chunkedDays.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEach { dayOffset ->
                    val timestamp = startDay + (dayOffset.toLong() * 24 * 60 * 60 * 1000)
                    val count = statsMap[timestamp]?.totalPlayCount ?: 0
                    val alpha = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (count > 0) alpha else 0.05f))
                            .border(1.dp, if (count > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun WeekdayAveragesChart(dailyStats: List<DailyPlayTime>) {
    val dayAverages = remember(dailyStats) {
        val sums = IntArray(7)
        val counts = IntArray(7)
        val cal = Calendar.getInstance()
        
        dailyStats.forEach { stat ->
            cal.timeInMillis = stat.date
            val day = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon = 0
            sums[day] += stat.totalPlayCount
            counts[day]++
        }
        
        FloatArray(7) { i -> if (counts[i] > 0) sums[i].toFloat() / counts[i] else 0f }
    }
    
    val maxAvg = dayAverages.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column {
        SectionHeader("Average Daily Plays")
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dayAverages.forEachIndexed { index, avg ->
                val heightFactor = avg / maxAvg
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight(heightFactor.coerceIn(0.05f, 1f))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(dayLabels[index], style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

fun Modifier.verticalScrollbar(): Modifier = this // Simplified for now
