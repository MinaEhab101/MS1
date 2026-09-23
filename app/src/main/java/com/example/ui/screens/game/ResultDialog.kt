package com.example.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.Player
import com.example.game.WinType
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ResultDialog(
    winner: Player,
    winType: WinType?,
    isUserWinner: Boolean,
    ratingChange: Int,
    coinsEarned: Int,
    onRematchClicked: () -> Unit,
    onHomeClicked: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + scaleIn(tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CardDark, BoardWoodDark)
                        )
                    )
                    .border(2.dp, GoldPrimary, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Trophy / Crown Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(GoldSecondary, GoldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = BoardWoodDark,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Main Title
                    Text(
                        text = if (isUserWinner) "VICTORY!" else "DEFEAT",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUserWinner) GoldSecondary else AccentRed,
                        letterSpacing = 2.sp
                    )

                    // Win Type Badge (Single, Gammon, Backgammon)
                    winType?.let {
                        val badgeText = when (it) {
                            WinType.SINGLE -> "Standard Victory (1 pt)"
                            WinType.GAMMON -> "GAMMON! Double Score (2 pts)"
                            WinType.BACKGAMMON -> "BACKGAMMON! Triple Score (3 pts)"
                        }
                        Box(
                            modifier = Modifier
                                .background(BoardWoodMedium, RoundedCornerShape(8.dp))
                                .border(1.dp, GoldDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = GoldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = if (isUserWinner) "Spectacular game! You bore off all checkers."
                        else "Good game! Review your strategy and try again.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Rewards breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BoardWoodDark, RoundedCornerShape(12.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rating", color = TextMuted, fontSize = 12.sp)
                            Text(
                                text = if (ratingChange >= 0) "+$ratingChange" else "$ratingChange",
                                color = if (ratingChange >= 0) AccentGreen else AccentRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Coins", color = TextMuted, fontSize = 12.sp)
                            Text(
                                text = "+$coinsEarned",
                                color = GoldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onHomeClicked,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("result_home_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Home", color = TextPrimary)
                        }

                        Button(
                            onClick = onRematchClicked,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("result_rematch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = BoardWoodDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rematch")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rematch", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
