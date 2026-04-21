package com.igorthepadna.play_pause.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.igorthepadna.play_pause.MainViewModel
import com.igorthepadna.play_pause.data.Album
import com.igorthepadna.play_pause.data.Artist
import com.igorthepadna.play_pause.data.Song
import com.igorthepadna.play_pause.utils.ArtworkColors
import com.igorthepadna.play_pause.utils.rememberArtworkColors
import com.igorthepadna.play_pause.utils.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailView(
    artist: Artist,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlayArtist: (List<Song>, Int, Boolean?) -> Unit = { _, _, _ -> },
    onSongClick: (Song) -> Unit = {},
    onSongDetailsClick: (Song) -> Unit = {},
    onPlayAllSongs: (List<Song>, Int, Boolean?) -> Unit = { _, _, _ -> },
    onPlaySpecificSongs: (List<Song>, Int, Boolean?) -> Unit = { _, _, _ -> },
    onExpandCategory: (String) -> Unit = {},
    viewModel: MainViewModel? = null
) {
    val allAlbums by viewModel?.sortedAlbums?.collectAsStateWithLifecycle(emptyList<Album>()) ?: remember { mutableStateOf(emptyList<Album>()) }
    val gridState = rememberLazyGridState()
    val artworkColors = rememberArtworkColors(
        artworkUri = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri,
        defaultPrimary = MaterialTheme.colorScheme.surface,
        defaultSecondary = MaterialTheme.colorScheme.primary
    )

    val (mainAlbums, singles) = remember(artist.albums) {
        artist.albums.partition { it.songs.size > 2 }
    }

    val unreleasedSongs = remember(artist.songs) {
        artist.songs.filter { song ->
            val folderName = song.path.substringBeforeLast('/').substringAfterLast('/')
            folderName.startsWith("XXXX", ignoreCase = true)
        }
    }

    val bannerThresholdPx = with(LocalDensity.current) { 200.dp.toPx() }
    val showBanner by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > bannerThresholdPx
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val bottomPadding = PaddingValues(
            top = 16.dp, 
            start = 16.dp,
            end = 16.dp,
            bottom = 140.dp
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = bottomPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScrollbar(gridState, padding = bottomPadding)
        ) {
            item(span = { GridItemSpan(2) }) {
                ArtistHighFidelityHeader(
                    artist = artist,
                    onBack = onBack,
                    onPlay = { onPlayArtist(artist.songs, 0, false) }
                )
            }

            // --- Albums Section (Horizontal) ---
            if (mainAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column {
                        SectionHeader(title = "Albums", onExpandClick = { onExpandCategory("Albums") })
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            lazyItems(mainAlbums.take(5)) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) },
                                    modifier = Modifier.width(180.dp),
                                    columns = 2
                                )
                            }
                        }
                    }
                }
            }

            // --- Singles Section (Horizontal) ---
            if (singles.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column {
                        SectionHeader(title = "Singles & EPs", onExpandClick = { onExpandCategory("Singles & EPs") })
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            lazyItems(singles.take(5)) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) },
                                    modifier = Modifier.width(180.dp),
                                    columns = 2
                                )
                            }
                        }
                    }
                }
            }

            // --- Unreleased Section ---
            if (unreleasedSongs.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        SectionHeader(title = "Unreleased", onExpandClick = { onExpandCategory("Unreleased") })
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            unreleasedSongs.take(5).forEach { song ->
                                val albumArt = remember(allAlbums, song) {
                                    allAlbums.find { it.id == song.albumId }?.artworkUri ?: song.albumArtUri
                                }
                                CompactSongItem(
                                    song = song,
                                    isPlaying = false,
                                    onClick = { onSongClick(song) },
                                    onDetailsClick = { onSongDetailsClick(song) },
                                    onSwipePlayNext = {},
                                    onSwipeAddToPlaylist = {},
                                    artworkUri = albumArt,
                                    containerColor = Color.Transparent
                                )
                            }
                        }
                    }
                }
            }

            // --- All Songs Section ---
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp)
                            .clickable { onExpandCategory("All Songs") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "All Songs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                        }

                        IconButton(
                            onClick = { onPlayAllSongs(artist.songs, 0, true) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle All",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        artist.songs.take(10).forEach { song ->
                            val albumArt = remember(allAlbums, song) {
                                allAlbums.find { it.id == song.albumId }?.artworkUri ?: song.albumArtUri
                            }
                            CompactSongItem(
                                song = song,
                                isPlaying = false, 
                                onClick = { onSongClick(song) },
                                onDetailsClick = { onSongDetailsClick(song) },
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                artworkUri = albumArt,
                                containerColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            ArtistPillBanner(artist = artist, artworkColors = artworkColors, onBack = onBack)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailView(
    title: String,
    artist: Artist,
    mainAlbums: List<Album>,
    singles: List<Album>,
    unreleasedSongs: List<Song>,
    viewMode: CategoryViewMode,
    columns: Int,
    onViewModeChange: (CategoryViewMode) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongDetailsClick: (Song) -> Unit,
    onPlayAllSongs: (List<Song>, Int, Boolean?) -> Unit,
    onPlaySpecificSongs: (List<Song>, Int, Boolean?) -> Unit,
    allAlbums: List<Album> = emptyList()
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, fontWeight = FontWeight.Black)
                        Text(
                            artist.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (viewMode == CategoryViewMode.GRID) {
                            TextButton(
                                onClick = {
                                    val newColumns = if (columns >= 4) 1 else columns + 1
                                    onColumnsChange(newColumns)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("$columns", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }

                        IconButton(onClick = {
                            val newMode = when (viewMode) {
                                CategoryViewMode.GRID -> CategoryViewMode.DETAILED
                                CategoryViewMode.DETAILED -> CategoryViewMode.COMPACT
                                CategoryViewMode.COMPACT -> if (title.contains("Song") || title == "Unreleased") CategoryViewMode.DETAILED else CategoryViewMode.GRID
                            }
                            onViewModeChange(newMode)
                        }) {
                            Icon(
                                when (viewMode) {
                                    CategoryViewMode.GRID -> Icons.Rounded.GridView
                                    CategoryViewMode.DETAILED -> Icons.Rounded.ViewStream
                                    CategoryViewMode.COMPACT -> Icons.Rounded.ViewHeadline
                                },
                                contentDescription = "View Mode"
                            )
                        }

                        Spacer(Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val effectiveColumns = if (viewMode == CategoryViewMode.GRID) columns else 1
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(effectiveColumns),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (title) {
                "Albums" -> {
                    gridItems(mainAlbums) { album ->
                        when (viewMode) {
                            CategoryViewMode.GRID -> AlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) },
                                columns = columns
                            )
                            CategoryViewMode.COMPACT -> CompactSongItem(
                                song = album.songs.first(),
                                isPlaying = false,
                                onClick = { onAlbumClick(album) },
                                onDetailsClick = {},
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                label = album.title,
                                secondaryLabel = album.artist,
                                artworkUri = album.artworkUri,
                                onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) }
                            )
                            CategoryViewMode.DETAILED -> SongItem(
                                song = album.songs.first(),
                                isPlaying = false,
                                onClick = { onAlbumClick(album) },
                                onDetailsClick = {},
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                label = album.title,
                                secondaryLabel = album.artist,
                                artworkUri = album.artworkUri
                            )
                        }
                    }
                }
                "Singles & EPs" -> {
                    gridItems(singles) { album ->
                        when (viewMode) {
                            CategoryViewMode.GRID -> AlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) },
                                columns = columns
                            )
                            CategoryViewMode.COMPACT -> CompactSongItem(
                                song = album.songs.first(),
                                isPlaying = false,
                                onClick = { onAlbumClick(album) },
                                onDetailsClick = {},
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                label = album.title,
                                secondaryLabel = album.artist,
                                artworkUri = album.artworkUri,
                                onPlayClick = { onPlaySpecificSongs(album.songs, 0, null) }
                            )
                            CategoryViewMode.DETAILED -> SongItem(
                                song = album.songs.first(),
                                isPlaying = false,
                                onClick = { onAlbumClick(album) },
                                onDetailsClick = {},
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                label = album.title,
                                secondaryLabel = album.artist,
                                artworkUri = album.artworkUri
                            )
                        }
                    }
                }
                "Unreleased" -> {
                    gridItems(unreleasedSongs) { song ->
                        val albumArt = remember(allAlbums, song) {
                            allAlbums.find { it.id == song.albumId }?.artworkUri ?: song.albumArtUri
                        }
                        when (viewMode) {
                            CategoryViewMode.GRID -> AlbumCard(
                                album = Album(
                                    id = song.albumId,
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUri = albumArt,
                                    songs = listOf(song)
                                ),
                                onClick = { onSongClick(song) },
                                onPlayClick = { onSongClick(song) },
                                columns = columns
                            )
                            CategoryViewMode.COMPACT -> CompactSongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onSongClick(song) },
                                onDetailsClick = { onSongDetailsClick(song) },
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                showArtist = false,
                                onPlayClick = { onSongClick(song) },
                                artworkUri = albumArt
                            )
                            CategoryViewMode.DETAILED -> SongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onSongClick(song) },
                                onDetailsClick = { onSongDetailsClick(song) },
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                artworkUri = albumArt
                            )
                        }
                    }
                }
                "All Songs" -> {
                    gridItems(artist.songs) { song ->
                        val albumArt = remember(allAlbums, song) {
                            allAlbums.find { it.id == song.albumId }?.artworkUri ?: song.albumArtUri
                        }
                        when (viewMode) {
                            CategoryViewMode.GRID -> AlbumCard(
                                album = Album(
                                    id = song.albumId,
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUri = albumArt,
                                    songs = listOf(song)
                                ),
                                onClick = { onSongClick(song) },
                                onPlayClick = { onSongClick(song) },
                                columns = columns
                            )
                            CategoryViewMode.COMPACT -> CompactSongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onSongClick(song) },
                                onDetailsClick = { onSongDetailsClick(song) },
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                showArtist = true,
                                onPlayClick = { onSongClick(song) },
                                artworkUri = albumArt
                            )
                            CategoryViewMode.DETAILED -> SongItem(
                                song = song,
                                isPlaying = false,
                                onClick = { onSongClick(song) },
                                onDetailsClick = { onSongDetailsClick(song) },
                                onSwipePlayNext = {},
                                onSwipeAddToPlaylist = {},
                                artworkUri = albumArt
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onExpandClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clickable { onExpandClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
    }
}

@Composable
fun ArtistPillBanner(
    artist: Artist,
    artworkColors: ArtworkColors,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, artworkColors.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, 
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            val model = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ArtistHighFidelityHeader(artist: Artist, onBack: () -> Unit, onPlay: () -> Unit) {
    val model = artist.thumbnailUri ?: artist.albums.firstOrNull()?.artworkUri
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
        ) {
            // Main Artwork (Centered Circle)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .padding(8.dp),
                shape = CircleShape,
                tonalElevation = 12.dp,
                shadowElevation = 20.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                var isError by remember { mutableStateOf(false) }
                Box(contentAlignment = Alignment.Center) {
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = artist.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { isError = true },
                            onSuccess = { isError = false }
                        )
                    }
                    if (model == null || isError) {
                        ArtistPlaceholder(artist.name)
                    }
                }
            }

            // Glassmorphic Back Button (Top Left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onBack() }
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(24.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, 
                    "Back", 
                    tint = Color.White, 
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }

            // Play Button Pill (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable { onPlay() }
                    .shadow(12.dp, CircleShape)
            ) {
                Box(modifier = Modifier.matchParentSize()) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(24.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
                Icon(
                    Icons.Rounded.PlayArrow,
                    null, 
                    tint = Color.White, 
                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Text(
            text = "${artist.albumCount} Albums • ${artist.trackCount} Songs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}
