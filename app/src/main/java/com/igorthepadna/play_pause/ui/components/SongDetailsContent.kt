package com.igorthepadna.play_pause.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SongDetailsContent(song: Song) {
    var genre by remember { mutableStateOf("Loading...") }
    var bitrate by remember { mutableStateOf("Loading...") }

    LaunchedEffect(song.path) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(song.path)
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Unknown Genre"
                val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                bitrate = if (br != null) "${br.toInt() / 1000} kbps" else "Unknown Bitrate"
            } catch (_: Exception) {
                genre = "Unknown"
                bitrate = "Unknown"
            } finally {
                retriever.release()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            ElevatedCard(
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Using standard Flow-like behavior with Column + Rows to avoid FlowRow binary mismatch crashes
        val details = listOf(
            Icons.Rounded.Album to song.album,
            Icons.Rounded.MusicNote to genre,
            Icons.Rounded.Timer to formatDuration(song.duration),
            Icons.Rounded.GraphicEq to bitrate,
            Icons.Rounded.FormatShapes to song.format.substringAfter("/"),
            Icons.Rounded.Numbers to "Track ${if (song.trackNumber > 0) song.trackNumber else "N/A"}",
            Icons.Rounded.SdStorage to String.format(Locale.getDefault(), "%.2f MB", song.size / (1024f * 1024f))
        )

        // Custom wrap layout using standard Compose features
        androidx.compose.ui.layout.Layout(
            content = {
                details.forEach { (icon, text) ->
                    DetailPill(icon, text)
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
            val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
            var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
            var currentRowWidth = 0
            val spacing = 8.dp.roundToPx()

            placeables.forEach { placeable ->
                if (currentRowWidth + placeable.width + spacing > constraints.maxWidth && currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
                currentRow.add(placeable)
                currentRowWidth += placeable.width + spacing
            }
            rows.add(currentRow)

            val height = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * spacing
            layout(constraints.maxWidth, height) {
                var y = 0
                rows.forEach { row ->
                    var x = 0
                    row.forEach { placeable ->
                        placeable.place(x, y)
                        x += placeable.width + spacing
                    }
                    y += row.maxOf { it.height } + spacing
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = song.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DetailPill(icon: ImageVector, text: String) {
    SuggestionChip(
        onClick = { },
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = null
    )
}
