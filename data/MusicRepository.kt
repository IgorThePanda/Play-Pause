package com.igorthepadna.play_pause.data

import android.content.ContentUris
import android.content.Context
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
            MediaStore.Audio.Media.YEAR,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.Audio.Media.ALBUM_ARTIST else MediaStore.Audio.Media.ARTIST
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
            val albumArtistColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
            } else -1

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
                val albumArtist = if (albumArtistColumn != -1) cursor.getString(albumArtistColumn) else null

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
                    albumArtist = albumArtist,
                    bitrate = if (duration > 0) "${(size * 8 / duration).toInt()} kbps" else null
                )
                
                songList.add(song)
            }
        }
        cachedSongs = songList
        cachedAlbums = null
        cachedArtists = null
        songList
    }

    suspend fun getAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.IO) {
        if (cachedAlbums != null && songs === cachedSongs) return@withContext cachedAlbums!!
        
        val albums = songs.groupBy { it.albumId }.map { (albumId, albumSongs) ->
            val firstSong = albumSongs.first()
            val albumDir = File(firstSong.path).parentFile
            
            var albumArtworkUri: Uri? = null
            var hasFolderCover = false
            
            // Faster check: instead of listFiles(), check common names first
            val extensions = listOf("jpg", "png", "jpeg", "webp")
            if (albumDir?.isDirectory == true) {
                for (ext in extensions) {
                    val cover = File(albumDir, "cover.$ext")
                    if (cover.exists()) {
                        albumArtworkUri = cover.toUri()
                        hasFolderCover = true
                        break
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

        val artistMap = mutableMapOf<String, MutableList<Song>>()
        
        songs.forEach { song ->
            val artists = splitArtists(song.artist)
            artists.forEach { name ->
                artistMap.getOrPut(name) { mutableListOf() }.add(song)
            }
        }

        val albumMap = albums.groupBy { it.artist }
        
        val artists = artistMap.map { (name, artistSongs) ->
            val artistAlbums = albumMap[name] ?: albums.filter { it.songs.any { s -> splitArtists(s.artist).contains(name) } }
            
            // Search for artist cover and info
            val thumbnailUri = findArtistCover(name, artistSongs) ?: artistAlbums.firstOrNull { it.artworkUri != null }?.artworkUri
            val artistInfo = findArtistInfo(name, artistSongs)

            Artist(
                name = name,
                albums = artistAlbums.distinctBy { it.id },
                songs = artistSongs,
                albumCount = artistAlbums.size,
                trackCount = artistSongs.size,
                thumbnailUri = thumbnailUri,
                info = artistInfo
            )
        }.sortedBy { it.name.lowercase() }
        
        cachedArtists = artists
        artists
    }

    private fun splitArtists(artistString: String?): List<String> {
        if (artistString == null || artistString.isBlank()) return listOf("Unknown Artist")
        
        val delimiters = listOf(" & ", " feat. ", " ft. ", ", ", " Feat. ", " Ft. ", " / ", " with ")
        var result = listOf(artistString)
        
        delimiters.forEach { delimiter ->
            result = result.flatMap { part ->
                part.split(delimiter)
            }
        }
        
        return result.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun findArtistInfo(artistName: String, songs: List<Song>): String? {
        val artistLower = artistName.lowercase()
        val checkedDirs = mutableSetOf<String>()

        for (song in songs.take(3)) {
            var currentDir = File(song.path).parentFile
            var depth = 0
            while (currentDir != null && depth < 2) {
                val dirPath = currentDir.absolutePath
                if (checkedDirs.add(dirPath)) {
                    // 1. Try artist info.txt directly
                    val infoFile = File(currentDir, "artist info.txt")
                    if (infoFile.exists()) {
                        return try {
                            infoFile.readText().trim()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    // 2. Try [artist name].txt directly
                    val artistSpecificFile = File(currentDir, "$artistLower.txt")
                    if (artistSpecificFile.exists()) {
                        return try {
                            artistSpecificFile.readText().trim()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                currentDir = currentDir.parentFile
                depth++
                if (dirPath.lowercase().endsWith("/music") || dirPath.lowercase().endsWith("/download")) break
            }
        }
        return null
    }

    private fun findArtistCover(artistName: String, songs: List<Song>): Uri? {
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        // Check for artist.jpg, folder.jpg etc. directly which is faster than listFiles()
        val commonNames = listOf("artist", "folder", "cover", "front")
        val artistLower = artistName.lowercase()
        
        val checkedDirs = mutableSetOf<String>()
        
        for (song in songs.take(3)) {
            var currentDir = File(song.path).parentFile
            var depth = 0
            while (currentDir != null && depth < 2) {
                val dirPath = currentDir.absolutePath
                if (checkedDirs.add(dirPath)) {
                    // 1. Try common names directly (fastest)
                    for (name in commonNames) {
                        for (ext in extensions) {
                            val file = File(currentDir, "$name.$ext")
                            if (file.exists()) return file.toUri()
                        }
                    }
                    // 2. Try artist name directly
                    for (ext in extensions) {
                        val file = File(currentDir, "$artistLower.$ext")
                        if (file.exists()) return file.toUri()
                    }
                    
                    // 3. Fallback to listFiles if directories are small (optional, but let's stick to fast checks)
                }
                currentDir = currentDir.parentFile
                depth++
                if (dirPath.lowercase().endsWith("/music") || dirPath.lowercase().endsWith("/download")) break
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
