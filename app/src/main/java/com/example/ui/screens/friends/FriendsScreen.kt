package com.example.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.Friend
import com.example.firebase.FirestoreRepository
import com.example.game.GameMode
import com.example.ui.theme.AccentGreen
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
fun FriendsScreen(
    firestoreRepository: FirestoreRepository,
    onStartGame: (GameMode) -> Unit,
    onNavigateBack: () -> Unit
) {
    var friends by remember { mutableStateOf(firestoreRepository.getInitialFriends()) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var newFriendName by remember { mutableStateOf("") }

    val avatars = listOf("🦁", "🦅", "👑", "🎲", "⚔️")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends & Challenges", color = GoldSecondary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("friends_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BoardWoodDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendDialog = true },
                containerColor = GoldPrimary,
                contentColor = BoardWoodDark,
                modifier = Modifier.testTag("add_friend_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Friend")
            }
        },
        containerColor = SurfaceDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(friends) { friend ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, GoldDark.copy(alpha = 0.5f))))
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val emoji = avatars.getOrElse(friend.avatarIndex) { "👑" }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CardBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(friend.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (friend.isOnline) AccentGreen else Color.Gray)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${friend.rating} pts", color = TextSecondary, fontSize = 12.sp)
                                    Text(if (friend.isOnline) "  •  Online" else "  •  Offline", color = if (friend.isOnline) AccentGreen else TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        // Challenge Button
                        Button(
                            onClick = {
                                val roomCode = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
                                onStartGame(GameMode.PrivateRoom(roomCode, true))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SportsEsports, contentDescription = "Challenge", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Challenge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Friend", fontWeight = FontWeight.Bold, color = GoldSecondary) },
            text = {
                Column {
                    Text("Enter player display name or friend code:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFriendName,
                        onValueChange = { newFriendName = it },
                        label = { Text("Friend Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFriendName.isNotBlank()) {
                            val newFriend = Friend(
                                id = "f_${System.currentTimeMillis()}",
                                name = newFriendName.trim(),
                                rating = 1200 + (0..400).random(),
                                isOnline = true,
                                avatarIndex = (0..4).random()
                            )
                            friends = friends + newFriend
                            newFriendName = ""
                            showAddFriendDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    Text("Add Friend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
