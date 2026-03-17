package com.igorthepadna.play_pause.data

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val path: String,
    val size: Long,
    val format: String,
    val dateAdded: Long,
    val trackNumber: Int,
    val albumId: Long,
    var genre: String? = null,
    var bitrate: String? = null,
    var channels: String? = null,
    var year: Int = 0
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songs: List<Song>,
    val year: Int = 0
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Long> = emptyList(),
    val isFavorite: Boolean = false
)

enum class LibraryFilter(val label: String, val icon: ImageVector) {
    ALBUMS("Albums", Icons.Rounded.Album),
    SONGS("Songs", Icons.Rounded.MusicNote),
    ARTISTS("Artists", Icons.Rounded.Person),
    GENRES("Genres", Icons.Rounded.GraphicEq),
    PLAYLISTS("Playlists", Icons.Rounded.PlaylistPlay),
    FILE_SYSTEM("Files", Icons.Rounded.Folder)
}

enum class SortType {
    TITLE, ARTIST, DURATION, RELEASE_DATE, DATE_ADDED
}

enum class SortOrder {
    ASC, DESC
}
