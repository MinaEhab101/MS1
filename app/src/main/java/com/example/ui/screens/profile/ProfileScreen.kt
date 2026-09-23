package com.example.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
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
import kotlinx.coroutines.launch

data class ProfileAchievement(
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

data class RecentMatchRecord(
    val gameTitle: String,
    val icon: String,
    val opponentName: String,
    val result: String, // WIN, LOSS, DRAW
    val ratingChange: String,
    val timeAgo: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfile?,
    authRepository: AuthRepository,
    firestoreRepository: FirestoreRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val avatars = listOf("🦁", "🦅", "👑", "🎲", "⚔️")

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var editedAvatar by remember(profile) { mutableIntStateOf(profile?.avatarIndex ?: 0) }

    val achievements = listOf(
        ProfileAchievement("Grandmaster Debut", "Play your first 3D Chess game", "♟️", true),
        ProfileAchievement("Domino Domino!", "Score a double-six round victory", "🁫", (profile?.wins ?: 0) >= 1),
        ProfileAchievement("Backgammon Gammon", "Win without opponent taking a checker off", "🎲", (profile?.wins ?: 0) >= 2),
        ProfileAchievement("Online Gladiator", "Complete an Online Multiplayer duel", "🌐", (profile?.totalGames ?: 0) >= 1),
        ProfileAchievement("Royal Streak", "Achieve a 3-game winning streak", "🔥", (profile?.bestStreak ?: 0) >= 3),
        ProfileAchievement("Centurion King", "Reach Level 5 and earn 10,000 Coins", "👑", (profile?.level ?: 1) >= 5)
    )

    val matchHistory = listOf(
        RecentMatchRecord("Chess 3D", "♟️", "Grandmaster_Fischer", "WIN", "+24 ELO", "10m ago"),
        RecentMatchRecord("Domino 3D", "🁫", "Ivory_Tactician", "WIN", "+18 PTS", "1h ago"),
        RecentMatchRecord("Backgammon 3D", "🎲", "Sultan_Tawla", "LOSS", "-12 PTS", "3h ago"),
        RecentMatchRecord("Chess 3D", "♟️", "Kasparov_AI", "DRAW", "+4 ELO", "Yesterday")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملف اللاعب (Profile)", color = GoldSecondary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                val updated = (profile ?: UserProfile()).copy(
                                    username = editedName.ifBlank { "Player" },
                                    avatarIndex = editedAvatar
                                )
                                authRepository.saveUser(updated)
                                scope.launch { firestoreRepository.syncUserProfile(updated) }
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.testTag("profile_edit_save_button")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = GoldPrimary
                        )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar & Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentEmoji = avatars.getOrElse(if (isEditing) editedAvatar else (profile?.avatarIndex ?: 0)) { "👑" }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark)))
                            .border(2.5.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentEmoji, fontSize = 38.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isEditing) {
                        Text("Select Avatar:", color = GoldPrimary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            avatars.forEachIndexed { idx, emo ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (editedAvatar == idx) GoldDark else BoardWoodMedium)
                                        .border(2.dp, if (editedAvatar == idx) GoldSecondary else CardBorder, CircleShape)
                                        .clickable { editedAvatar = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emo, fontSize = 18.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else {
                        Text(
                            text = profile?.username ?: "Player",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "المستوى Level ${profile?.level ?: 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldSecondary
                            )
                            Text(
                                text = "  •  XP: ${profile?.xp ?: 250}/1000",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // XP Progress Bar
                        val progress = ((profile?.xp ?: 250) % 1000) / 1000f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = GoldPrimary,
                            trackColor = BoardWoodDark,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Account Provider Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val authBadge = when (profile?.authProvider) {
                                "google" -> "Google Account"
                                "email" -> "Verified Email"
                                else -> "Guest Player"
                            }
                            val badgeIcon = when (profile?.authProvider) {
                                "google" -> "🌐"
                                "email" -> "✉️"
                                else -> "👤"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BoardWoodDark)
                                    .border(1.dp, GoldDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$badgeIcon $authBadge",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (!profile?.email.isNullOrBlank() && profile?.authProvider != "guest") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = profile?.email ?: "",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- 3 Game Ratings Section ---
            Text(
                text = "تقييم الألعاب المستقل (GAME RATINGS)",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chess Rating
                RatingBadgeCard(
                    modifier = Modifier.weight(1f),
                    emoji = "♟️",
                    title = "Chess 3D",
                    rating = profile?.chessRating ?: 1200,
                    tintColor = Color(0xFF9575CD)
                )

                // Domino Rating
                RatingBadgeCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🁫",
                    title = "Domino 3D",
                    rating = profile?.dominoRating ?: 1200,
                    tintColor = Color(0xFF81C784)
                )

                // Backgammon Rating
                RatingBadgeCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎲",
                    title = "Tawla 3D",
                    rating = profile?.backgammonRating ?: 1200,
                    tintColor = GoldSecondary
                )
            }

            // --- Career Statistics Grid ---
            Text(
                text = "إحصائيات المسيرة (CAREER STATS)",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Total Games", value = "${profile?.totalGames ?: 0}")
                StatCard(modifier = Modifier.weight(1f), label = "Win Rate", value = "${profile?.winRate ?: 0}%", valueColor = AccentGreen)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Wins", value = "${profile?.wins ?: 0}", valueColor = AccentGreen)
                StatCard(modifier = Modifier.weight(1f), label = "Losses", value = "${profile?.losses ?: 0}", valueColor = AccentRed)
                StatCard(modifier = Modifier.weight(1f), label = "Draws", value = "${profile?.draws ?: 0}", valueColor = GoldSecondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Current Streak", value = "${profile?.currentStreak ?: 0} 🔥")
                StatCard(modifier = Modifier.weight(1f), label = "Best Streak", value = "${profile?.bestStreak ?: 0} 🏆")
                StatCard(modifier = Modifier.weight(1f), label = "Gold Coins", value = "${profile?.coins ?: 1000} 🪙", valueColor = GoldPrimary)
            }

            // --- Match History Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "سجل المباريات (MATCH HISTORY)",
                        color = GoldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder)))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchHistory.forEach { match ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceDark.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(match.icon, fontSize = 18.sp)
                                Column {
                                    Text(match.gameTitle, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("vs ${match.opponentName} • ${match.timeAgo}", color = TextMuted, fontSize = 10.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val badgeColor = when (match.result) {
                                    "WIN" -> AccentGreen
                                    "LOSS" -> AccentRed
                                    else -> GoldSecondary
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(match.result, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(match.ratingChange, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- Achievements Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = "Achievements", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الإنجازات (ACHIEVEMENTS)",
                        color = GoldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                achievements.forEach { ach ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (ach.isUnlocked) CardDark else Color(0xFF161616)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(if (ach.isUnlocked) listOf(CardBorder, GoldDark) else listOf(CardBorder, CardBorder))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = ach.icon, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    color = if (ach.isUnlocked) GoldSecondary else TextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = ach.description, color = TextMuted, fontSize = 11.sp)
                            }
                            if (ach.isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldPrimary.copy(alpha = 0.2f))
                                        .border(1.dp, GoldPrimary, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("UNLOCKED", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("LOCKED", color = TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingBadgeCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    rating: Int,
    tintColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CardBorder, tintColor.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$rating", color = tintColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
