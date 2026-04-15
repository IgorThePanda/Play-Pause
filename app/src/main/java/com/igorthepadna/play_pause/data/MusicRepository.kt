package com.igorthepadna.play_pause.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.igorthepadna.play_pause.data.db.AlbumEntity
import com.igorthepadna.play_pause.data.db.AppDatabase
import com.igorthepadna.play_pause.data.db.ArtistEntity
import com.igorthepadna.play_pause.data.db.PlaylistBackup
import com.igorthepadna.play_pause.data.db.PlaylistEntity
import com.igorthepadna.play_pause.data.db.PlaylistSongEntity
import com.igorthepadna.play_pause.data.db.SongEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MusicRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()

    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null

    private fun getMusicPaths(): List<String> {
        val paths = mutableListOf<String>()
        // Internal storage Music folder
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
        
        // SD Card and other external storage volumes
        val externalFilesDirs = context.getExternalFilesDirs(null)
        for (file in externalFilesDirs) {
            if (file != null) {
                val path = file.absolutePath
                if (path.contains("/Android/data/")) {
                    val rootPath = path.substringBefore("/Android/data/")
                    val musicPath = File(rootPath, Environment.DIRECTORY_MUSIC).absolutePath
                    if (!paths.contains(musicPath)) {
                        paths.add(musicPath)
                    }
                }
            }
        }
        return paths
    }

    suspend fun getSongs(useCache: Boolean = true, onlyMusicFolder: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (useCache) {
            val cached = playlistDao.getAllCachedSongs()
            if (cached.isNotEmpty()) {
                val songs = cached.map { it.toDomain() }
                // If we need to filter cache, we do it here
                val filtered = if (onlyMusicFolder) {
                    val musicPaths = getMusicPaths()
                    songs.filter { song -> musicPaths.any { song.path.startsWith(it) } }
                } else songs
                
                cachedSongs = filtered
                return@withContext filtered
            }
        }

        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )

        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = mutableListOf<String>()

        if (onlyMusicFolder) {
            val musicPaths = getMusicPaths()
            if (musicPaths.isNotEmpty()) {
                val pathSelection = musicPaths.joinToString(" OR ") { "${MediaStore.Audio.Media.DATA} LIKE ?" }
                selection += " AND ($pathSelection)"
                musicPaths.forEach { selectionArgs.add("$it%") }
            }
        }

        context.contentResolver.query(
            collection,
            projection,
            selection,
            if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val discColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val format = cursor.getString(mimeColumn) ?: ""
                val dateAddedValue = cursor.getLong(dateAddedColumn)
                val trackNumber = if (trackColumn != -1) cursor.getInt(trackColumn) else 0
                val discNumber = if (discColumn != -1) cursor.getInt(discColumn) else 1
                val albumId = cursor.getLong(albumIdColumn)
                val year = cursor.getInt(yearColumn)

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    uri = contentUri,
                    albumArtUri = albumArtUri,
                    path = path,
                    size = size,
                    format = format,
                    dateAdded = dateAddedValue,
                    trackNumber = trackNumber,
                    discNumber = if (discNumber == 0) 1 else discNumber,
                    albumId = albumId,
                    year = year,
                    bitrate = if (duration > 0) "${(size * 8 / duration).toInt()} kbps" else null,
                    lyrics = null
                )

                songList.add(song)
            }
        }
        
        // Background cache update
        playlistDao.clearSongs()
        playlistDao.insertSongs(songList.map { it.toEntity() })
        
        cachedSongs = songList
        cachedAlbums = null
        cachedArtists = null
        songList
    }

    private fun Song.toEntity() = SongEntity(
        id = id, title = title, artist = artist, album = album, duration = duration,
        uri = uri.toString(), albumArtUri = albumArtUri?.toString(), path = path,
        size = size, format = format, dateAdded = dateAdded, trackNumber = trackNumber,
        discNumber = discNumber, albumId = albumId, year = year
    )

    private fun SongEntity.toDomain() = Song(
        id = id, title = title, artist = artist, album = album, duration = duration,
        uri = Uri.parse(uri), albumArtUri = albumArtUri?.let { Uri.parse(it) }, path = path,
        size = size, format = format, dateAdded = dateAdded, trackNumber = trackNumber,
        discNumber = discNumber, albumId = albumId, year = year,
        bitrate = if (duration > 0) "${(size * 8 / duration).toInt()} kbps" else null
    )

    private fun Album.toEntity() = AlbumEntity(
        id = id, title = title, artist = artist, artworkUri = artworkUri?.toString(),
        year = year, hasFolderCover = hasFolderCover
    )

    private fun AlbumEntity.toDomain(songs: List<Song>) = Album(
        id = id, title = title, artist = artist, artworkUri = artworkUri?.let { Uri.parse(it) },
        songs = songs.filter { it.albumId == id }.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })),
        year = year, allCovers = songs.filter { it.albumId == id }.mapNotNull { it.albumArtUri }.distinct(),
        hasFolderCover = hasFolderCover
    )

    private fun Artist.toEntity() = ArtistEntity(
        name = name, albumCount = albumCount, trackCount = trackCount, thumbnailUri = thumbnailUri?.toString()
    )

    private fun ArtistEntity.toDomain(songs: List<Song>, albums: List<Album>) = Artist(
        name = name, albums = albums.filter { it.artist == name },
        songs = songs.filter { it.artist == name },
        albumCount = albumCount, trackCount = trackCount,
        thumbnailUri = thumbnailUri?.let { Uri.parse(it) }
    )

    suspend fun getAlbums(songs: List<Song>, useCache: Boolean = true): List<Album> = withContext(Dispatchers.IO) {
        if (useCache) {
            val cached = playlistDao.getAllCachedAlbums()
            if (cached.isNotEmpty()) {
                val albums = cached.map { it.toDomain(songs) }
                cachedAlbums = albums
                return@withContext albums
            }
        }
        
        if (cachedAlbums != null && songs === cachedSongs) return@withContext cachedAlbums!!

        val albums = songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val firstSong = albumSongs.first()
            val albumDir = File(firstSong.path).parentFile

            var albumArtworkUri: Uri? = null
            var hasFolderCover = false

            if (albumDir?.isDirectory == true) {
                val files = albumDir.listFiles()
                if (files != null) {
                    val extensions = setOf("jpg", "png", "jpeg", "webp")
                    val commonNames = setOf("cover", "folder", "front", "album")

                    val fileMap = files.filter { it.isFile }.associateBy { it.name.lowercase().trim() }

                    outer@for (name in commonNames) {
                        for (ext in extensions) {
                            fileMap["$name.$ext"]?.let {
                                albumArtworkUri = it.toUri()
                                hasFolderCover = true
                                break@outer
                            }
                        }
                    }
                }
            }

            if (albumArtworkUri == null) {
                albumArtworkUri = firstSong.albumArtUri
            }

            Album(
                id = albumId,
                title = firstSong.album,
                artist = firstSong.artist,
                artworkUri = albumArtworkUri,
                songs = albumSongs.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })),
                year = firstSong.year,
                allCovers = albumSongs.mapNotNull { it.albumArtUri }.distinct(),
                hasFolderCover = hasFolderCover
            )
        }
        
        playlistDao.clearAlbums()
        playlistDao.insertAlbums(albums.map { it.toEntity() })
        
        cachedAlbums = albums
        albums
    }

    suspend fun getArtists(songs: List<Song>, albums: List<Album>, useCache: Boolean = true): List<Artist> = withContext(Dispatchers.IO) {
        if (useCache) {
            val cached = playlistDao.getAllCachedArtists()
            if (cached.isNotEmpty()) {
                val artists = cached.map { it.toDomain(songs, albums) }
                cachedArtists = artists
                return@withContext artists
            }
        }
        
        if (cachedArtists != null && songs === cachedSongs && albums === cachedAlbums) return@withContext cachedArtists!!

        val albumMap = albums.groupBy { it.artist }
        val dirCache = mutableMapOf<String, Map<String, File>>()

        val artists = songs.groupBy { it.artist }.map { (name, artistSongs) ->
            val artistAlbums = albumMap[name] ?: emptyList()
            val thumbnailUri = findArtistCoverOptimized(name, artistSongs, dirCache) ?: artistAlbums.firstOrNull { it.artworkUri != null }?.artworkUri

            Artist(
                name = name,
                albums = artistAlbums,
                songs = artistSongs,
                albumCount = artistAlbums.size,
                trackCount = artistSongs.size,
                thumbnailUri = thumbnailUri
            )
        }
        
        playlistDao.clearArtists()
        playlistDao.insertArtists(artists.map { it.toEntity() })

        cachedArtists = artists
        artists
    }

    private fun findArtistCoverOptimized(
        artistName: String,
        songs: List<Song>,
        dirCache: MutableMap<String, Map<String, File>>
    ): Uri? {
        val extensions = setOf("jpg", "jpeg", "png", "webp")
        val genericNames = setOf("cover", "artist", "folder", "front")

        val artistLower = artistName.lowercase().trim()
        val artistSanitized = artistLower.replace(Regex("[^a-z0-9]"), "")
        val artistUnderscore = artistLower.replace(" ", "_")

        // Sample first song from each album
        val sampleSongs = songs.distinctBy { it.albumId }.take(10)

        for (song in sampleSongs) {
            val albumDir = File(song.path).parentFile ?: continue

            var currentDir: File? = albumDir
            var depth = 0
            while (currentDir != null && depth < 3) {
                val path = currentDir.absolutePath
                val fileMap = dirCache.getOrPut(path) {
                    currentDir?.listFiles()?.filter { it.isFile }?.associateBy { it.name.lowercase().trim() } ?: emptyMap()
                }

                if (fileMap.isNotEmpty()) {
                    // 1. Priority: [ArtistName].* variations
                    for (ext in extensions) {
                        // Exact match
                        fileMap["$artistLower.$ext"]?.let { return it.toUri() }
                        // Underscore match (e.g., 50_cent.jpg)
                        fileMap["$artistUnderscore.$ext"]?.let { return it.toUri() }
                        // Sanitized match (e.g., 50cent.jpg)
                        fileMap["$artistSanitized.$ext"]?.let { return it.toUri() }
                    }

                    // 2. Priority: artist.*
                    for (ext in extensions) {
                        fileMap["artist.$ext"]?.let { return it.toUri() }
                    }

                    // 3. Priority: generic names in parent or matching folder
                    val dirNameLower = currentDir.name.lowercase().trim()
                    val dirNameSanitized = dirNameLower.replace(Regex("[^a-z0-9]"), "")

                    val isLikelyArtistFolder = dirNameLower == artistLower ||
                                              dirNameLower == artistUnderscore ||
                                              dirNameSanitized == artistSanitized ||
                                              dirNameLower.contains(artistLower) ||
                                              depth > 0

                    if (isLikelyArtistFolder) {
                        for (name in genericNames) {
                            if (name == "artist") continue
                            for (ext in extensions) {
                                fileMap["$name.$ext"]?.let { return it.toUri() }
                            }
                        }
                    }
                }

                currentDir = currentDir.parentFile
                depth++

                val dirPathLower = currentDir?.absolutePath?.lowercase() ?: ""
                if (dirPathLower.endsWith("/music") || dirPathLower.endsWith("/download") || dirPathLower.endsWith("/downloads") || dirPathLower.endsWith("/0")) break
            }
        }
        return null
    }

    suspend fun scanMusicFolders(onlyMusicFolder: Boolean = false) = withContext(Dispatchers.IO) {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val filesToScan = mutableListOf<String>()

        fun collectFiles(dir: File) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) collectFiles(file)
                else if (file.extension.lowercase(Locale.getDefault()) in listOf("mp3", "m4a", "wav", "flac")) {
                    filesToScan.add(file.absolutePath)
                }
            }
        }

        if (musicDir.exists()) collectFiles(musicDir)
        if (!onlyMusicFolder && downloadDir.exists()) collectFiles(downloadDir)

        if (filesToScan.isNotEmpty()) {
            MediaScannerConnection.scanFile(context, filesToScan.toTypedArray(), null, null)
        }
    }

    // Playlist Database Operations
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    isFavorite = entity.isFavorite,
                    songs = emptyList() // We'll load songs separately when needed
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, id: String = java.util.UUID.randomUUID().toString(), isFavorite: Boolean = false) {
        playlistDao.insertPlaylist(PlaylistEntity(id = id, name = name, isFavorite = isFavorite))
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.deletePlaylist(it)
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: Long) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Long>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.songId }
        }
    }

    fun getPlaylistWithSongs(playlistId: String): Flow<Playlist?> {
        return playlistDao.getPlaylistWithSongs(playlistId).map { playlistWithSongs ->
            playlistWithSongs?.let {
                val songs = it.songs.mapNotNull { ps ->
                    cachedSongs?.find { s -> s.id == ps.songId }
                }
                Playlist(
                    id = it.playlist.id,
                    name = it.playlist.name,
                    isFavorite = it.playlist.isFavorite,
                    songs = songs.map { s -> s.id }
                )
            }
        }
    }

    suspend fun exportPlaylists(outputStream: OutputStream) {
        val playlists = playlistDao.getAllPlaylistsSync()
        val songs = playlistDao.getAllPlaylistSongsSync()
        val backup = PlaylistBackup(playlists, songs)
        val json = Json.encodeToString(backup)
        outputStream.use { it.write(json.toByteArray()) }
    }

    suspend fun importPlaylists(inputStream: InputStream) {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        try {
            val backup = Json.decodeFromString<PlaylistBackup>(jsonString)
            playlistDao.insertPlaylists(backup.playlists)
            playlistDao.insertPlaylistSongs(backup.playlistSongs)
        } catch (e: Exception) {
            // If not JSON, try parsing as M3U
            importFromM3U(jsonString)
        }
    }

    suspend fun getLyrics(path: String): String? = withContext(Dispatchers.IO) {
        // 1. Try to find external .lrc file first
        val lrcFile = File(path.substringBeforeLast(".") + ".lrc")
        if (lrcFile.exists()) {
            try {
                return@withContext lrcFile.readText()
            } catch (e: Exception) {
                // ignore and fall back
            }
        }

        // 2. Fallback to embedded lyrics
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            // METADATA_KEY_LYRIC is 1000. It's not a public constant in all SDK versions.
            val lyrics = retriever.extractMetadata(1000) 
            retriever.release()
            lyrics
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDetailedBitrate(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            } else {
                null
            }
            val bitsPerSample = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
            } else {
                null
            }
            retriever.release()

            val kbps = bitrate?.toLongOrNull()?.let { it / 1000 }
            val khz = sampleRate?.toDoubleOrNull()?.let { it / 1000.0 }
            
            buildString {
                if (kbps != null) append("$kbps kbps")
                if (khz != null) {
                    if (isNotEmpty()) append(" • ")
                    append(String.format(Locale.US, "%.1f kHz", khz))
                }
                if (bitsPerSample != null) {
                    if (isNotEmpty()) append(" • ")
                    append("$bitsPerSample-bit")
                }
            }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun importFromM3U(content: String) {
        val lines = content.lines()
        val playlistName = "Imported Playlist ${System.currentTimeMillis()}"
        val playlistId = UUID.randomUUID().toString()
        
        createPlaylist(playlistId, playlistName)
        
        lines.forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                // Try to find song by path
                // Samsung Music .m3u often uses relative paths or full paths
                val song = cachedSongs?.find { 
                    it.path == line || 
                    it.path.endsWith(line.replace("/", File.separator)) ||
                    it.path.contains(line.substringAfterLast("/")) 
                }
                if (song != null) {
                    playlistDao.addSongToPlaylist(playlistId, song.id)
                }
            }
        }
    }
}
