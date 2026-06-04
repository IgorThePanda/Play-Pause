package com.igorthepadna.play_pause.ui.components.playlists

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igorthepadna.play_pause.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailGeneratorSheet(
    playlistId: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    
    val icons = listOf(
        Icons.Rounded.MusicNote,
        Icons.Rounded.Favorite,
        Icons.Rounded.Headset,
        Icons.Rounded.LibraryMusic,
        Icons.Rounded.Album,
        Icons.Rounded.AudioFile,
        Icons.Rounded.Mic,
        Icons.Rounded.Piano,
        Icons.Rounded.QueueMusic,
        Icons.Rounded.Radio,
        Icons.Rounded.Star,
        Icons.Rounded.Cloud
    )

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF607D8B)  // Blue Grey
    )

    var selectedIcon by remember { mutableStateOf(icons[0]) }
    var selectedColor by remember { mutableStateOf(colors[0]) }
    
    val painter = rememberVectorPainter(selectedIcon)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "GENERATE THUMBNAIL",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Preview
        Surface(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp)),
            color = selectedColor,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Select Icon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier.height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(icons) { icon ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { selectedIcon = icon }
                        .border(
                            width = 2.dp,
                            color = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (selectedIcon == icon) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Select Color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(40.dp),
            modifier = Modifier.height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { selectedColor = color }
                        .border(
                            width = 3.dp,
                            color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val size = 512
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val composeCanvas = Canvas(bitmap.asImageBitmap())
                val drawScope = CanvasDrawScope()
                
                drawScope.draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = composeCanvas,
                    size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat())
                ) {
                    drawRect(color = selectedColor)
                    val iconSize = size * 0.6f
                    val offset = (size - iconSize) / 2
                    
                    inset(offset, offset) {
                        with(painter) {
                            draw(
                                size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                                alpha = 1f,
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                            )
                        }
                    }
                }

                viewModel.setPlaylistCover(playlistId, bitmap)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Generate & Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun ThumbnailGeneratorSheetPreview() {
    MaterialTheme {
        Surface {
            // Mock ViewModel would be complex, just showing the layout with a null-safe approach if possible
            // But since it's a preview, I'll just mock the call.
        }
    }
}
