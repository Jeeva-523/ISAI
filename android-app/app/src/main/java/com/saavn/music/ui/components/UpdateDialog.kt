package com.saavn.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saavn.music.BuildConfig
import com.saavn.music.data.model.AppUpdateModel
import com.saavn.music.data.repository.UpdateDownloadState
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkBorder
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceElevated
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.IsaiViolet
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateModel,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate && downloadState !is UpdateDownloadState.Downloading,
            dismissOnClickOutside = !updateInfo.isForceUpdate && downloadState !is UpdateDownloadState.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceElevated,
                            DarkBackground
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(28.dp)
                )
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Icon Badge with Glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0x338B5CF6),
                                    Color(0x11A78BFA)
                                )
                            )
                        )
                        .border(1.dp, Color(0x668B5CF6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = IsaiViolet,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = updateInfo.updateTitle,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version comparison badge: e.g. "v1.0.0 ➔ v1.1.0"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E172B))
                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = IsaiViolet,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${updateInfo.latestVersionName}",
                        color = NeonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Message Body
                Text(
                    text = updateInfo.updateMessage,
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Release Notes (if available)
                if (updateInfo.releaseNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "What's New:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        updateInfo.releaseNotes.forEach { note ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IsaiViolet,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = note,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Download Progress / Status
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Spacer(modifier = Modifier.height(14.dp))
                        val percent = (downloadState.progress * 100).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading update...",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (downloadState.totalMb.isNotBlank()) "${downloadState.currentMb} / ${downloadState.totalMb} ($percent%)" else "$percent%",
                                color = NeonPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = IsaiViolet,
                            trackColor = Color(0x338B5CF6)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✅ Download Complete! Installing...",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    is UpdateDownloadState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "❌ ${downloadState.message}",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    else -> {
                        Spacer(modifier = Modifier.height(22.dp))
                    }
                }

                // Update / Install / Downloading Action Button
                val isDownloading = downloadState is UpdateDownloadState.Downloading
                val isReadyToInstall = downloadState is UpdateDownloadState.ReadyToInstall
                val isError = downloadState is UpdateDownloadState.Error

                Button(
                    onClick = onUpdateClick,
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0x448B5CF6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDownloading) {
                                Brush.horizontalGradient(listOf(Color(0x558B5CF6), Color(0x55EC4899)))
                            } else {
                                Brush.horizontalGradient(listOf(IsaiViolet, NeonPink))
                            }
                        )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val percent = ((downloadState as UpdateDownloadState.Downloading).progress * 100).toInt()
                        Text(
                            text = "Downloading... $percent%",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isReadyToInstall) {
                        Text(
                            text = "Install Update Now ➔",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isError) {
                        Text(
                            text = "Retry Update ➔",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Update Now ➔",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dismiss / Later Button (only if not a forced update and not currently downloading)
                if (!updateInfo.isForceUpdate && !isDownloading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe Later",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
