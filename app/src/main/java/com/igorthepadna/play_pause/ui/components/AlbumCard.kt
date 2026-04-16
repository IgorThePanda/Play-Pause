package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
    modifier: Modifier = Modifier
) {
    val artworkColors = rememberArtworkColors(
        artworkUri = album.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surfaceContainerHigh,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val commonRounding = 16.dp // Matching rounding for artwork and metadata pill

    // Outer container
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Artwork Section
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(commonRounding),
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

                // Blurred Accent Pill for Song Count
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(artworkColors.secondary.copy(alpha = 0.4f)) // Semi-transparent accent
                        .blur(16.dp) // Blur effect
                )

                // Actual content container (to keep icons/text sharp)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .height(30.dp),
                    shape = CircleShape,
                    color = artworkColors.secondary.copy(alpha = 0.7f), // Higher opacity for contrast
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val pillTextColor = remember(artworkColors.secondary) {
                            val hsl = FloatArray(3)
                            androidx.core.graphics.ColorUtils.colorToHSL(artworkColors.secondary.toArgb(), hsl)
                            if (hsl[2] > 0.6f) Color.Black else Color.White
                        }

                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = pillTextColor
                        )
                        Text(
                            text = album.songs.size.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            ),
                            color = pillTextColor
                        )
                    }
                }
            }

            // 2. Metadata Segment
            // Use a slightly darker/more opaque background to ensure contrast in light mode
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(commonRounding),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(artworkColors.secondary)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

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
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isLightMode) Color.DarkGray else Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCoverImage(model: Any?, size: Int = 400) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .crossfade(true)
            .size(size)
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        error = painterResource(R.drawable.ic_launcher_foreground),
        placeholder = painterResource(R.drawable.ic_launcher_foreground)
    )
}
