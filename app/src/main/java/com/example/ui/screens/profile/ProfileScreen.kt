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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile", color = GoldSecondary, fontWeight = FontWeight.Bold) },
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentEmoji = avatars.getOrElse(if (isEditing) editedAvatar else (profile?.avatarIndex ?: 0)) { "👑" }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark)))
                            .border(2.5.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentEmoji, fontSize = 40.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        Text(
                            text = "Level ${profile?.level ?: 1}  •  Rating: ${profile?.rating ?: 1200} pts",
                            fontSize = 13.sp,
                            color = GoldSecondary
                        )
                    }
                }
            }

            // Career Statistics Grid
            Text(
                text = "CAREER STATISTICS",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Total Games", value = "${profile?.totalGames ?: 0}")
                StatCard(modifier = Modifier.weight(1f), label = "Win Rate", value = "${profile?.winRate ?: 0}%", valueColor = AccentGreen)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Wins", value = "${profile?.wins ?: 0}", valueColor = AccentGreen)
                StatCard(modifier = Modifier.weight(1f), label = "Losses", value = "${profile?.losses ?: 0}", valueColor = AccentRed)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Current Streak", value = "${profile?.currentStreak ?: 0} 🔥")
                StatCard(modifier = Modifier.weight(1f), label = "Best Streak", value = "${profile?.bestStreak ?: 0} 🏆")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Coins Balance", value = "${profile?.coins ?: 1000} 🪙", valueColor = GoldSecondary)
                StatCard(modifier = Modifier.weight(1f), label = "Global Rank", value = "#${(profile?.level ?: 1) * 3}")
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, CardBorder)))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
