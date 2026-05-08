package com.igorthepadna.play_pause.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SongDetailsContent(
    song: Song,
    artworkColors: ArtworkColors,
    onLyricClick: () -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onNavigateToArtist: ((String) -> Unit)? = null,
    onNavigateToAlbum: ((Long) -> Unit)? = null,
    onNavigateToGenre: ((String) -> Unit)? = null
) {
    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    var genre by remember { mutableStateOf("Loading...") }
    var bitrate by remember { mutableStateOf("Loading...") }
    var lyricType by remember { mutableStateOf("Checking...") }

    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogText by remember { mutableStateOf<String?>(null) }
    var clickedPillRect by remember { mutableStateOf<Rect?>(null) }

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

            // Lyric detection logic
            val lrcFile = java.io.File(song.path.substringBeforeLast(".") + ".lrc")
            var rawLyrics: String? = null
            var source = ""

            if (lrcFile.exists()) {
                source = "LRC File"
                rawLyrics = try { lrcFile.readText() } catch (e: Exception) { null }
            }

            if (rawLyrics == null) {
                val lyrRetriever = MediaMetadataRetriever()
                try {
                    lyrRetriever.setDataSource(song.path)
                    rawLyrics = lyrRetriever.extractMetadata(1000)
                    if (rawLyrics != null) source = "Embedded"
                } catch (_: Exception) {
                } finally {
                    lyrRetriever.release()
                }
            }

            lyricType = if (rawLyrics != null) {
                val hasWordSync = rawLyrics.contains(Regex("<\\d{1,2}:\\d{1,2}"))
                val hasLineSync = rawLyrics.contains(Regex("\\[\\d{1,2}:\\d{1,2}"))
                
                when {
                    hasWordSync -> "$source (Word-synced)"
                    hasLineSync -> "$source (Verse-synced)"
                    else -> "$source (Plain text)"
                }
            } else {
                "None found"
            }
        }
    }

    // Define details here so they are accessible to both the Layout and the Popup
    val details = listOfNotNull(
        Triple(Icons.Rounded.Album, song.album, "Album"),
        Triple(Icons.Rounded.MusicNote, genre, "Genre"),
        Triple(Icons.Rounded.Timer, formatDuration(song.duration), "Duration"),
        Triple(Icons.Rounded.GraphicEq, bitrate, "Bitrate"),
        Triple(Icons.Rounded.FormatShapes, song.format.substringAfter("/").uppercase(), "Format"),
        Triple(Icons.Rounded.Lyrics, lyricType, "Lyrics"),
        Triple(Icons.Rounded.Numbers, "Track ${if (song.trackNumber > 0) song.trackNumber else "N/A"}", "Track"),
        if (song.discNumber > 0) Triple(Icons.Rounded.Album, "Disc ${song.discNumber}", "Disc") else null,
        Triple(Icons.Rounded.SdStorage, String.format(Locale.getDefault(), "%.2f MB", song.size / (1024f * 1024f)), "Size")
    )

    val guides = mapOf(
        "Bitrate" to "The amount of data processed per unit of time. Higher bitrate generally means better audio quality but larger file size.",
        "Format" to "The file extension or codec used to encode the audio. Common formats include MP3 (lossy), FLAC (lossless), and WAV (uncompressed).",
        "Duration" to "The total length of the audio track.",
        "Size" to "The disk space occupied by the audio file on your device.",
        "Track" to "The position of the song within an album or disc.",
        "Disc" to "The disc number if the album consists of multiple discs.",
        "Lyrics" to "The type of lyrics synchronization found for this song."
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryAnim.value
                translationY = (100.dp.toPx() * (1f - entryAnim.value))
            }
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
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val artistText = song.artist
                ArtistSubtitle(
                    artistText = artistText,
                    style = MaterialTheme.typography.titleLarge,
                    mainColor = artworkColors.secondary,
                    modifier = if (onNavigateToArtist != null) Modifier.clickable { onNavigateToArtist(artistText) } else Modifier
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
                color = artworkColors.secondary
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        androidx.compose.ui.layout.Layout(
            content = {
                details.forEach { (icon, text, type) ->
                    DetailPill(
                        icon = icon,
                        text = text,
                        color = artworkColors.secondary,
                        onClick = { rect ->
                            when (type) {
                                "Album" -> onNavigateToAlbum?.invoke(song.albumId)
                                "Genre" -> if (genre != "Unknown" && genre != "Loading...") onNavigateToGenre?.invoke(genre)
                                "Lyrics" -> onLyricClick()
                                else -> {
                                    clickedPillRect = rect
                                    infoDialogTitle = type
                                    infoDialogText = guides[type]
                                }
                            }
                        }
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFolderClick(song.path) },
            shape = RoundedCornerShape(20.dp),
            color = artworkColors.secondary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp), tint = artworkColors.secondary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "File Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = artworkColors.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
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

    if (infoDialogTitle != null) {
        Popup(
            onDismissRequest = { infoDialogTitle = null },
            properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { infoDialogTitle = null },
                contentAlignment = Alignment.Center
            ) {
                val animProgress = remember { Animatable(0f) }
                
                LaunchedEffect(infoDialogTitle) {
                    animProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .widthIn(max = 400.dp)
                        .graphicsLayer {
                            val lerp = animProgress.value
                            scaleX = 0.6f + (0.4f * lerp)
                            scaleY = 0.6f + (0.4f * lerp)
                            alpha = lerp
                            
                            clickedPillRect?.let { rect ->
                                // Calculate translation to center from the pill's position
                                // This is a rough approximation since we're in a Popup
                                val screenCenterX = size.width / 2
                                val screenCenterY = size.height / 2
                                val pillCenterX = rect.center.x
                                val pillCenterY = rect.center.y
                                
                                translationX = (pillCenterX - screenCenterX) * (1f - lerp)
                                translationY = (pillCenterY - screenCenterY) * (1f - lerp)
                            }
                        }
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = artworkColors.secondary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = details.find { it.third == infoDialogTitle }?.first ?: Icons.Rounded.Info
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = artworkColors.secondary
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            text = infoDialogTitle!!,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            text = infoDialogText ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = { infoDialogTitle = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = artworkColors.secondary
                            )
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPill(
    icon: ImageVector, 
    text: String, 
    color: Color, 
    onPositioned: (Rect) -> Unit = {},
    onClick: ((Rect) -> Unit)? = null
) {
    var layoutRect by remember { mutableStateOf<Rect?>(null) }
    
    SuggestionChip(
        onClick = { layoutRect?.let { onClick?.invoke(it) } },
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color) },
        shape = RoundedCornerShape(16.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = null,
        modifier = Modifier.onGloballyPositioned { 
            val rect = it.boundsInWindow()
            layoutRect = rect
            onPositioned(rect)
        }
    )
}
