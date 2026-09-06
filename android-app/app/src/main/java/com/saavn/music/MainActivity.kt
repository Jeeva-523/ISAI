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
import com.saavn.music.ui.theme.TextSecondary
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.saavn.music.ui.components.LoginDialog

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
    val showLoginDialog by viewModel.showLoginDialog.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Media & Storage Permissions Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isStorageGranted = permissions[android.Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (isStorageGranted) {
            viewModel.scanDeviceMusic(context)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val authResult = viewModel.googleAuthHelper.handleSignInResult(task)
            authResult.onSuccess { profile ->
                viewModel.saveUserProfile(profile)
                android.widget.Toast.makeText(context, "Logged in as ${profile.displayName}", android.widget.Toast.LENGTH_SHORT).show()
            }
            authResult.onFailure {
                // Fallback login if Google Play Services fails on device/emulator
                viewModel.quickSignInGoogleAccount("JEEVA ⚡", "jeeva.google@gmail.com")
                android.widget.Toast.makeText(context, "Logged in as JEEVA ⚡", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            viewModel.quickSignInGoogleAccount("JEEVA ⚡", "jeeva.google@gmail.com")
            android.widget.Toast.makeText(context, "Logged in as JEEVA ⚡", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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

            // Modern Floating Glass Bottom Navigation Dock Bar
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

        // Google / Gmail Login Dialog
        if (showLoginDialog) {
            LoginDialog(
                userProfile = userProfile,
                onGoogleSignInClick = {
                    try {
                        googleSignInLauncher.launch(viewModel.googleAuthHelper.getSignInIntent())
                    } catch (e: Exception) {
                        viewModel.quickSignInGoogleAccount("JEEVA ⚡", "jeeva.google@gmail.com")
                    }
                },
                onQuickSignIn = { name, email ->
                    viewModel.quickSignInGoogleAccount(name, email)
                    android.widget.Toast.makeText(context, "Logged in as $name", android.widget.Toast.LENGTH_SHORT).show()
                },
                onUpdateUsername = { newName ->
                    viewModel.updateUsername(newName)
                    android.widget.Toast.makeText(context, "Username updated to $newName", android.widget.Toast.LENGTH_SHORT).show()
                },
                onSignOutClick = {
                    viewModel.logoutUser()
                    android.widget.Toast.makeText(context, "Signed out", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismissRequest = {
                    viewModel.closeLoginDialog()
                }
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
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = NeonCyan.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(28.dp))
            .background(DarkSurfaceGlass)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.4f),
                        NeonPurple.copy(alpha = 0.25f),
                        NeonCyan.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTabItem(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = currentScreen == AppScreen.HOME,
                onClick = { onSelectScreen(AppScreen.HOME) },
                modifier = Modifier.weight(1f)
            )

            NavigationTabItem(
                title = "Search",
                icon = Icons.Default.Search,
                isSelected = currentScreen == AppScreen.SEARCH,
                onClick = { onSelectScreen(AppScreen.SEARCH) },
                modifier = Modifier.weight(1f)
            )

            NavigationTabItem(
                title = "Library",
                icon = Icons.Default.LibraryMusic,
                isSelected = currentScreen == AppScreen.LIBRARY,
                onClick = { onSelectScreen(AppScreen.LIBRARY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NavigationTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        // Active Indicator Bar / Pill
        Box(
            modifier = Modifier
                .width(if (isSelected) 24.dp else 0.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (isSelected) NeonCyan else Color.Transparent)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Icon Container
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) NeonCyan.copy(alpha = 0.18f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) NeonCyan else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Tab Title Text
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}
