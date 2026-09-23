package com.example.ui.screens.replay

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.game.GameHistoryManager
import com.example.game.GameRecord
import com.example.game.GameType
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameReplayScreen(
    soundManager: SoundManager?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { GameHistoryManager.initialize(context) }

    val records by GameHistoryManager.records.collectAsState()
    var selectedRecord by remember { mutableStateOf<GameRecord?>(records.firstOrNull()) }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) } // 0.5f, 1.0f, 2.0f

    // Auto-advance loop when playing
    LaunchedEffect(isPlaying, currentStepIndex, selectedRecord, playbackSpeed) {
        val record = selectedRecord ?: return@LaunchedEffect
        if (isPlaying) {
            val stepDelay = (1400 / playbackSpeed).toLong()
            delay(stepDelay)
            if (currentStepIndex < record.steps.size - 1) {
                currentStepIndex++
                soundManager?.playCheckerMove()
            } else {
                isPlaying = false
                soundManager?.playVictory()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BoardWoodDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سجل ومسرح الإعادة 3D",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldSecondary
                        )
                        Text(
                            text = "إعادة عرض وتكتيك أفضل المباريات خطوة بخطوة",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("replay_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldSecondary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .border(1.dp, GoldDark, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${records.size} مباريات",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0B08)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val record = selectedRecord
            if (record != null) {
                // Active Replay Card & 3D Visual Theater
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GoldPrimary, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Title & Result Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "ضد: ${record.opponentName} • ${record.dateString}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (record.isWin) Brush.horizontalGradient(listOf(PointHighlight, Color(0xFF1B5E20)))
                                        else Brush.horizontalGradient(listOf(Color(0xFFC62828), Color(0xFF8E0000)))
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (record.isWin) "🏆 فوز (${record.finalScore})" else "خسارة",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3D Replay Board Simulation Display
                        val currentStep = record.steps.getOrNull(currentStepIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.radialGradient(listOf(Color(0xFF381D10), Color(0xFF190C06))))
                                .border(1.2.dp, GoldDark, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (record.gameType == GameType.BACKGAMMON) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🎲", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "الخطوة ${currentStepIndex + 1} من ${record.steps.size}",
                                                color = GoldSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (currentStep?.dice?.isNotEmpty() == true) {
                                                Text(
                                                    text = "النرد: [${currentStep.dice.joinToString(", ")}]",
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Move details badge
                                    if (currentStep?.fromPoint != null && currentStep.toPoint != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF26180E))
                                                .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "حركة من نقطة ${currentStep.fromPoint} ➔ ${if (currentStep.toPoint == -1) "إخراج القطعة (Off)" else "نقطة ${currentStep.toPoint}"}" +
                                                        if (currentStep.isHit) " ⚔️ صيد بلطة!" else "",
                                                color = if (currentStep.isHit) Color(0xFFFF5252) else GoldSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    // Dominoes Replay
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🎴", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "دومينو: الخطوة ${currentStepIndex + 1} من ${record.steps.size}",
                                                color = GoldSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "القطعة: ${currentStep?.dominoTileText ?: "-"} (${currentStep?.dominoEndText ?: ""})",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tactical Move Commentary Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14100C)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎙️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentStep?.commentary ?: "لا يوجد تعليق لهذه الحركة.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Replay Progress Slider
                        Slider(
                            value = currentStepIndex.toFloat(),
                            onValueChange = {
                                currentStepIndex = it.toInt()
                                isPlaying = false
                            },
                            valueRange = 0f..(record.steps.size - 1).coerceAtLeast(1).toFloat(),
                            steps = (record.steps.size - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldSecondary,
                                inactiveTrackColor = CardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Playback Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Restart
                            IconButton(
                                onClick = {
                                    currentStepIndex = 0
                                    isPlaying = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    tint = TextMuted
                                )
                            }

                            // Step Backward
                            IconButton(
                                onClick = {
                                    if (currentStepIndex > 0) {
                                        currentStepIndex--
                                        isPlaying = false
                                        soundManager?.playCheckerMove()
                                    }
                                },
                                enabled = currentStepIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Prev Step",
                                    tint = if (currentStepIndex > 0) GoldSecondary else TextMuted
                                )
                            }

                            // Play / Pause Main Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldPrimary, GoldDark)))
                                    .clickable {
                                        if (currentStepIndex >= record.steps.size - 1) {
                                            currentStepIndex = 0
                                        }
                                        isPlaying = !isPlaying
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = BoardWoodDark,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Step Forward
                            IconButton(
                                onClick = {
                                    if (currentStepIndex < record.steps.size - 1) {
                                        currentStepIndex++
                                        isPlaying = false
                                        soundManager?.playCheckerMove()
                                    }
                                },
                                enabled = currentStepIndex < record.steps.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Next Step",
                                    tint = if (currentStepIndex < record.steps.size - 1) GoldSecondary else TextMuted
                                )
                            }

                            // Speed Selector
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF261D15))
                                    .clickable {
                                        playbackSpeed = when (playbackSpeed) {
                                            0.5f -> 1.0f
                                            1.0f -> 2.0f
                                            else -> 0.5f
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Saved Matches List Header
            Text(
                text = "قائمة المباريات المحفوظة",
                color = GoldSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { rec ->
                    val isCurrent = rec.id == selectedRecord?.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) GoldPrimary else CardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedRecord = rec
                                currentStepIndex = 0
                                isPlaying = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) CardDark else Color(0xFF14100C)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (rec.gameType == GameType.BACKGAMMON) "🎲" else "🎴",
                                    fontSize = 22.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = rec.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${rec.steps.size} حركات • ${rec.dateString}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = if (rec.isWin) "فوز 🏆" else "خسارة",
                                color = if (rec.isWin) PointHighlight else Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
