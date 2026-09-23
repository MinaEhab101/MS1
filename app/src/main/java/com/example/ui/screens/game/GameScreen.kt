package com.example.ui.screens.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserProfile
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.GraphicsSettings
import com.example.game.Player
import com.example.ui.components.GraphicsSettingsDialog
import com.example.ui.components.RoyalDailyRewardDialog
import com.example.ui.components.RoyalGameModule
import com.example.ui.components.UnifiedGameSelectionModal
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDomino: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.boardState.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val ratingChange by viewModel.ratingChange.collectAsState()
    val coinsEarned by viewModel.coinsEarned.collectAsState()
    val graphicsQuality by GraphicsSettings.currentQuality.collectAsState()

    // Dynamic 3D Camera Controller
    val cameraController = rememberDynamicCameraController()

    // Auto action focus when rolling dice or picking piece
    LaunchedEffect(state.phase, state.turn) {
        if (state.phase == GamePhase.ROLL_DICE && state.turn == Player.WHITE) {
            cameraController.triggerActionZoom(true)
        } else {
            cameraController.triggerActionZoom(false)
        }
    }

    val animatedPitch by animateFloatAsState(
        targetValue = if (graphicsQuality.enableCameraTilt) cameraController.activePreset.basePitch else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "cameraPitch"
    )
    val animatedZoom by animateFloatAsState(
        targetValue = if (graphicsQuality.enableCameraTilt) {
            (cameraController.activePreset.baseZoom + cameraController.userZoomOffset).coerceIn(0.95f, 1.55f)
        } else 1.02f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "cameraZoom"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (graphicsQuality.enableCameraTilt) cameraController.activePreset.offsetYRatio else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "cameraOffsetY"
    )

    var showResignConfirm by remember { mutableStateOf(false) }
    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showGraphicsDialog by remember { mutableStateOf(false) }
    var showGameSwitcherModal by remember { mutableStateOf(false) }
    var autoBetEnabled by remember { mutableStateOf(false) }

    var showTutorial by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showReplay by remember { mutableStateOf(false) }

    val ruleViolation by viewModel.ruleViolationMessage.collectAsState()

    // Auto dismiss rule violation message after 3.5s
    LaunchedEffect(ruleViolation) {
        if (ruleViolation != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearRuleViolationMessage()
        }
    }

    val whitePipCount = remember(state.points, state.barWhite) { state.calculateWhitePipCount() }
    val blackPipCount = remember(state.points, state.barBlack) { state.calculateBlackPipCount() }

    BackHandler {
        if (state.phase != GamePhase.GAME_OVER) {
            showResignConfirm = true
        } else {
            onNavigateBack()
        }
    }

    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("Leave Match?", fontWeight = FontWeight.Bold) },
            text = { Text("Leaving during an active match will count as a forfeit.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResignConfirm = false
                        viewModel.resign(context as? Activity)
                        onNavigateBack()
                    }
                ) {
                    Text("Forfeit & Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) {
                    Text("Stay in Game")
                }
            }
        )
    }

    // Daily Reward Modal
    RoyalDailyRewardDialog(
        isOpen = showDailyRewardDialog,
        onDismiss = { showDailyRewardDialog = false },
        onRewardClaimed = { _ ->
            viewModel.soundManager.playButtonClick()
        },
        soundManager = viewModel.soundManager
    )

    // Graphics Settings Dialog
    GraphicsSettingsDialog(
        isOpen = showGraphicsDialog,
        onDismiss = { showGraphicsDialog = false }
    )

    // Unified Game Selection Lounge Modal (Backgammon <-> Dominoes)
    UnifiedGameSelectionModal(
        isOpen = showGameSwitcherModal,
        currentModule = RoyalGameModule.BACKGAMMON,
        userProfile = userProfile,
        onDismiss = { showGameSwitcherModal = false },
        onSelectBackgammon = { /* Already here */ },
        onSelectDominoes = {
            onNavigateToDomino()
        }
    )

    // Missions Dialog
    if (showMissionsDialog) {
        AlertDialog(
            onDismissRequest = { showMissionsDialog = false },
            confirmButton = {
                TextButton(onClick = { showMissionsDialog = false }) {
                    Text("Close", color = GoldPrimary)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Quests", tint = GoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("المهام الملكية (Royal Quests)", color = GoldSecondary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👑 Win 3 matches today (+500 Coins) - Progress: 1/3", color = TextPrimary)
                    Text("🎲 Bear off 15 checkers in one game (+300 Coins) - In Progress", color = TextPrimary)
                    Text("🏆 Reach 1,300 Rating points (+1,000 Coins) - Progress: ${userProfile?.rating ?: 1200}/1300", color = TextPrimary)
                }
            },
            containerColor = BoardWoodDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Chat / Stickers Quick Dialog
    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            confirmButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text("Close", color = GoldPrimary)
                }
            },
            title = { Text("الدردشة والملصقات (Chat & Stickers)", color = GoldSecondary, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("👋 مرحباً!", modifier = Modifier.clickable { showChatDialog = false; viewModel.soundManager.playButtonClick() }, fontSize = 20.sp)
                    Text("👑 حظاً موفقاً", modifier = Modifier.clickable { showChatDialog = false; viewModel.soundManager.playButtonClick() }, fontSize = 20.sp)
                    Text("🎲 رمية ملكية!", modifier = Modifier.clickable { showChatDialog = false; viewModel.soundManager.playButtonClick() }, fontSize = 20.sp)
                    Text("🔥 لعبة ممتازة", modifier = Modifier.clickable { showChatDialog = false; viewModel.soundManager.playButtonClick() }, fontSize = 20.sp)
                }
            },
            containerColor = BoardWoodDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = SurfaceDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient Luxury Mahogany Room Backdrop
            Image(
                painter = painterResource(id = R.drawable.luxury_table_backdrop),
                contentDescription = "Luxury Tabletop Backdrop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay for visual clarity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.38f),
                                Color.Black.copy(alpha = 0.76f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TOP ROYAL HEADER BAR (Responsive & Adaptive for ALL Screen Sizes)
                TopRoyalHeader(
                    userProfile = userProfile,
                    state = state,
                    gameMode = viewModel.gameMode,
                    whitePipCount = whitePipCount,
                    blackPipCount = blackPipCount,
                    onMenuClicked = { showResignConfirm = true },
                    onLeaderboardClicked = onNavigateToLeaderboard,
                    onGraphicsClicked = { showGraphicsDialog = true },
                    onGameSwitcherClicked = { showGameSwitcherModal = true },
                    onCameraCycle = {
                        viewModel.soundManager.playButtonClick()
                        cameraController.cyclePreset()
                    },
                    cameraPreset = cameraController.activePreset
                )

                // AI Thinking progress bar
                AnimatedVisibility(
                    visible = isAiThinking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = GoldPrimary,
                        trackColor = BoardWoodMedium
                    )
                }

                // 2. MAIN 3D BOARD CANVAS (Adaptive aspect ratio container fitting all phone ratios)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current

                    // Calculate balanced board dimensions that fit within constraints
                    // Ideal backgammon aspect ratio (height / width) is ~ 1.38f
                    val availableW = maxWidth
                    val availableH = maxHeight
                    val targetAspect = 1.38f

                    val boardWidth = if (availableH / availableW > targetAspect) {
                        availableW.coerceAtMost(560.dp)
                    } else {
                        (availableH / targetAspect).coerceAtMost(560.dp)
                    }
                    val boardHeight = (boardWidth * targetAspect).coerceAtMost(availableH)

                    // 3D Perspective Board Container with Dynamic Camera Controller
                    Box(
                        modifier = Modifier
                            .size(width = boardWidth, height = boardHeight)
                            .graphicsLayer {
                                rotationX = animatedPitch
                                cameraDistance = 16f * density.density
                                scaleX = animatedZoom
                                scaleY = animatedZoom
                                translationY = animatedOffsetY * size.height
                                transformOrigin = TransformOrigin(0.5f, 0.45f)
                            }
                    ) {
                        BoardCanvas(
                            state = state,
                            onPointClicked = { point -> viewModel.onPointClicked(point) },
                            onBarClicked = { player -> viewModel.onBarClicked(player) },
                            onBearOffClicked = { viewModel.onBearOffTrayClicked() }
                        )

                        // 3D Physical Table Dice Overlay
                        TableDiceOverlay(
                            state = state,
                            isAiThinking = isAiThinking,
                            soundManager = viewModel.soundManager,
                            onRollDiceClicked = { viewModel.onRollDiceClicked() }
                        )

                        // Royal Particle Overlay (Victory & Special Events)
                        RoyalParticleOverlay(
                            triggerHitX = null,
                            triggerHitY = null,
                            triggerWin = state.phase == GamePhase.GAME_OVER && state.winner != null
                        )
                    }

                    // Floating Interactive Camera Controller (Top-Center of Board)
                    FloatingCameraControls(
                        controller = cameraController,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        onSoundClick = { viewModel.soundManager.playButtonClick() }
                    )

                    // Rule Violation Toast/Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = ruleViolation != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 46.dp)
                    ) {
                        ruleViolation?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2B1410).copy(alpha = 0.95f))
                                    .border(1.5.dp, Color(0xFFEF5350), RoundedCornerShape(14.dp))
                                    .clickable { showTutorial = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("rule_violation_banner"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alert",
                                        tint = Color(0xFFFF7043),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "📖 الدليل",
                                        color = GoldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Floating Right Side Action Buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 1. Interactive Tutorial (دليل التعليم التفاعلي)
                        FloatingSideActionButton(
                            icon = Icons.Default.School,
                            label = "تعليم",
                            tag = "side_tutorial_button",
                            onClick = {
                                viewModel.soundManager.playButtonClick()
                                showTutorial = true
                            }
                        )

                        // 2. Appearance Shop (متجر المظاهر 3D)
                        FloatingSideActionButton(
                            icon = Icons.Default.ShoppingBag,
                            label = "المتجر",
                            tag = "side_shop_button",
                            onClick = {
                                viewModel.soundManager.playButtonClick()
                                showShop = true
                            }
                        )

                        // 3. Game Replay (سجل الإعادة 3D)
                        FloatingSideActionButton(
                            icon = Icons.Default.History,
                            label = "إعادة",
                            tag = "side_replay_button",
                            onClick = {
                                viewModel.soundManager.playButtonClick()
                                showReplay = true
                            }
                        )

                        // 4. Daily Reward (مكافأة يومية)
                        FloatingSideActionButton(
                            icon = Icons.Default.CardGiftcard,
                            label = "مكافأة",
                            tag = "side_daily_reward_button",
                            onClick = {
                                viewModel.soundManager.playButtonClick()
                                showDailyRewardDialog = true
                            }
                        )

                        // 5. Switch to Domino 3D (دومينو)
                        FloatingSideActionButton(
                            icon = Icons.Default.Casino,
                            label = "الدومينو",
                            tag = "side_domino_button",
                            onClick = {
                                viewModel.soundManager.playButtonClick()
                                showGameSwitcherModal = true
                            }
                        )
                    }
                }

                // 3. BOTTOM ROYAL ACTION BAR
                BottomRoyalActionBar(
                    state = state,
                    isAiThinking = isAiThinking,
                    autoBetEnabled = autoBetEnabled,
                    onAutoBetToggled = { autoBetEnabled = it },
                    onChatClicked = { showChatDialog = true },
                    onStickersClicked = { showChatDialog = true },
                    onUndoClicked = {
                        viewModel.soundManager.playButtonClick()
                        viewModel.undoTurnMove()
                    },
                    onRollDiceClicked = { viewModel.onRollDiceClicked() },
                    onResignClicked = { showResignConfirm = true }
                )
            }
        }

        // Interactive Tutorial Overlay
        InteractiveTutorialOverlay(
            isOpen = showTutorial,
            soundManager = viewModel.soundManager,
            onDismiss = { showTutorial = false }
        )

        // Appearance Shop Screen Dialog
        if (showShop) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showShop = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.example.ui.screens.shop.AppearanceShopScreen(
                    userProfile = userProfile,
                    authRepository = viewModel.authRepository,
                    soundManager = viewModel.soundManager,
                    onNavigateBack = { showShop = false }
                )
            }
        }

        // Game History & Replay Screen Dialog
        if (showReplay) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showReplay = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.example.ui.screens.replay.GameReplayScreen(
                    soundManager = viewModel.soundManager,
                    onNavigateBack = { showReplay = false }
                )
            }
        }

        // Victory / Defeat Result Dialog
        if (state.phase == GamePhase.GAME_OVER && state.winner != null) {
            ResultDialog(
                winner = state.winner!!,
                winType = state.winType,
                isUserWinner = state.winner == Player.WHITE,
                ratingChange = ratingChange,
                coinsEarned = coinsEarned,
                onRematchClicked = { viewModel.restartGame() },
                onHomeClicked = { onNavigateBack() }
            )
        }
    }
}

