package com.saavn.music.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saavn.music.connect.ListenTogetherManager
import com.saavn.music.connect.RoomSyncStatus
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherBottomSheet(
    listenManager: ListenTogetherManager,
    currentSong: YouTubeSong? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val currentRoom by listenManager.currentRoom.collectAsState()
    val syncStatus by listenManager.syncStatus.collectAsState()
    val errorMessage by listenManager.errorMessage.collectAsState()

    var joinCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val myDeviceId = listenManager.deviceId
    val isHost = currentRoom?.hostDeviceId == myDeviceId

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IsaiLime.copy(alpha = 0.15f))
                            .border(1.dp, IsaiLime.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Listen Together",
                            tint = IsaiLime,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Listen Together",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Multi-device music sync (Up to 3 devices)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Room Content
            val room = currentRoom
            if (room != null) {
                // ACTIVE ROOM VIEW
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Room Code Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IsaiLime.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROOM CODE",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                when (syncStatus) {
                                    RoomSyncStatus.SYNCED -> {
                                        Text(
                                            text = "● Synchronized",
                                            color = IsaiLime,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    RoomSyncStatus.SYNCING -> {
                                        Text(
                                            text = "● Syncing playback...",
                                            color = Color(0xFFFFD700),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "⚠ Reconnecting...",
                                            color = Color(0xFFFF9800),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = room.roomCode,
                                    color = IsaiLime,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ISAI Room", room.roomCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Current Song in Room
                    room.playbackState.song?.let { song ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = song.artwork,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = if (room.playbackState.state == "PLAYING") "▶ Playing" else "⏸ Paused",
                                    color = IsaiLime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Host vs Guest indicator
                    if (isHost) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = IsaiLime.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IsaiLime.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👑 You are Host",
                                    color = IsaiLime,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Your controls sync to guests)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = IsaiLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Listening along with Host ${room.hostDeviceName}",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Connected Devices List (Max 3)
                    val deviceList = room.devices.values.filter { it.connected }
                    Column {
                        Text(
                            text = "Connected Devices (${deviceList.size}/3)",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        deviceList.forEach { dev ->
                            val isMe = dev.deviceId == myDeviceId
                            val isDevHost = dev.deviceId == room.hostDeviceId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMe) IsaiLime.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (dev.deviceType == "ANDROID") Icons.Default.Smartphone else Icons.Default.Laptop,
                                        contentDescription = null,
                                        tint = if (isMe) IsaiLime else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Column {
                                        Text(
                                            text = dev.deviceName + if (isMe) " (You)" else "",
                                            color = if (isMe) IsaiLime else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (dev.connected) "● Active" else "○ Offline",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isDevHost) {
                                    Text(
                                        text = "HOST",
                                        color = IsaiLime,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(IsaiLime.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Leave Room Button
                    Button(
                        onClick = {
                            listenManager.leaveRoom()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Leave",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Leave Room", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                // NOT IN A ROOM VIEW: Create or Join
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Create Room
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Start a Listening Room",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sync audio playback seamlessly with up to 3 devices.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    isLoading = true
                                    listenManager.createRoom(currentSong) { result ->
                                        isLoading = false
                                        if (result.isFailure) {
                                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = IsaiLime),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isLoading) "Creating Room..." else "⚡ Create New Room",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                        Text(
                            text = "  OR JOIN  ",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    }

                    // Join Room
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter Room Code",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() },
                                placeholder = { Text("e.g. ISAI-7K92", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IsaiLime,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        isLoading = true
                                        listenManager.joinRoom(joinCodeInput) { result ->
                                            isLoading = false
                                            if (result.isSuccess) {
                                                joinCodeInput = ""
                                            } else {
                                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Join failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading && joinCodeInput.isNotBlank(),
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IsaiLime),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Join", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
