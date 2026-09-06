package com.saavn.music.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.saavn.music.data.model.UserProfile
import com.saavn.music.ui.theme.*

@Composable
fun LoginDialog(
    userProfile: UserProfile?,
    onGoogleSignInClick: () -> Unit,
    onQuickSignIn: (String, String) -> Unit = { _, _ -> },
    onUpdateUsername: (String) -> Unit = {},
    onSignOutClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var editingUsername by remember(userProfile) {
        mutableStateOf(userProfile?.displayName ?: "")
    }
    var showEditNameField by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.3f))
                    ),
                    RoundedCornerShape(24.dp)
                ),
            color = DarkSurfaceGlass
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.15f),
                                DarkBackground.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                // Close Icon
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (userProfile == null) {
                        // LOGGED OUT STATE
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Welcome to ISAI",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sign in with your Google Account for 1-click access and personal playlists.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Google Sign In Button (Standard Intent)
                        Button(
                            onClick = onGoogleSignInClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F1F1F)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1-Click Instant Google Account Login Fallback
                        OutlinedButton(
                            onClick = {
                                onQuickSignIn("JEEVA ⚡", "jeeva.google@gmail.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeonCyan
                            )
                        ) {
                            Text(
                                text = "⚡ 1-Click Instant Login (Google)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                    } else {
                        // LOGGED IN STATE
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, NeonCyan, CircleShape)
                        ) {
                            if (!userProfile.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Avatar",
                                    tint = NeonCyan,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userProfile.displayName.ifBlank { "Google User" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = userProfile.email,
                            fontSize = 13.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Username Section
                        if (showEditNameField) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = editingUsername,
                                    onValueChange = { editingUsername = it },
                                    label = { Text("Edit Username", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (editingUsername.isNotBlank()) {
                                            onUpdateUsername(editingUsername.trim())
                                            showEditNameField = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { showEditNameField = true }
                            ) {
                                Text(
                                    text = "✏️ Edit Username",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout Button
                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF5252)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF5252)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Out",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
