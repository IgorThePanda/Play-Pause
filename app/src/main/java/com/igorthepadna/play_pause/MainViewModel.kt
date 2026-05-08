package com.igorthepadna.play_pause

import android.app.Application
import android.content.Context
import android.net.Uri
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
import kotlinx.serialization.Serializable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    private val _currentPlayingId = MutableStateFlow(
        prefs.getLong("last_played_id", -1L)
    )
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
        .debounce(300L) // Balanced for responsiveness and performance
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

    private val _showBitrateInfo = MutableStateFlow(prefs.getBoolean("show_bitrate_info", true))
    val showBitrateInfo = _showBitrateInfo.asStateFlow()

    private val _navBarAtTop = MutableStateFlow(prefs.getBoolean("navbar_at_top", false))
    val navBarAtTop = _navBarAtTop.asStateFlow()

    // General Settings
    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean("gapless_playback", true))
    val gaplessPlayback = _gaplessPlayback.asStateFlow()

    private val _scanOnlyMusicFolder = MutableStateFlow(prefs.getBoolean("scan_only_music", false))
    val scanOnlyMusicFolder = _scanOnlyMusicFolder.asStateFlow()

    private val _navBarOrder = MutableStateFlow(
        prefs.getString("navbar_order", null)?.split(",")?.mapNotNull { name ->
            runCatching { LibraryFilter.valueOf(name) }.getOrNull()
        } ?: LibraryFilter.entries
    )
    val navBarOrder = _navBarOrder.asStateFlow()

    fun setNavBarOrder(order: List<LibraryFilter>) {
        _navBarOrder.value = order
        prefs.edit().putString("navbar_order", order.joinToString(",") { it.name }).apply()
    }

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

    // Default Behavior Settings
    private val _shuffleByDefault = MutableStateFlow(prefs.getBoolean("shuffle_by_default", false))
    val shuffleByDefault = _shuffleByDefault.asStateFlow()

    private val _repeatByDefault = MutableStateFlow(prefs.getBoolean("repeat_by_default", false))
    val repeatByDefault = _repeatByDefault.asStateFlow()

    private val _lyricsByDefault = MutableStateFlow(prefs.getBoolean("lyrics_by_default", false))
    val lyricsByDefault = _lyricsByDefault.asStateFlow()

    // Persistent Navigation State
    private val _currentFilter = MutableStateFlow(
        runCatching { LibraryFilter.valueOf(prefs.getString("last_filter", LibraryFilter.ALBUMS.name)!!) }.getOrDefault(LibraryFilter.ALBUMS)
    )
    val currentFilter = _currentFilter.asStateFlow()

    private val _artistSelectionDialog = MutableStateFlow<List<String>?>(null)
    val artistSelectionDialog = _artistSelectionDialog.asStateFlow()

    fun showArtistSelection(artists: List<String>) {
        _artistSelectionDialog.value = artists
    }

    fun dismissArtistSelection() {
        _artistSelectionDialog.value = null
    }

    private val _isPlayerFullScreen = MutableStateFlow(false)
    val isPlayerFullScreen = _isPlayerFullScreen.asStateFlow()

    private val _isLyricsOnCover = MutableStateFlow(false)
    val isLyricsOnCover = _isLyricsOnCover.asStateFlow()

    private val _isFullScreenLyricsVisible = MutableStateFlow(false)
    val isFullScreenLyricsVisible = _isFullScreenLyricsVisible.asStateFlow()

    private val _isCompactLyricsMode = MutableStateFlow(false)
    val isCompactLyricsMode = _isCompactLyricsMode.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog = _showCreatePlaylistDialog.asStateFlow()

    private val _songSelectionPlaylistId = MutableStateFlow<String?>(null)
    val songSelectionPlaylistId = _songSelectionPlaylistId.asStateFlow()

    private val _coverEditingPlaylistId = MutableStateFlow<String?>(null)
    val coverEditingPlaylistId = _coverEditingPlaylistId.asStateFlow()

    fun setShowCreatePlaylistDialog(show: Boolean) {
        _showCreatePlaylistDialog.value = show
    }

    fun setShowSongSelectionForPlaylist(playlistId: String?) {
        _songSelectionPlaylistId.value = playlistId
    }

    fun setCoverEditingPlaylistId(playlistId: String?) {
        _coverEditingPlaylistId.value = playlistId
    }

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
            it.title.contains(query, ignoreCase = true) || 
            it.artist.contains(query, ignoreCase = true) ||
            (it.albumArtist?.contains(query, ignoreCase = true) ?: false)
        }
        
        when (currentSettings.sortType) {
            SortType.TITLE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.title } else list.sortedByDescending { it.title }
            SortType.ARTIST -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.artist } else list.sortedByDescending { it.artist }
            SortType.RELEASE_DATE -> if (currentSettings.sortOrder == SortOrder.ASC) list.sortedBy { it.year } else list.sortedByDescending { it.year }
            else -> list
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val albumArtMap: StateFlow<Map<Long, android.net.Uri?>> = _allAlbums.map { albums ->
        albums.associate { it.id to it.artworkUri }
    }.debounce(300) // Optimization: Prevent UI thread from being overwhelmed during library scanning
    .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val sortedAlbums = combine(_allAlbums, debouncedSearchQuery, _tabSortSettings) { albums, query, settings ->
        val currentSettings = settings[LibraryFilter.ALBUMS] ?: TabSortSettings()
        
        val list = if (query.isBlank()) {
            albums
        } else {
            albums.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true)
            }
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
        
        var list = if (query.isBlank()) artists else artists.filter { artist ->
            artist.name.contains(query, ignoreCase = true) ||
            artist.songs.any { it.title.contains(query, ignoreCase = true) } ||
            artist.featuredSongs.any { it.title.contains(query, ignoreCase = true) }
        }
        
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

    val genres = _songs.map { songs ->
        songs.mapNotNull { it.genre }.distinct().sorted()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val songsMap = songs.map { list ->
        list.associateBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentPlayingSong = combine(currentPlayingId, songsMap) { id, map ->
        map[id]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPlayingAlbum = combine(currentPlayingSong, _allAlbums) { song, albums ->
        song?.let { s -> albums.find { it.id == s.albumId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Persistence
    @Serializable
    sealed class Selection {
        @Serializable
        data class Album(val id: Long) : Selection()
        @Serializable
        data class Artist(val name: String) : Selection()
        @Serializable
        data class ArtistCategory(val artistName: String, val category: String) : Selection()
        @Serializable
        data class Playlist(val id: String) : Selection()
        @Serializable
        data class PlaylistInfo(val id: String) : Selection()
        @Serializable
        data class Genre(val name: String) : Selection()
        @Serializable
        data object Stats : Selection()
    }

    private val _selectionStack = MutableStateFlow<List<Selection>>(
        runCatching { 
            Json.decodeFromString<List<Selection>>(prefs.getString("selection_stack", "[]")!!)
        }.getOrDefault(emptyList())
    )
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
            is Selection.PlaylistInfo -> last
            is Selection.Genre -> last.name
            is Selection.Stats -> "STATS"
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
        if (selection is Selection.Playlist || selection is Selection.PlaylistInfo) {
            val id = when(selection) {
                is Selection.Playlist -> selection.id
                is Selection.PlaylistInfo -> selection.id
                else -> ""
            }
            repository.getPlaylistWithSongs(id)
        }
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSelections() {
        _selectionStack.value = emptyList()
        saveSelectionStack()
    }

    fun popSelection() {
        if (_selectionStack.value.isNotEmpty()) {
            _selectionStack.value = _selectionStack.value.dropLast(1)
            saveSelectionStack()
        }
    }

    fun setSelectedAlbumId(id: Long?) {
        if (id == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Album(id)
            saveSelectionStack()
        }
    }

    fun setSelectedArtistName(name: String?) {
        if (name == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Artist(name)
            saveSelectionStack()
        }
    }

    fun setSelectedArtistCategory(artistName: String, category: String) {
        _selectionStack.value = _selectionStack.value + Selection.ArtistCategory(artistName, category)
        saveSelectionStack()
    }

    fun setSelectedPlaylistId(id: String?) {
        if (id == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Playlist(id)
            saveSelectionStack()
        }
    }

    fun setShowStats() {
        _selectionStack.value = _selectionStack.value + Selection.Stats
        saveSelectionStack()
    }

    fun setSelectedPlaylistInfoId(id: String?) {
        if (id == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.PlaylistInfo(id)
            saveSelectionStack()
        }
    }

    fun setSelectedGenreName(name: String?) {
        if (name == null) {
            clearSelections()
        } else {
            _selectionStack.value = _selectionStack.value + Selection.Genre(name)
            saveSelectionStack()
        }
    }

    private fun saveSelectionStack() {
        prefs.edit().putString("selection_stack", Json.encodeToString(_selectionStack.value)).apply()
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

    fun setShowBitrateInfo(show: Boolean) {
        _showBitrateInfo.value = show
        prefs.edit().putBoolean("show_bitrate_info", show).apply()
    }

    fun setNavBarAtTop(top: Boolean) {
        _navBarAtTop.value = top
        prefs.edit().putBoolean("navbar_at_top", top).apply()
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

    fun setShuffleByDefault(enabled: Boolean) {
        _shuffleByDefault.value = enabled
        prefs.edit().putBoolean("shuffle_by_default", enabled).apply()
    }

    fun setRepeatByDefault(enabled: Boolean) {
        _repeatByDefault.value = enabled
        prefs.edit().putBoolean("repeat_by_default", enabled).apply()
    }

    fun setLyricsByDefault(enabled: Boolean) {
        _lyricsByDefault.value = enabled
        prefs.edit().putBoolean("lyrics_by_default", enabled).apply()
    }

    fun setCurrentFilter(filter: LibraryFilter) {
        if (_currentFilter.value != filter) {
            _currentFilter.value = filter
            clearSelections()
            prefs.edit().putString("last_filter", filter.name).apply()
        }
    }

    fun setPlayerFullScreen(full: Boolean) {
        _isPlayerFullScreen.value = full
        if (!full) {
            _isLyricsOnCover.value = false
        }
    }

    fun setLyricsOnCover(visible: Boolean) {
        _isLyricsOnCover.value = visible
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
            _lyricPreviewSongId.value = null
            _isCompactLyricsMode.value = false
        }
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

        val song = songsMap.value[mediaId] ?: return

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
    fun playSongs(songs: List<Song>, startIndex: Int, shuffle: Boolean? = null) {
        val player = _player.value ?: return
        if (songs.isEmpty()) return
        val safeStartIndex = startIndex.coerceIn(0, songs.size - 1)

        val finalShuffle = shuffle ?: _shuffleByDefault.value
        val finalRepeatMode = if (_repeatByDefault.value) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

        // Quick check for queue stability
        val isSameQueue = player.mediaItemCount == songs.size && 
                         safeStartIndex < player.mediaItemCount &&
                         player.getMediaItemAt(safeStartIndex).mediaId == songs[safeStartIndex].id.toString()
        
        if (isSameQueue) {
            player.seekTo(safeStartIndex, 0L)
            player.play()
            player.shuffleModeEnabled = finalShuffle
            player.repeatMode = finalRepeatMode
            
            if (_lyricsByDefault.value) {
                setPlayerFullScreen(true)
                setLyricsOnCover(true)
            }
            return
        }

        // Immediate playback of the clicked song for zero-latency feedback
        val targetSong = songs[safeStartIndex]
        val targetMediaItem = targetSong.toMediaItem()
        
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(targetMediaItem)
        player.prepare()
        player.play()
        player.shuffleModeEnabled = finalShuffle
        player.repeatMode = finalRepeatMode

        if (_lyricsByDefault.value) {
            setPlayerFullScreen(true)
            setLyricsOnCover(true)
        }

        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.Main) {
            try {
                // Load remaining songs in background to prevent UI blocking (IPC overhead)
                if (songs.size > 1) {
                    val beforeSongs = songs.subList(0, safeStartIndex)
                    val afterSongs = songs.subList(safeStartIndex + 1, songs.size)

                    withContext(Dispatchers.Default) {
                        val afterItems = afterSongs.map { it.toMediaItem() }
                        val beforeItems = beforeSongs.map { it.toMediaItem() }
                        
                        withContext(Dispatchers.Main) {
                            if (afterItems.isNotEmpty()) {
                                player.addMediaItems(afterItems)
                            }
                            if (beforeItems.isNotEmpty()) {
                                player.addMediaItems(0, beforeItems)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    fun setPlaylistCover(playlistId: String, uri: Uri?) {
        viewModelScope.launch {
            repository.setPlaylistCover(playlistId, uri)
        }
    }

    fun updatePlaylistName(playlistId: String, name: String) {
        viewModelScope.launch {
            repository.updatePlaylistName(playlistId, name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            popSelection()
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
                    .setArtworkUri(if (trackNumber > 0 || discNumber > 1) albumArtUri else uri)
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

    private var lastRecordedMediaId: Long = -1L
    private var currentSongPlayTime: Long = 0L
    private var lastTimerTick: Long = 0L
    private val PLAY_THRESHOLD_MS = 15000L

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            if (id != _currentPlayingId.value) {
                currentSongPlayTime = 0L
                lastRecordedMediaId = -1L
            }
            _currentPlayingId.value = id
            prefs?.edit()?.putLong("last_played_id", id)?.apply()
            loadLyricsForCurrentSong()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                lastTimerTick = System.currentTimeMillis()
                startTrackingStats()
            }
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            savePlaybackPosition()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                savePlaybackPosition()
            }
        }
    }

    private var statsJob: Job? = null
    private fun startTrackingStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                val player = _player.value
                if (player != null && player.isPlaying) {
                    val now = System.currentTimeMillis()
                    val delta = now - lastTimerTick
                    lastTimerTick = now
                    
                    val currentId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
                    if (currentId != -1L && currentId != lastRecordedMediaId) {
                        currentSongPlayTime += delta
                        if (currentSongPlayTime >= PLAY_THRESHOLD_MS) {
                            repository.recordPlayEvent(currentId)
                            lastRecordedMediaId = currentId
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun savePlaybackPosition() {
        val player = _player.value ?: return
        if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
            prefs.edit().putLong("last_played_position", player.currentPosition).apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        _player.value?.removeListener(playerListener)
    }

    fun getTopTracks(limit: Int = 10) = repository.getTopTracks(limit)
    fun getTopArtists(limit: Int = 10) = repository.getTopArtists(limit)
    fun getTotalPlayCount() = repository.getTotalPlayCount()
    fun getDailyPlayCounts(since: Long) = repository.getDailyPlayCounts(since)
    fun clearStats() = viewModelScope.launch { repository.clearStats() }

    fun exportStats(outputStream: OutputStream) {
        viewModelScope.launch {
            // Reusing exportPlaylists logic which now includes playEvents
            repository.exportPlaylists(outputStream)
        }
    }

    fun exportPlaylists(outputStream: OutputStream) {
        viewModelScope.launch {
            repository.exportPlaylists(outputStream)
        }
    }

    fun exportPlaylistsToFolder(playlistIds: List<String>, folderUri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch {
            playlistIds.forEach { id ->
                val playlist = repository.getPlaylistSync(id) ?: return@forEach
                val filename = "playlist_${playlist.name.replace(" ", "_")}.json"
                try {
                    val treeUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        folderUri, 
                        android.provider.DocumentsContract.getTreeDocumentId(folderUri)
                    )
                    val fileUri = android.provider.DocumentsContract.createDocument(
                        contentResolver, 
                        treeUri, 
                        "application/json", 
                        filename
                    )
                    if (fileUri != null) {
                        contentResolver.openOutputStream(fileUri)?.use { os ->
                            repository.exportSinglePlaylist(id, os)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun importPlaylists(inputStream: InputStream) {
        viewModelScope.launch {
            repository.importPlaylists(inputStream)
        }
    }
}
