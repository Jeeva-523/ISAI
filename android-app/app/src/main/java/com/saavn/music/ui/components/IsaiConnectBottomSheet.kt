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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.connect.DeviceInfo
import com.saavn.music.connect.IsaiConnectManager
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsaiConnectBottomSheet(
    connectManager: IsaiConnectManager,
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
                            .background(IsaiLime.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = IsaiLime,
                            modifier = Modifier.size(24.dp)
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
                        Text(
                            text = "Seamless multi-device audio sync",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
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
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaiLime.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = IsaiLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "CURRENT AUDIO OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IsaiLime,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (currentActiveId == myDeviceId)
                                "Playing on This Device (${connectManager.deviceName})"
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
                subtitle = "This Android Phone",
                platform = "android",
                isActivePlayer = currentActiveId == myDeviceId,
                presenceText = "🟢 Active now",
                presenceColor = IsaiLime,
                onClick = { connectManager.transferPlaybackToDevice(myDeviceId) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Available Devices
            val otherDevices = devices.filter { it.deviceId != myDeviceId }
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
                            device.isActive && diffMs < 35000 -> "🟢 Active now" to IsaiLime
                            diffMs < 300000 -> "🟡 Active ${(diffMs / 60000).coerceAtLeast(1)}m ago" to Color(0xFFFFC107)
                            else -> "⚫ Offline" to TextSecondary
                        }

                        DeviceItemRow(
                            deviceName = device.deviceName,
                            subtitle = device.platform.replaceFirstChar { it.uppercase() },
                            platform = device.platform,
                            isActivePlayer = currentActiveId == device.deviceId,
                            presenceText = pText,
                            presenceColor = pColor,
                            onClick = { connectManager.transferPlaybackToDevice(device.deviceId) }
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
    val icon = when (platform.lowercase()) {
        "android", "ios", "tablet" -> Icons.Default.PhoneAndroid
        "windows", "mac" -> Icons.Default.Laptop
        else -> Icons.Default.Computer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isActivePlayer) IsaiLime.copy(alpha = 0.12f) else DarkBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActivePlayer) IsaiLime else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActivePlayer) IsaiLime else Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActivePlayer) IsaiLime else Color.White
                        )
                    )
                    Text(
                        text = presenceText,
                        style = MaterialTheme.typography.bodySmall.copy(color = presenceColor)
                    )
                }
            }

            if (isActivePlayer) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = IsaiLime
                ) {
                    Text(
                        text = "Active Player",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
