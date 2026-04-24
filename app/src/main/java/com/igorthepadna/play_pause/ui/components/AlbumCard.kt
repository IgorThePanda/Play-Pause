package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.utils.rememberArtworkColors

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    isPlaying: Boolean = false,
    onNavigateToArtist: ((String) -> Unit)? = null
) {
    AlbumCard(
        title = album.title,
        artist = album.artist,
        artworkUri = album.artworkUri,
        onClick = onClick,
        onPlayClick = onPlayClick,
        modifier = modifier,
        columns = columns,
        isPlaying = isPlaying,
        songCount = album.songs.size,
        allCovers = album.allCovers,
        hasFolderCover = album.hasFolderCover,
        onNavigateToArtist = onNavigateToArtist
    )
}

@Composable
fun AlbumCard(
    title: String,
    artist: String,
    artworkUri: android.net.Uri?,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    isPlaying: Boolean = false,
    songCount: Int = 0,
    allCovers: List<android.net.Uri> = emptyList(),
    hasFolderCover: Boolean = true,
    onNavigateToArtist: ((String) -> Unit)? = null
) {
    val artworkColors = rememberArtworkColors(
        artworkUri = artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surfaceContainerHigh,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val showDetails = columns <= 2
    val showMetadata = columns < 4
    val showArtist = columns <= 2
    val showTitle = columns <= 3

    val rounding = when {
        columns <= 1 -> 24.dp
        columns == 2 -> 16.dp
        columns == 3 -> 12.dp
        else -> 8.dp
    }

    // Outer container - Optimized Surface usage
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (showMetadata) 28.dp else rounding),
        color = when {
            isPlaying -> artworkColors.secondary.copy(alpha = 0.15f)
            showMetadata -> MaterialTheme.colorScheme.surfaceContainerLowest
            else -> Color.Transparent
        },
        border = if (isPlaying) BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.5f)) else null,
        tonalElevation = if (showMetadata || isPlaying) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(if (showDetails) 10.dp else if (showMetadata) 6.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(if (showDetails) 10.dp else 6.dp)
        ) {
            // 1. Artwork Section
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(rounding))
            ) {
                val showGrid = !hasFolderCover && allCovers.size > 1
                
                if (showGrid) {
                    val displayCovers = allCovers.take(4)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AlbumCoverImage(displayCovers.getOrNull(0), size = if (columns > 2) 80 else 150, modifier = Modifier.weight(1f).fillMaxHeight())
                            AlbumCoverImage(displayCovers.getOrNull(1), size = if (columns > 2) 80 else 150, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AlbumCoverImage(displayCovers.getOrNull(2), size = if (columns > 2) 80 else 150, modifier = Modifier.weight(1f).fillMaxHeight())
                            AlbumCoverImage(displayCovers.getOrNull(3), size = if (columns > 2) 80 else 150, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                } else {
                    AlbumCoverImage(artworkUri, size = if (columns > 2) 200 else 400)
                }

                if (showDetails) {
                    val accentColor = artworkColors.secondary
                    val isLightMode = MaterialTheme.colorScheme.surface.toArgb().let { colorInt ->
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
                        hsl[2] > 0.5f
                    }
                    
                    val overlayBgColor = if (isLightMode) accentColor.copy(alpha = 0.5f) else accentColor.copy(alpha = 0.8f)
                    val contentColor = if (isLightMode) {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(accentColor.toArgb(), hsl)
                        if (hsl[2] > 0.6f) Color.Black else Color.White
                    } else Color.White

                    // 1. Song Count Pill (No Blur)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .height(30.dp),
                        shape = CircleShape,
                        color = overlayBgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp).fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(14.dp), tint = contentColor)
                            Text(
                                text = songCount.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.sp, color = contentColor),
                                modifier = Modifier.clearAndSetSemantics { }
                            )
                        }
                    }

                    // 2. Quick Play Button (No Blur)
                    if (onPlayClick != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(36.dp),
                            shape = CircleShape,
                            color = overlayBgColor,
                            onClick = onPlayClick
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, "Play Album $title", modifier = Modifier.size(22.dp), tint = contentColor)
                            }
                        }
                    }
                }
            }

            // 2. Metadata Segment
            if (showMetadata) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(rounding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = if (showDetails) 12.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val isLightMode = MaterialTheme.colorScheme.surface.toArgb().let {
                                val hsl = FloatArray(3)
                                androidx.core.graphics.ColorUtils.colorToHSL(it, hsl)
                                hsl[2] > 0.5f
                            }

                            val titleColor = remember(artworkColors.secondary, isLightMode) {
                                val hsl = FloatArray(3)
                                androidx.core.graphics.ColorUtils.colorToHSL(artworkColors.secondary.toArgb(), hsl)
                                if (isLightMode) {
                                    if (hsl[2] > 0.4f) hsl[2] = 0.3f
                                    hsl[1] = (hsl[1] + 0.2f).coerceAtMost(1f)
                                } else {
                                    if (hsl[2] < 0.6f) hsl[2] = 0.8f
                                }
                                Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                            }

                            if (showTitle) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = (-0.2).sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = titleColor
                                )
                            }
                            if (showArtist) {
                                val artists = remember(artist) { MusicRepository.splitArtists(artist) }
                                if (artists.size > 1) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        artists.forEachIndexed { index, artistName ->
                                            Text(
                                                text = artistName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isLightMode) Color.DarkGray else Color.LightGray),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (index < artists.size - 1) {
                                                Text(
                                                    text = " & ",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = (if (isLightMode) Color.DarkGray else Color.LightGray).copy(alpha = 0.5f))
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isLightMode) Color.DarkGray else Color.LightGray),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCoverImage(model: Any?, size: Int = 400, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(100)
            .size(size)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build(),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = painterResource(R.drawable.ic_launcher_foreground),
        placeholder = painterResource(R.drawable.ic_launcher_foreground)
    )
}
