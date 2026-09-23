package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.GraphicsSettingsDialog
import com.example.ui.components.RoyalGameModule
import com.example.ui.components.UnifiedGameSelectionModal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdMobBanner
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.firebase.FirestoreRepository
import com.example.game.AIDifficulty
import com.example.game.DailyRewardManager
import com.example.game.GameMode
import com.example.ui.components.RoyalDailyRewardDialog
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    profile: UserProfile?,
    firestoreRepository: FirestoreRepository,
    onStartGame: (GameMode) -> Unit,
    onNavigateToDomino: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToSettings: () -> Unit,
    soundManager: SoundManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAiDifficultyDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(AIDifficulty.MEDIUM) }

    var showDailyRewardDialog by remember { mutableStateOf(false) }
    val canClaimDailyReward by DailyRewardManager.canClaimToday.collectAsState()
    val dailyStreak by DailyRewardManager.currentStreak.collectAsState()

    LaunchedEffect(Unit) {
        DailyRewardManager.checkStatus(context)
        if (DailyRewardManager.canClaimToday.value) {
            showDailyRewardDialog = true
        }
    }

    var showMatchmakingDialog by remember { mutableStateOf(false) }
    var matchmakingStatus by remember { mutableStateOf("Searching for opponent...") }

    var showRoomDialog by remember { mutableStateOf(false) }

    val avatars = listOf("🦁", "🦅", "👑", "🎲", "⚔️")
    val avatarEmoji = avatars.getOrElse(profile?.avatarIndex ?: 0) { "👑" }

    var showGraphicsDialog by remember { mutableStateOf(false) }
    var showUnifiedGameModal by remember { mutableStateOf(false) }

    GraphicsSettingsDialog(
        isOpen = showGraphicsDialog,
        onDismiss = { showGraphicsDialog = false }
    )

    UnifiedGameSelectionModal(
        isOpen = showUnifiedGameModal,
        currentModule = RoyalGameModule.BACKGAMMON,
        userProfile = profile,
        onDismiss = { showUnifiedGameModal = false },
        onSelectBackgammon = { /* Already in Backgammon */ },
        onSelectDominoes = { onNavigateToDomino() }
    )

    RoyalDailyRewardDialog(
        isOpen = showDailyRewardDialog,
        onDismiss = { showDailyRewardDialog = false },
        onRewardClaimed = { coins ->
            soundManager?.playButtonClick()
            scope.launch {
                val newCoins = (profile?.coins ?: 1000) + coins
                firestoreRepository.updateUserCoins(profile?.userId ?: "", newCoins)
            }
        },
        soundManager = soundManager
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // --- 1. Top Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .border(1.dp, CardBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Info Clickable
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToProfile() }
                    .testTag("home_profile_button")
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(GoldSecondary, GoldDark)))
                        .border(1.5.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatarEmoji, fontSize = 22.sp)
                }
                Column {
                    Text(
                        text = profile?.username ?: "Player",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = GoldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${profile?.rating ?: 1200} pts",
                            color = GoldSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "  •  Lvl ${profile?.level ?: 1}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Coins & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(BoardWoodDark, RoundedCornerShape(16.dp))
                        .border(1.dp, GoldDark, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = GoldSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${profile?.coins ?: 1000}",
                        color = GoldSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Game Switcher (Backgammon / Dominoes)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2E7D32), BoardWoodMedium)))
                        .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                        .clickable { showUnifiedGameModal = true }
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎴", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "الألعاب الملكية",
                            color = GoldSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Graphics Settings Button
                IconButton(
                    onClick = { showGraphicsDialog = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("home_graphics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Graphics Settings",
                        tint = GoldSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }

        // --- 2. Main Game Mode Menu ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Royal Hero Brand Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Brush.horizontalGradient(listOf(GoldDark, GoldSecondary, GoldPrimary, GoldDark)), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark, BoardWoodDark)))
                            .border(2.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.backgammon_king_logo),
                            contentDescription = "Backgammon King Crest",
                            modifier = Modifier.size(66.dp).clip(CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = "BACKGAMMON KING",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldSecondary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Royal 3D Backgammon Experience",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPrimary
                        )
                        Text(
                            text = "طاولة الزهر الملكية ثلاثية الأبعاد",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Royal Daily Reward Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDailyRewardDialog = true }
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(GoldDark, GoldSecondary, GoldPrimary, GoldDark)),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("home_daily_reward_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BoardWoodMedium)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark)))
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Daily Bounty",
                                tint = BoardWoodDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ROYAL DAILY REWARD",
                                    color = GoldSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• Day $dailyStreak",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (canClaimDailyReward) "Free Gold Coins waiting for you today!" else "Reward claimed! Streak active",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showDailyRewardDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canClaimDailyReward) GoldPrimary else CardDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (canClaimDailyReward) "CLAIM" else "VIEW",
                            color = if (canClaimDailyReward) BoardWoodDark else GoldSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3D DOMINO KING GAME CARD (NEW FEATURE)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF2E7D32), GoldPrimary, Color(0xFF1B5E20))),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("mode_domino_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102D22))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDomino() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.radialGradient(listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))))
                                    .border(1.5.dp, GoldPrimary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🁢", fontSize = 28.sp)
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DOMINO KING 3D",
                                        color = GoldSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldPrimary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "NEW",
                                            color = BoardWoodDark,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Text(
                                    text = "لعبة الدومينو الملكية 3D باحترافية عالية",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "قطع عاجية ثلاثية الأبعاد • أونلاين وذكاء اصطناعي",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onNavigateToDomino() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "PLAY",
                                color = BoardWoodDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Domino Quick Mode Chips (Online, AI, 2 Players, Room)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDark.copy(alpha = 0.7f))
                                .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToDomino() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🌐", fontSize = 14.sp)
                                Text(text = "أونلاين", color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDark.copy(alpha = 0.7f))
                                .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToDomino() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🤖", fontSize = 14.sp)
                                Text(text = "ضد الذكاء", color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDark.copy(alpha = 0.7f))
                                .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToDomino() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "👥", fontSize = 14.sp)
                                Text(text = "لاعبان", color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDark.copy(alpha = 0.7f))
                                .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToDomino() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🔑", fontSize = 14.sp)
                                Text(text = "غرفة", color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Text(
                text = "🎲 طـاولـة الـزهـر الـمـلـكـيـة (BACKGAMMON MODES)",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            // 1. Play Online
            GameModeCard(
                title = "Online",
                subtitle = "Fast matchmaking with players worldwide",
                icon = Icons.Default.Public,
                highlightColor = GoldPrimary,
                testTag = "mode_online_button",
                onClick = {
                    showMatchmakingDialog = true
                    matchmakingStatus = "Searching for opponent..."
                    scope.launch {
                        delay(1400)
                        matchmakingStatus = "Match found! Preparing board..."
                        delay(800)
                        showMatchmakingDialog = false
                        onStartGame(GameMode.OnlineMatch("match_${System.currentTimeMillis()}", true))
                    }
                }
            )

            // 2. Play vs AI
            GameModeCard(
                title = "Play vs AI",
                subtitle = "Practice against 4 difficulty levels",
                icon = Icons.Default.SmartToy,
                highlightColor = Color(0xFF64B5F6),
                testTag = "mode_ai_button",
                onClick = { showAiDifficultyDialog = true }
            )

            // 3. 2 Players (Pass & Play)
            GameModeCard(
                title = "2 Players",
                subtitle = "Local pass & play on the same device",
                icon = Icons.Default.Casino,
                highlightColor = Color(0xFFFFB74D),
                testTag = "mode_local_button",
                onClick = { onStartGame(GameMode.Local2Player) }
            )

            // 4. Private Room
            GameModeCard(
                title = "Private Room",
                subtitle = "Create or join a room with custom code",
                icon = Icons.Default.MeetingRoom,
                highlightColor = Color(0xFFBA68C8),
                testTag = "mode_private_room_button",
                onClick = { showRoomDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "COMMUNITY & SETTINGS",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            // 5 & 6. Friends & Leaderboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToFriends() }
                        .testTag("home_friends_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = "Friends", tint = GoldSecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Friends", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Invite & Play", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLeaderboard() }
                        .testTag("home_leaderboard_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Leaderboard", tint = GoldSecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Leaderboard", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Top 10 Kings", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }

            // 7 & 8. Profile & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToProfile() }
                        .testTag("home_profile_card_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = GoldSecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Profile", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Stats & Level", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSettings() }
                        .testTag("home_settings_card_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = GoldSecondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Audio & 3D", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        // --- 3. Bottom Banner Ad ---
        AdMobBanner()
    }

    // AI Difficulty Selection Dialog
    if (showAiDifficultyDialog) {
        AlertDialog(
            onDismissRequest = { showAiDifficultyDialog = false },
            title = { Text("Select AI Difficulty", fontWeight = FontWeight.Bold, color = GoldSecondary) },
            text = {
                Column {
                    AIDifficulty.entries.forEach { diff ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDifficulty = diff }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedDifficulty == diff),
                                onClick = { selectedDifficulty = diff },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(diff.label, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val desc = when (diff) {
                                    AIDifficulty.EASY -> "Casual player, forgiving moves"
                                    AIDifficulty.MEDIUM -> "Balanced tactical play"
                                    AIDifficulty.HARD -> "Advanced positional strategy & primes"
                                    AIDifficulty.EXPERT -> "Master-level depth & anchor control"
                                }
                                Text(desc, fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAiDifficultyDialog = false
                        onStartGame(GameMode.VsAI(selectedDifficulty))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    Text("Start Game", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDifficultyDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Matchmaking Dialog
    if (showMatchmakingDialog) {
        AlertDialog(
            onDismissRequest = { showMatchmakingDialog = false },
            title = { Text("Matchmaking", fontWeight = FontWeight.Bold, color = GoldSecondary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = matchmakingStatus, color = TextPrimary, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMatchmakingDialog = false }) {
                    Text("Cancel Search", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Private Room Dialog
    if (showRoomDialog) {
        var roomCodeInput by remember { mutableStateOf("") }
        var generatedCode by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRoomDialog = false },
            title = { Text("Private Room", fontWeight = FontWeight.Bold, color = GoldSecondary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (generatedCode == null) {
                        Button(
                            onClick = {
                                val code = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
                                generatedCode = code
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                        ) {
                            Text("Create New Room", fontWeight = FontWeight.Bold)
                        }

                        Text("OR Join an existing room:", color = TextMuted, fontSize = 12.sp)

                        androidx.compose.material3.OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase() },
                            label = { Text("6-Digit Room Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (roomCodeInput.isNotBlank()) {
                                    showRoomDialog = false
                                    onStartGame(GameMode.PrivateRoom(roomCodeInput, false))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = roomCodeInput.length >= 4,
                            colors = ButtonDefaults.buttonColors(containerColor = BoardWoodMedium)
                        ) {
                            Text("Join Room")
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Share this Room Code with your friend:", fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = generatedCode!!,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldSecondary,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    showRoomDialog = false
                                    onStartGame(GameMode.PrivateRoom(generatedCode!!, true))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                            ) {
                                Text("Start Room Match", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRoomDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    highlightColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, highlightColor.copy(alpha = 0.5f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(highlightColor.copy(alpha = 0.15f))
                    .border(1.5.dp, highlightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = highlightColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
