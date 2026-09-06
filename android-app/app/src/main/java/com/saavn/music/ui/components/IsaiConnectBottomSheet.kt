package com.saavn.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.connect.DeviceInfo
import com.saavn.music.connect.IsaiConnectManager
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextSecondary

private fun formatDeviceDisplayName(rawName: String): String {
    val cleaned = rawName
        .replace(Regex("(?i)[0-9A-Z]{7,}"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (cleaned.isNotBlank()) cleaned else "Jeeva's Phone"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsaiConnectBottomSheet(
    connectManager: IsaiConnectManager,
    viewModel: com.saavn.music.ui.MainViewModel? = null,
    onDismissRequest: () -> Unit
) {
    val devices by connectManager.devices.collectAsState()
    val playbackState by connectManager.playbackState.collectAsState()

    val myDeviceId = connectManager.deviceId
    val currentActiveId = playbackState?.currentDeviceId?.ifEmpty { myDeviceId } ?: myDeviceId

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.2f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ISAI Connect",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        val activeEmail = connectManager.userEmail.ifBlank { "Guest Account" }
                        Text(
                            text = "⚡ Syncing account: $activeEmail",
                            style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan)
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Audio Output Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "CURRENT AUDIO OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (currentActiveId == myDeviceId)
                                "Playing on ${formatDeviceDisplayName(connectManager.deviceName)}"
                            else
                                "Playing on Remote Device",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: This Device
            Text(
                text = "THIS DEVICE",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            DeviceItemRow(
                deviceName = connectManager.deviceName,
                subtitle = "This Phone",
                platform = "android",
                isActivePlayer = currentActiveId == myDeviceId,
                presenceText = "Active now",
                presenceColor = IsaiLime,
                onClick = {
                    connectManager.transferPlaybackToDevice(myDeviceId)
                    if (viewModel != null) {
                        val sync = connectManager.playbackState.value
                        if (sync != null && sync.currentSongId.isNotBlank()) {
                            val song = com.saavn.music.data.model.YouTubeSong(
                                videoId = sync.currentSongId,
                                title = sync.currentTitle,
                                channelTitle = sync.currentArtist,
                                thumbnailUrl = sync.currentArtwork,
                                audioUrl = sync.currentAudioUrl.ifBlank { null }
                            )
                            val startSec = (sync.positionMs / 1000f).coerceAtLeast(0f)
                            viewModel.playSong(song, startPositionSec = startSec)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Available Devices
            val otherDevices = devices.filter { it.deviceId != myDeviceId && connectManager.isDeviceOnline(it) }
            Text(
                text = "AVAILABLE DEVICES (${otherDevices.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (otherDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBackground)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log into your ISAI account on Chrome or Laptop to switch playback seamlessly!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        overflow = TextOverflow.Clip
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 240.dp)
                ) {
                    items(otherDevices) { device ->
                        val diffMs = System.currentTimeMillis() - device.lastActiveAt
                        val (pText, pColor) = when {
                            device.isActive && diffMs < 35000 -> "Active now" to IsaiLime
                            diffMs < 300000 -> "Active ${(diffMs / 60000).coerceAtLeast(1)}m ago" to Color(0xFFFFC107)
                            else -> "Offline" to TextSecondary
                        }

                        DeviceItemRow(
                            deviceName = device.deviceName,
                            subtitle = device.platform.replaceFirstChar { it.uppercase() },
                            platform = device.platform,
                            isActivePlayer = currentActiveId == device.deviceId,
                            presenceText = pText,
                            presenceColor = pColor,
                            onClick = {
                                val currentSong = viewModel?.ytPlayerController?.currentSong?.value
                                val posMs = ((viewModel?.ytPlayerController?.currentPositionSec?.value ?: 0f) * 1000).toLong()
                                if (currentSong != null) {
                                    connectManager.sendCommand(
                                        action = "PLAY_SONG",
                                        positionMs = posMs,
                                        song = currentSong,
                                        targetDeviceId = device.deviceId
                                    )
                                    viewModel.ytPlayerController.pause()
                                }
                                connectManager.transferPlaybackToDevice(device.deviceId)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DeviceItemRow(
    deviceName: String,
    subtitle: String,
    platform: String,
    isActivePlayer: Boolean,
    presenceText: String,
    presenceColor: Color,
    onClick: () -> Unit
) {
    val cleanName = formatDeviceDisplayName(deviceName)
    val icon = when (platform.lowercase()) {
        "android", "ios", "tablet" -> Icons.Default.Smartphone
        "windows", "mac" -> Icons.Default.Laptop
        else -> Icons.Default.Computer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActivePlayer) 10.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = NeonCyan.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActivePlayer) DarkSurfaceGlass else DarkBackground)
            .border(
                width = if (isActivePlayer) 1.5.dp else 1.dp,
                brush = if (isActivePlayer) {
                    Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                } else {
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Container Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActivePlayer) {
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f)))
                            } else {
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                            }
                        )
                        .border(
                            1.dp,
                            if (isActivePlayer) NeonCyan.copy(alpha = 0.4f) else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActivePlayer) Icons.Default.GraphicEq else icon,
                        contentDescription = null,
                        tint = if (isActivePlayer) NeonCyan else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isActivePlayer) Color.White else TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isActivePlayer) NeonCyan else presenceColor)
                        )
                        Text(
                            text = if (isActivePlayer) "Active Speaker" else presenceText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isActivePlayer) NeonCyan else presenceColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isActivePlayer) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "⚡ Playing",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkBackground,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Switch",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
