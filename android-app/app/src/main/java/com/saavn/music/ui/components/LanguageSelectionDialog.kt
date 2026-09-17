package com.saavn.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

data class MusicLanguageItem(
    val id: String,
    val name: String,
    val nativeName: String,
    val icon: String,
    val gradientColors: List<Color>
)

val MUSIC_LANGUAGES = listOf(
    MusicLanguageItem("tamil", "Tamil", "தமிழ்", "🎵", listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
    MusicLanguageItem("telugu", "Telugu", "తెలుగు", "🎶", listOf(Color(0xFFEC4899), Color(0xFFBE185D))),
    MusicLanguageItem("hindi", "Hindi", "हिंदी", "✨", listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
    MusicLanguageItem("malayalam", "Malayalam", "മലയാളം", "🌴", listOf(Color(0xFF10B981), Color(0xFF047857))),
    MusicLanguageItem("kannada", "Kannada", "ಕನ್ನಡ", "🌟", listOf(Color(0xFF06B6D4), Color(0xFF0E7490))),
    MusicLanguageItem("english", "English", "English", "🎧", listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
)

@Composable
fun LanguageSelectionDialog(
    initialSelected: List<String> = emptyList(),
    currentLanguages: List<String> = initialSelected,
    onSaveLanguages: (List<String>) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var selectedLanguages by remember {
        val base = currentLanguages.ifEmpty { initialSelected }
        mutableStateOf(if (base.isEmpty()) listOf("tamil") else base)
    }

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null, dismissOnClickOutside = onDismiss != null)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(DarkBackground)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🌐 PERSONALIZE YOUR MUSIC",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What do you want to listen to?",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose your music languages to customize your Home feed. Universal search finds songs from any language anytime!",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(MUSIC_LANGUAGES) { item ->
                        val isSelected = selectedLanguages.contains(item.id)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(item.gradientColors)
                                    else Brush.linearGradient(listOf(DarkSurface, DarkSurfaceGlass))
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else GlassBorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        if (selectedLanguages.size > 1) {
                                            selectedLanguages = selectedLanguages - item.id
                                        }
                                    } else {
                                        selectedLanguages = selectedLanguages + item.id
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.icon, fontSize = 22.sp)
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.name,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = item.nativeName,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (selectedLanguages.isNotEmpty()) {
                            onSaveLanguages(selectedLanguages.toList())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan
                    )
                ) {
                    val btnLabel = if (onDismiss != null) {
                        "Save Preferences 🚀 (${selectedLanguages.size} Selected)"
                    } else {
                        "Start Listening 🚀 (${selectedLanguages.size} Selected)"
                    }
                    Text(
                        text = btnLabel,
                        color = DarkBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