@Composable
private fun TopRoyalHeader(
    userProfile: UserProfile?,
    state: com.example.game.BoardState,
    gameMode: GameMode,
    whitePipCount: Int,
    blackPipCount: Int,
    onMenuClicked: () -> Unit,
    onLeaderboardClicked: () -> Unit,
    onGraphicsClicked: () -> Unit,
    onGameSwitcherClicked: () -> Unit,
    onCameraCycle: () -> Unit,
    cameraPreset: CameraPreset
) {
    val oppName = when (gameMode) {
        is GameMode.VsAI -> "Ahmed_123"
        is GameMode.Local2Player -> "Player 2"
        is GameMode.OnlineMatch -> "Ahmed_123"
        is GameMode.PrivateRoom -> "Guest_Room"
    }

    val userName = userProfile?.username ?: "MinaXO"
    val userCoins = userProfile?.coins ?: 1250
    val oppCoins = 980

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Upper utility bar: Menu, Game Title / Switcher, Quick Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Hamburger Menu & Game Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMenuClicked,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDark.copy(alpha = 0.85f))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .testTag("game_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Clickable Title that opens the Game Module Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BoardWoodDark.copy(alpha = 0.6f))
                        .clickable { onGameSwitcherClicked() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👑", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BACKGAMMON 3D",
                        color = GoldSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right: Quick Switch to Dominoes, Graphics Settings, Leaderboard
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Switch to Dominoes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B5E20))
                        .border(1.dp, GoldPrimary, RoundedCornerShape(8.dp))
                        .clickable { onGameSwitcherClicked() }
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🁢", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "الدومينو",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Graphics Settings Dialog Button
                IconButton(
                    onClick = onGraphicsClicked,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDark.copy(alpha = 0.85f))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Graphics Settings",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Leaderboard
                IconButton(
                    onClick = onLeaderboardClicked,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDark.copy(alpha = 0.85f))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = GoldSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lower match bar: Player 1 (Left) - VS Badge (Center) - Player 2 (Right)
        // Using weight(1f) ensures NO text clipping on any narrow phone screen!
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Player 1 Card (Blue Neon Glow)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.5.dp,
                        if (state.turn == Player.WHITE) Color(0xFF2979FF) else Color(0xFF1565C0).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF2979FF)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.92f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF448AFF), Color(0xFF0D47A1))))
                                .border(1.2.dp, Color(0xFF82B1FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Player 1",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "👑", fontSize = 9.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coins",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$userCoins",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "🇪🇬", fontSize = 10.sp)
                            }
                        }
                    }

                    // White Pip Count Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$whitePipCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "نقاط",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Center VS Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BoardWoodDark)
                    .border(1.dp, GoldDark, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VS",
                    color = GoldSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Player 2 Card (Crimson Neon Glow)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        1.5.dp,
                        if (state.turn == Player.BLACK) Color(0xFFFF1744) else Color(0xFFC2185B).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFFF1744)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.92f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Black Pip Count Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$blackPipCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "نقاط",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = oppName,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🇰🇼", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coins",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$oppCoins",
                                    color = GoldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFB71C1C))))
                                .border(1.2.dp, Color(0xFFFF8A80), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (gameMode is GameMode.VsAI) Icons.Default.SmartToy else Icons.Default.Person,
                                contentDescription = "Opponent",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSideActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.2.dp, Brush.linearGradient(listOf(GoldDark, GoldSecondary)), RoundedCornerShape(10.dp))
            .testTag(tag),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = GoldSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomRoyalActionBar(
    state: com.example.game.BoardState,
    isAiThinking: Boolean,
    autoBetEnabled: Boolean,
    onAutoBetToggled: (Boolean) -> Unit,
    onChatClicked: () -> Unit,
    onStickersClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onRollDiceClicked: () -> Unit,
    onResignClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Controls: Chat & Stickers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BottomActionButton(
                    icon = Icons.Default.ChatBubble,
                    label = "الدردشة",
                    tag = "bottom_chat_button",
                    onClick = onChatClicked
                )
                BottomActionButton(
                    icon = Icons.Default.Mood,
                    label = "الملصقات",
                    tag = "bottom_stickers_button",
                    onClick = onStickersClicked
                )
            }

            // Center Controls: Undo, Big Highlighted Roll Dice Button, Resign
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Undo Button (تراجع)
                BottomActionButton(
                    icon = Icons.Default.Undo,
                    label = "تراجع",
                    tag = "bottom_undo_button",
                    enabled = state.turnMoves.isNotEmpty() && !isAiThinking,
                    onClick = onUndoClicked
                )

                // Highlighted Roll Dice Button (رمي النرد)
                val canRoll = state.phase == GamePhase.ROLL_DICE &&
                        state.turn == Player.WHITE && !isAiThinking

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canRoll) Brush.radialGradient(listOf(GoldSecondary, GoldPrimary, GoldDark))
                            else Brush.radialGradient(listOf(CardDark, BoardWoodDark))
                        )
                        .border(
                            2.dp,
                            if (canRoll) GoldSecondary else CardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = canRoll) { onRollDiceClicked() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("bottom_roll_dice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Roll Dice",
                            tint = if (canRoll) BoardWoodDark else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "رمي النرد",
                            color = if (canRoll) BoardWoodDark else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Resign Button (استسلام)
                BottomActionButton(
                    icon = Icons.Default.Flag,
                    label = "استسلام",
                    tag = "bottom_resign_button",
                    onClick = onResignClicked
                )
            }

            // Right Toggle: Auto Bet / Doubling (المراهنة التلقائية)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "مراهنة",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = autoBetEnabled,
                    onCheckedChange = onAutoBetToggled,
                    modifier = Modifier.scale(0.72f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldPrimary,
                        checkedTrackColor = GoldDark,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BoardWoodDark
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .testTag(tag)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) TextPrimary else TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            color = if (enabled) TextPrimary else TextMuted.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

