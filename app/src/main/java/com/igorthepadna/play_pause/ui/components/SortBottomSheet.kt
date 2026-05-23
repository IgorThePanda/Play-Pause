package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.igorthepadna.play_pause.TabSortSettings
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.LyricsFilter
import com.igorthepadna.play_pause.data.SortOrder
import com.igorthepadna.play_pause.data.SortType

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

        if (currentFilter == LibraryFilter.SONGS) {
            Spacer(Modifier.height(24.dp))
            Text("Lyrics Filter", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LyricsFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = settings.lyricsFilter == filter,
                        onClick = { onUpdate(settings.copy(lyricsFilter = filter)) },
                        label = { 
                            Text(when(filter) {
                                LyricsFilter.ALL -> "All Songs"
                                LyricsFilter.ANY -> "Any Lyrics"
                                LyricsFilter.VERSE_SYNCED -> "Verse Synced"
                                LyricsFilter.WORD_SYNCED -> "Word Synced"
                                LyricsFilter.NONE -> "No Lyrics"
                            }) 
                        },
                        leadingIcon = if (settings.lyricsFilter == filter) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
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
