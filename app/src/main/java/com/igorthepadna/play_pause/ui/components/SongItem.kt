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
import androidx.compose.material3.LoadingIndicator
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
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onSwipePlayNext: () -> Unit,
    onSwipeAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState()

    // Trigger actions and snap back
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            when (dismissState.currentValue) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipePlayNext()
                SwipeToDismissBoxValue.EndToStart -> onSwipeAddToPlaylist()
                SwipeToDismissBoxValue.Settled -> {}
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val progress = dismissState.progress
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            
            if (isSwiping && direction != null) {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondary
                        else -> Color.Transparent
                    }, label = "swipe_bg"
                )
                
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.AutoMirrored.Rounded.PlaylistPlay
                    SwipeToDismissBoxValue.EndToStart -> Icons.AutoMirrored.Rounded.PlaylistAdd
                    SwipeToDismissBoxValue.Settled -> Icons.AutoMirrored.Rounded.PlaylistPlay
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(color.copy(alpha = progress))
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.scale(progress.coerceIn(0.7f, 1.3f)),
                        tint = Color.White.copy(alpha = progress)
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
                            LoadingIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
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
fun CompactSongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onSwipePlayNext: () -> Unit,
    onSwipeAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    showArtist: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            when (dismissState.currentValue) {
                SwipeToDismissBoxValue.StartToEnd -> onSwipePlayNext()
                SwipeToDismissBoxValue.EndToStart -> onSwipeAddToPlaylist()
                SwipeToDismissBoxValue.Settled -> {}
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val progress = dismissState.progress
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            
            if (isSwiping && direction != null) {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondary
                        else -> Color.Transparent
                    }, label = "compact_swipe_bg"
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(color.copy(alpha = progress))
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        Icons.AutoMirrored.Rounded.PlaylistPlay
                    else Icons.AutoMirrored.Rounded.PlaylistAdd
                    
                    Icon(
                        icon, 
                        null, 
                        tint = Color.White.copy(alpha = progress), 
                        modifier = Modifier.size(20.dp).scale(progress.coerceIn(0.8f, 1.2f))
                    )
                }
            }
        },
        modifier = modifier
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            label = "compact_song_bg"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showArtist && !isPlaying) 58.dp else 52.dp)
                .clickable { onClick() },
            shape = shape,
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track Number Pill
                if (song.trackNumber > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(width = 30.dp, height = 20.dp),
                        shape = CircleShape,
                        color = if (isPlaying) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = song.trackNumber.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = if (isPlaying) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                    )
                    if (isPlaying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Now Playing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (showArtist) {
                                Text(
                                    text = " • ${song.artist}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (showArtist) {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDetailsClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.MoreVert, 
                        contentDescription = "Details",
                        modifier = Modifier.size(20.dp),
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
