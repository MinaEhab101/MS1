package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.audio.MusicManager
import com.example.audio.SoundManager
import com.example.firebase.AuthRepository
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    musicManager: MusicManager,
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    var sfxVol by remember { mutableFloatStateOf(soundManager.sfxVolume) }
    var musicVol by remember { mutableFloatStateOf(musicManager.musicVolume) }
    var vibrationEnabled by remember { mutableStateOf(soundManager.isVibrationEnabled) }
    var adsEnabled by remember { mutableStateOf(AdManager.isAdsEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Settings", color = GoldSecondary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BoardWoodDark)
            )
        },
        containerColor = SurfaceDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "AUDIO & HAPTICS",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Sound Effects
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "SFX", tint = GoldSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sound Effects", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text("${(sfxVol * 100).toInt()}%", color = TextSecondary, fontSize = 13.sp)
                        }
                        Slider(
                            value = sfxVol,
                            onValueChange = {
                                sfxVol = it
                                soundManager.sfxVolume = it
                            },
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldDark)
                        )
                    }

                    // Background Music
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Music", tint = GoldSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Background Music", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text("${(musicVol * 100).toInt()}%", color = TextSecondary, fontSize = 13.sp)
                        }
                        Slider(
                            value = musicVol,
                            onValueChange = {
                                musicVol = it
                                musicManager.musicVolume = it
                                if (it > 0f && !musicManager.isPlaying) {
                                    musicManager.startBackgroundMusic()
                                } else if (it <= 0.01f) {
                                    musicManager.stopBackgroundMusic()
                                }
                            },
                            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldDark)
                        )
                    }

                    // Vibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Vibration, contentDescription = "Vibration", tint = GoldSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibration & Haptics", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = {
                                vibrationEnabled = it
                                soundManager.isVibrationEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldDark)
                        )
                    }
                }
            }

            Text(
                text = "GRAPHICS QUALITY",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            // Graphics Quality Card (Low / Medium / High / Ultra)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder)))
            ) {
                val currentQuality by com.example.game.GraphicsSettings.currentQuality.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "3D Engine Fidelity",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Adjust shadow softness, 3D reflections, and lighting effects according to your device performance.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (quality in com.example.game.GraphicsQuality.values()) {
                            val isSelected = quality == currentQuality
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) GoldPrimary else BoardWoodDark
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) GoldSecondary else CardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        com.example.game.GraphicsSettings.setQuality(context, quality)
                                        soundManager.playButtonClick()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = quality.displayName,
                                    color = if (isSelected) BoardWoodDark else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Active: ${currentQuality.description}",
                        color = GoldSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "GAMEPLAY & PREFERENCES",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Google AdMob Ads", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Support game development", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = {
                                adsEnabled = it
                                AdManager.isAdsEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout Button
            Button(
                onClick = {
                    authRepository.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f), contentColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Backgammon King v1.0.0 (Royal 3D Edition)\nBuilt with Kotlin & Jetpack Compose",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                lineHeight = 16.sp
            )
        }
    }
}
