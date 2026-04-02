package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean, // Optimization: Boolean is stable, Long state change triggers mass recomposition
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onSwipePlayNext: () -> Unit,
    onSwipeAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipePlayNext()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeAddToPlaylist()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> 
            val threshold = with(density) { 100.dp.toPx() }
            maxOf(threshold, totalDistance * 0.25f)
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }, label = "swipe_bg"
            )
            
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.AutoMirrored.Rounded.PlaylistPlay
                SwipeToDismissBoxValue.EndToStart -> Icons.AutoMirrored.Rounded.PlaylistAdd
                else -> null
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (icon != null) {
                    val scale by animateFloatAsState(
                        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) 1.4f else 1f,
                        label = "icon_scale"
                    )
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.scale(scale),
                        tint = Color.White
                    )
                }
            }
        },
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
            label = "song_item_bg"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.albumArtUri)
                            .crossfade(true)
                            .size(160) // Optimize: Don't load full-res art for small thumbnails
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )
                    
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDetailsClick) {
                    Icon(
                        Icons.Default.MoreVert, 
                        contentDescription = "Details",
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionSheet(
    song: Song,
    playlists: List<Playlist>,
    artworkColors: ArtworkColors,
    onPlaylistSelected: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onFavoriteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val isFavorite = remember(playlists, song.id) {
        playlists.find { it.id == "favorites" }?.songs?.contains(song.id) == true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = artworkColors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Add to Playlist",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onFavoriteClick,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) artworkColors.secondary else artworkColors.secondary.copy(alpha = 0.1f),
                    contentColor = if (isFavorite) contentColorFor(artworkColors.secondary) else artworkColors.secondary
                ),
                border = if (!isFavorite) BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.3f)) else null
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, 
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isFavorite) "Favorited" else "Favorite", fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = artworkColors.primary,
                    contentColor = contentColorFor(artworkColors.primary)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create New", fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "YOUR PLAYLISTS",
            style = MaterialTheme.typography.labelMedium.copy(
                color = artworkColors.secondary,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        playlists.filter { !it.isFavorite }.forEach { playlist ->
            Surface(
                onClick = { onPlaylistSelected(playlist.id) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                color = artworkColors.secondary.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(artworkColors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistPlay, 
                            contentDescription = null, 
                            tint = artworkColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        playlist.name, 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = artworkColors.primary,
                        focusedLabelColor = artworkColors.primary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(contentColor = artworkColors.primary)
                ) {
                    Text("Create", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}
