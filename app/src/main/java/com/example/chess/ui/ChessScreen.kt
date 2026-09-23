package com.example.chess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessGameMode
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.viewmodel.ChessViewModel
import com.example.data.UserProfile
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    onNavigateToLeaderboard: () -> Unit
) {
    val state by viewModel.gameState.collectAsState()
    var showHistorySheet by remember { mutableStateOf(false) }
    var showResignConfirm by remember { mutableStateOf(false) }
    var showDrawConfirm by remember { mutableStateOf(false) }

    val isWhiteTurn = state.turn == PieceColor.WHITE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // --- 1. Top Bar with Back, Game Title, and Action Buttons ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .border(1.dp, CardBorder)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("chess_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldSecondary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "♟️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROYAL CHESS 3D",
                        color = GoldSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = when (state.mode) {
                        ChessGameMode.VS_AI -> "vs AI (${state.aiDifficulty.label})"
                        ChessGameMode.PASS_AND_PLAY -> "2 Players (Pass & Play)"
                        ChessGameMode.ONLINE -> "Online Matchmaking"
                        ChessGameMode.PRIVATE_ROOM -> "Room: ${state.roomCode ?: "Private"}"
                    },
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Leaderboard Quick Shortcut
                IconButton(onClick = onNavigateToLeaderboard) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = GoldPrimary
                    )
                }

                // 3D View Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.is3DView) GoldPrimary else CardDark)
                        .border(1.dp, if (state.is3DView) GoldSecondary else CardBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggle3DView() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("chess_toggle_3d"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.is3DView) "3D" else "2D",
                        color = if (state.is3DView) BoardWoodDark else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- 2. Opponent Player Header Card (Black) ---
        PlayerHeaderCard(
            name = state.opponentName,
            avatarEmoji = if (state.mode == ChessGameMode.PASS_AND_PLAY) "👤" else "🤖",
            rating = state.opponentRating,
            timeSec = state.blackTimeRemainingSec,
            isCurrentTurn = !isWhiteTurn && !state.isGameOver,
            capturedPieces = state.capturedByWhite,
            isBlack = true
        )

        // --- 3. Status Alert Banner (Check / Checkmate / Draw) ---
        AnimatedVisibility(
            visible = state.isInCheck || state.isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (state.isInCheck && !state.isGameOver) Color(0xFFB71C1C)
                        else Color(0xFF1B5E20)
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        state.isCheckmate -> "CHECKMATE (كش مات)! Winner: ${if (state.winner == PieceColor.WHITE) "White" else "Black"}"
                        state.isStalemate -> "STALEMATE (تعادل بالحصار)!"
                        state.isDraw -> "DRAW (تعادل): ${state.drawReason ?: "Game Ended"}"
                        state.isInCheck -> "CHECK (كش ملك)! King is under attack!"
                        else -> ""
                    },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }

        // --- 4. Main 3D Chess Board Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ChessCanvas3D(
                state = state,
                onSquareClicked = { sq -> viewModel.onSquareClicked(sq) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 5. User Player Header Card (White) ---
        PlayerHeaderCard(
            name = userProfile?.username ?: "You (White)",
            avatarEmoji = listOf("🦁", "🦅", "👑", "🎲", "⚔️").getOrElse(userProfile?.avatarIndex ?: 0) { "👑" },
            rating = userProfile?.chessRating ?: 1200,
            timeSec = state.whiteTimeRemainingSec,
            isCurrentTurn = isWhiteTurn && !state.isGameOver,
            capturedPieces = state.capturedByBlack,
            isBlack = false
        )

        // --- 6. Bottom Game Controls Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .border(1.dp, CardBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flip Board Button
            IconButton(
                onClick = { viewModel.toggleBoardFlipped() },
                modifier = Modifier.testTag("chess_flip_board")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Flip Board",
                    tint = GoldSecondary
                )
            }

            // Undo Button (available in AI and Pass & Play)
            if (state.mode == ChessGameMode.VS_AI || state.mode == ChessGameMode.PASS_AND_PLAY) {
                IconButton(
                    onClick = { viewModel.undoMove() },
                    enabled = state.moveHistory.isNotEmpty() && !state.isGameOver,
                    modifier = Modifier.testTag("chess_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Move",
                        tint = if (state.moveHistory.isNotEmpty()) GoldPrimary else TextMuted
                    )
                }
            }

            // Move History Drawer Button
            Button(
                onClick = { showHistorySheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = BoardWoodMedium),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("chess_history_button")
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = GoldSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Moves (${state.moveHistory.size})", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Draw Offer
            IconButton(
                onClick = { showDrawConfirm = true },
                enabled = !state.isGameOver,
                modifier = Modifier.testTag("chess_draw_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = "Offer Draw",
                    tint = if (!state.isGameOver) Color(0xFF64B5F6) else TextMuted
                )
            }

            // Resign Button
            IconButton(
                onClick = { showResignConfirm = true },
                enabled = !state.isGameOver,
                modifier = Modifier.testTag("chess_resign_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Resign",
                    tint = if (!state.isGameOver) Color(0xFFFF5252) else TextMuted
                )
            }
        }
    }

    // --- Promotion Dialog ---
    if (state.pendingPromotionMove != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPromotion() },
            title = {
                Text(
                    text = "Pawn Promotion (ترقية البيدق)",
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Choose a piece to promote your pawn:",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            Pair(PieceType.QUEEN, "♕"),
                            Pair(PieceType.ROOK, "♖"),
                            Pair(PieceType.BISHOP, "♗"),
                            Pair(PieceType.KNIGHT, "♘")
                        ).forEach { (type, symbol) ->
                            Card(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clickable { viewModel.selectPromotionPiece(type) }
                                    .testTag("promote_${type.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BoardWoodMedium),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(GoldDark, GoldPrimary)))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = symbol, fontSize = 34.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPromotion() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- Move History Bottom Sheet ---
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = CardDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Move Log (سجل النقلات الرسمية)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.moveHistory.isEmpty()) {
                    Text("No moves yet.", color = TextMuted, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        val pairs = state.moveHistory.chunked(2)
                        itemsIndexed(pairs) { index, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = pair.getOrNull(0)?.sanNotation ?: "",
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = pair.getOrNull(1)?.sanNotation ?: "",
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Resign Confirmation Dialog ---
    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("Resign Game (استسلام)", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
            text = { Text("Are you sure you want to resign this chess match?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showResignConfirm = false
                        viewModel.resignGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("Resign", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- Draw Offer Dialog ---
    if (showDrawConfirm) {
        AlertDialog(
            onDismissRequest = { showDrawConfirm = false },
            title = { Text("Offer Draw (طلب تعادل)", fontWeight = FontWeight.Bold, color = GoldSecondary) },
            text = { Text("Do you want to agree on a draw for this match?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDrawConfirm = false
                        viewModel.offerDraw()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    Text("Agree Draw", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- Game Over Victory / Defeat Modal ---
    if (state.isGameOver) {
        val won = state.winner == PieceColor.WHITE
        val isDraw = state.isDraw || state.isStalemate

        AlertDialog(
            onDismissRequest = { /* Require button click */ },
            title = {
                Text(
                    text = when {
                        isDraw -> "🤝 MATCH DRAWN (تعادل)"
                        won -> "🏆 VICTORY (فوز ملكي)!"
                        else -> "⚔️ DEFEAT (هزيمة)"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (won) GoldSecondary else if (isDraw) Color(0xFF64B5F6) else Color(0xFFFF5252),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.drawReason ?: if (won) "Checkmate! You outsmarted the opponent." else "Checkmate! Better luck next match.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CHESS RATING", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = if (won) "+18 pts" else if (isDraw) "+2 pts" else "-14 pts",
                                color = if (won || isDraw) GoldPrimary else Color(0xFFFF5252),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = if (won) "+150 💰" else "+30 💰",
                                color = GoldSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("XP", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = "+100 XP",
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startNewGame(state.mode, state.aiDifficulty)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = BoardWoodDark)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rematch", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rematch (إعادة اللعب)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Back to Lobby", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun PlayerHeaderCard(
    name: String,
    avatarEmoji: String,
    rating: Int,
    timeSec: Int,
    isCurrentTurn: Boolean,
    capturedPieces: List<com.example.chess.model.ChessPiece>,
    isBlack: Boolean
) {
    val minutes = timeSec / 60
    val seconds = timeSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val isTimeLow = timeSec in 1..30

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .border(
                width = if (isCurrentTurn) 1.5.dp else 1.dp,
                color = if (isCurrentTurn) GoldPrimary else CardBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrentTurn) BoardWoodMedium else CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isBlack) Color(0xFF26202A) else Color(0xFFFFFDF5))
                        .border(1.dp, if (isBlack) Color(0xFF90A4AE) else GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatarEmoji, fontSize = 18.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (isCurrentTurn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldPrimary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "THINKING",
                                    color = BoardWoodDark,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$rating pts",
                            color = GoldSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Captured pieces mini row
                        if (capturedPieces.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val pieceSymbols = capturedPieces.take(6).joinToString("") { p ->
                                when (p.type) {
                                    PieceType.QUEEN -> "♛"
                                    PieceType.ROOK -> "♜"
                                    PieceType.BISHOP -> "♝"
                                    PieceType.KNIGHT -> "♞"
                                    PieceType.PAWN -> "♟"
                                    else -> ""
                                }
                            }
                            Text(text = pieceSymbols, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }

            // Digital Chess Clock Timer
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTimeLow) Color(0xFFD32F2F) else BoardWoodDark)
                    .border(1.dp, if (isCurrentTurn) GoldPrimary else CardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = if (isTimeLow) Color.White else GoldSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeFormatted,
                    color = if (isTimeLow) Color.White else GoldSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
