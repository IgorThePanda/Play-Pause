package com.igorthepadna.play_pause.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUri: String?,
    val path: String,
    val size: Long,
    val format: String,
    val dateAdded: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val albumArtist: String?,
    val albumId: Long,
    val year: Int
)

@Entity(tableName = "cached_albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val year: Int,
    val hasFolderCover: Boolean
)

@Entity(tableName = "cached_artists")
data class ArtistEntity(
    @PrimaryKey val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val thumbnailUri: String?
)

@Serializable
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSongEntity(
    val playlistId: String,
    val songId: Long,
    val position: Int
)

@Serializable
data class PlaylistBackup(
    val playlists: List<PlaylistEntity>,
    val playlistSongs: List<PlaylistSongEntity>
)
