package com.igorthepadna.play_pause

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class ColorSchemeType {
    DYNAMIC, PURPLE, BLUE, GREEN, ORANGE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(listOf(
        Playlist(id = "favorites", name = "Favorites", isFavorite = true)
    ))
    val playlists = _playlists.asStateFlow()

    // Appearance Settings
    private val _themeMode = MutableStateFlow(ThemeMode.AUTO)
    val themeMode = _themeMode.asStateFlow()

    private val _colorSchemeType = MutableStateFlow(ColorSchemeType.DYNAMIC)
    val colorSchemeType = _colorSchemeType.asStateFlow()

    private val _useArtworkAccent = MutableStateFlow(true)
    val useArtworkAccent = _useArtworkAccent.asStateFlow()

    // General Settings
    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback = _gaplessPlayback.asStateFlow()

    fun setPlayer(player: Player?) {
        _player.value = player
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setColorSchemeType(type: ColorSchemeType) {
        _colorSchemeType.value = type
    }

    fun setUseArtworkAccent(use: Boolean) {
        _useArtworkAccent.value = use
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
    }

    fun loadSongs(refresh: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (refresh) {
                repository.scanMusicFolders()
            }
            _songs.value = repository.getSongs()
            _isRefreshing.value = false
        }
    }

    private fun Song.toMediaItem(isPlayNext: Boolean = false): MediaItem {
        val extras = Bundle().apply {
            putBoolean("is_play_next", isPlayNext)
            putString("bitrate", bitrate ?: "")
            putString("mime_type", format ?: "")
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(albumArtUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val player = _player.value ?: return
        val mediaItems = songs.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun addPlayNext(song: Song) {
        val player = _player.value ?: return
        val index = if (player.mediaItemCount > 0) player.currentMediaItemIndex + 1 else 0
        player.addMediaItem(index, song.toMediaItem(isPlayNext = true))
    }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name)
        _playlists.value = _playlists.value + newPlaylist
    }

    fun addToPlaylist(playlistId: String, songId: Long) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                if (!playlist.songs.contains(songId)) {
                    playlist.copy(songs = playlist.songs + songId)
                } else playlist
            } else playlist
        }
    }

    fun toggleFavorite(songId: Long) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == "favorites") {
                if (playlist.songs.contains(songId)) {
                    playlist.copy(songs = playlist.songs - songId)
                } else {
                    playlist.copy(songs = playlist.songs + songId)
                }
            } else playlist
        }
    }
}
