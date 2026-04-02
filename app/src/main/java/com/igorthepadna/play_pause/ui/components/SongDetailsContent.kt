package com.igorthepadna.play_pause.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SongDetailsContent(
    song: Song,
    artworkColors: ArtworkColors
) {
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
            .padding(horizontal = 24.dp, vertical = 24.dp)
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleLarge,
                    color = artworkColors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            ElevatedCard(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "SONG INFORMATION",
            style = MaterialTheme.typography.labelMedium.copy(
                color = artworkColors.secondary,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom wrap layout using standard Compose features
        val details = listOf(
            Icons.Rounded.Album to song.album,
            Icons.Rounded.MusicNote to genre,
            Icons.Rounded.Timer to formatDuration(song.duration),
            Icons.Rounded.GraphicEq to bitrate,
            Icons.Rounded.FormatShapes to song.format.substringAfter("/"),
            Icons.Rounded.Numbers to "Track ${if (song.trackNumber > 0) song.trackNumber else "N/A"}",
            Icons.Rounded.SdStorage to String.format(Locale.getDefault(), "%.2f MB", song.size / (1024f * 1024f))
        )

        androidx.compose.ui.layout.Layout(
            content = {
                details.forEach { (icon, text) ->
                    DetailPill(icon, text, artworkColors.secondary)
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

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = artworkColors.secondary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = artworkColors.secondary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = song.path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun DetailPill(icon: ImageVector, text: String, color: Color) {
    SuggestionChip(
        onClick = { },
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color) },
        shape = RoundedCornerShape(16.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null
    )
}
