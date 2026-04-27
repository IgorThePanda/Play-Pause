package com.igorthepadna.play_pause.ui.components.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.data.Playlist

@Composable
fun PlaylistView(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    albumArtMap: Map<Long, android.net.Uri?>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    columns: Int = 2
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(playlists) { playlist ->
            PlaylistCard(
                playlist = playlist, 
                onClick = { onPlaylistClick(playlist) },
                albumArtMap = albumArtMap
            )
        }
        item {
            CreatePlaylistCard(onClick = onCreatePlaylist)
        }
    }
}

@Composable
fun CreatePlaylistCard(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Create Playlist",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Create playlist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Text(
            text = "Tap to start",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    albumArtMap: Map<Long, android.net.Uri?>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            PlaylistCoverImage(
                coverUri = playlist.coverUri,
                songCovers = playlist.songs.map { albumArtMap[it] },
                iconSize = 48.dp
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Text(
            text = "${playlist.songs.size} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
