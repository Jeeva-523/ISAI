package com.saavn.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.components.IsaiConnectBottomSheet
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val authService = viewModel.authService
    val isUserLoggedIn = authService.isUserLoggedIn()
    val isEmailVerified = authService.isEmailVerified()

    val currentEmail = userProfile?.email?.takeIf { it.isNotBlank() }
        ?: authService.getCurrentUser()?.email?.takeIf { it.isNotBlank() }
        ?: "jeevananthravikumar@gmail.com"

    val currentDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: authService.getCurrentUser()?.displayName?.takeIf { it.isNotBlank() }
        ?: "Jeevananth"


    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(currentDisplayName) }
    var showConnectSheet by remember { mutableStateOf(false) }

    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoritesCount = favorites.size
    val playlistsCount = playlists.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. Top Navigation Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.HOME) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Profile & Account",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Basic Details & Preferences",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 2. Main Hero Profile Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = NeonCyan.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DarkSurfaceVariant,
                                    DarkSurfaceGlass
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.3f))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Profile Avatar Ring
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(12.dp, CircleShape, spotColor = NeonCyan)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(
                                    2.dp,
                                    Brush.sweepGradient(
                                        listOf(NeonCyan, NeonPurple, IsaiLime, NeonCyan)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile?.photoUrl != null) {
                                AsyncImage(
                                    model = userProfile?.photoUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Display Name with Edit Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = currentDisplayName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    editNameInput = currentDisplayName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Display Name",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Email Address
                        Text(
                            text = currentEmail,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Badges Row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Email Verification Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isEmailVerified) Color(0xFF103B2B) else Color(0xFF3B2B10)
                                    )
                                    .border(
                                        1.dp,
                                        if (isEmailVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isEmailVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEmailVerified) "Email Verified" else "Unverified",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmailVerified) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // ISAI Premium Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonPurple.copy(alpha = 0.3f), IsaiLime.copy(alpha = 0.3f))
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        NeonCyan.copy(alpha = 0.6f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ISAI Premium Listener",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Section: Personal & Account Details
            item {
                ProfileSectionContainer(title = "Account Details") {
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Display Name",
                        value = currentDisplayName,
                        onRowClick = {
                            editNameInput = currentDisplayName
                            showEditNameDialog = true
                        }
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "Email Address",
                        value = currentEmail
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.Verified,
                        label = "Security Status",
                        value = if (isEmailVerified) "Email Verified ✓" else "Verification Pending ⚠️"
                    )
                }
            }

            // 4. Section: Audio & Streaming Preferences
            item {
                ProfileSectionContainer(title = "Audio & Playback Quality") {
                    ProfileDetailRow(
                        icon = Icons.Default.Headset,
                        label = "Streaming Quality",
                        value = "320 kbps Ultra HD"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.GraphicEq,
                        label = "Audio Engine",
                        value = "ExoPlayer 3D Equalizer Active"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.Language,
                        label = "Music Language",
                        value = "Tamil, English, Hindi"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.MusicNote,
                        label = "Offline Storage",
                        value = "Extreme High Quality Cache"
                    )
                }
            }

            // 5. Section: App Stats & Connectivity
            item {
                ProfileSectionContainer(title = "Device & App Info") {
                    ProfileDetailRow(
                        icon = Icons.Default.Smartphone,
                        label = "Active Device",
                        value = "Jeeva's Phone (Current)"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.CloudSync,
                        label = "ISAI Connect",
                        value = "Device Sync Ready",
                        onRowClick = { showConnectSheet = true }
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.Favorite,
                        label = "Favorite Tracks",
                        value = "$favoritesCount Liked Songs"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.PlaylistPlay,
                        label = "Custom Playlists",
                        value = "$playlistsCount Playlists"
                    )
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    ProfileDetailRow(
                        icon = Icons.Default.Equalizer,
                        label = "App Version",
                        value = "ISAI v2.4.0 (2026 Build)"
                    )
                }
            }

            // 6. Action Control Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // ISAI Connect Launcher Button
                    Button(
                        onClick = { showConnectSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceGlass
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "📱 Open ISAI Connect Multi-Device",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sign In / Switch Account / Logout Button
                    if (isUserLoggedIn) {
                        Button(
                            onClick = {
                                viewModel.logoutUser()
                                android.widget.Toast.makeText(context, "Signed out successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFEF4444).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A1215)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "🚪 Sign Out Account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.openLoginDialog() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IsaiLime
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "🔑 Sign In / Register Account",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Display Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Edit Display Name",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your preferred name for ISAI Music profile:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            viewModel.updateUsername(editNameInput.trim())
                            android.widget.Toast.makeText(context, "Username updated!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ISAI Connect Bottom Sheet
    if (showConnectSheet) {
        IsaiConnectBottomSheet(
            connectManager = viewModel.isaiConnectManager,
            viewModel = viewModel,
            onDismissRequest = { showConnectSheet = false }
        )
    }
}

@Composable
private fun ProfileSectionContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceGlass)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    onRowClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onRowClick != null) Modifier.clickable { onRowClick() } else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
