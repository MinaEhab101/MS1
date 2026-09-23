package com.example.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.game.BoardState
import com.example.game.GamePhase
import com.example.game.Player
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Realistic 3D-styled physical dice throw overlay onto the Backgammon table.
 *
 * Implements:
 * - Ballistic launch from player's side onto the wooden/felt board
 * - Multi-axis 3D tumbling rotation (X, Y, Z angles)
 * - Two-stage physics bounce with felt compression and dampening
 * - Dynamic fast pip shuffling while in flight to simulate rotating 3D faces
 * - Synchronized table contact impacts with sound and haptics
 * - Dynamic 3D contact shadows that elevate during flight and anchor on landing
 * - Full tournament double presentation (displaying all 4 dice when doubles hit)
 * - Active remaining vs. played dice states
 * - Interactive "Tap Table to Roll" invitation on the felt
 */
@Composable
fun TableDiceOverlay(
    state: BoardState,
    isAiThinking: Boolean,
    soundManager: SoundManager?,
    onRollDiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastRoll = state.lastRoll
    val rollAnim = remember { Animatable(1f) }
    var isRollingActive by remember { mutableStateOf(false) }

    // Fast face values during roll
    var shuffleD1 by remember { mutableIntStateOf(1) }
    var shuffleD2 by remember { mutableIntStateOf(1) }

    // Natural random landing resting tilt angles and position jitter
    var restAngle1 by remember { mutableStateOf(-7f) }
    var restAngle2 by remember { mutableStateOf(11f) }
    var restJitterX1 by remember { mutableStateOf(-6f) }
    var restJitterY1 by remember { mutableStateOf(4f) }
    var restJitterX2 by remember { mutableStateOf(8f) }
    var restJitterY2 by remember { mutableStateOf(-5f) }

    // Detect new dice roll event
    LaunchedEffect(lastRoll) {
        if (lastRoll != null) {
            isRollingActive = true
            // Generate random resting variations
            restAngle1 = Random.nextInt(-14, -3).toFloat()
            restAngle2 = Random.nextInt(4, 16).toFloat()
            restJitterX1 = Random.nextInt(-12, 4).toFloat()
            restJitterY1 = Random.nextInt(-8, 8).toFloat()
            restJitterX2 = Random.nextInt(2, 14).toFloat()
            restJitterY2 = Random.nextInt(-8, 8).toFloat()

            // Play initial throw shake sound
            soundManager?.playDiceRoll()

            // Reset and launch animation
            rollAnim.snapTo(0f)

            // Rapid pip shuffle coroutine during flight
            val shuffleJob = launch {
                while (rollAnim.value < 0.78f) {
                    shuffleD1 = Random.nextInt(1, 7)
                    shuffleD2 = Random.nextInt(1, 7)
                    delay(45)
                }
                shuffleD1 = lastRoll.first
                shuffleD2 = lastRoll.second
            }

            // Animate 3D throw and bounce
            rollAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = LinearEasing)
            )

            // Second impact sound on bounce landing
            soundManager?.playDiceImpact()

            shuffleJob.cancel()
            shuffleD1 = lastRoll.first
            shuffleD2 = lastRoll.second
            isRollingActive = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boardW = maxWidth
        val boardH = maxHeight

        // Table landing target area: right half of board, centered vertically on open felt
        // Right quadrant is roughly between 52% and 90% of board width, centered at ~72%
        val targetCenterX = boardW * 0.72f
        val targetCenterY = boardH * 0.50f

        val animProgress = rollAnim.value
        val isWhiteTurn = state.turn == Player.WHITE

        // Flight physics calculations:
        // Launch origin: White launches from bottom (+Y), Black launches from top (-Y)
        val launchOffsetY = if (isWhiteTurn) 240.dp else (-240).dp
        val launchOffsetX = if (isWhiteTurn) 40.dp else (-40.dp)

        // Trajectory progress: ease-out curve
        val travelProgress = FastOutSlowInEasing.transform(animProgress)
        val currentOffsetX = launchOffsetX * (1f - travelProgress)
        val currentOffsetY = launchOffsetY * (1f - travelProgress)

        // Height arc (Z axis): peaks around 0.35, lands at 0.60, bounces to 0.78, settles at 1.0
        val zHeight = calculateBounceHeight(animProgress)
        val dieScale = 1.0f + (zHeight * 0.38f) // Scale larger when high in the air

        // 3D rotation angles while spinning in air
        val spinFactor = 1f - animProgress
        val rotX1 = (spinFactor * 720f)
        val rotY1 = (spinFactor * 540f)
        val rotZ1 = (spinFactor * 360f) + restAngle1

        val rotX2 = (spinFactor * 540f)
        val rotY2 = (spinFactor * 720f)
        val rotZ2 = (spinFactor * 480f) + restAngle2

        // Current face values (shuffling during throw, final on landing)
        val d1Value = if (animProgress < 0.78f) shuffleD1 else (lastRoll?.first ?: 1)
        val d2Value = if (animProgress < 0.78f) shuffleD2 else (lastRoll?.second ?: 1)

        // Count how many times each face value is still available in state.dice
        val isDouble = lastRoll != null && lastRoll.first == lastRoll.second
        val remainingDice = state.dice

        // Sensor Shake-to-Roll physics hook: Shaking phone triggers natural dice roll throw
        RememberShakeDetector(
            isEnabled = state.phase == GamePhase.ROLL_DICE && state.winner == null && !isAiThinking,
            onShake = {
                soundManager?.playDiceRoll()
                onRollDiceClicked()
            }
        )

        // If in ROLL_DICE phase: Show interactive "Tap to Roll" invitation on the board
        if (state.phase == GamePhase.ROLL_DICE && state.winner == null && !isAiThinking) {
            TapToRollInvitation(
                isWhiteTurn = isWhiteTurn,
                onRollClicked = onRollDiceClicked,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (boardW * 0.22f), y = 0.dp)
            )
        }

        // Display rolled dice on the table
        if (lastRoll != null) {
            val isDie1Used = !isRollingActive && !remainingDice.contains(d1Value)
            // For die 2: if it's not a double, check if remainingDice contains d2; if double, check if count >= 2
            val isDie2Used = !isRollingActive && if (isDouble) {
                remainingDice.count { it == d2Value } < 2
            } else {
                !remainingDice.contains(d2Value)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = (boardW * 0.22f) + currentOffsetX,
                        y = currentOffsetY
                    )
            ) {
                if (isDouble && animProgress >= 0.78f && state.initialDice.size == 4) {
                    // Four dice for doubles layout
                    DoublesDiceCluster(
                        value = d1Value,
                        remainingCount = remainingDice.size,
                        restAngle = restAngle1
                    )
                } else {
                    // Standard pair of dice thrown on the table
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Die 1
                        PhysicsDieItem(
                            value = d1Value,
                            isUsed = isDie1Used,
                            scale = dieScale,
                            zHeight = zHeight,
                            rotX = rotX1,
                            rotY = rotY1,
                            rotZ = rotZ1,
                            jitterX = restJitterX1.dp,
                            jitterY = restJitterY1.dp,
                            testTag = "table_die_1"
                        )

                        // Die 2
                        PhysicsDieItem(
                            value = d2Value,
                            isUsed = isDie2Used,
                            scale = dieScale,
                            zHeight = zHeight,
                            rotX = rotX2,
                            rotY = rotY2,
                            rotZ = rotZ2,
                            jitterX = restJitterX2.dp,
                            jitterY = restJitterY2.dp,
                            testTag = "table_die_2"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates simulated Z-height with 2 physical bounces on felt table.
 * 0.0 = firmly resting on felt; 1.0 = peak flight altitude.
 */
private fun calculateBounceHeight(t: Float): Float {
    return when {
        t < 0.55f -> {
            // Initial parabolic flight arc in the air
            val normalized = t / 0.55f
            sin(normalized * PI).toFloat()
        }
        t < 0.78f -> {
            // First bounce rebound (peak ~ 0.35 height)
            val normalized = (t - 0.55f) / (0.78f - 0.55f)
            (sin(normalized * PI) * 0.35f).toFloat()
        }
        t < 0.94f -> {
            // Second minor bounce (peak ~ 0.12 height)
            val normalized = (t - 0.78f) / (0.94f - 0.78f)
            (sin(normalized * PI) * 0.12f).toFloat()
        }
        else -> 0f // Settled on table
    }
}

/**
 * Individual physical 3D die container with dynamic shadow and 3D perspective rotation.
 */
@Composable
private fun PhysicsDieItem(
    value: Int,
    isUsed: Boolean,
    scale: Float,
    zHeight: Float,
    rotX: Float,
    rotY: Float,
    rotZ: Float,
    jitterX: Dp,
    jitterY: Dp,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    // Dynamic 3D shadow: expands, blurs, and shifts down when die is high in the air
    val shadowBlur = 4.dp + (18f * zHeight).dp
    val shadowOffsetY = 3.dp + (22f * zHeight).dp
    val shadowAlpha = 0.55f - (zHeight * 0.25f)
    val shadowScaleX = 1f + (zHeight * 0.25f)

    Box(
        modifier = modifier
            .offset(x = jitterX, y = jitterY)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // 1. Realistic Cast Shadow on the felt
        Box(
            modifier = Modifier
                .offset(y = shadowOffsetY)
                .size(44.dp)
                .scale(scaleX = shadowScaleX, scaleY = 0.65f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = shadowAlpha),
                            Color.Black.copy(alpha = shadowAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // 2. Physical Die Cube
        Box(
            modifier = Modifier
                .scale(scale)
                .graphicsLayer {
                    rotationX = rotX
                    rotationY = rotY
                    rotationZ = rotZ
                    cameraDistance = 16f * density
                }
        ) {
            ProfessionalDieFace(
                value = value,
                isUsed = isUsed,
                size = 46.dp
            )
        }
    }
}

/**
 * High-craft 3D die face with glossy ivory finish, chamfered bevels, specular reflections,
 * and deeply recessed tournament pips.
 */
@Composable
fun ProfessionalDieFace(
    value: Int,
    isUsed: Boolean,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val opacity = if (isUsed) 0.38f else 1.0f

    Box(
        modifier = modifier
            .size(size)
            .alpha(opacity)
            .shadow(
                elevation = if (isUsed) 1.dp else 4.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            // Polished Ivory Resin Body
            .background(
                brush = Brush.linearGradient(
                    colors = if (isUsed) {
                        listOf(Color(0xFF88847C), Color(0xFF6B6760))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFFAF5EC), Color(0xFFEDE4D2))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                ),
                shape = RoundedCornerShape(10.dp)
            )
            // Outer 3D bevel edge
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = if (isUsed) {
                        listOf(Color.White.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.3f))
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            GoldPrimary.copy(alpha = 0.4f),
                            Color(0xFFB5A68F).copy(alpha = 0.8f)
                        )
                    }
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Top-left Specular Gloss Highlight
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle glossy curved reflection arc across top edge
            val glossPath = Path().apply {
                moveTo(w * 0.12f, h * 0.08f)
                quadraticBezierTo(w * 0.5f, h * 0.04f, w * 0.88f, h * 0.08f)
                quadraticBezierTo(w * 0.5f, h * 0.22f, w * 0.12f, h * 0.08f)
                close()
            }
            drawPath(
                path = glossPath,
                color = Color.White.copy(alpha = if (isUsed) 0.15f else 0.55f)
            )

            // Bottom-right 3D shadow edge bevel
            val bevelPath = Path().apply {
                moveTo(w * 0.96f, h * 0.15f)
                lineTo(w * 0.96f, h * 0.96f)
                lineTo(w * 0.15f, h * 0.96f)
            }
            drawPath(
                path = bevelPath,
                color = Color(0xFF33291F).copy(alpha = 0.35f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw recessed pips
        Canvas(modifier = Modifier.size(size * 0.76f)) {
            val w = this.size.width
            val h = this.size.height

            // Tournament colors: Pip 1 (Ace) is rich crimson red, other pips are onyx dark
            val pipColor = when {
                isUsed -> Color(0xFF3B3630)
                value == 1 -> Color(0xFFC62828) // Tournament Ace Red
                else -> Color(0xFF1E1A17)      // Polished Onyx Black
            }

            val pipRadius = w * 0.115f
            val c = w / 2f
            val left = w * 0.27f
            val right = w * 0.73f
            val top = h * 0.27f
            val bottom = h * 0.73f
            val midY = h * 0.5f

            // Helper to draw recessed pip with inner shadow & gloss dot
            fun drawRecessedPip(cx: Float, cy: Float, radius: Float) {
                // Pip inset drop shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(cx, cy + 1f)
                )
                // Pip main body
                drawCircle(
                    color = pipColor,
                    radius = radius,
                    center = Offset(cx, cy)
                )
                // Inner specular gloss highlight dot
                if (!isUsed) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = radius * 0.35f,
                        center = Offset(cx - radius * 0.3f, cy - radius * 0.3f)
                    )
                }
            }

            when (value) {
                1 -> {
                    // Central larger Ace pip
                    drawRecessedPip(c, c, pipRadius * 1.35f)
                }
                2 -> {
                    drawRecessedPip(left, top, pipRadius)
                    drawRecessedPip(right, bottom, pipRadius)
                }
                3 -> {
                    drawRecessedPip(left, top, pipRadius)
                    drawRecessedPip(c, c, pipRadius)
                    drawRecessedPip(right, bottom, pipRadius)
                }
                4 -> {
                    drawRecessedPip(left, top, pipRadius)
                    drawRecessedPip(right, top, pipRadius)
                    drawRecessedPip(left, bottom, pipRadius)
                    drawRecessedPip(right, bottom, pipRadius)
                }
                5 -> {
                    drawRecessedPip(left, top, pipRadius)
                    drawRecessedPip(right, top, pipRadius)
                    drawRecessedPip(c, c, pipRadius)
                    drawRecessedPip(left, bottom, pipRadius)
                    drawRecessedPip(right, bottom, pipRadius)
                }
                6 -> {
                    drawRecessedPip(left, top, pipRadius)
                    drawRecessedPip(right, top, pipRadius)
                    drawRecessedPip(left, midY, pipRadius)
                    drawRecessedPip(right, midY, pipRadius)
                    drawRecessedPip(left, bottom, pipRadius)
                    drawRecessedPip(right, bottom, pipRadius)
                }
            }
        }

        // If die is spent/used: draw discreet check badge
        if (isUsed) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Used",
                    tint = GoldPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Four-dice cluster presentation when a Double is rolled (e.g. 4x moves).
 */
@Composable
private fun DoublesDiceCluster(
    value: Int,
    remainingCount: Int,
    restAngle: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.rotate(restAngle * 0.5f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProfessionalDieFace(value = value, isUsed = remainingCount < 4, size = 38.dp)
            ProfessionalDieFace(value = value, isUsed = remainingCount < 3, size = 38.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProfessionalDieFace(value = value, isUsed = remainingCount < 2, size = 38.dp)
            ProfessionalDieFace(value = value, isUsed = remainingCount < 1, size = 38.dp)
        }
    }
}

/**
 * Interactive glowing "Tap Table to Roll" invitation on the felt table.
 */
@Composable
private fun TapToRollInvitation(
    isWhiteTurn: Boolean,
    onRollClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_roll")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = GoldPrimary.copy(alpha = pulseGlow))
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF332014).copy(alpha = 0.92f),
                        BoardWoodDark.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(GoldPrimary, GoldDark)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRollClicked
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("tap_table_to_roll"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Roll",
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "هز الهاتف 📳 أو انقر",
                    color = GoldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Shake or Tap to Roll",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
