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
import android.provider.MediaStore
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.InputStream
import java.io.OutputStream

import com.igorthepadna.play_pause.ui.components.CategoryViewMode

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

data class ViewModeSettings(
    val viewMode: CategoryViewMode = CategoryViewMode.DETAILED,
    val columns: Int = 2
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

    private val _currentBitrate = MutableStateFlow<String?>(null)
    val currentBitrate = _currentBitrate.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading = _isLyricsLoading.asStateFlow()

    private var lastLoadedMediaId: Long = -1L

    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Debounced search query
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(20L) // Even more aggressive for truly "instant" feedback
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

    private val _scanOnlyMusicFolder = MutableStateFlow(prefs.getBoolean("scan_only_music", false))
    val scanOnlyMusicFolder = _scanOnlyMusicFolder.asStateFlow()

    // Lyric Settings
    private val _lyricFontSize = MutableStateFlow(prefs.getFloat("lyric_font_size", 28f))
    val lyricFontSize = _lyricFontSize.asStateFlow()

    private val _lyricInactiveAlpha = MutableStateFlow(prefs.getFloat("lyric_inactive_alpha", 0.35f))
    val lyricInactiveAlpha = _lyricInactiveAlpha.asStateFlow()

    private val _lyricActiveScale = MutableStateFlow(prefs.getFloat("lyric_active_scale", 1.05f))
    val lyricActiveScale = _lyricActiveScale.asStateFlow()

    private val _lyricLineSpacing = MutableStateFlow(prefs.getFloat("lyric_line_spacing", 12f))
    val lyricLineSpacing = _lyricLineSpacing.asStateFlow()

    private val _showLyricsProgress = MutableStateFlow(prefs.getBoolean("show_lyrics_progress", true))
    val showLyricsProgress = _showLyricsProgress.asStateFlow()

    private val _showTimerOnPlayButton = MutableStateFlow(prefs.getBoolean("show_timer_on_play_button", false))
    val showTimerOnPlayButton = _showTimerOnPlayButton.asStateFlow()

    // Persistent Navigation State
    private val _currentFilter = MutableStateFlow(
        runCatching { LibraryFilter.valueOf(prefs.getString("last_filter", LibraryFilter.ALBUMS.name)!!) }.getOrDefault(LibraryFilter.ALBUMS)
    )
    val currentFilter = _currentFilter.asStateFlow()

    private val _isPlayerFullScreen = MutableStateFlow(false)
    val isPlayerFullScreen = _isPlayerFullScreen.asStateFlow()

    private val _isFullScreenLyricsVisible = MutableStateFlow(false)
    val isFullScreenLyricsVisible = _isFullScreenLyricsVisible.asStateFlow()

    private val _isCompactLyricsMode = MutableStateFlow(false)
    val isCompactLyricsMode = _isCompactLyricsMode.asStateFlow()

    private val _lyricPreviewSongId = MutableStateFlow<Long?>(null)
    val lyricPreviewSongId = _lyricPreviewSongId.asStateFlow()

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

    private val _viewModeSettings = MutableStateFlow<Map<String, ViewModeSettings>>(
        prefs.all.filterKeys { it.startsWith("view_mode_") }.mapValues { entry ->
            val value = entry.value as String
            val parts = value.split("|")
            ViewModeSettings(
                viewMode = runCatching { CategoryViewMode.valueOf(parts[0]) }.getOrDefault(CategoryViewMode.DETAILED),
                columns = parts.getOrNull(1)?.toIntOrNull() ?: 2
            )
        }.mapKeys { it.key.removePrefix("view_mode_") }
    )
    val viewModeSettings = _viewModeSettings.asStateFlow()

    private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        private var debounceJob: Job? = null
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(2000) // Wait for multiple changes to settle
                loadSongs(fromMediaStore = true)
            }
        }
    }

    init {
        application.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver
        )
    }

    // Highly Optimized Flows
    val filteredSongs = combine(_songs, debouncedSearchQuery, _tabSortSettings) { allSongs, query, settings ->
        val currentSettings = settings[LibraryFilter.SONGS] ?: TabSortSettings()
        
        // Fast path for empty query
        if (query.isBlank()) {
            return@combine when (currentSettings.sortType) {
                SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) allSongs.sortedBy { it.title } else allSongs.sortedByDescending { it.title }
                SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) allSongs.sortedBy { it.artist } else allSongs.sortedByDescending { it.artist }
                SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) allSongs.sortedBy { it.year } else allSongs.sortedByDescending { it.year }
                else -> allSongs
            }
        }

        // Optimized filtering: check title first as it's more likely to match
        val list = allSongs.filter { 
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
        }
        
        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.title } else list.sortedByDescending { it.title }
            SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.artist } else list.sortedByDescending { it.artist }
            SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.year } else list.sortedByDescending { it.year }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedAlbums = combine(_allAlbums, debouncedSearchQuery, _tabSortSettings) { albums, query, settings ->
        val currentSettings = settings[LibraryFilter.ALBUMS] ?: TabSortSettings()
        
        // Fast path for empty query
        if (query.isBlank()) {
            return@combine when (currentSettings.sortType) {
                SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) albums.sortedBy { it.title } else albums.sortedByDescending { it.title }
                SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) albums.sortedBy { it.artist } else albums.sortedByDescending { it.artist }
                SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) albums.sortedBy { it.year } else albums.sortedByDescending { it.year }
                else -> albums
            }
        }

        val list = albums.filter { 
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
        }
        
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

        if (query.isBlank() && !currentSettings.showOnlyAlbumArtists) {
             return@combine when (currentSettings.sortType) {
                SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) artists.sortedBy { it.name } else artists.sortedByDescending { it.name }
                SortType.ALBUM_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) artists.sortedBy { it.albumCount } else artists.sortedByDescending { it.albumCount }
                SortType.TRACK_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) artists.sortedBy { it.trackCount } else artists.sortedByDescending { it.trackCount }
                else -> artists
            }
        }

        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.name } else list.sortedByDescending { it.name }
            SortType.ALBUM_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.albumCount } else list.sortedByDescending { it.albumCount }
            SortType.TRACK_COUNT -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.trackCount } else list.sortedByDescending { it.trackCount }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumArtMap: StateFlow<Map<Long, android.net.Uri?>> = _allAlbums.map { albums ->
        albums.associate { it.id to it.artworkUri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // UI Persistence
    sealed class Selection {
        data class Album(val id: Long) : Selection()
        data class Artist(val name: String) : Selection()
        data class ArtistCategory(val artistName: String, val category: String) : Selection()
        data class Playlist(val id: String) : Selection()
        data class Genre(val name: String) : Selection()
    }

    private val _selectionStack = MutableStateFlow<List<Selection>>(emptyList())
    val selectionStack = _selectionStack.asStateFlow()

    val selectedDetail = combine(
        _selectionStack,
        sortedAlbums,
        sortedArtists,
        playlists,
        _songs
    ) { stack, albums, artists, playlists, _ ->
        val last = stack.lastOrNull() ?: return@combine null
        when (last) {
            is Selection.Album -> albums.find { it.id == last.id }
            is Selection.Artist -> artists.find { it.name == last.name }
            is Selection.ArtistCategory -> {
                val artist = artists.find { it.name == last.artistName }
                if (artist != null) (artist to last.category) else null
            }
            is Selection.Playlist -> playlists.find { it.id == last.id }
            is Selection.Genre -> last.name
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Keep individual flows for backward compatibility/internal logic if needed, but derived from the stack
    val selectedAlbum = selectedDetail.map { it as? Album }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedArtist = selectedDetail.map { it as? Artist }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedGenreName = selectedDetail.map { it as? String }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedGenreSongs = combine(selectedGenreName, _songs) { name, songs ->
        if (name == null) emptyList()
        else songs.filter { it.genre == name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPlaylist = _selectionStack.map { it.lastOrNull() }.flatMapLatest { selection ->
        if (selection is Selection.Playlist) repository.getPlaylistWithSongs(selection.id)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSelections() {
        _selectionStack.value = emptyList()
    }

    fun popSelection() {
        if (_selectionStack.value.isNotEmpty()) {
            _selectionStack.value = _selectionStack.value.dropLast(1)
        }
    }

    fun setSelectedAlbumId(id: Long?) {
        if (id == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Album(id)
        }
    }

    fun setSelectedArtistName(name: String?) {
        if (name == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Artist(name)
        }
    }

    fun setSelectedArtistCategory(artistName: String, category: String) {
        _selectionStack.value = _selectionStack.value + Selection.ArtistCategory(artistName, category)
    }

    fun setSelectedPlaylistId(id: String?) {
        if (id == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Playlist(id)
        }
    }

    fun setSelectedGenreName(name: String?) {
        if (name == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Genre(name)
        }
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

    fun setScanOnlyMusicFolder(only: Boolean) {
        _scanOnlyMusicFolder.value = only
        prefs.edit().putBoolean("scan_only_music", only).apply()
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

    fun setShowLyricsProgress(show: Boolean) {
        _showLyricsProgress.value = show
        prefs.edit().putBoolean("show_lyrics_progress", show).apply()
    }

    fun setShowTimerOnPlayButton(show: Boolean) {
        _showTimerOnPlayButton.value = show
        prefs.edit().putBoolean("show_timer_on_play_button", show).apply()
    }

    fun setCurrentFilter(filter: LibraryFilter) {
        _currentFilter.value = filter
        prefs.edit().putString("last_filter", filter.name).apply()
    }

    fun setPlayerFullScreen(full: Boolean) {
        _isPlayerFullScreen.value = full
    }

    private val _viewingLyrics = MutableStateFlow<String?>(null)
    val viewingLyrics: StateFlow<String?> = _viewingLyrics.asStateFlow()

    fun setFullScreenLyricsVisible(visible: Boolean, compactMode: Boolean = false, songId: Long? = null) {
        _isFullScreenLyricsVisible.value = visible
        _isCompactLyricsMode.value = compactMode
        _lyricPreviewSongId.value = songId
        if (visible && songId != null) {
            val song = _songs.value.find { it.id == songId }
            if (song != null) {
                viewModelScope.launch {
                    _viewingLyrics.value = repository.getLyrics(song.path)
                }
            }
        } else if (!visible) {
            _viewingLyrics.value = null
        }
    }

    fun updateSortSettings(filter: LibraryFilter, settings: TabSortSettings) {
        _tabSortSettings.value = _tabSortSettings.value.toMutableMap().apply {
            put(filter, settings)
        }
    }

    fun updateViewModeSettings(key: String, settings: ViewModeSettings) {
        _viewModeSettings.value = _viewModeSettings.value.toMutableMap().apply {
            put(key, settings)
        }
        prefs.edit().putString("view_mode_$key", "${settings.viewMode.name}|${settings.columns}").apply()
    }

    fun loadSongs(refresh: Boolean = false, fromMediaStore: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val onlyMusic = _scanOnlyMusicFolder.value
            
            if (refresh) {
                _isRefreshing.value = true
                repository.scanMusicFolders(onlyMusic)
            }

            // 1. Fetch data (either from cache or by scanning MediaStore)
            val useCache = !refresh && !fromMediaStore
            val songs = repository.getSongs(useCache = useCache, onlyMusicFolder = onlyMusic)
            _songs.value = songs
            
            val albums = repository.getAlbums(songs, useCache = useCache)
            _allAlbums.value = albums
            
            val artists = repository.getArtists(songs, albums, useCache = useCache)
            _allArtists.value = artists

            if (refresh) {
                _isRefreshing.value = false
            }
        }
    }

    fun loadLyricsForCurrentSong() {
        val player = _player.value ?: return
        val currentItem = player.currentMediaItem ?: return
        val mediaId = currentItem.mediaId.toLongOrNull() ?: return

        // Skip if already loaded for this specific song
        if (mediaId == lastLoadedMediaId && (_currentLyrics.value != null || _currentBitrate.value != null)) {
            return
        }

        val song = _songs.value.find { it.id == mediaId } ?: return

        viewModelScope.launch {
            _isLyricsLoading.value = true
            
            // Only clear if the song has actually changed
            if (mediaId != lastLoadedMediaId) {
                _currentLyrics.value = null
                _currentBitrate.value = null
                lastLoadedMediaId = mediaId
            }
            
            launch {
                if (_currentLyrics.value == null) {
                    _currentLyrics.value = repository.getLyrics(song.path)
                }
            }
            launch {
                if (_currentBitrate.value == null) {
                    _currentBitrate.value = repository.getDetailedBitrate(song.path)
                }
            }
            
            _isLyricsLoading.value = false
        }
    }

    fun setPlayer(p: Player?) {
        _player.value?.removeListener(playerListener)
        _player.value = p
        p?.addListener(playerListener)
        _currentPlayingId.value = p?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
        loadLyricsForCurrentSong()
    }

    private var playJob: Job? = null
    fun playSongs(songs: List<Song>, startIndex: Int, shuffle: Boolean = false) {
        val player = _player.value ?: return
        if (songs.isEmpty()) return
        val safeStartIndex = startIndex.coerceIn(0, songs.size - 1)
        
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.Default) {
            val mediaItems = songs.map { it.toMediaItem() }
            withContext(Dispatchers.Main) {
                try {
                    player.stop()
                    player.clearMediaItems()
                    
                    // Optimization for large lists to prevent Binder transaction failures
                    if (mediaItems.size > 500) {
                        // Add target item first for immediate feedback
                        player.setMediaItem(mediaItems[safeStartIndex])
                        player.prepare()
                        player.play()
                        player.shuffleModeEnabled = shuffle

                        val before = mediaItems.subList(0, safeStartIndex)
                        val after = mediaItems.subList(safeStartIndex + 1, mediaItems.size)
                        
                        // Add the rest in background chunks
                        after.chunked(200).forEach { chunk ->
                            player.addMediaItems(chunk)
                        }
                        var insertPos = 0
                        before.chunked(200).forEach { chunk ->
                            player.addMediaItems(insertPos, chunk)
                            insertPos += chunk.size
                        }
                    } else {
                        player.setMediaItems(mediaItems, safeStartIndex, 0L)
                        player.shuffleModeEnabled = shuffle
                        player.prepare()
                        player.play()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addPlayNext(song: Song) {
        val player = _player.value ?: return
        val index = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        player.addMediaItem(index, song.toMediaItem(isPlayNext = true))
    }

    fun addToPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            val favoritesId = "favorites"
            // Ensure Favorites playlist exists
            val playlists = repository.getAllPlaylists().first()
            if (playlists.none { it.id == favoritesId }) {
                repository.createPlaylist("Favorites", favoritesId, isFavorite = true)
            }
            
            val playlistSongs = repository.getSongsForPlaylist(favoritesId).first()
            if (playlistSongs.contains(songId)) {
                repository.removeSongFromPlaylist(favoritesId, songId)
            } else {
                repository.addSongToPlaylist(favoritesId, songId)
            }
        }
    }

    private fun Song.toMediaItem(isPlayNext: Boolean = false): MediaItem {
        val extras = Bundle().apply {
            putBoolean("is_play_next", isPlayNext)
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .setExtras(extras)
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentPlayingId.value = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            // Don't clear manually here, let loadLyricsForCurrentSong handle it gracefully
            loadLyricsForCurrentSong()
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        _player.value?.removeListener(playerListener)
    }

    fun exportPlaylists(outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportPlaylists(outputStream)
        }
    }

    fun importPlaylists(inputStream: InputStream) {
        viewModelScope.launch {
            repository.importPlaylists(inputStream)
        }
    }
}
