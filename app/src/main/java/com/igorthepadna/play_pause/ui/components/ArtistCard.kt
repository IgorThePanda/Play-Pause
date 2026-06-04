package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.Artist
import com.igorthepadna.play_pause.utils.rememberArtworkColors

@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    isPlaying: Boolean = false
) {
    val showMetadata = columns < 4
    val showDetails = columns <= 2
    val showPlayButton = columns <= 3

    val artworkColors = rememberArtworkColors(
        artworkUri = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surfaceContainerHigh,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple()
            )
            .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(if (showMetadata) 8.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isPlaying) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
            tonalElevation = 2.dp
        ) {
            val model = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri
            var isError by remember { mutableStateOf(false) }

            Box(contentAlignment = Alignment.Center) {
                if (model != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(model)
                            .crossfade(100)
                            .size(400) // Optimization: Loaded at fixed size for the grid
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build(),
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { isError = true },
                        onSuccess = { isError = false }
                    )
                }
                
                if (model == null || isError) {
                    ArtistPlaceholder(artist.name)
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(if (columns <= 2) 32.dp else 24.dp)
                        )
                    }
                } else if (onPlayClick != null && showPlayButton) {
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

                    val buttonSize = if (columns <= 2) 42.dp else 36.dp
                    val iconSize = if (columns <= 2) 24.dp else 20.dp

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(if (columns <= 2) 8.dp else 6.dp)
                                .size(buttonSize),
                            shape = CircleShape,
                            color = overlayBgColor,
                            contentColor = contentColor,
                            onClick = onPlayClick,
                            tonalElevation = 4.dp,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                }
            }
        }
        if (showMetadata) {
            Spacer(modifier = Modifier.height(if (columns <= 2) 12.dp else 8.dp))
            ArtistSubtitle(
                artistText = artist.name,
                style = when {
                    columns <= 2 -> MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    columns == 3 -> MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    else -> MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center
                    )
                },
                mainColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
            if (showDetails) {
                Text(
                    text = "${artist.albumCount} Albums • ${artist.trackCount} Songs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ArtistPlaceholder(name: String) {
    val firstLetter = name.trim().firstOrNull()?.uppercase() ?: "?"
    
    // Generate a consistent color based on the name
    val containerColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer
    )
    val onContainerColors = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.onErrorContainer
    )
    
    val colorIndex = remember(name) { Math.abs(name.hashCode()) % containerColors.size }
    val backgroundColor = containerColors[colorIndex]
    val contentColor = onContainerColors[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstLetter,
            style = MaterialTheme.typography.displayMedium,
            color = contentColor,
            fontWeight = FontWeight.Black
        )
    }
}
