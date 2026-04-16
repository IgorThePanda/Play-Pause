package com.igorthepadna.play_pause.data

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class LyricWord(
    val timestamp: Long,
    val text: String
)

@Immutable
data class LyricLine(
    val timestamp: Long,
    val text: String,
    val speaker: String? = null,
    val words: List<LyricWord> = emptyList()
)

@Immutable
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
    val discNumber: Int = 1,
    val albumArtist: String? = null,
    val albumId: Long,
    val genre: String? = null,
    val bitrate: String? = null,
    val channels: String? = null,
    val year: Int = 0,
    val lyrics: String? = null
)

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songs: List<Song>,
    val year: Int = 0,
    val allCovers: List<Uri> = emptyList(),
    val hasFolderCover: Boolean = false
)

@Immutable
data class Artist(
    val name: String,
    val albums: List<Album>,
    val songs: List<Song>,
    val albumCount: Int,
    val trackCount: Int,
    val thumbnailUri: Uri? = null
)

@Immutable
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
    PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay),
    FILE_SYSTEM("Files", Icons.Rounded.Folder)
}

enum class SortType {
    TITLE, ARTIST, DURATION, RELEASE_DATE, DATE_ADDED, ALBUM_COUNT, TRACK_COUNT
}

enum class SortOrder {
    ASC, DESC
}
