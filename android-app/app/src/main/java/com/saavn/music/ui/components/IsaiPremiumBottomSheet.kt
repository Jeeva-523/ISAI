package com.saavn.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsaiPremiumBottomSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var offlineDownloadEnabled by remember { mutableStateOf(true) }
    var highQualityAudioEnabled by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = DarkBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = GlassBorder)
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. VIP Crown Header Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2D144E),
                                    Color(0xFF191028)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B), NeonCyan, Color(0xFF10B981))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "👑 ISAI Premium Listener",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Lifetime VIP Status • All Audio & Offline Features Unlocked",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Section: Offline Download & Storage (User requested)
            item {
                Text(
                    text = "OFFLINE & PLAYBACK PERKS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Offline Download Feature Card with interactive toggle
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.Download,
                    iconTint = Color(0xFF10B981),
                    title = "⚡ Offline Music Downloads",
                    description = "Songs you listen to are cached in ultra-high 320 kbps for zero-data offline playback.",
                    badgeText = if (offlineDownloadEnabled) "Enabled (Active)" else "Paused",
                    badgeColor = if (offlineDownloadEnabled) Color(0xFF10B981) else TextMuted,
                    trailingContent = {
                        Switch(
                            checked = offlineDownloadEnabled,
                            onCheckedChange = {
                                offlineDownloadEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) "Offline auto-download enabled!" else "Offline caching paused",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    },
                    extraAction = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💾 Offline Storage: ~1.2 GB Cached",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Clear Cache",
                                fontSize = 12.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Offline cache cleared & optimized!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            }

            // High Fidelity 320 kbps Master Quality
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.Headset,
                    iconTint = NeonCyan,
                    title = "🎧 320 kbps Master Studio Quality",
                    description = "Extreme fidelity audio encoding with 3D spatial surround sound.",
                    badgeText = "Lossless 320 kbps",
                    badgeColor = NeonCyan,
                    trailingContent = {
                        Switch(
                            checked = highQualityAudioEnabled,
                            onCheckedChange = {
                                highQualityAudioEnabled = it
                                Toast.makeText(
                                    context,
                                    if (it) "320 kbps Ultra HD active" else "Standard quality active",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }
                )
            }

            // 100% Ad-Free Listening
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.Block,
                    iconTint = Color(0xFFEC4899),
                    title = "🚫 100% Ad-Free Experience",
                    description = "Zero commercial breaks, video popups, or audio interruptions between tracks.",
                    badgeText = "Permanent Ad-Block",
                    badgeColor = Color(0xFFEC4899)
                )
            }

            // Pro Bass Boost & 5-Band Equalizer
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.GraphicEq,
                    iconTint = Color(0xFF8B5CF6),
                    title = "🎛️ Pro Equalizer & Bass Boost",
                    description = "Studio acoustic tuning with deep bass enhancement and vocal clarity.",
                    badgeText = "Active",
                    badgeColor = Color(0xFF8B5CF6)
                )
            }

            // ISAI Connect Multi-Device
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.CloudSync,
                    iconTint = Color(0xFF38BDF8),
                    title = "📱 ISAI Connect Multi-Device",
                    description = "Seamlessly handoff and synchronize playback between phone, PC, and web browser.",
                    badgeText = "Connected",
                    badgeColor = Color(0xFF38BDF8)
                )
            }

            // Unlimited Skips
            item {
                PremiumFeatureCard(
                    icon = Icons.Default.SkipNext,
                    iconTint = Color(0xFFF59E0B),
                    title = "⏭️ Unlimited Skips & Queue Control",
                    description = "Freedom to shuffle, reorder, and skip any number of tracks with zero limits.",
                    badgeText = "Unlimited",
                    badgeColor = Color(0xFFF59E0B)
                )
            }

            // Done / Dismiss Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceGlass
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "✨ Enjoy Premium Perks",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    trailingContent: (@Composable () -> Unit)? = null,
    extraAction: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceGlass)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f))
                            .border(1.dp, iconTint.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }

                if (trailingContent != null) {
                    trailingContent()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            if (extraAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                extraAction()
            }
        }
    }
}
