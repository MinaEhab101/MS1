package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.SoundManager
import com.example.game.DailyRewardManager
import com.example.game.DailyRewardTier
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodLight
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CoinParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color
)

@Composable
fun RoyalDailyRewardDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onRewardClaimed: (coins: Int) -> Unit,
    soundManager: SoundManager? = null
) {
    if (!isOpen) return

    val context = LocalContext.current
    val currentStreak by DailyRewardManager.currentStreak.collectAsState()
    val canClaim by DailyRewardManager.canClaimToday.collectAsState()

    var showClaimCelebration by remember { mutableStateOf(false) }
    var claimedAmount by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "chest_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Coin celebration explosion particles
    val particles = remember { mutableStateListOf<CoinParticle>() }
    val particleProgress = remember { Animatable(0f) }

    LaunchedEffect(showClaimCelebration) {
        if (showClaimCelebration) {
            particles.clear()
            val colors = listOf(GoldPrimary, GoldSecondary, Color(0xFFFFD54F), Color(0xFFFFF9C4))
            for (i in 0 until 40) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 260f + 90f
                particles.add(
                    CoinParticle(
                        x = 300f,
                        y = 350f,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 60f,
                        size = Random.nextFloat() * 8f + 5f,
                        color = colors.random()
                    )
                )
            }
            particleProgress.snapTo(0f)
            particleProgress.animateTo(1f, tween(1200, easing = LinearEasing))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable { onDismiss() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clickable(enabled = false) {}
                    .border(
                        2.5.dp,
                        Brush.linearGradient(listOf(GoldDark, GoldSecondary, GoldPrimary, GoldDark)),
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("royal_daily_reward_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BoardWoodDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark)))
                                    .border(1.5.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Daily Reward",
                                    tint = BoardWoodDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ROYAL DAILY REWARD",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldSecondary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "المكافأة الملكية اليومية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoldPrimary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CardDark)
                                .border(1.dp, CardBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Claim your royal bounty every consecutive day to unlock the grand King's Vault on Day 7!",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7-Day Horizontal Cards Carousel
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DailyRewardManager.rewardTiers) { tier ->
                            val isClaimed = tier.day < currentStreak || (tier.day == currentStreak && !canClaim)
                            val isToday = tier.day == currentStreak && canClaim
                            val isLocked = tier.day > currentStreak

                            DailyRewardTierCard(
                                tier = tier,
                                isToday = isToday,
                                isClaimed = isClaimed,
                                isLocked = isLocked,
                                pulseScale = if (isToday) pulseScale else 1f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Claim Status Box / Button
                    if (canClaim) {
                        val todayTier = DailyRewardManager.rewardTiers.find { it.day == currentStreak }
                            ?: DailyRewardManager.rewardTiers[0]

                        Button(
                            onClick = {
                                soundManager?.playDiceRoll() // Celebratory sound
                                val coins = DailyRewardManager.claimReward(context)
                                claimedAmount = coins
                                showClaimCelebration = true
                                onRewardClaimed(coins)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(pulseScale)
                                .testTag("claim_daily_reward_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coins",
                                    tint = BoardWoodDark,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CLAIM +${todayTier.coins} COINS",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BoardWoodDark,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    } else {
                        // Already Claimed Today banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardDark)
                                .border(1.dp, GoldDark, RoundedCornerShape(14.dp))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Claimed",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reward Claimed Today! Come back tomorrow for Day $currentStreak",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Success banner after claim
                    AnimatedVisibility(
                        visible = showClaimCelebration,
                        enter = fadeIn() + scaleIn()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 +$claimedAmount Gold Coins Added to Balance!",
                                color = GoldSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Canvas for floating reward explosion particles
            if (showClaimCelebration && particleProgress.value < 1f) {
                val t = particleProgress.value
                val alpha = (1f - t).coerceIn(0f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    particles.forEach { p ->
                        val px = p.x + p.vx * t
                        val py = p.y + p.vy * t + 0.5f * 200f * t * t
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size * (1f - t * 0.5f),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRewardTierCard(
    tier: DailyRewardTier,
    isToday: Boolean,
    isClaimed: Boolean,
    isLocked: Boolean,
    pulseScale: Float
) {
    val borderColor = when {
        isToday -> GoldSecondary
        isClaimed -> GoldDark.copy(alpha = 0.5f)
        else -> CardBorder
    }

    val containerColor = when {
        isToday -> BoardWoodMedium
        isClaimed -> CardDark.copy(alpha = 0.6f)
        else -> CardDark
    }

    Card(
        modifier = Modifier
            .width(86.dp)
            .scale(pulseScale)
            .border(if (isToday) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = tier.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) GoldSecondary else TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isToday) Brush.radialGradient(listOf(GoldSecondary, GoldDark))
                        else Brush.radialGradient(listOf(CardDark, BoardWoodDark))
                    )
                    .border(
                        1.dp,
                        if (isToday) GoldPrimary else CardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isClaimed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Claimed",
                        tint = GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (tier.isSpecial) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Grand Chest",
                        tint = if (isToday) BoardWoodDark else GoldSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = if (isToday) BoardWoodDark else GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "+${tier.coins}",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isToday) GoldPrimary else if (isClaimed) TextMuted else TextPrimary
            )

            Text(
                text = if (isClaimed) "Claimed" else if (isToday) "Today!" else "Day ${tier.day}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (isToday) GoldSecondary else TextMuted
            )
        }
    }
}
