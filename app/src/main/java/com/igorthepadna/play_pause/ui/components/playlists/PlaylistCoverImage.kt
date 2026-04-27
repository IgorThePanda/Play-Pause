package com.igorthepadna.play_pause.ui.components.playlists

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.R

@Composable
fun PlaylistCoverImage(
    coverUri: Uri?,
    songCovers: List<Uri?>,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_launcher_foreground)
            )
        } else {
            val distinctCovers = songCovers.filterNotNull().distinct().take(4)
            if (distinctCovers.size >= 4) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(model = distinctCovers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = distinctCovers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        AsyncImage(model = distinctCovers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = distinctCovers[3], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                }
            } else if (distinctCovers.isNotEmpty()) {
                AsyncImage(
                    model = distinctCovers[0],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
