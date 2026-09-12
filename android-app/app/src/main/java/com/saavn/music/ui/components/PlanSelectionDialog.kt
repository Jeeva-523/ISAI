package com.saavn.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextSecondary

@Composable
fun PlanSelectionDialog(
    currentPlan: String = "FREE",
    isPaymentLoading: Boolean = false,
    paymentMessage: String? = null,
    onSelectFreePlan: () -> Unit = {},
    onInitiatePayment: (planType: String) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    var selectedPlan by remember {
        mutableStateOf(
            if (currentPlan.equals("YEARLY", ignoreCase = true)) "YEARLY"
            else if (currentPlan.equals("PREMIUM", ignoreCase = true) || currentPlan.equals("MONTHLY", ignoreCase = true)) "MONTHLY"
            else "MONTHLY"
        )
    }

    Dialog(
        onDismissRequest = {
            if (!isPaymentLoading) onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.6f), NeonCyan.copy(alpha = 0.5f))),
                    RoundedCornerShape(28.dp)
                ),
            color = DarkSurface,
            shadowElevation = 24.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "ISAI Plans",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), NeonCyan)))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                            Text(
                                text = "Select your subscription plan to unlock full potential",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { if (!isPaymentLoading) onDismissRequest() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Plan Cards
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // --- PLAN 1: PREMIUM MONTHLY (₹49 / month) ---
                        item {
                            val isSelected = selectedPlan == "MONTHLY"
                            val bgBrush = if (isSelected) {
                                Brush.linearGradient(listOf(Color(0xFF2B1647), Color(0xFF18132A)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF16171E), Color(0xFF16171E)))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgBrush)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        brush = if (isSelected) Brush.horizontalGradient(listOf(NeonCyan, Color(0xFFF59E0B))) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f))),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedPlan = "MONTHLY" }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "💎", fontSize = 24.sp)
                                            Column {
                                                Text(
                                                    text = "Premium Monthly",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "₹49 / month",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = NeonCyan
                                                )
                                            }
                                        }

                                        SelectionRadio(isSelected = isSelected, color = NeonCyan)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        PlanFeatureItem(emoji = "🎧", text = "High Quality Audio (320kbps)")
                                        PlanFeatureItem(emoji = "📱", text = "2–3 Device Sync (Connect)")
                                        PlanFeatureItem(emoji = "🎶", text = "Listen Together Room")
                                        PlanFeatureItem(emoji = "🔀", text = "Advanced Queue & Crossfade")
                                        PlanFeatureItem(emoji = "😴", text = "Sleep Timer & Premium Themes")
                                        PlanFeatureItem(emoji = "🤖", text = "Advanced Recommendations")
                                        PlanFeatureItem(emoji = "🚫", text = "No Ads (100% Ad-Free)")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            selectedPlan = "MONTHLY"
                                            onInitiatePayment("MONTHLY")
                                        },
                                        enabled = !isPaymentLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonCyan
                                        )
                                    ) {
                                        Text(
                                            text = "GET PREMIUM MONTHLY - ₹49",
                                            color = Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // --- PLAN 2: PREMIUM YEARLY (₹399 / year) - BEST VALUE ---
                        item {
                            val isSelected = selectedPlan == "YEARLY"
                            val bgBrush = if (isSelected) {
                                Brush.linearGradient(listOf(Color(0xFF381B10), Color(0xFF1E1428)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF16171E), Color(0xFF16171E)))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgBrush)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        brush = if (isSelected) Brush.horizontalGradient(listOf(Color(0xFFF59E0B), NeonCyan)) else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f))),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedPlan = "YEARLY" }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "👑", fontSize = 24.sp)
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = "Premium Yearly",
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFF59E0B))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "SAVE 32%",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "₹399 / year (₹33/mo)",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                        }

                                        SelectionRadio(isSelected = isSelected, color = Color(0xFFF59E0B))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        PlanFeatureItem(emoji = "✨", text = "All Premium features included")
                                        PlanFeatureItem(emoji = "💰", text = "Save ₹189 compared to monthly billing")
                                        PlanFeatureItem(emoji = "⚡", text = "Uninterrupted 12 months VIP access")
                                        PlanFeatureItem(emoji = "🚫", text = "Zero Ads across entire app")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            selectedPlan = "YEARLY"
                                            onInitiatePayment("YEARLY")
                                        },
                                        enabled = !isPaymentLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF59E0B)
                                        )
                                    ) {
                                        Text(
                                            text = "GET PREMIUM YEARLY - ₹399",
                                            color = Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // --- PLAN 3: ISAI FREE (₹0) ---
                        item {
                            val isSelected = selectedPlan == "FREE"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF1D2028) else Color(0xFF14151B))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) IsaiLime else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedPlan = "FREE" }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "🆓", fontSize = 24.sp)
                                            Column {
                                                Text(
                                                    text = "ISAI Free",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "₹0 Free Forever",
                                                    fontSize = 13.sp,
                                                    color = IsaiLime,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        SelectionRadio(isSelected = isSelected, color = IsaiLime)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        PlanFeatureItem(emoji = "🎵", text = "Unlimited song streaming")
                                        PlanFeatureItem(emoji = "🔎", text = "Search & Playlists")
                                        PlanFeatureItem(emoji = "📢", text = "Supported by AdMob ads")
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            selectedPlan = "FREE"
                                            onSelectFreePlan()
                                        },
                                        enabled = !isPaymentLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, IsaiLime.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "Continue with ISAI Free",
                                            color = IsaiLime,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safe Razorpay Checkout Notice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Secured by Razorpay • UPI / Cards / NetBanking",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Loading Overlay for Order Creation & Cryptographic Verification
                if (isPaymentLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = paymentMessage ?: "Processing...",
                                color = Color.White,
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

@Composable
private fun SelectionRadio(isSelected: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) color else Color.Transparent)
            .border(1.5.dp, if (isSelected) color else Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PlanFeatureItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = emoji, fontSize = 13.sp)
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
