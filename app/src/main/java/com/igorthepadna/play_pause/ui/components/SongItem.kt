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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
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
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.igorthepadna.play_pause.R
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors

@Composable
private fun SongArtwork(
    song: Song,
    hasTrackInfo: Boolean,
    providedArtworkUri: android.net.Uri?,
    modifier: Modifier = Modifier,
    size: Int = 160
) {
    val context = LocalContext.current
    
    // Fallback chain logic:
    // 1. If it's an unnumbered track, try the song's own URI (embedded art) first.
    // 2. Use the explicitly provided art if available (passed from album context).
    // 3. Fallback to the album's artwork URI.
    // 4. Try the song's URI as a last resort.
    val artworkChain = remember(song.id, providedArtworkUri, hasTrackInfo) {
        val list = mutableListOf<android.net.Uri>()
        if (!hasTrackInfo) {
            list.add(song.uri)
        }
        providedArtworkUri?.let { if (it !in list) list.add(it) }
        song.albumArtUri?.let { if (it !in list) list.add(it) }
        if (song.uri !in list) list.add(song.uri)
        list
    }

    var loadIndex by remember(song.id) { mutableIntStateOf(0) }
    val currentUri = artworkChain.getOrNull(loadIndex)

    if (currentUri != null) {
        key(song.id, loadIndex) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentUri)
                    .crossfade(true)
                    .size(size)
                    .build(),
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        loadIndex++
                    }
                }
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Success) {
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(size.dp / 5),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onSwipePlayNext: () -> Unit,
    onSwipeAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToArtist: ((String) -> Unit)? = null,
    showArtist: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    label: String? = null,
    secondaryLabel: String? = null,
    artworkUri: android.net.Uri? = null,
    containerColor: Color? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null
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
            
            if (progress > 0.01f && direction != null && direction != SwipeToDismissBoxValue.Settled) {
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
                    else -> Icons.AutoMirrored.Rounded.PlaylistPlay
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .clip(shape)
                        .background(color.copy(alpha = progress))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(20.dp)
                            .align(if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd),
                        tint = contentColorFor(color)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        val backgroundColor = containerColor ?: if (isPlaying) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
        else 
            Color.Transparent

        Surface(
            modifier = modifier
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
                // ALWAYS leading Content (Icon, Custom or Artwork)
                if (leadingContent != null) {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent
                    ) {
                        leadingContent()
                    }
                } else if (leadingIcon != null) {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    SongArtwork(
                        song = song,
                        hasTrackInfo = false, // Universal tile ignores track numbers
                        providedArtworkUri = artworkUri,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        size = 80
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label ?: song.title,
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
                                val artistText = secondaryLabel ?: song.artist
                                Text(
                                    text = " • $artistText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (showArtist) {
                        val artistText = secondaryLabel ?: song.artist
                        val artists = remember(artistText) { MusicRepository.splitArtists(artistText) }
                        if (artists.size > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                artists.forEachIndexed { index, artist ->
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (index < artists.size - 1) {
                                        Text(
                                            text = " & ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = artistText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(onClick = onDetailsClick) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Song options",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onSwipePlayNext: () -> Unit,
    onSwipeAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToArtist: ((String) -> Unit)? = null,
    label: String? = null,
    secondaryLabel: String? = null,
    artworkUri: android.net.Uri? = null,
    containerColor: Color? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null
) {
    UniversalSongItem(
        song = song,
        isPlaying = isPlaying,
        onClick = onClick,
        onDetailsClick = onDetailsClick,
        onSwipePlayNext = onSwipePlayNext,
        onSwipeAddToPlaylist = onSwipeAddToPlaylist,
        modifier = modifier,
        onNavigateToArtist = onNavigateToArtist,
        label = label,
        secondaryLabel = secondaryLabel,
        artworkUri = artworkUri,
        containerColor = containerColor,
        leadingIcon = leadingIcon,
        leadingContent = leadingContent
    )
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
    onNavigateToArtist: ((String) -> Unit)? = null,
    showArtist: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    label: String? = null,
    secondaryLabel: String? = null,
    artworkUri: android.net.Uri? = null,
    onPlayClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null
) {
    UniversalSongItem(
        song = song,
        isPlaying = isPlaying,
        onClick = onClick,
        onDetailsClick = onDetailsClick,
        onSwipePlayNext = onSwipePlayNext,
        onSwipeAddToPlaylist = onSwipeAddToPlaylist,
        modifier = modifier,
        onNavigateToArtist = onNavigateToArtist,
        showArtist = showArtist,
        shape = shape,
        label = label,
        secondaryLabel = secondaryLabel,
        artworkUri = artworkUri,
        containerColor = containerColor,
        leadingIcon = leadingIcon,
        leadingContent = leadingContent
    )
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
                val artists = remember(song.artist) { MusicRepository.splitArtists(song.artist) }
                if (artists.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        artists.forEachIndexed { index, artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = artworkColors.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            if (index < artists.size - 1) {
                                Text(
                                    text = " & ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = artworkColors.secondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = artworkColors.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
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
