package com.igorthepadna.play_pause

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Artist
import com.igorthepadna.play_pause.data.LibraryFilter
import com.igorthepadna.play_pause.data.MusicRepository
import com.igorthepadna.play_pause.data.Playlist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.data.SortOrder
import com.igorthepadna.play_pause.data.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

enum class ColorSchemeType {
    DYNAMIC, VIBRANT, PURPLE, BLUE, GREEN, ORANGE
}

data class TabSortSettings(
    val sortType: SortType = SortType.TITLE,
    val sortOrder: SortOrder = SortOrder.ASC,
    val showOnlyAlbumArtists: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val prefs = application.getSharedPreferences("play_pause_prefs", Context.MODE_PRIVATE)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    private val _allArtists = MutableStateFlow<List<Artist>>(emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _currentPlayingId = MutableStateFlow(-1L)
    val currentPlayingId = _currentPlayingId.asStateFlow()

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading = _isLyricsLoading.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(listOf(
        Playlist(id = "favorites", name = "Favorites", isFavorite = true)
    ))
    val playlists = _playlists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Debounced search query
    private val debouncedSearchQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Appearance Settings
    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.AUTO.name)!!) }.getOrDefault(ThemeMode.AUTO)
    )
    val themeMode = _themeMode.asStateFlow()

    private val _colorSchemeType = MutableStateFlow(
        runCatching { ColorSchemeType.valueOf(prefs.getString("color_scheme_type", ColorSchemeType.DYNAMIC.name)!!) }.getOrDefault(ColorSchemeType.DYNAMIC)
    )
    val colorSchemeType = _colorSchemeType.asStateFlow()

    private val _useArtworkAccent = MutableStateFlow(prefs.getBoolean("use_artwork_accent", true))
    val useArtworkAccent = _useArtworkAccent.asStateFlow()

    // General Settings
    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean("gapless_playback", true))
    val gaplessPlayback = _gaplessPlayback.asStateFlow()

    // Lyric Settings
    private val _lyricFontSize = MutableStateFlow(prefs.getFloat("lyric_font_size", 28f))
    val lyricFontSize = _lyricFontSize.asStateFlow()

    private val _lyricInactiveAlpha = MutableStateFlow(prefs.getFloat("lyric_inactive_alpha", 0.35f))
    val lyricInactiveAlpha = _lyricInactiveAlpha.asStateFlow()

    private val _lyricActiveScale = MutableStateFlow(prefs.getFloat("lyric_active_scale", 1.05f))
    val lyricActiveScale = _lyricActiveScale.asStateFlow()

    private val _lyricLineSpacing = MutableStateFlow(prefs.getFloat("lyric_line_spacing", 12f))
    val lyricLineSpacing = _lyricLineSpacing.asStateFlow()

    // Persistent Navigation State
    private val _currentFilter = MutableStateFlow(
        runCatching { LibraryFilter.valueOf(prefs.getString("last_filter", LibraryFilter.ALBUMS.name)!!) }.getOrDefault(LibraryFilter.ALBUMS)
    )
    val currentFilter = _currentFilter.asStateFlow()

    private val _isPlayerFullScreen = MutableStateFlow(prefs.getBoolean("player_full_screen", false))
    val isPlayerFullScreen = _isPlayerFullScreen.asStateFlow()

    // Per-tab Sort Settings
    private val _tabSortSettings = MutableStateFlow<Map<LibraryFilter, TabSortSettings>>(
        LibraryFilter.entries.associateWith { filter ->
            val type = runCatching { SortType.valueOf(prefs.getString("sort_type_${filter.name}", 
                when(filter) {
                    LibraryFilter.SONGS, LibraryFilter.ALBUMS, LibraryFilter.ARTISTS -> SortType.TITLE.name
                    else -> SortType.TITLE.name
                }
            )!!) }.getOrDefault(SortType.TITLE)
            
            val order = runCatching { SortOrder.valueOf(prefs.getString("sort_order_${filter.name}", SortOrder.ASC.name)!!) }.getOrDefault(SortOrder.ASC)
            val onlyAlbumArtists = prefs.getBoolean("only_album_artists_${filter.name}", false)
            TabSortSettings(type, order, onlyAlbumArtists)
        }
    )
    val tabSortSettings = _tabSortSettings.asStateFlow()

    // Highly Optimized Flows
    val filteredSongs = combine(_songs, debouncedSearchQuery, _tabSortSettings) { allSongs, query, settings ->
        val currentSettings = settings[LibraryFilter.SONGS] ?: TabSortSettings()
        var list = if (query.isBlank()) allSongs 
        else allSongs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
        
        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.title } else list.sortedByDescending { it.title }
            SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.artist } else list.sortedByDescending { it.artist }
            SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.year } else list.sortedByDescending { it.year }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedAlbums = combine(_allAlbums, debouncedSearchQuery, _tabSortSettings) { albums, query, settings ->
        val currentSettings = settings[LibraryFilter.ALBUMS] ?: TabSortSettings()
        var list = if (query.isBlank()) albums 
        else albums.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
        
        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.title } else list.sortedByDescending { it.title }
            SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.artist } else list.sortedByDescending { it.artist }
            SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.year } else list.sortedByDescending { it.year }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedArtists = combine(_allArtists, debouncedSearchQuery, _tabSortSettings) { artists, query, settings ->
        val currentSettings = settings[LibraryFilter.ARTISTS] ?: TabSortSettings()
        var list = if (query.isBlank()) artists else artists.filter { it.name.contains(query, true) }
        
        if (currentSettings.showOnlyAlbumArtists) {
            list = list.filter { it.albumCount > 1 || (it.albumCount == 1 && it.songs.size > 2) }
        }

        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.name } else list.sortedByDescending { it.name }
            SortType.ALBUM_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.albumCount } else list.sortedByDescending { it.albumCount }
            SortType.TRACK_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.trackCount } else list.sortedByDescending { it.trackCount }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Persistence
    private val _selectedAlbumId = MutableStateFlow<Long?>(null)
    val selectedAlbumId = _selectedAlbumId.asStateFlow()

    private val _selectedArtistName = MutableStateFlow<String?>(null)
    val selectedArtistName = _selectedArtistName.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedAlbumId(id: Long?) {
        _selectedAlbumId.value = id
    }

    fun setSelectedArtistName(name: String?) {
        _selectedArtistName.value = name
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setColorSchemeType(type: ColorSchemeType) {
        _colorSchemeType.value = type
        prefs.edit().putString("color_scheme_type", type.name).apply()
    }

    fun setUseArtworkAccent(use: Boolean) {
        _useArtworkAccent.value = use
        prefs.edit().putBoolean("use_artwork_accent", use).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
    }

    fun setLyricFontSize(size: Float) {
        _lyricFontSize.value = size
        prefs.edit().putFloat("lyric_font_size", size).apply()
    }

    fun setLyricInactiveAlpha(alpha: Float) {
        _lyricInactiveAlpha.value = alpha
        prefs.edit().putFloat("lyric_inactive_alpha", alpha).apply()
    }

    fun setLyricActiveScale(scale: Float) {
        _lyricActiveScale.value = scale
        prefs.edit().putFloat("lyric_active_scale", scale).apply()
    }

    fun setLyricLineSpacing(spacing: Float) {
        _lyricLineSpacing.value = spacing
        prefs.edit().putFloat("lyric_line_spacing", spacing).apply()
    }

    fun setCurrentFilter(filter: LibraryFilter) {
        _currentFilter.value = filter
        prefs.edit().putString("last_filter", filter.name).apply()
    }

    fun setPlayerFullScreen(full: Boolean) {
        _isPlayerFullScreen.value = full
        prefs.edit().putBoolean("player_full_screen", full).apply()
    }

    fun updateSortSettings(filter: LibraryFilter, settings: TabSortSettings) {
        _tabSortSettings.value = _tabSortSettings.value.toMutableMap().apply {
            put(filter, settings)
        }
        prefs.edit().apply {
            putString("sort_type_${filter.name}", settings.sortType.name)
            putString("sort_order_${filter.name}", settings.sortOrder.name)
            putBoolean("only_album_artists_${filter.name}", settings.showOnlyAlbumArtists)
        }.apply()
    }

    fun loadSongs(refresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            if (refresh) repository.scanMusicFolders()
            
            val newSongs = repository.getSongs()
            _songs.value = newSongs
            
            // Perform all heavy processing here, while the loading indicator is up
            val albums = repository.getAlbums(newSongs)
            _allAlbums.value = albums
            
            val artists = repository.getArtists(newSongs, albums)
            _allArtists.value = artists

            _isRefreshing.value = false
        }
    }

    fun loadLyricsForCurrentSong() {
        val player = _player.value ?: return
        val currentItem = player.currentMediaItem ?: return
        val mediaId = currentItem.mediaId.toLongOrNull() ?: return

        val song = _songs.value.find { it.id == mediaId } ?: return
        
        viewModelScope.launch {
            _isLyricsLoading.value = true
            _currentLyrics.value = null // Clear old lyrics while loading
            _currentLyrics.value = repository.getLyrics(song.path)
            _isLyricsLoading.value = false
        }
    }

    fun addPlayNext(song: Song) {
        val player = _player.value ?: return
        val index = if (player.mediaItemCount > 0) player.currentMediaItemIndex + 1 else 0
        player.addMediaItem(index, song.toMediaItem(isPlayNext = true))
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
        if (songs.isEmpty()) return

        val safeStartIndex = startIndex.coerceIn(0, songs.lastIndex)

        // For large libraries, setting thousands of MediaItems at once can cause a
        // TransactionTooLargeException because it's an IPC call to the PlaybackService.
        // We solve this by setting the first item immediately and adding the rest in chunks.
        if (songs.size > 100) {
            // Set initial item to start playback as fast as possible
            player.setMediaItem(songs[safeStartIndex].toMediaItem())
            player.prepare()
            player.play()

            val chunkSize = 100
            // Add items before the current one in chunks
            val beforeItems = songs.subList(0, safeStartIndex)
            for (i in 0 until beforeItems.size step chunkSize) {
                val end = (i + chunkSize).coerceAtMost(beforeItems.size)
                val chunk = beforeItems.subList(i, end).map { it.toMediaItem() }
                player.addMediaItems(i, chunk)
            }

            // Add items after the current one in chunks
            val afterItems = songs.subList(safeStartIndex + 1, songs.size)
            for (i in 0 until afterItems.size step chunkSize) {
                val end = (i + chunkSize).coerceAtMost(afterItems.size)
                val chunk = afterItems.subList(i, end).map { it.toMediaItem() }
                player.addMediaItems(player.mediaItemCount, chunk)
            }
        } else {
            val mediaItems = songs.map { it.toMediaItem() }
            player.setMediaItems(mediaItems, safeStartIndex, 0L)
            player.prepare()
            player.play()
        }
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

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(id = UUID.randomUUID().toString(), name = name)
        _playlists.value = _playlists.value + newPlaylist
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

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentPlayingId.value = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            // Set loading immediately to prevent "unavailable" flash
            _isLyricsLoading.value = true
            _currentLyrics.value = null 
            loadLyricsForCurrentSong()
        }
    }

    fun setPlayer(p: Player?) {
        _player.value?.removeListener(playerListener)
        _player.value = p
        p?.addListener(playerListener)
        _currentPlayingId.value = p?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
        loadLyricsForCurrentSong()
    }

    override fun onCleared() {
        super.onCleared()
        _player.value?.removeListener(playerListener)
    }
}
