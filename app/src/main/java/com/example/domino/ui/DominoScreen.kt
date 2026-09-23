package com.example.domino.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.domino.engine.DominoEngine
import com.example.domino.model.DominoEnd
import com.example.domino.model.DominoGameMode
import com.example.domino.model.DominoTile
import com.example.domino.model.PlacedTile
import com.example.domino.viewmodel.DominoViewModel
import com.example.game.AIDifficulty
import com.example.game.Player
import com.example.ui.screens.game.RoyalParticleOverlay
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.GraphicsSettingsDialog
import com.example.ui.components.RoyalGameModule
import com.example.ui.components.UnifiedGameSelectionModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DominoScreen(
    viewModel: DominoViewModel,
    userProfile: UserProfile? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBackgammon: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    var showModeDialog by remember { mutableStateOf(false) }
    var showGraphicsDialog by remember { mutableStateOf(false) }
    var showGameSwitcherModal by remember { mutableStateOf(false) }

    val isCurrentPlayerTurn = (state.turn == Player.WHITE) || (state.isPassPlay && !state.isPassPlayHidden)
    val currentHand = if (state.turn == Player.WHITE) state.playerHand else state.opponentHand
    val hasValidMove = DominoEngine.hasAnyValidMove(currentHand, state)
    val canDraw = state.boneyard.isNotEmpty() && !state.isGameOver && isCurrentPlayerTurn
    val canPass = !hasValidMove && state.boneyard.isEmpty() && !state.isGameOver && isCurrentPlayerTurn

    // Graphics Settings Dialog
    GraphicsSettingsDialog(
        isOpen = showGraphicsDialog,
        onDismiss = { showGraphicsDialog = false }
    )

    // Unified Game Selection Modal (Dominoes <-> Backgammon)
    UnifiedGameSelectionModal(
        isOpen = showGameSwitcherModal,
        currentModule = RoyalGameModule.DOMINOES,
        userProfile = userProfile,
        onDismiss = { showGameSwitcherModal = false },
        onSelectBackgammon = {
            onNavigateToBackgammon()
        },
        onSelectDominoes = { /* Already in Domino */ }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BoardWoodDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DOMINO KING 3D",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Mode Badge Chip (Clickable to switch mode)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldPrimary.copy(alpha = 0.2f))
                                    .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                                    .clickable { showModeDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (state.mode) {
                                        DominoGameMode.ONLINE -> "🌐 أونلاين"
                                        DominoGameMode.VS_AI -> "🤖 ضد الذكاء"
                                        DominoGameMode.PASS_AND_PLAY -> "👥 لاعبين"
                                        DominoGameMode.PRIVATE_ROOM -> "🔑 غرفة خاصة"
                                    },
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "لعبة الدومينو الملكية الفاخرة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("domino_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldSecondary
                        )
                    }
                },
                actions = {
                    // Quick Switch to Backgammon
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF5A2A12), BoardWoodMedium)))
                            .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                            .clickable { showGameSwitcherModal = true }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎲", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "طاولة الزهر",
                                color = GoldSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Graphics Settings Dialog
                    IconButton(
                        onClick = { showGraphicsDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Graphics Settings",
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.startNewGame() },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("domino_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = GoldSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BoardWoodDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BoardWoodDark),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Opponent Card (Backgammon King Royal Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BoardWoodMedium),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (state.turn == Player.BLACK) listOf(Color(0xFFFF5252), Color(0xFFFF1744))
                        else listOf(CardBorder, CardBorder)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Opponent Profile Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFE57373), Color(0xFFC62828))))
                                .border(1.8.dp, if (state.turn == Player.BLACK) Color(0xFFFF5252) else CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = state.opponentAvatar, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.opponentName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = state.opponentCountry, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🪙 ${state.opponentCoins}",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🁢 ${state.opponentHandCount} قطع متبقية",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Opponent Turn / Score status
                    Column(horizontalAlignment = Alignment.End) {
                        if (state.turn == Player.BLACK && !state.isGameOver) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isAiThinking) "يفكر الآن... ⏳" else "دور الخصم",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "النقاط: ${state.opponentScore}",
                            color = GoldSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 2. Central 3D Domino Table Board with Dynamic Camera Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                DominoCanvas(
                    state = state,
                    onEndClicked = { end -> viewModel.onPlayOnEnd(end) },
                    modifier = Modifier.fillMaxSize()
                )

                // Particle overlay for victory
                RoyalParticleOverlay(
                    triggerHitX = null,
                    triggerHitY = null,
                    triggerWin = state.isGameOver && state.winner == Player.WHITE
                )
            }

            // 3. Selection Choice Floating Action Bar (If selected tile matches BOTH ends)
            AnimatedVisibility(
                visible = state.selectedTile != null && state.validEndsForSelected.size > 1 && !state.isGameOver,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BoardWoodMedium),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(PointHighlight, GoldPrimary, PointHighlight))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.onPlayOnEnd(DominoEnd.LEFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = PointHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(
                                text = "⬅️ العب يسار (${state.leftOpenEnd})",
                                color = BoardWoodDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { viewModel.onPlayOnEnd(DominoEnd.RIGHT) },
                            colors = ButtonDefaults.buttonColors(containerColor = PointHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(
                                text = "العب يمين (${state.rightOpenEnd}) ➡️",
                                color = BoardWoodDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Player Tile Rack & Controls (Large, Prominent 3D Tiles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDark)
                    .border(width = 1.5.dp, brush = Brush.verticalGradient(listOf(CardBorder, GoldDark)), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                // Player Status Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0))))
                                .border(1.8.dp, if (state.turn == Player.WHITE) PointHighlight else CardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👑", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (state.isPassPlay && state.turn == Player.BLACK) "يد اللاعب 2 (${state.opponentHand.size} قطع)"
                                    else "${userProfile?.username ?: "يدك"} (${state.playerHand.size} قطع)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نقاطك: ${state.playerScore}",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isCurrentPlayerTurn && !state.isGameOver) {
                                Text(
                                    text = if (hasValidMove) "اختر قطعة للعب (انقر للوضع المباشر)" else "لا توجد حركات! اسحب من البنك",
                                    color = if (hasValidMove) PointHighlight else Color(0xFFFFB74D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Action Buttons (Draw / Pass)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canDraw) {
                            Button(
                                onClick = { viewModel.onDrawClicked() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Draw",
                                        tint = BoardWoodDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "اسحب (${state.boneyard.size})",
                                        color = BoardWoodDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (canPass) {
                            Button(
                                onClick = { viewModel.onPassClicked() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = "تمرير الدور",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Player Hand Tiles (Prominent, High-Resolution Rack)
                if (state.isPassPlay && state.isPassPlayHidden) {
                    // Privacy screen for Pass & Play
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BoardWoodMedium)
                            .border(1.dp, GoldDark, RoundedCornerShape(12.dp))
                            .clickable { viewModel.revealPassPlayHand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Show Hand",
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "دور اللاعب التالي - انقر لكشف قطعك",
                                color = GoldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    val activeHand = if (state.isPassPlay && state.turn == Player.BLACK) state.opponentHand else state.playerHand

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeHand) { tile ->
                            val isPlayable = DominoEngine.getValidEndsForTile(tile, state).isNotEmpty()
                            val isSelected = state.selectedTile?.id == tile.id

                            PlayerRackTile(
                                tile = tile,
                                isPlayable = isPlayable && isCurrentPlayerTurn,
                                isSelected = isSelected,
                                onClick = { viewModel.onTileSelected(tile) }
                            )
                        }
                    }
                }
            }
        }

        // Mode Selection Dialog
        if (showModeDialog) {
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { showModeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CardDark)
                    ) {
                        Text("إغلاق", color = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Modes", tint = GoldSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر نمط اللعبة", color = GoldSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeDialogItem(
                            title = "🌐 أونلاين سريع (Online Match)",
                            desc = "لعب تنافسي مع لاعبين حقيقيين",
                            onClick = {
                                showModeDialog = false
                                viewModel.startNewGame(mode = DominoGameMode.ONLINE)
                            }
                        )

                        ModeDialogItem(
                            title = "🤖 ضد الذكاء الاصطناعي (AI)",
                            desc = "تدريب بمستويات ذكاء متعددة",
                            onClick = {
                                showModeDialog = false
                                viewModel.startNewGame(mode = DominoGameMode.VS_AI, difficulty = AIDifficulty.HARD)
                            }
                        )

                        ModeDialogItem(
                            title = "👥 لاعبين على نفس الجهاز (Pass & Play)",
                            desc = "لعب محلي ممتع مع صديق",
                            onClick = {
                                showModeDialog = false
                                viewModel.startNewGame(mode = DominoGameMode.PASS_AND_PLAY)
                            }
                        )

                        ModeDialogItem(
                            title = "🔑 غرفة خاصة (Private Room)",
                            desc = "أنشئ غرفة أو انضم لغرفة صديق",
                            onClick = {
                                showModeDialog = false
                                viewModel.startNewGame(mode = DominoGameMode.PRIVATE_ROOM)
                            }
                        )
                    }
                },
                containerColor = BoardWoodDark,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Game Over Dialog
        if (state.isGameOver) {
            AlertDialog(
                onDismissRequest = { /* require button */ },
                confirmButton = {
                    Button(
                        onClick = { viewModel.startNewGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("لعبة جديدة (Play Again)", color = BoardWoodDark, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = CardDark)
                    ) {
                        Text("الرئيسية", color = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = GoldSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.winner == Player.WHITE) "👑 فوز ساحق! VICTORY" else "انتهت الجولة GAME OVER",
                            color = if (state.winner == Player.WHITE) GoldSecondary else Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = state.winReason,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النتيجة النهائية: أنت ${state.playerScore} - الخصم ${state.opponentScore}",
                            color = GoldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (state.winner == Player.WHITE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🪙 جائزتك: +100 عملة ذهبية!",
                                color = PointHighlight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                containerColor = BoardWoodDark,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun ModeDialogItem(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BoardWoodMedium),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CardBorder, GoldDark)))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = GoldSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = desc, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun PlayerRackTile(
    tile: DominoTile,
    isPlayable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Elevate selected tile slightly for tactile feel
    val offsetY = if (isSelected) (-8).dp else 0.dp

    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .size(width = 54.dp, height = 98.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent)
            .border(
                width = if (isSelected) 2.5.dp else if (isPlayable) 1.8.dp else 1.dp,
                color = if (isSelected) GoldSecondary else if (isPlayable) PointHighlight else CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = isPlayable) { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRackDominoTile(
                tile = tile,
                isSelected = isSelected,
                isPlayable = isPlayable
            )
        }
    }
}

private fun DrawScope.drawRackDominoTile(
    tile: DominoTile,
    isSelected: Boolean,
    isPlayable: Boolean
) {
    val w = size.width
    val h = size.height

    // 1. Layered Drop Shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.50f),
        topLeft = Offset(3f, 5f),
        size = Size(w - 6f, h - 6f),
        cornerRadius = CornerRadius(9f, 9f)
    )

    // 2. 3D Beveled Side Rim (Warm Antique Bone Ivory)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFD6C8AE), Color(0xFFB5A486), Color(0xFF8A795E))
        ),
        topLeft = Offset(2f, 3.5f),
        size = Size(w - 4f, h - 5f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // 3. Ivory Porcelain Surface with Pillow Sheen
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFE), Color(0xFFFCF9F1), Color(0xFFF4ECD8), Color(0xFFE5D7BD)),
            center = Offset(w * 0.30f, h * 0.25f),
            radius = w * 1.5f
        ),
        topLeft = Offset(2f, 2f),
        size = Size(w - 4f, h - 4f),
        cornerRadius = CornerRadius(7.5f, 7.5f)
    )

    // 4. Subtle Outer Border
    drawRoundRect(
        color = if (isSelected) GoldPrimary else if (isPlayable) PointHighlight else Color(0xFFAFA085),
        topLeft = Offset(2f, 2f),
        size = Size(w - 4f, h - 4f),
        cornerRadius = CornerRadius(7.5f, 7.5f),
        style = Stroke(width = if (isSelected || isPlayable) 2.2f else 1.2f)
    )

    // 5. Center Dividing Inset Groove & Polished Brass Spinner Pin
    val midY = h / 2f
    val pinRadius = 4.2f

    // Groove
    drawLine(
        color = Color(0xFF5E4F39),
        start = Offset(6f, midY - 0.7f),
        end = Offset(w - 6f, midY - 0.7f),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.75f),
        start = Offset(6f, midY + 1f),
        end = Offset(w - 6f, midY + 1f),
        strokeWidth = 1.2f
    )

    // Brass Spinner Pin
    drawCircle(
        color = Color(0xFF423726),
        radius = pinRadius + 1.2f,
        center = Offset(w / 2f, midY)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFD4AF37), Color(0xFF8B6B1B)),
            center = Offset(w / 2f - 1.2f, midY - 1.2f),
            radius = pinRadius * 1.4f
        ),
        radius = pinRadius,
        center = Offset(w / 2f, midY)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = pinRadius * 0.3f,
        center = Offset(w / 2f - pinRadius * 0.35f, midY - pinRadius * 0.35f)
    )

    // 6. Draw Pips (Large, Crisp, Carved)
    val dotRadius = 4.6f
    val dotColor = Color(0xFF141210)
    val halfH = (h - 4f) / 2f

    // Top half dots
    drawPipsPattern(
        count = tile.left,
        center = Offset(w / 2f, 2f + halfH / 2f),
        halfSize = halfH * 0.36f,
        radius = dotRadius,
        color = dotColor
    )

    // Bottom half dots
    drawPipsPattern(
        count = tile.right,
        center = Offset(w / 2f, 2f + halfH + halfH / 2f),
        halfSize = halfH * 0.36f,
        radius = dotRadius,
        color = dotColor
    )

    // 7. Clear Corner Numerals for effortless identification
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(190, 95, 75, 45)
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.LEFT
        }
        drawText(tile.left.toString(), 6f, 24f, paint)
        drawText(tile.right.toString(), 6f, midY + 24f, paint)
    }
}
