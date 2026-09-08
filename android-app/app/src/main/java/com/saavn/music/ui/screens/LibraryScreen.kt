package com.saavn.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saavn.music.data.local.UserPlaylist
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkBorder
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("💖 Favorites", "📂 Playlists", "🕒 Recent", "📱 Local")

    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val localDeviceSongs by viewModel.localStorage.localDeviceSongs.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForView by remember { mutableStateOf<UserPlaylist?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Personal music collection & local songs",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedTab == 3) {
                    // Refresh Device Songs Button
                    IconButton(
                        onClick = { viewModel.scanDeviceMusic(context) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Device Songs",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (selectedTab == 1) {
                    // New Playlist Button
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple)))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = DarkBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceGlass,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan,
                    height = 2.5.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        selectedPlaylistForView = null
                    },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) NeonCyan else TextMuted
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Favorites List
                if (favorites.isEmpty()) {
                    EmptyLibraryView(
                        icon = Icons.Default.Favorite,
                        title = "No Favorites Yet",
                        subtitle = "Tap the heart icon on any song to save it here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(favorites) { index, song ->
                            YouTubeSongRowItem(
                                index = index + 1,
                                song = song,
                                isCurrent = (viewModel.ytPlayerController.currentSong.collectAsState().value?.videoId == song.videoId),
                                isPlaying = viewModel.ytPlayerController.isPlaying.collectAsState().value,
                                onClick = { viewModel.playSong(song, favorites) },
                                isFav = true,
                                onToggleFav = { viewModel.toggleFavorite(song) },
                                onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onPlayNext = { viewModel.playNextInQueue(song) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Playlists List
                val activeViewPlaylist = selectedPlaylistForView
                if (activeViewPlaylist != null) {
                    // Viewing specific playlist
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = activeViewPlaylist.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "${activeViewPlaylist.songs.size} songs",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            TextButton(onClick = { selectedPlaylistForView = null }) {
                                Text("Back to Playlists", color = TextSecondary)
                            }
                        }

                        if (activeViewPlaylist.songs.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Default.PlaylistPlay,
                                title = "Playlist is Empty",
                                subtitle = "Add songs to this playlist from song options."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                itemsIndexed(activeViewPlaylist.songs) { index, song ->
                                    val isFav = viewModel.isFavorite(song.videoId)
                                    YouTubeSongRowItem(
                                        index = index + 1,
                                        song = song,
                                        isCurrent = (viewModel.ytPlayerController.currentSong.collectAsState().value?.videoId == song.videoId),
                                        isPlaying = viewModel.ytPlayerController.isPlaying.collectAsState().value,
                                        onClick = { viewModel.playSong(song, activeViewPlaylist.songs) },
                                        isFav = isFav,
                                        onToggleFav = { viewModel.toggleFavorite(song) },
                                        onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                                        onAddToQueue = { viewModel.addToQueue(song) },
                                        onPlayNext = { viewModel.playNextInQueue(song) }
                                    )
                                }
                            }
                        }
                    }
                } else if (playlists.isEmpty()) {
                    EmptyLibraryView(
                        icon = Icons.Default.PlaylistPlay,
                        title = "No Playlists",
                        subtitle = "Tap the + button to create your first Tamil music playlist!"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(playlists) { pl ->
                            PlaylistItemCard(
                                playlist = pl,
                                onClick = { selectedPlaylistForView = pl },
                                onDelete = { viewModel.deletePlaylist(pl.id) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Recently Played (Latest 20)
                if (recentlyPlayed.isEmpty()) {
                    EmptyLibraryView(
                        icon = Icons.Default.History,
                        title = "No Recent History",
                        subtitle = "Songs you play will automatically appear here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(recentlyPlayed) { index, song ->
                            val isFav = viewModel.isFavorite(song.videoId)
                            YouTubeSongRowItem(
                                index = index + 1,
                                song = song,
                                isCurrent = (viewModel.ytPlayerController.currentSong.collectAsState().value?.videoId == song.videoId),
                                isPlaying = viewModel.ytPlayerController.isPlaying.collectAsState().value,
                                onClick = { viewModel.playSong(song, recentlyPlayed) },
                                isFav = isFav,
                                onToggleFav = { viewModel.toggleFavorite(song) },
                                onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onPlayNext = { viewModel.playNextInQueue(song) }
                            )
                        }
                    }
                }
            }

            3 -> {
                // Device Storage Songs
                if (localDeviceSongs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyLibraryView(
                            icon = Icons.Default.SdCard,
                            title = "No Local Songs Found",
                            subtitle = "Tap the refresh button to scan MP3/Audio files stored on your device."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(
                            onClick = { viewModel.scanDeviceMusic(context) },
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Scan Local Songs", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(localDeviceSongs) { index, song ->
                            val isFav = viewModel.isFavorite(song.videoId)
                            YouTubeSongRowItem(
                                index = index + 1,
                                song = song,
                                isCurrent = (viewModel.ytPlayerController.currentSong.collectAsState().value?.videoId == song.videoId),
                                isPlaying = viewModel.ytPlayerController.isPlaying.collectAsState().value,
                                onClick = { viewModel.playSong(song, localDeviceSongs) },
                                isFav = isFav,
                                onToggleFav = { viewModel.toggleFavorite(song) },
                                onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onPlayNext = { viewModel.playNextInQueue(song) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog to Create Playlist
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(text = "New Playlist", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName.trim())
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PlaylistItemCard(
    playlist: UserPlaylist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = playlist.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.songs.size} tracks",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = { onDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
