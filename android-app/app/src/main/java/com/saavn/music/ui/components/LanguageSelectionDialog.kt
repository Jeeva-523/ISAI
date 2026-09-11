package com.saavn.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.saavn.music.ui.theme.*

data class LanguageItem(
    val id: String,
    val name: String,
    val nativeName: String,
    val icon: String,
    val gradientColors: List<Color>
)

val MUSIC_LANGUAGES = listOf(
    LanguageItem("tamil", "Tamil", "தமிழ்", "🎵", listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
    LanguageItem("telugu", "Telugu", "తెలుగు", "🔥", listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
    LanguageItem("hindi", "Hindi", "हिंदी", "✨", listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))),
    LanguageItem("malayalam", "Malayalam", "മലയാളം", "🌴", listOf(Color(0xFF10B981), Color(0xFF06B6D4))),
    LanguageItem("english", "English", "Global Hits", "🌍", listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
    LanguageItem("kannada", "Kannada", "ಕನ್ನಡ", "🎼", listOf(Color(0xFFF97316), Color(0xFFEAB308))),
    LanguageItem("punjabi", "Punjabi", "ਪੰਜਾਬੀ", "🥁", listOf(Color(0xFF84CC16), Color(0xFF10B981)))
)

@Composable
fun LanguageSelectionDialog(
    initialSelected: List<String> = listOf("tamil"),
    onSaveLanguages: (List<String>) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var selectedLanguages by remember {
        mutableStateOf(
            if (initialSelected.isNotEmpty()) initialSelected.toSet() else setOf("tamil")
        )
    }

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1329), Color(0xFF110B1D))
                    )
                )
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1730))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🌐 PERSONALIZE YOUR MUSIC",
                        color = Color(0xFFA78BFA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What do you want to listen to?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose your music languages to customize your Home feed. Universal search finds songs from any language anytime!",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Language Grid
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
                                    Brush.linearGradient(
                                        if (isSelected) item.gradientColors else listOf(Color(0xFF1E1730), Color(0xFF161026))
                                    )
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0xFF8B5CF6).copy(alpha = 0.2f),
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
                                                tint = Color(0xFF8B5CF6),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = item.nativeName,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Continue / Save Button
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
                        containerColor = Color(0xFF8B5CF6)
                    )
                ) {
                    val btnLabel = if (onDismiss != null) {
                        "Save Preferences 🚀 (${selectedLanguages.size} Selected)"
                    } else {
                        "Start Listening 🚀 (${selectedLanguages.size} Selected)"
                    }
                    Text(
                        text = btnLabel,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
