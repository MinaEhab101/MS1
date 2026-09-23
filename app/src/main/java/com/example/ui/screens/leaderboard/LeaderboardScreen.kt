package com.example.ui.screens.leaderboard

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.firebase.FirestoreRepository
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

enum class LeaderboardGameTab(val key: String, val titleAr: String, val titleEn: String, val emoji: String) {
    CHESS("chess", "الشطرنج", "Chess", "♟️"),
    DOMINO("domino", "الدومينو", "Domino", "🁫"),
    BACKGAMMON("backgammon", "طاولة الزهر", "Backgammon", "🎲")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    firestoreRepository: FirestoreRepository,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LeaderboardGameTab.CHESS) }
    var leaderboard by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val avatars = listOf("🦁", "🦅", "👑", "🎲", "⚔️")

    LaunchedEffect(selectedTab) {
        isLoading = true
        leaderboard = firestoreRepository.getTopLeaderboard(selectedTab.key)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Trophy", tint = GoldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Royal Leaderboards (لوحة الصدارة)", color = GoldSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("leaderboard_back_button")) {
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
        ) {
            // --- 3 Tabs: Chess / Domino / Backgammon ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LeaderboardGameTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabBg = when (tab) {
                        LeaderboardGameTab.CHESS -> Brush.horizontalGradient(listOf(Color(0xFF311B92), Color(0xFF512DA8)))
                        LeaderboardGameTab.DOMINO -> Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))
                        LeaderboardGameTab.BACKGAMMON -> Brush.horizontalGradient(listOf(Color(0xFF5A2A12), BoardWoodMedium))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isSelected) Modifier.background(tabBg)
                                else Modifier.background(Color.Transparent)
                            )
                            .border(1.dp, if (isSelected) GoldPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = tab.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tab.titleAr,
                                color = if (isSelected) GoldSecondary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- Subheader title with active tab info ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top 10 — ${selectedTab.titleEn} Champions",
                    color = GoldSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Rating & Stats",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(leaderboard) { index, player ->
                        val rank = index + 1
                        val rankColor = when (rank) {
                            1 -> GoldPrimary
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> TextMuted
                        }

                        val displayedRating = when (selectedTab) {
                            LeaderboardGameTab.CHESS -> player.chessRating
                            LeaderboardGameTab.DOMINO -> player.dominoRating
                            LeaderboardGameTab.BACKGAMMON -> player.backgammonRating
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    if (rank <= 3) listOf(CardBorder, rankColor) else listOf(CardBorder, CardBorder)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (rank <= 3) rankColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .border(1.dp, rankColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (rank == 1) "🥇" else if (rank == 2) "🥈" else if (rank == 3) "🥉" else "#$rank",
                                            color = rankColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (rank <= 3) 14.sp else 12.sp
                                        )
                                    }

                                    // Avatar emoji
                                    val emoji = avatars.getOrElse(player.avatarIndex) { "👑" }
                                    Text(emoji, fontSize = 22.sp)

                                    Column {
                                        Text(
                                            text = player.username,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Level ${player.level}  •  ${player.wins} Wins",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Rating Badge
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BoardWoodDark)
                                        .border(1.dp, if (rank <= 3) rankColor else CardBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$displayedRating",
                                        color = GoldSecondary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
