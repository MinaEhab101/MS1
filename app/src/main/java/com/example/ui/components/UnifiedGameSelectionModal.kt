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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class RoyalGameModule {
    CHESS,
    DOMINOES,
    BACKGAMMON
}

@Composable
fun UnifiedGameSelectionModal(
    isOpen: Boolean,
    currentModule: RoyalGameModule = RoyalGameModule.CHESS,
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSelectChess: () -> Unit = {},
    onSelectDominoes: () -> Unit = {},
    onSelectBackgammon: () -> Unit = {},
    onSelectGameMode: (module: RoyalGameModule, modeIndex: Int) -> Unit = { _, _ -> }
) {
    if (!isOpen) return

    var activeTab by remember { mutableStateOf(currentModule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = BoardWoodDark,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close Button and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "صالة الألعاب الثلاثية 3D",
                                color = GoldSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Royal 3D Board Game Suite",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // VIP User Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, GoldDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👤", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = userProfile?.username ?: "Player",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userProfile?.coins ?: 1000}",
                            color = GoldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3-way Module Segmented Switcher (Chess, Domino, Backgammon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Chess Tab
                    val isChess = (activeTab == RoyalGameModule.CHESS)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isChess) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF2C2442), Color(0xFF4527A0))))
                                else Modifier.background(Color.Transparent)
                            )
                            .border(1.dp, if (isChess) GoldPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { activeTab = RoyalGameModule.CHESS }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "♟️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "شطرنج",
                                color = if (isChess) GoldSecondary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dominoes Tab
                    val isDomino = (activeTab == RoyalGameModule.DOMINOES)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isDomino) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))))
                                else Modifier.background(Color.Transparent)
                            )
                            .border(1.dp, if (isDomino) GoldPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { activeTab = RoyalGameModule.DOMINOES }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🁫", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "دومينو",
                                color = if (isDomino) GoldSecondary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Backgammon Tab
                    val isBg = (activeTab == RoyalGameModule.BACKGAMMON)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isBg) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF5A2A12), BoardWoodMedium)))
                                else Modifier.background(Color.Transparent)
                            )
                            .border(1.dp, if (isBg) GoldPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { activeTab = RoyalGameModule.BACKGAMMON }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎲", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "طاولة",
                                color = if (isBg) GoldSecondary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Module Showcase Card
                when (activeTab) {
                    RoyalGameModule.CHESS -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.radialGradient(listOf(Color(0xFF512DA8), Color(0xFF311B92))))
                                            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "♟️", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "CHESS 3D (الشطرنج)",
                                            color = GoldSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "قواعد رسمية كاملة، ذكاء اصطناعي خبير وتحدي أونلاين",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GameModeQuickButtons(
                                    onOnline = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.CHESS, 0)
                                        onSelectChess()
                                    },
                                    onAI = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.CHESS, 1)
                                        onSelectChess()
                                    },
                                    onPassPlay = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.CHESS, 2)
                                        onSelectChess()
                                    },
                                    onRoom = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.CHESS, 3)
                                        onSelectChess()
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSelectChess()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("enter_chess_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Chess", tint = BoardWoodDark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "دخول الشطرنج (Enter Chess 3D)", color = BoardWoodDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    RoyalGameModule.DOMINOES -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFF2E7D32), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F261C))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.radialGradient(listOf(Color(0xFF388E3C), Color(0xFF1B5E20))))
                                            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🁫", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "DOMINO 3D (الدومينو)",
                                            color = GoldSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "أحجار عاجية 3D مع سحب وتراص وحساب النقاط تلقائياً",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GameModeQuickButtons(
                                    onOnline = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.DOMINOES, 0)
                                        onSelectDominoes()
                                    },
                                    onAI = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.DOMINOES, 1)
                                        onSelectDominoes()
                                    },
                                    onPassPlay = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.DOMINOES, 2)
                                        onSelectDominoes()
                                    },
                                    onRoom = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.DOMINOES, 3)
                                        onSelectDominoes()
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSelectDominoes()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("enter_domino_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Dominoes", tint = BoardWoodDark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "دخول الدومينو (Enter Dominoes)", color = BoardWoodDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    RoyalGameModule.BACKGAMMON -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, GoldDark, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.radialGradient(listOf(Color(0xFF8D532B), BoardWoodDark)))
                                            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🎲", fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "BACKGAMMON 3D (الطاولة)",
                                            color = GoldSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "طاولة الزهر الملكية بنرد فيزيائي ثلاثي الأبعاد",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                GameModeQuickButtons(
                                    onOnline = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.BACKGAMMON, 0)
                                        onSelectBackgammon()
                                    },
                                    onAI = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.BACKGAMMON, 1)
                                        onSelectBackgammon()
                                    },
                                    onPassPlay = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.BACKGAMMON, 2)
                                        onSelectBackgammon()
                                    },
                                    onRoom = {
                                        onDismiss()
                                        onSelectGameMode(RoyalGameModule.BACKGAMMON, 3)
                                        onSelectBackgammon()
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSelectBackgammon()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("enter_backgammon_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Backgammon", tint = BoardWoodDark, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "دخول طاولة الزهر (Enter Backgammon)", color = BoardWoodDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun GameModeQuickButtons(
    onOnline: () -> Unit,
    onAI: () -> Unit,
    onPassPlay: () -> Unit,
    onRoom: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Online
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .clickable { onOnline() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Public, contentDescription = "Online", tint = GoldSecondary, modifier = Modifier.size(16.dp))
                Text(text = "أونلاين", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // AI
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .clickable { onAI() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI", tint = GoldSecondary, modifier = Modifier.size(16.dp))
                Text(text = "الذكاء", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2P
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .clickable { onPassPlay() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Group, contentDescription = "2 Players", tint = GoldSecondary, modifier = Modifier.size(16.dp))
                Text(text = "لاعبان", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Room
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .clickable { onRoom() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = "Room", tint = GoldSecondary, modifier = Modifier.size(16.dp))
                Text(text = "غرفة", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
