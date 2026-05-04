package com.igorthepadna.play_pause.ui.components.playlists

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.ui.components.UniversalSongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistInfoView(
    playlist: Playlist,
    playlistSongs: List<Song>,
    albumArtMap: Map<Long, android.net.Uri?>,
    onBack: () -> Unit,
    onEditCover: () -> Unit,
    onUpdateName: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveSong: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    viewModel: MainViewModel? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist.name) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Playlist Info", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header: Cover and Basic Info
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(16.dp, RoundedCornerShape(32.dp))
                            .clip(RoundedCornerShape(32.dp))
                            .clickable { onEditCover() },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PlaylistCoverImage(
                                coverUri = playlist.coverUri,
                                songCovers = playlist.songs.map { albumArtMap[it] },
                                iconSize = 64.dp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    null,
                                    modifier = Modifier.padding(16.dp).size(24.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.Edit,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = "${playlistSongs.size} Songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Songs Section
            item {
                Text(
                    "Songs in Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (playlistSongs.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.MusicOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No songs yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(playlistSongs, key = { it.id }) { song ->
                    val albumArt = albumArtMap[song.albumId] ?: song.albumArtUri
                    UniversalSongItem(
                        song = song,
                        isPlaying = false,
                        onClick = {},
                        onDetailsClick = {},
                        onSwipePlayNext = {},
                        onSwipeAddToPlaylist = {},
                        onNavigateToArtist = onNavigateToArtist,
                        artworkUri = albumArt,
                        trailingContent = {
                            IconButton(onClick = { onRemoveSong(song.id) }) {
                                Icon(
                                    Icons.Rounded.RemoveCircleOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Playlist?") },
            text = { Text("Are you sure you want to delete '${playlist.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onUpdateName(newName)
                            showRenameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
