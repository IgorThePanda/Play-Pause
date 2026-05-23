package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.LyricLine
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.data.db.SkipRuleEntity
import com.igorthepadna.play_pause.data.db.SkipType
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipSettingsSheet(
    song: Song,
    viewModel: MainViewModel,
    artworkColors: ArtworkColors,
    onDismiss: () -> Unit
) {
    val skipRules by viewModel.allSkipRules.collectAsState()
    val songRules = remember(skipRules, song.id) {
        skipRules.filter { it.mediaId == song.id.toString() }
    }
    
    val entireSongRule = songRules.find { it.type == SkipType.ENTIRE_SONG }
    val sectionRules = songRules.filter { it.type == SkipType.SECTION }

    val rawLyrics by viewModel.currentLyrics.collectAsState()
    val parsedLyrics = remember(rawLyrics) { parseLrc(rawLyrics) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Skip Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Entire Song Toggle
        Surface(
            onClick = {
                if (entireSongRule != null) {
                    viewModel.removeSkipRule(entireSongRule)
                } else {
                    viewModel.addSkipRule(SkipRuleEntity(mediaId = song.id.toString(), type = SkipType.ENTIRE_SONG))
                }
            },
            shape = RoundedCornerShape(20.dp),
            color = if (entireSongRule != null) artworkColors.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = if (entireSongRule != null) androidx.compose.foundation.BorderStroke(1.dp, artworkColors.secondary) else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (entireSongRule != null) Icons.Rounded.Block else Icons.Rounded.MusicNote,
                    null,
                    tint = if (entireSongRule != null) artworkColors.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Skip Entire Song", fontWeight = FontWeight.Bold)
                    Text(
                        "Always skip this song when it comes up",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = entireSongRule != null,
                    onCheckedChange = { checked ->
                        if (entireSongRule != null) {
                            viewModel.removeSkipRule(entireSongRule)
                        } else {
                            viewModel.addSkipRule(SkipRuleEntity(mediaId = song.id.toString(), type = SkipType.ENTIRE_SONG))
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = artworkColors.secondary, checkedTrackColor = artworkColors.secondary.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "SKIP SECTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = artworkColors.secondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (sectionRules.isEmpty()) {
            Text(
                "No sections to skip yet. Add one below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            sectionRules.forEach { rule ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FastForward, null, tint = artworkColors.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${formatDuration(rule.startTime)} → ${formatDuration(rule.endTime)}",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { viewModel.removeSkipRule(rule) }) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        var showAddSection by remember { mutableStateOf(false) }

        if (!showAddSection) {
            Button(
                onClick = { showAddSection = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = artworkColors.secondary)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Skip Section")
            }
        } else {
            AddSectionView(
                song = song,
                parsedLyrics = parsedLyrics,
                onAdd = { start, end ->
                    viewModel.addSkipRule(SkipRuleEntity(mediaId = song.id.toString(), type = SkipType.SECTION, startTime = start, endTime = end))
                    showAddSection = false
                },
                onCancel = { showAddSection = false },
                artworkColors = artworkColors
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionView(
    song: Song,
    parsedLyrics: List<LyricLine>,
    onAdd: (Long, Long) -> Unit,
    onCancel: () -> Unit,
    artworkColors: ArtworkColors
) {
    var range by remember { mutableStateOf(0f..song.duration.toFloat().coerceAtLeast(1f)) }
    var useLyrics by rememberSaveable(parsedLyrics.isNotEmpty()) { mutableStateOf(parsedLyrics.isNotEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(20.dp)
    ) {
        // --- Expressive Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedCard(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (useLyrics) "Select lyrics range" else "Select time range",
                    style = MaterialTheme.typography.labelMedium,
                    color = artworkColors.secondary
                )
            }

            if (parsedLyrics.isNotEmpty()) {
                FilledTonalIconButton(
                    onClick = { useLyrics = !useLyrics },
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = artworkColors.secondary.copy(alpha = 0.15f),
                        contentColor = artworkColors.secondary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (useLyrics) Icons.Rounded.Timer else Icons.Rounded.Lyrics, 
                        null, 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (useLyrics) {
            var startLine by remember { mutableStateOf<LyricLine?>(null) }
            var endLine by remember { mutableStateOf<LyricLine?>(null) }

            val beginningLine = remember { LyricLine(timestamp = 0L, text = "[Beginning]") }
            val endLineMarker = remember { LyricLine(timestamp = song.duration, text = "[End]") }
            val displayLyrics = remember(parsedLyrics) {
                listOf(beginningLine) + parsedLyrics + listOf(endLineMarker)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(4.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayLyrics) { line ->
                        val isSelected = remember(startLine, endLine, line) {
                            val startTs = startLine?.timestamp ?: -1L
                            val endTs = endLine?.timestamp ?: -1L
                            if (startTs != -1L && endTs != -1L) {
                                line.timestamp in startTs..endTs
                            } else {
                                line == startLine || line == endLine
                            }
                        }

                        Surface(
                            onClick = {
                                if (startLine == null) {
                                    startLine = line
                                } else if (endLine == null) {
                                    if (line.timestamp < startLine!!.timestamp) {
                                        endLine = startLine
                                        startLine = line
                                    } else {
                                        endLine = line
                                    }
                                } else {
                                    startLine = line
                                    endLine = null
                                }
                            },
                            color = if (isSelected) artworkColors.secondary.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = line.text,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = if (line == beginningLine || line == endLineMarker) 
                                    MaterialTheme.typography.labelLarge.copy(color = artworkColors.secondary, fontWeight = FontWeight.Black)
                                else MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) { 
                    Text("Cancel") 
                }
                
                Button(
                    onClick = {
                        val start = startLine?.timestamp ?: 0L
                        val end = if (endLine == endLineMarker) {
                            song.duration
                        } else {
                            endLine?.let { el ->
                                val idx = parsedLyrics.indexOf(el)
                                parsedLyrics.getOrNull(idx + 1)?.timestamp ?: song.duration
                            } ?: (start + 1000L).coerceAtMost(song.duration)
                        }
                        onAdd(start, end)
                    },
                    enabled = startLine != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = artworkColors.secondary)
                ) { 
                    Text("Add Section", fontWeight = FontWeight.Bold) 
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(20.dp)
            ) {
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..song.duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = artworkColors.secondary,
                        inactiveTrackColor = artworkColors.secondary.copy(alpha = 0.2f),
                        thumbColor = artworkColors.secondary
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDuration(range.start.toLong()), 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = artworkColors.secondary
                    )
                    Text(
                        text = formatDuration(range.endInclusive.toLong()), 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = artworkColors.secondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) { 
                    Text("Cancel") 
                }
                
                Button(
                    onClick = { onAdd(range.start.toLong(), range.endInclusive.toLong()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = artworkColors.secondary)
                ) { 
                    Text("Add Section", fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
}
