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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MusicRepository(private val context: Context) {

    private var cachedSongs: List<Song>? = null
    private var cachedAlbums: List<Album>? = null
    private var cachedArtists: List<Artist>? = null

    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection, 
            projection, 
            selection, 
            null, 
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
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
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
                val trackNumber = cursor.getInt(trackColumn)
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
                    albumId = albumId,
                    year = year,
                    bitrate = if (duration > 0) "${(size * 8 / duration).toInt()} kbps" else null,
                    lyrics = null 
                )
                
                songList.add(song)
            }
        }
        cachedSongs = songList
        cachedAlbums = null
        cachedArtists = null
        songList
    }

    suspend fun getLyrics(path: String): String? = withContext(Dispatchers.IO) {
        if (path.isEmpty()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null

        // 1. Check for companion .lrc file
        val lrcFile = File(file.parent, "${file.nameWithoutExtension}.lrc")
        if (lrcFile.exists()) {
            try {
                return@withContext lrcFile.readText()
            } catch (e: Exception) {
                // fallback to embedded
            }
        }

        // 2. Check for embedded lyrics
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            // METADATA_KEY_LYRICS constant value is 23
            return@withContext retriever.extractMetadata(23)
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    suspend fun getAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.IO) {
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
                songs = albumSongs.sortedBy { it.trackNumber },
                year = firstSong.year,
                allCovers = albumSongs.mapNotNull { it.albumArtUri }.distinct(),
                hasFolderCover = hasFolderCover
            )
        }
        cachedAlbums = albums
        albums
    }

    suspend fun getArtists(songs: List<Song>, albums: List<Album>): List<Artist> = withContext(Dispatchers.IO) {
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

    suspend fun scanMusicFolders() = withContext(Dispatchers.IO) {
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
        if (downloadDir.exists()) collectFiles(downloadDir)

        if (filesToScan.isNotEmpty()) {
            MediaScannerConnection.scanFile(context, filesToScan.toTypedArray(), null, null)
        }
    }
}
