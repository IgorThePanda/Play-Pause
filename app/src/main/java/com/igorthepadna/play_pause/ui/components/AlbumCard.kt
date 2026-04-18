package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.igorthepadna.play_pause.utils.rememberArtworkColors

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2
) {
    val artworkColors = rememberArtworkColors(
        artworkUri = album.artworkUri,
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

    val commonRounding = rounding // Use dynamic rounding

    // Outer container
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (showMetadata) 28.dp else rounding),
        color = if (showMetadata) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        tonalElevation = if (showMetadata) 1.dp else 0.dp
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
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(rounding),
                    tonalElevation = 0.dp
                ) {
                    val showGrid = !album.hasFolderCover && album.allCovers.size > 1
                    
                    if (showGrid) {
                        val displayCovers = album.allCovers.take(4)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AlbumCoverImage(displayCovers.getOrNull(0), size = 300)
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AlbumCoverImage(displayCovers.getOrNull(1), size = 300)
                                }
                            }
                            Row(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AlbumCoverImage(displayCovers.getOrNull(2), size = 300)
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AlbumCoverImage(displayCovers.getOrNull(3), size = 300)
                                }
                            }
                        }
                    } else {
                        AlbumCoverImage(album.artworkUri, size = 500)
                    }
                }

                if (showDetails) {
                    val isLightMode = MaterialTheme.colorScheme.surface.toArgb().let { colorInt ->
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
                        hsl[2] > 0.5f
                    }

                    // Use artwork secondary color as accent
                    val accentColor = artworkColors.secondary
                    
                    val overlayBgColor = if (isLightMode) {
                        accentColor.copy(alpha = 0.4f)
                    } else {
                        accentColor.copy(alpha = 0.7f)
                    }

                    val contentColor = if (isLightMode) {
                        // Ensure contrast in light mode
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(accentColor.toArgb(), hsl)
                        if (hsl[2] > 0.6f) Color.Black else Color.White
                    } else {
                        Color.White
                    }

                    // 1. Song Count Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                    ) {
                        // Real Blur: Blurred artwork background
                        AlbumCoverImage(
                            album.artworkUri,
                            size = 100,
                            modifier = Modifier
                                .matchParentSize()
                                .blur(20.dp)
                        )
                        // Themed tint overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(overlayBgColor)
                        )
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = contentColor
                            )
                            Text(
                                text = album.songs.size.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = contentColor
                                ),
                                modifier = Modifier.clearAndSetSemantics { }
                            )
                        }
                    }

                    // 2. Quick Play Button
                    if (onPlayClick != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onPlayClick)
                        ) {
                            // Real Blur: Blurred artwork background
                            AlbumCoverImage(
                                album.artworkUri,
                                size = 100,
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(20.dp)
                            )
                            // Themed tint overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(overlayBgColor)
                            )
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Album ${album.title}",
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.Center),
                                tint = contentColor
                            )
                        }
                    }
                }
            }

            // 2. Metadata Segment
            // Use a slightly darker/more opaque background to ensure contrast in light mode
            if (showMetadata) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(commonRounding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = if (showDetails) 12.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Removed Vertical accent bar per request

                        Column(modifier = Modifier.weight(1f)) {
                            // Advanced Contrast Logic
                            val isLightMode = MaterialTheme.colorScheme.surface.toArgb().let {
                                val hsl = FloatArray(3)
                                androidx.core.graphics.ColorUtils.colorToHSL(it, hsl)
                                hsl[2] > 0.5f
                            }

                            val titleColor = remember(artworkColors.secondary, isLightMode) {
                                val hsl = FloatArray(3)
                                androidx.core.graphics.ColorUtils.colorToHSL(artworkColors.secondary.toArgb(), hsl)
                                
                                if (isLightMode) {
                                    // In light mode, ensure the color is dark enough
                                    if (hsl[2] > 0.4f) hsl[2] = 0.3f
                                    // Increase saturation for "pop"
                                    hsl[1] = (hsl[1] + 0.2f).coerceAtMost(1f)
                                } else {
                                    // In dark mode, ensure it's light enough
                                    if (hsl[2] < 0.6f) hsl[2] = 0.8f
                                }
                                Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                            }

                            if (showTitle) {
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = titleColor
                                )
                            }
                            if (showArtist) {
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isLightMode) Color.DarkGray else Color.LightGray
                                    ),
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

@Composable
private fun AlbumCoverImage(model: Any?, size: Int = 400, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(true)
            .size(size)
            .build(),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = painterResource(R.drawable.ic_launcher_foreground),
        placeholder = painterResource(R.drawable.ic_launcher_foreground)
    )
}
