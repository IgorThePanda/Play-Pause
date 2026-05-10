package com.igorthepadna.play_pause.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId"
    )
    val songs: List<PlaylistSongEntity>
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM cached_songs")
    suspend fun getAllCachedSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM cached_songs")
    suspend fun clearSongs()

    @Transaction
    suspend fun deleteSongById(songId: Long) {
        deleteSongFromCache(songId)
        deleteSongFromPlaylists(songId)
    }

    @Query("DELETE FROM cached_songs WHERE id = :songId")
    suspend fun deleteSongFromCache(songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun deleteSongFromPlaylists(songId: Long)

    @Query("SELECT * FROM cached_albums")
    suspend fun getAllCachedAlbums(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM cached_albums")
    suspend fun clearAlbums()

    @Query("SELECT * FROM cached_artists")
    suspend fun getAllCachedArtists(): List<ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM cached_artists")
    suspend fun clearArtists()

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getSongsForPlaylist(playlistId: String): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long)

    @Transaction
    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        val currentMaxPosition = getMaxPosition(playlistId) ?: -1
        insertPlaylistSong(PlaylistSongEntity(playlistId, songId, currentMaxPosition + 1))
    }

    @Query("UPDATE playlists SET coverUri = :coverUri WHERE id = :playlistId")
    suspend fun updatePlaylistCover(playlistId: String, coverUri: String?)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: String, name: String)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs")
    suspend fun getAllPlaylistSongsSync(): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(playlistSongs: List<PlaylistSongEntity>)

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: String): Flow<PlaylistWithSongs?>

    @androidx.room.Transaction
    @androidx.room.Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithSongsSync(playlistId: String): PlaylistWithSongs?

    @androidx.room.Query("SELECT * FROM pinned_items ORDER BY addedAt DESC")
    fun getAllPinnedItems(): kotlinx.coroutines.flow.Flow<List<PinnedItemEntity>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPinnedItem(item: PinnedItemEntity)

    @androidx.room.Query("DELETE FROM pinned_items WHERE type = :type AND mediaId = :mediaId")
    suspend fun deletePinnedItem(type: String, mediaId: String)
}
