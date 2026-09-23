package com.example.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.game.GraphicsQuality
import com.example.game.GraphicsSettings
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class RoyalParticle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val maxAge: Float = 1f
)

@Composable
fun RoyalParticleOverlay(
    triggerHitX: Float?,
    triggerHitY: Float?,
    triggerWin: Boolean,
    modifier: Modifier = Modifier
) {
    val quality by GraphicsSettings.currentQuality.collectAsState()
    val particles = remember { mutableStateListOf<RoyalParticle>() }
    val progress = remember { Animatable(0f) }

    // Hit particle burst
    LaunchedEffect(triggerHitX, triggerHitY) {
        if (triggerHitX != null && triggerHitY != null) {
            val count = (18 * quality.particleMultiplier).toInt().coerceAtLeast(6)
            particles.clear()
            val colors = listOf(GoldPrimary, GoldSecondary, Color(0xFFFFD54F), Color(0xFFFFF9C4))
            for (i in 0 until count) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 120f + 40f
                particles.add(
                    RoyalParticle(
                        startX = triggerHitX,
                        startY = triggerHitY,
                        velocityX = cos(angle) * speed,
                        velocityY = sin(angle) * speed,
                        size = Random.nextFloat() * 4f + 3f,
                        color = colors.random()
                    )
                )
            }
            progress.snapTo(0f)
            progress.animateTo(1f, tween(500, easing = LinearEasing))
            particles.clear()
        }
    }

    // Victory confetti celebration
    LaunchedEffect(triggerWin) {
        if (triggerWin) {
            val count = (40 * quality.particleMultiplier).toInt().coerceAtLeast(12)
            particles.clear()
            val colors = listOf(GoldPrimary, GoldSecondary, Color(0xFFD4AF37), Color(0xFFE57373), Color.White)
            for (i in 0 until count) {
                particles.add(
                    RoyalParticle(
                        startX = Random.nextFloat() * 800f,
                        startY = 0f,
                        velocityX = Random.nextFloat() * 40f - 20f,
                        velocityY = Random.nextFloat() * 180f + 90f,
                        size = Random.nextFloat() * 6f + 4f,
                        color = colors.random()
                    )
                )
            }
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1800, easing = LinearEasing))
        }
    }

    if (particles.isNotEmpty() && progress.value < 1f) {
        val currentT = progress.value
        val alpha = (1f - currentT).coerceIn(0f, 1f)

        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { p ->
                val px = p.startX + p.velocityX * currentT
                val py = p.startY + p.velocityY * currentT + (if (triggerWin) 0.5f * 200f * currentT * currentT else 0f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * (1f - currentT * 0.4f),
                    center = Offset(px, py)
                )
            }
        }
    }
}
