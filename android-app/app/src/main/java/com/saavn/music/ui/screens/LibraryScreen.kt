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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.data.local.UserPlaylist
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.components.PlaylistNativeAdCard
import com.saavn.music.ui.components.SongListNativeAdItem
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkBorder
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

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
    val publicPlaylists by viewModel.publicPlaylists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val localDeviceSongs by viewModel.localStorage.localDeviceSongs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistSubTab by remember { mutableIntStateOf(0) }
    var selectedPlaylistForView by remember { mutableStateOf<UserPlaylist?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- 1. Top Modern Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your Library",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f))))
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${favorites.size + localDeviceSongs.size} Songs",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Personal collection & device storage",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedTab == 3) {
                    IconButton(
                        onClick = { viewModel.scanDeviceMusic(context) },
                        modifier = Modifier
                            .size(42.dp)
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

                IconButton(
                    onClick = { showCreatePlaylistDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple)))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = DarkBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- 2. Stats Summary Quick Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Favorites",
                count = "${favorites.size}",
                icon = Icons.Default.Favorite,
                tint = NeonPink,
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 0; selectedPlaylistForView = null }
            )
            StatCard(
                title = "Playlists",
                count = "${playlists.size}",
                icon = Icons.Default.PlaylistPlay,
                tint = NeonCyan,
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 1; selectedPlaylistForView = null }
            )
            StatCard(
                title = "Local",
                count = "${localDeviceSongs.size}",
                icon = Icons.Default.SdCard,
                tint = IsaiLime,
                isSelected = selectedTab == 3,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 3; selectedPlaylistForView = null }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. Pill-Shaped Segmented Tab Bar ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(tabs) { index, title ->
                val isSelected = selectedTab == index
                val tabBrush = if (isSelected) Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)) else SolidColor(DarkSurfaceGlass)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(tabBrush)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else GlassBorderSubtle,
                            RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            selectedTab = index
                            selectedPlaylistForView = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) DarkBackground else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Main Tab Content ---
        when (selectedTab) {
            0 -> {
                // Favorites List
                if (favorites.isEmpty()) {
                    EmptyLibraryView(
                        icon = Icons.Default.Favorite,
                        title = "No Favorites Yet",
                        subtitle = "Tap the heart icon on any song to save your top Tamil tracks here!"
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

                            if ((index + 1) % 5 == 0) {
                                SongListNativeAdItem(
                                    userProfile = userProfile,
                                    slotIndex = 200 + ((index + 1) / 5),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Playlists List
                val activeViewPlaylist = selectedPlaylistForView
                if (activeViewPlaylist != null) {
                    // Specific Playlist Detail View
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedPlaylistForView = null },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceGlass)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activeViewPlaylist.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "${activeViewPlaylist.songs.size} tracks",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (activeViewPlaylist.songs.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.playSong(activeViewPlaylist.songs.first(), activeViewPlaylist.songs) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = DarkBackground, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play All", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (activeViewPlaylist.songs.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Default.PlaylistPlay,
                                title = "Playlist is Empty",
                                subtitle = "Add songs to this playlist from any song's option menu."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                itemsIndexed(activeViewPlaylist.songs) { index, song ->
                                    val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
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

                                    if ((index + 1) == 3 || ((index + 1) > 3 && (index + 1) % 5 == 0)) {
                                        PlaylistNativeAdCard(
                                            userProfile = userProfile,
                                            playlistId = "${activeViewPlaylist.id}_${index + 1}",
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-Tab Switcher: My Playlists vs Community Playlists
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // My Playlists Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (playlistSubTab == 0) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceGlass)
                                    .border(1.dp, if (playlistSubTab == 0) NeonCyan else GlassBorderSubtle, RoundedCornerShape(20.dp))
                                    .clickable { playlistSubTab = 0 }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "📁 My Playlists (${playlists.size})",
                                    color = if (playlistSubTab == 0) NeonCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Community Playlists Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (playlistSubTab == 1) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceGlass)
                                    .border(1.dp, if (playlistSubTab == 1) NeonCyan else GlassBorderSubtle, RoundedCornerShape(20.dp))
                                    .clickable { playlistSubTab = 1 }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "🌐 Community (${publicPlaylists.size})",
                                    color = if (playlistSubTab == 1) NeonCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val activeList = if (playlistSubTab == 0) playlists else publicPlaylists
                        val currentUid = viewModel.getCurrentUserId()

                        if (activeList.isEmpty()) {
                            if (playlistSubTab == 0) {
                                EmptyLibraryView(
                                    icon = Icons.Default.PlaylistPlay,
                                    title = "No Custom Playlists",
                                    subtitle = "Tap the + button above to create your first custom playlist!"
                                )
                            } else {
                                EmptyLibraryView(
                                    icon = Icons.Default.Language,
                                    title = "No Community Playlists Yet",
                                    subtitle = "Create a public playlist to share your favourite tracks with all ISAI users!"
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(activeList) { pl ->
                                    val canDelete = (playlistSubTab == 0) || (pl.creatorId.isNotBlank() && pl.creatorId == currentUid)
                                    PlaylistItemCard(
                                        playlist = pl,
                                        isCommunity = (playlistSubTab == 1),
                                        onClick = { viewModel.openCustomPlaylist(pl) },
                                        onDelete = if (canDelete) { { viewModel.deletePlaylist(pl.id) } } else null,
                                        onPlayAll = {
                                            if (pl.songs.isNotEmpty()) {
                                                viewModel.playSong(pl.songs.first(), pl.songs)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Recently Played
                if (recentlyPlayed.isEmpty()) {
                    EmptyLibraryView(
                        icon = Icons.Default.History,
                        title = "No Listening History",
                        subtitle = "Songs you play will automatically appear here for quick replay!"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(recentlyPlayed) { index, song ->
                            val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
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

                            if ((index + 1) % 5 == 0) {
                                SongListNativeAdItem(
                                    userProfile = userProfile,
                                    slotIndex = 400 + ((index + 1) / 5),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // Local Storage Songs
                if (localDeviceSongs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyLibraryView(
                            icon = Icons.Default.SdCard,
                            title = "No Local Songs Found",
                            subtitle = "Tap below to scan MP3 & audio files saved on your device storage."
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.scanDeviceMusic(context) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Device Storage", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        itemsIndexed(localDeviceSongs) { index, song ->
                            val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
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

                            if ((index + 1) % 5 == 0) {
                                SongListNativeAdItem(
                                    userProfile = userProfile,
                                    slotIndex = 500 + ((index + 1) / 5),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Playlist Dialog ---
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = {
                Text(text = "✨ Create Playlist", color = TextPrimary, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("e.g. AR Rahman Mass Beats") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Privacy Setting:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Private Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isPublic) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceGlass)
                                .border(
                                    1.5.dp,
                                    if (!isPublic) NeonCyan else GlassBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { isPublic = false }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🔒", fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Private",
                                        color = if (!isPublic) NeonCyan else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Only you can see this playlist",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Public Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPublic) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceGlass)
                                .border(
                                    1.5.dp,
                                    if (isPublic) NeonCyan else GlassBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { isPublic = true }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🌐", fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Public",
                                        color = if (isPublic) NeonCyan else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Visible to all ISAI app users",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName.trim(), isPublic = isPublic)
                            showCreatePlaylistDialog = false
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Create", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    tint: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) tint.copy(alpha = 0.15f) else DarkSurfaceGlass)
            .border(1.dp, if (isSelected) tint else GlassBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(text = title, fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: UserPlaylist,
    isCommunity: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playlist.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (playlist.isPublic) NeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f))
                                .border(0.8.dp, if (playlist.isPublic) NeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (playlist.isPublic) "🌐 Public" else "🔒 Private",
                                color = if (playlist.isPublic) NeonCyan else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    val creatorText = if (playlist.creatorName.isNotBlank()) "by ${playlist.creatorName} • " else ""
                    Text(
                        text = "$creatorText${playlist.songs.size} tracks",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playlist.songs.isNotEmpty()) {
                    IconButton(
                        onClick = onPlayAll,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    icon: ImageVector,
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
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.1f))
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
