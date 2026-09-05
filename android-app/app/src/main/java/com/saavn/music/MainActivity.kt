package com.saavn.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.components.AddToPlaylistDialog
import com.saavn.music.ui.components.MiniPlayer
import com.saavn.music.ui.screens.HomeScreen
import com.saavn.music.ui.screens.LibraryScreen
import com.saavn.music.ui.screens.PlayerScreen
import com.saavn.music.ui.screens.SearchScreen
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.SaavnMusicTheme
import com.saavn.music.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaavnMusicTheme {
                IsaiApp()
            }
        }
    }
}

@Composable
fun IsaiApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showFullPlayer by viewModel.showFullPlayer.collectAsState()
    val addToPlaylistSong by viewModel.showAddToPlaylistDialog.collectAsState()

    // Back button handling
    BackHandler(enabled = showFullPlayer || currentScreen != AppScreen.HOME) {
        when {
            showFullPlayer -> viewModel.closeFullPlayer()
            currentScreen != AppScreen.HOME -> viewModel.setScreen(AppScreen.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .systemBarsPadding()
    ) {
        // Main Screen Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                AppScreen.LIBRARY -> LibraryScreen(viewModel = viewModel)
            }
        }

        // Bottom Controls Column: Floating MiniPlayer + Glass Bottom Navigation Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Persistent Mini Player
            MiniPlayer(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            // Modern Glass Bottom Navigation Bar
            GlassBottomNavigationBar(
                currentScreen = currentScreen,
                onSelectScreen = { viewModel.setScreen(it) }
            )
        }

        // Full Screen Embedded YouTube Player Modal
        AnimatedVisibility(
            visible = showFullPlayer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            PlayerScreen(viewModel = viewModel)
        }

        // Add to Playlist Dialog
        addToPlaylistSong?.let { song ->
            AddToPlaylistDialog(
                song = song,
                viewModel = viewModel,
                onDismiss = { viewModel.closeAddToPlaylistDialog() }
            )
        }
    }
}

@Composable
fun GlassBottomNavigationBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTabItem(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = currentScreen == AppScreen.HOME,
                onClick = { onSelectScreen(AppScreen.HOME) }
            )

            NavigationTabItem(
                title = "Search",
                icon = Icons.Default.Search,
                isSelected = currentScreen == AppScreen.SEARCH,
                onClick = { onSelectScreen(AppScreen.SEARCH) }
            )

            NavigationTabItem(
                title = "Library",
                icon = Icons.Default.LibraryMusic,
                isSelected = currentScreen == AppScreen.LIBRARY,
                onClick = { onSelectScreen(AppScreen.LIBRARY) }
            )
        }
    }
}

@Composable
fun NavigationTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f)))
                    else Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) NeonCyan else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
