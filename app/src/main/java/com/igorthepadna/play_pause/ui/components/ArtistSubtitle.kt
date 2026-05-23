package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.igorthepadna.play_pause.data.MusicRepository

@Composable
fun ArtistSubtitle(
    artistText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    mainColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    separatorColor: Color = mainColor.copy(alpha = 0.5f),
    maxLines: Int = 1,
    onArtistClick: ((String) -> Unit)? = null
) {
    val artists = remember(artistText) { MusicRepository.splitArtists(artistText) }
    
    if (artists.size > 1) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            artists.forEachIndexed { index, artist ->
                Text(
                    text = artist,
                    style = style,
                    color = mainColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = (if (index == 0) Modifier.weight(1f, fill = false) else Modifier)
                        .then(
                            if (onArtistClick != null) {
                                Modifier.clickable { onArtistClick(artist) }
                            } else {
                                Modifier
                            }
                        )
                )
                if (index < artists.size - 1) {
                    Text(
                        text = " & ",
                        style = style,
                        color = separatorColor,
                        maxLines = 1
                    )
                }
            }
        }
    } else {
        Text(
            text = artistText,
            style = style,
            color = mainColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.then(
                if (onArtistClick != null) {
                    Modifier.clickable { onArtistClick(artistText) }
                } else {
                    Modifier
                }
            )
        )
    }
}
