package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GraphicsQuality
import com.example.game.GraphicsSettings
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun GraphicsSettingsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val currentQuality by GraphicsSettings.currentQuality.collectAsState()
    val isAutoScaling by GraphicsSettings.isAutoScaling.collectAsState()
    val profile by GraphicsSettings.deviceProfile.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("حفظ وإغلاق (Save)", color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Graphics Settings",
                    tint = GoldSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "إعدادات الرسوميات والأداء",
                        color = GoldSecondary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3D Graphics & Performance Scaling",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device Hardware Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldDark, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = BoardWoodDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Device specs",
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = profile?.deviceTierLabel ?: "Device Tier Detected",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RAM: ${"%.1f".format(profile?.totalRamGb ?: 4.0f)} GB • CPU: ${profile?.cpuCores ?: 8} Cores",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, GoldPrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "مُطابق 60 FPS",
                                color = GoldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dynamic Auto-Scaling Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الضبط التلقائي الذكي (Auto-Scaling)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ضبط الجودة تلقائياً لتناسب مواصفات هاتفك بدون تقطيع",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = isAutoScaling,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val detected = GraphicsSettings.detectDevicePerformance(context)
                                GraphicsSettings.setQuality(context, detected.recommendedQuality, isAuto = true)
                            } else {
                                GraphicsSettings.setQuality(context, currentQuality, isAuto = false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldPrimary,
                            checkedTrackColor = BoardWoodMedium
                        )
                    )
                }

                // Quality Presets
                Text(
                    text = "مستويات الجودة (Quality Presets):",
                    color = GoldSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                GraphicsQuality.values().forEach { quality ->
                    val isSelected = (currentQuality == quality)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BoardWoodMedium.copy(alpha = 0.8f) else CardDark)
                            .border(
                                1.5.dp,
                                if (isSelected) GoldPrimary else CardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                GraphicsSettings.setQuality(context, quality, isAuto = false)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = quality.displayName,
                                    color = if (isSelected) GoldSecondary else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (profile?.recommendedQuality == quality) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(موصى به)",
                                        color = GoldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = quality.description,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = BoardWoodDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = CardDark,
        shape = RoundedCornerShape(18.dp)
    )
}
