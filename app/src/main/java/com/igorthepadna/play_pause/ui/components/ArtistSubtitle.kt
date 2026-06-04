package com.igorthepadna.play_pause.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
    
    // Safety check for empty text
    if (artistText.isBlank()) return

    // Simple and safe auto-scaling: 
    // We use a local state for font size that starts at the style's size.
    // We only shrink it if an overflow is detected, with a hard floor.
    var currentFontSize by remember(artistText, style.fontSize) { mutableStateOf(style.fontSize) }
    var readyToDraw by remember(artistText) { mutableStateOf(false) }

    val currentStyle = style.copy(fontSize = currentFontSize)

    if (artists.size > 1) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .drawWithContent { if (readyToDraw) drawContent() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            artists.forEachIndexed { index, artist ->
                Text(
                    text = artist,
                    style = currentStyle,
                    color = mainColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.didOverflowWidth) {
                            if (currentFontSize.value > 8f) {
                                currentFontSize = (currentFontSize.value - 0.5f).sp
                            } else {
                                readyToDraw = true
                            }
                        } else {
                            readyToDraw = true
                        }
                    },
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
                        style = currentStyle,
                        color = separatorColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    } else {
        Text(
            text = artistText,
            style = currentStyle,
            color = mainColor,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Visible,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowWidth) {
                    if (currentFontSize.value > 8f) {
                        currentFontSize = (currentFontSize.value - 0.5f).sp
                    } else {
                        readyToDraw = true
                    }
                } else {
                    readyToDraw = true
                }
            },
            modifier = modifier
                .drawWithContent { if (readyToDraw) drawContent() }
                .then(
                    if (onArtistClick != null) {
                        Modifier.clickable { onArtistClick(artistText) }
                    } else {
                        Modifier
                    }
                )
        )
    }
}
