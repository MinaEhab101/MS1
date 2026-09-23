package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 900)
        )
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(BoardWoodMedium, BoardWoodDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            // App Royal Emblem
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = GoldPrimary)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldSecondary, GoldDark, BoardWoodDark)
                        )
                    )
                    .border(3.5.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.backgammon_king_logo),
                    contentDescription = "Backgammon King Logo",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BACKGAMMON KING",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldSecondary,
                letterSpacing = 3.sp
            )

            Text(
                text = "Royal 3D Backgammon Experience",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldPrimary,
                letterSpacing = 1.sp
            )

            Text(
                text = "طاولة الزهر الملكية ثلاثية الأبعاد",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                color = GoldPrimary,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
