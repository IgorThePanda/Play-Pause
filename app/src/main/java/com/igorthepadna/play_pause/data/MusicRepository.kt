package com.igorthepadna.play_pause.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import androidx.core.net.toUri
import com.igorthepadna.play_pause.data.db.AlbumEntity
import com.igorthepadna.play_pause.data.db.AppDatabase
import com.igorthepadna.play_pause.data.db.ArtistEntity
import com.igorthepadna.play_pause.data.db.PlayEvent
import com.igorthepadna.play_pause.data.db.PlaylistBackup
import com.igorthepadna.play_pause.data.db.PlaylistBackupV2
import com.igorthepadna.play_pause.data.db.FullBackupV2
import com.igorthepadna.play_pause.data.db.SongBackup
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
    private val statsDao = database.statsDao()

    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null

    private fun getMusicPaths(): List<String> {
        val paths = mutableListOf<String>()
        paths.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
        val externalFilesDirs = context.getExternalFilesDirs(null)
        for (file in externalFilesDirs) {
            if (file != null) {
                val path = file.absolutePath
                if (path.contains("/Android/data/")) {
                    val rootPath = path.substringBefore("/Android/data/")
                    val musicPath = File(rootPath, Environment.DIRECTORY_MUSIC).absolutePath
                    if (!paths.contains(musicPath)) paths.add(musicPath)
                }
            }
        }
        return paths
    }

    suspend fun getSongs(useCache: Boolean = true, onlyMusicFolder: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (useCache && cachedSongs != null) return@withContext cachedSongs!!
        if (useCache) {
            val cached = playlistDao.getAllCachedSongs()
            if (cached.isNotEmpty()) {
                val songs = cached.map { it.toDomain() }
                val filtered = if (onlyMusicFolder) {
                    val musicPaths = getMusicPaths()
                    songs.filter { song -> musicPaths.any { song.path.startsWith(it) } }
                } else songs
                cachedSongs = filtered
                return@withContext filtered
            }
        }

        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.DISC_NUMBER, "album_artist", MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.YEAR)

        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val selectionArgs = mutableListOf<String>()
        if (onlyMusicFolder) {
            val musicPaths = getMusicPaths()
            if (musicPaths.isNotEmpty()) {
                selection += " AND (${musicPaths.joinToString(" OR ") { "${MediaStore.Audio.Media.DATA} LIKE ?" }})"
                musicPaths.forEach { selectionArgs.add("$it%") }
            }
        }

        context.contentResolver.query(collection, projection, selection, if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(), "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val discCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
            val albArtCol = cursor.getColumnIndex("album_artist")
            val albIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawTitle = cursor.getString(titleCol) ?: "Unknown Title"
                val rawArtist = cursor.getString(artistCol) ?: "Unknown Artist"
                var track = if (trackCol != -1) cursor.getInt(trackCol) else 0
                var disc = if (discCol != -1) cursor.getInt(discCol) else 1
                if (track >= 1000) { disc = track / 1000; track %= 1000 }
                
                val (cleanTitle, cleanArtist) = processSongMetadata(rawTitle, rawArtist)

                songList.add(Song(
                    id = id, title = cleanTitle, artist = cleanArtist, album = cursor.getString(albumCol) ?: "Unknown Album",
                    duration = cursor.getLong(durCol), uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albIdCol)),
                    path = cursor.getString(dataCol) ?: "", size = cursor.getLong(sizeCol), format = cursor.getString(mimeCol) ?: "",
                    dateAdded = cursor.getLong(dateCol), trackNumber = track, discNumber = if (disc == 0) 1 else disc,
                    albumArtist = if (albArtCol != -1) cursor.getString(albArtCol) else null, albumId = cursor.getLong(albIdCol),
                    year = cursor.getInt(yearCol), bitrate = if (cursor.getLong(durCol) > 0) "${(cursor.getLong(sizeCol) * 8 / cursor.getLong(durCol)).toInt()} kbps" else null
                ))
            }
        }
        playlistDao.clearSongs(); playlistDao.insertSongs(songList.map { it.toEntity() })
        cachedSongs = songList; cachedAlbums = null; cachedArtists = null; songList
    }

    private fun Song.toEntity() = SongEntity(id, title, artist, album, duration, uri.toString(), albumArtUri?.toString(), path, size, format, dateAdded, trackNumber, discNumber, albumArtist, albumId, year)
    private fun SongEntity.toDomain(): Song {
        val (cleanTitle, cleanArtist) = processSongMetadata(title, artist)
        return Song(
            id = id, title = cleanTitle, artist = cleanArtist, album = album, duration = duration,
            uri = Uri.parse(uri), albumArtUri = albumArtUri?.let { Uri.parse(it) }, path = path,
            size = size, format = format, dateAdded = dateAdded, 
            trackNumber = if (trackNumber >= 1000) trackNumber % 1000 else trackNumber, 
            discNumber = discNumber, albumArtist = albumArtist, albumId = albumId, year = year, 
            bitrate = if (duration > 0) "${(size * 8 / duration).toInt()} kbps" else null
        )
    }
    private fun Album.toEntity() = AlbumEntity(id, title, artist, artworkUri?.toString(), year, hasFolderCover)
    private fun AlbumEntity.toDomain(songs: List<Song>) = Album(id, title, artist, artworkUri?.let { Uri.parse(it) }, songs.filter { it.albumId == id }.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })), year, songs.filter { it.albumId == id }.mapNotNull { it.albumArtUri }.distinct(), hasFolderCover)
    private fun Artist.toEntity() = ArtistEntity(name, albumCount, trackCount, thumbnailUri?.toString())
    private fun ArtistEntity.toDomain(songs: List<Song>, albums: List<Album>): Artist {
        val artistSongs = songs.filter { splitArtists(it.artist).firstOrNull() == name }
        val artistFeaturedSongs = songs.filter { val split = splitArtists(it.artist); split.contains(name) && split.firstOrNull() != name }
        val artistAlbums = albums.filter { it.artist == name || it.songs.any { s -> splitArtists(s.artist).contains(name) } }
        return Artist(name, artistAlbums, artistSongs, artistFeaturedSongs, albumCount, trackCount, thumbnailUri?.let { Uri.parse(it) })
    }

    suspend fun getAlbums(songs: List<Song>, useCache: Boolean = true): List<Album> = withContext(Dispatchers.IO) {
        if (useCache && cachedAlbums != null) return@withContext cachedAlbums!!
        if (useCache) {
            val cached = playlistDao.getAllCachedAlbums()
            if (cached.isNotEmpty()) { val albums = cached.map { it.toDomain(songs) }; cachedAlbums = albums; return@withContext albums }
        }
        val albums = songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val first = albumSongs.first(); val albumDir = File(first.path).parentFile
            var artUri: Uri? = null; var hasFolder = false
            if (albumDir?.isDirectory == true) {
                val files = albumDir.listFiles(); if (files != null) {
                    val exts = setOf("jpg", "png", "jpeg", "webp"); val names = setOf("cover", "folder", "front", "album")
                    val fileMap = files.filter { it.isFile }.associateBy { it.name.lowercase().trim() }
                    outer@for (n in names) for (e in exts) fileMap["$n.$e"]?.let { artUri = it.toUri(); hasFolder = true; break@outer }
                }
            }
            Album(albumId, first.album, first.albumArtist ?: first.artist, artUri ?: first.albumArtUri, albumSongs.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })), first.year, albumSongs.mapNotNull { it.albumArtUri }.distinct(), hasFolder)
        }
        playlistDao.clearAlbums(); playlistDao.insertAlbums(albums.map { it.toEntity() })
        cachedAlbums = albums; albums
    }

    suspend fun getArtists(songs: List<Song>, albums: List<Album>, useCache: Boolean = true): List<Artist> = withContext(Dispatchers.IO) {
        if (useCache && cachedArtists != null) return@withContext cachedArtists!!
        if (useCache) {
            val cached = playlistDao.getAllCachedArtists()
            if (cached.isNotEmpty()) { val artists = cached.map { it.toDomain(songs, albums) }; cachedArtists = artists; return@withContext artists }
        }
        val artistSongsMap = mutableMapOf<String, MutableList<Song>>(); val featuredSongsMap = mutableMapOf<String, MutableList<Song>>()
        songs.forEach { song ->
            val artists = splitArtists(song.artist)
            if (artists.isNotEmpty()) {
                artistSongsMap.getOrPut(artists[0]) { mutableListOf() }.add(song)
                if (artists.size > 1) artists.drop(1).forEach { name -> featuredSongsMap.getOrPut(name) { mutableListOf() }.add(song); artistSongsMap.getOrPut(name) { mutableListOf() } }
            }
        }
        val artistAlbumsMap = mutableMapOf<String, MutableSet<Album>>()
        albums.forEach { album ->
            artistAlbumsMap.getOrPut(album.artist) { mutableSetOf() }.add(album)
            album.songs.flatMap { splitArtists(it.artist) }.distinct().forEach { name -> artistAlbumsMap.getOrPut(name) { mutableSetOf() }.add(album) }
        }
        val dirCache = mutableMapOf<String, Map<String, File>>()
        val artists = artistSongsMap.map { (name, aSongs) ->
            val distinctAlbums = artistAlbumsMap[name]?.toList() ?: emptyList()
            val featSongs = featuredSongsMap[name] ?: emptyList()
            Artist(name, distinctAlbums, aSongs, featSongs, distinctAlbums.size, (aSongs + featSongs).distinctBy { it.id }.size, findArtistCoverOptimized(name, aSongs, dirCache) ?: distinctAlbums.firstOrNull { it.artworkUri != null }?.artworkUri)
        }.sortedBy { it.name.lowercase() }
        playlistDao.clearArtists(); playlistDao.insertArtists(artists.map { it.toEntity() })
        cachedArtists = artists; artists
    }

    companion object {
        private val splitCache = LruCache<String, List<String>>(500)
        private val PROTECTED_ARTISTS = setOf("Tyler, The Creator", "Earth, Wind & Fire", "Crosby, Stills, Nash & Young", "Emerson, Lake & Palmer", "Peter, Paul and Mary", "Blood, Sweat & Tears", "Anderson, Bruford, Wakeman, Howe", "Marina and the Diamonds", "Florence + The Machine")

        fun splitArtists(artistString: String?): List<String> {
            if (artistString.isNullOrBlank()) return listOf("Unknown Artist")
            val cached = try { splitCache.get(artistString) } catch (e: Exception) { null }
            if (cached != null) return cached
            var processed: String = artistString!!; val replacements = mutableListOf<Pair<String, String>>()
            PROTECTED_ARTISTS.forEachIndexed { index, name ->
                val token = "___PROT_${index}___"
                if (processed.contains(name, ignoreCase = true)) {
                    val regex = Regex(Regex.escape(name), RegexOption.IGNORE_CASE)
                    regex.find(processed)?.let { replacements.add(token to it.value); processed = regex.replace(processed, token) }
                }
            }
            val delimiters = listOf(" & ", " feat. ", " feat ", " ft. ", " ft ", " / ", " with ", " Featuring ", ", ")
            var parts = listOf(processed)
            delimiters.forEach { d -> parts = parts.flatMap { it.split(d) } }
            val finalResult = parts.map { part ->
                var restored = part.trim()
                replacements.forEach { (token, original) -> restored = restored.replace(token, original) }
                restored
            }.filter { it.isNotBlank() }.distinct()
            try { splitCache.put(artistString, finalResult) } catch (e: Exception) {}
            return finalResult
        }

        fun extractFeaturedArtistsFromTitle(title: String): List<String> {
            val features = mutableListOf<String>()
            val patterns = listOf(
                Regex("""\((?:feat\.?|ft\.?|featuring|with)\s+([^)]+)\)""", RegexOption.IGNORE_CASE), 
                Regex("""\[(?:feat\.?|ft\.?|featuring|with)\s+([^\]]+)\]""", RegexOption.IGNORE_CASE), 
                Regex("""\s(?:feat\.?|ft\.?|featuring|with)\s+(.+)""", RegexOption.IGNORE_CASE)
            )
            patterns.forEach { regex -> regex.findAll(title).forEach { match -> val section = match.groups[1]?.value ?: ""; if (section.isNotBlank()) features.addAll(splitArtists(section)) } }
            return features.distinct()
        }

        fun cleanTitle(title: String): String {
            val patterns = listOf(
                Regex("""\s*\((?:feat\.?|ft\.?|featuring|with)\s+[^)]+\)""", RegexOption.IGNORE_CASE), 
                Regex("""\s*\[(?:feat\.?|ft\.?|featuring|with)\s+([^\]]+)\]""", RegexOption.IGNORE_CASE), 
                Regex("""\s(?:feat\.?|ft\.?|featuring|with)\s+.+""", RegexOption.IGNORE_CASE)
            )
            var cleaned = title; patterns.forEach { cleaned = it.replace(cleaned, "").trim() }
            return cleaned
        }

        fun processSongMetadata(rawTitle: String, rawArtist: String): Pair<String, String> {
            val features = extractFeaturedArtistsFromTitle(rawTitle)
            val cleanedTitle = cleanTitle(rawTitle)
            val artistList = splitArtists(rawArtist).toMutableList()
            features.forEach { feat -> if (!artistList.any { it.equals(feat, true) }) artistList.add(feat) }
            return cleanedTitle to artistList.joinToString(", ")
        }
    }

    private fun findArtistCoverOptimized(artistName: String, songs: List<Song>, dirCache: MutableMap<String, Map<String, File>>): Uri? {
        val exts = setOf("jpg", "jpeg", "png", "webp"); val generic = setOf("cover", "artist", "folder", "front")
        val lower = artistName.lowercase().trim(); val san = lower.replace(Regex("[^a-z0-9]"), ""); val und = lower.replace(" ", "_")
        for (song in songs.distinctBy { it.albumId }.take(10)) {
            var current: File? = File(song.path).parentFile; var depth = 0
            while (current != null && depth < 3) {
                val path = current.absolutePath
                val fileMap = dirCache.getOrPut(path) { current?.listFiles()?.filter { it.isFile }?.associateBy { it.name.lowercase().trim() } ?: emptyMap() }
                if (fileMap.isNotEmpty()) {
                    for (e in exts) { fileMap["$lower.$e"]?.let { return it.toUri() }; fileMap["$und.$e"]?.let { return it.toUri() }; fileMap["$san.$e"]?.let { return it.toUri() } }
                    for (e in exts) fileMap["artist.$e"]?.let { return it.toUri() }
                    if (current.name.lowercase().trim() == lower || depth > 0) for (n in generic) { if (n == "artist") continue; for (e in exts) fileMap["$n.$e"]?.let { return it.toUri() } }
                }
                current = current.parentFile; depth++
                if (path.lowercase().endsWith("/music") || path.lowercase().endsWith("/0")) break
            }
        }
        return null
    }

    suspend fun scanMusicFolders(onlyMusicFolder: Boolean = false) = withContext(Dispatchers.IO) {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = mutableListOf<String>()
        fun collect(dir: File) { dir.listFiles()?.forEach { if (it.isDirectory) collect(it) else if (it.extension.lowercase() in listOf("mp3", "m4a", "wav", "flac")) files.add(it.absolutePath) } }
        if (musicDir.exists()) collect(musicDir)
        if (!onlyMusicFolder && downloadDir.exists()) collect(downloadDir)
        if (files.isNotEmpty()) MediaScannerConnection.scanFile(context, files.toTypedArray(), null, null)
    }

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylistsWithSongs().map { entities -> entities.map { item -> Playlist(item.playlist.id, item.playlist.name, item.songs.map { it.songId }, item.playlist.isFavorite, item.playlist.coverUri?.let { Uri.parse(it) }) } }
    suspend fun createPlaylist(name: String, id: String = UUID.randomUUID().toString(), isFavorite: Boolean = false) = playlistDao.insertPlaylist(PlaylistEntity(id, name, isFavorite))
    suspend fun deletePlaylist(playlistId: String) = playlistDao.getPlaylistById(playlistId)?.let { playlistDao.deletePlaylist(it) }
    suspend fun addSongToPlaylist(playlistId: String, songId: Long) = playlistDao.addSongToPlaylist(playlistId, songId)
    suspend fun getPlaylistSync(playlistId: String): Playlist? = playlistDao.getPlaylistById(playlistId)?.let { Playlist(it.id, it.name, emptyList(), it.isFavorite, it.coverUri?.let { u -> Uri.parse(u) }) }
    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) = playlistDao.removeSongFromPlaylist(playlistId, songId)
    fun getSongsForPlaylist(playlistId: String): Flow<List<Long>> = playlistDao.getSongsForPlaylist(playlistId).map { entities -> entities.map { it.songId } }
    fun getPlaylistWithSongs(playlistId: String): Flow<Playlist?> = playlistDao.getPlaylistWithSongs(playlistId).map { p -> p?.let { Playlist(it.playlist.id, it.playlist.name, it.songs.map { it.songId }, it.playlist.isFavorite, it.playlist.coverUri?.let { u -> Uri.parse(u) }) } }
    suspend fun setPlaylistCover(playlistId: String, uri: Uri?) = playlistDao.updatePlaylistCover(playlistId, uri?.toString())
    suspend fun updatePlaylistName(playlistId: String, name: String) = playlistDao.updatePlaylistName(playlistId, name)

    suspend fun exportPlaylists(outputStream: OutputStream) {
        val playlists = playlistDao.getAllPlaylistsSync()
        val backups = playlists.map { p ->
            val songsWith = playlistDao.getPlaylistWithSongsSync(p.id)
            val songs = songsWith?.songs?.mapNotNull { ps -> cachedSongs?.find { it.id == ps.songId }?.let { s -> SongBackup(s.title, s.artist, s.album, s.duration, s.path, s.id) } } ?: emptyList()
            PlaylistBackupV2(p, songs)
        }
        val playEvents = statsDao.getAllPlayEventsSync()
        outputStream.use { it.write(Json.encodeToString(FullBackupV2(backups, playEvents)).toByteArray()) }
    }

    suspend fun exportSinglePlaylist(playlistId: String, outputStream: OutputStream) {
        val entity = playlistDao.getPlaylistById(playlistId) ?: return
        val songsWith = playlistDao.getPlaylistWithSongsSync(playlistId)
        val songs = songsWith?.songs?.mapNotNull { ps -> cachedSongs?.find { it.id == ps.songId }?.let { s -> SongBackup(s.title, s.artist, s.album, s.duration, s.path, s.id) } } ?: emptyList()
        outputStream.use { it.write(Json.encodeToString(PlaylistBackupV2(entity, songs)).toByteArray()) }
    }

    suspend fun importPlaylists(inputStream: InputStream) {
        val json = inputStream.bufferedReader().use { it.readText() }
        try {
            try { 
                val fullBackup = Json.decodeFromString<FullBackupV2>(json)
                fullBackup.playlists.forEach { importPlaylistV2(it) }
                if (fullBackup.playEvents.isNotEmpty()) {
                    statsDao.insertPlayEvents(fullBackup.playEvents)
                }
                return 
            } catch (e: Exception) {}
            try { importPlaylistV2(Json.decodeFromString<PlaylistBackupV2>(json)); return } catch (e: Exception) {}
            val b = Json.decodeFromString<PlaylistBackup>(json); playlistDao.insertPlaylists(b.playlists); playlistDao.insertPlaylistSongs(b.playlistSongs)
        } catch (e: Exception) { importFromM3U(json) }
    }

    private suspend fun importPlaylistV2(backup: PlaylistBackupV2) {
        val existing = playlistDao.getPlaylistById(backup.playlist.id)
        val id = if (existing != null) UUID.randomUUID().toString() else backup.playlist.id
        val name = if (existing != null) "${backup.playlist.name} (Imported)" else backup.playlist.name
        playlistDao.insertPlaylist(backup.playlist.copy(id = id, name = name))
        backup.songs.forEachIndexed { i, s -> findSongRobustly(s)?.let { playlistDao.insertPlaylistSong(PlaylistSongEntity(id, it, i)) } }
    }

    private fun findSongRobustly(b: SongBackup): Long? {
        cachedSongs?.find { it.id == b.mediaStoreId }?.let { return it.id }
        cachedSongs?.find { it.path == b.path }?.let { return it.id }
        cachedSongs?.find { it.title.equals(b.title, true) && it.artist.equals(b.artist, true) && Math.abs(it.duration - b.duration) < 2000 }?.let { return it.id }
        return null
    }

    suspend fun getLyrics(path: String): String? = withContext(Dispatchers.IO) {
        val lrc = File(path.substringBeforeLast(".") + ".lrc")
        if (lrc.exists()) try { return@withContext lrc.readText() } catch (e: Exception) {}
        try { val r = MediaMetadataRetriever(); r.setDataSource(path); val l = r.extractMetadata(1000); r.release(); l } catch (e: Exception) { null }
    }

    suspend fun getDetailedBitrate(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val r = MediaMetadataRetriever(); r.setDataSource(path)
            val b = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val s = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE) else null
            val bp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE) else null
            r.release()
            buildString {
                b?.toLongOrNull()?.let { append("${it / 1000} kbps") }
                s?.toDoubleOrNull()?.let { if (isNotEmpty()) append(" • "); append(String.format(Locale.US, "%.1f kHz", it / 1000.0)) }
                bp?.let { if (isNotEmpty()) append(" • "); append("$it-bit") }
            }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) { null }
    }

    suspend fun recordPlayEvent(songId: Long) = withContext(Dispatchers.IO) {
        statsDao.insertPlayEvent(PlayEvent(songId = songId, timestamp = System.currentTimeMillis()))
    }

    fun getTopTracks(limit: Int = 10) = statsDao.getTopTracks(limit)
    fun getTopArtists(limit: Int = 10) = statsDao.getTopArtists(limit)
    fun getTotalPlayCount() = statsDao.getTotalPlayCount()
    fun getDailyPlayCounts(since: Long) = statsDao.getDailyPlayCounts(since)
    suspend fun clearStats() = statsDao.clearAllStats()

    private suspend fun importFromM3U(content: String) {
        val id = UUID.randomUUID().toString(); createPlaylist("Imported Playlist", id)
        content.lines().forEach { l -> if (l.isNotBlank() && !l.startsWith("#")) cachedSongs?.find { it.path == l || it.path.endsWith(l.replace("/", File.separator)) || it.path.contains(l.substringAfterLast("/")) }?.let { playlistDao.addSongToPlaylist(id, it.id) } }
    }
}
