package com.example.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.BoardState
import com.example.game.GamePhase
import com.example.game.Player
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextPrimary

@Composable
fun DiceControlBar(
    state: BoardState,
    isAiThinking: Boolean,
    onRollDiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(state.lastRoll) {
        if (state.lastRoll != null) {
            rotationAnim.snapTo(0f)
            rotationAnim.animateTo(360f, tween(400))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pip counts
        Column {
            Text(
                text = "White Pip: ${state.pipCount(Player.WHITE)}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Black Pip: ${state.pipCount(Player.BLACK)}",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Center: Dice display or Roll Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.phase == GamePhase.ROLL_DICE && !isAiThinking) {
                Button(
                    onClick = onRollDiceClicked,
                    modifier = Modifier.testTag("roll_dice_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = BoardWoodDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = "Roll Dice")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Roll Dice", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                // Show rolled dice and available moves
                val d1 = state.lastRoll?.first ?: 1
                val d2 = state.lastRoll?.second ?: 1
                val isDouble = d1 == d2

                if (state.lastRoll != null) {
                    Row(
                        modifier = Modifier.rotate(rotationAnim.value),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DieFace(value = d1, isActive = state.dice.contains(d1))
                        DieFace(value = d2, isActive = state.dice.contains(d2))
                        if (isDouble && state.dice.size >= 3) {
                            DieFace(value = d1, isActive = state.dice.size >= 3)
                            DieFace(value = d2, isActive = state.dice.size >= 4)
                        }
                    }
                }
            }
        }

        // Turn indicator
        Box(
            modifier = Modifier
                .background(
                    if (state.turn == Player.WHITE) Color(0xFF332014) else Color(0xFF1F1B18),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    if (state.turn == Player.WHITE) GoldPrimary else Color.Gray,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (state.turn == Player.WHITE) "White's Turn" else "Black's Turn",
                color = if (state.turn == Player.WHITE) GoldSecondary else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DieFace(
    value: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    ProfessionalDieFace(
        value = value,
        isUsed = !isActive,
        size = 36.dp,
        modifier = modifier
    )
}
