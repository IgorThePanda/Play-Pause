package com.igorthepadna.play_pause.ui.components

import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GenreCard(
    genre: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commonRounding = 16.dp

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
            // 1. Visual Section (Genre Icon/Placeholder)
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(commonRounding))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Style,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )

                // Themed Song Count Pill
                val isLightMode = MaterialTheme.colorScheme.surface.toArgb().let { colorInt ->
                    val hsl = FloatArray(3)
                    androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
                    hsl[2] > 0.5f
                }
                
                val accentColor = MaterialTheme.colorScheme.primary
                val pillBgColor = if (isLightMode) {
                    accentColor.copy(alpha = 0.4f)
                } else {
                    accentColor.copy(alpha = 0.7f)
                }

                val pillContentColor = if (isLightMode) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    Color.White
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 10.dp, start = 10.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                ) {
                    // Blur layer using accent color
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(16.dp)
                            .background(pillBgColor)
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
                            tint = pillContentColor
                        )
                        Text(
                            text = songCount.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = pillContentColor
                            )
                        )
                    }
                }
            }

            // 2. Metadata Segment
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(commonRounding),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Genre",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
