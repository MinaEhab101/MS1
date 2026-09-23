package com.example.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

data class TutorialStep(
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val icon: ImageVector,
    val highlightedZone: String,
    val isInteractive: Boolean = false
)

val TUTORIAL_STEPS = listOf(
    TutorialStep(
        titleAr = "اتجاه سير اللعبة ورقعة الطاولة",
        titleEn = "Board Layout & Direction",
        descriptionAr = "تتكون طاولة الزهر من 24 مثلثاً (نقاط). يتحرك اللاعب الأبيض عكس عقارب الساعة من النقطة 24 نحو 1 (بيته)، بينما يتحرك الأسود بالاتجاه المعاكس تماماً من 1 نحو 24.",
        descriptionEn = "Backgammon consists of 24 points. White moves counter-clockwise from 24 to 1 (Home Board), while Black moves in the opposite direction from 1 to 24.",
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        highlightedZone = "points_direction"
    ),
    TutorialStep(
        titleAr = "رمي النرد وقاعدة الدبل (المضاعفة)",
        titleEn = "Rolling Dice & Doubles",
        descriptionAr = "يرمي كل لاعب نردين في دوره لتحديد مسافة التقدم. إذا ظهر رقمان متطابقان (Double مثل 4-4)، تحصل على 4 حركات كاملة بدلاً من حركتين!",
        descriptionEn = "Players roll two dice. If you roll doubles (e.g., 4-4), you are rewarded with 4 moves of that value instead of 2!",
        icon = Icons.Default.Casino,
        highlightedZone = "dice_area"
    ),
    TutorialStep(
        titleAr = "النقاط المغلقة وبناء السدود (Blocking)",
        titleEn = "Blocked Points & Fortresses",
        descriptionAr = "أي نقطة تحتوي على قطعتين أو أكثر لنفس اللاعب تعتبر نقطة مغلقة ومحمية (سد). لا يمكن للخصم النزول عليها أو المرور عبرها كهدف نهائي.",
        descriptionEn = "Points with 2 or more checkers of the same player are safe closed points. Opponents cannot land on them.",
        icon = Icons.Default.Security,
        highlightedZone = "blocked_points"
    ),
    TutorialStep(
        titleAr = "صيد البلطة والحاجز (Hitting Blots & The Bar)",
        titleEn = "Hitting Blots & The Bar",
        descriptionAr = "القطعة الفردية التي تقف وحيدة على النقطة تسمى 'بلطة' (Blot). إذا نزل الخصم عليها، تُصاد وتُرسل للحاجز الأوسط. يجب إلزامياً إعادة إدخال القطع من الحاجز أولاً قبل تحريك أي قطعة أخرى!",
        descriptionEn = "A single checker alone on a point is a 'Blot'. If an opponent lands on it, it is hit and sent to the Bar. Bar checkers must re-enter before any other moves!",
        icon = Icons.Default.TouchApp,
        highlightedZone = "bar_area"
    ),
    TutorialStep(
        titleAr = "إخراج القطع والفوز (Bearing Off)",
        titleEn = "Bearing Off & Victory",
        descriptionAr = "عندما تجمع كافة قطعك الـ15 داخل بيتك (النقاط 1-6 للأبيض)، تبدأ مرحلة إخراج القطع حسب أرقام النرد. أول لاعب يخرج جميع قطعه يفوز بالمباراة!",
        descriptionEn = "Once all 15 checkers are gathered safely in your Home Board (1-6), you can bear them off. The first to bear off all 15 checkers wins!",
        icon = Icons.Default.Flag,
        highlightedZone = "bear_off_area"
    ),
    TutorialStep(
        titleAr = "تجربة تفاعلية: جرب حركتك الأولى!",
        titleEn = "Interactive Demo: Try a Move!",
        descriptionAr = "لديك نرد بقيمة [5]. انقر على القطعة المضيئة على النقطة 8 وحركها إلى النقطة 3!",
        descriptionEn = "You rolled a [5]. Tap the glowing checker on point 8 and move it to point 3!",
        icon = Icons.Default.School,
        highlightedZone = "demo_point_8",
        isInteractive = true
    )
)

@Composable
fun InteractiveTutorialOverlay(
    isOpen: Boolean,
    soundManager: SoundManager?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var demoPieceSelected by remember { mutableStateOf(false) }
    var demoCompleted by remember { mutableStateOf(false) }

    val step = TUTORIAL_STEPS[currentStepIndex]
    val totalSteps = TUTORIAL_STEPS.size

    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .testTag("interactive_tutorial_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(CardDark)
                .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Step Index and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldSecondary, GoldDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = BoardWoodDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "دليل اللاعب الذكي (${currentStepIndex + 1}/$totalSteps)",
                        color = GoldSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("tutorial_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Title
            Text(
                text = step.titleAr,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = step.titleEn,
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14100C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = step.descriptionAr,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Demo Zone for Step 6
            if (step.isInteractive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (demoCompleted) PointHighlight else GoldPrimary.copy(alpha = glowAlpha),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = BoardWoodDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (demoCompleted) "✨ أحسنت صنعاً! حركة صحيحة 100%"
                            else if (demoPieceSelected) "الآن انقر على النقطة [3] لإتمام الحركة بالنرد (5)"
                            else "انقر على القطعة البيضاء في النقطة [8] لتحديدها",
                            color = if (demoCompleted) PointHighlight else GoldSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mini Interactive Board Representation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Point 8 (Source)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (demoPieceSelected) GoldDark else Color(0xFF261D15))
                                    .border(
                                        2.dp,
                                        if (demoPieceSelected) GoldSecondary else GoldPrimary.copy(alpha = glowAlpha),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (!demoCompleted) {
                                            demoPieceSelected = true
                                            soundManager?.playCheckerSelect()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("tutorial_demo_point_8")
                            ) {
                                Text(text = "نقطة 8", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (demoCompleted) "فارغة" else "⚪ قطعة", color = TextPrimary, fontSize = 12.sp)
                            }

                            // Movement Arrow
                            Text(
                                text = "⮜ 5 خطوات ⮜",
                                color = GoldPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )

                            // Point 3 (Target)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (demoCompleted) Color(0xFF1B5E20) else Color(0xFF261D15))
                                    .border(
                                        2.dp,
                                        if (demoPieceSelected && !demoCompleted) PointHighlight.copy(alpha = glowAlpha)
                                        else if (demoCompleted) PointHighlight else CardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = demoPieceSelected && !demoCompleted) {
                                        demoCompleted = true
                                        soundManager?.playCheckerMove()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("tutorial_demo_point_3")
                            ) {
                                Text(text = "نقطة 3", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (demoCompleted) "⚪ استقرت هنا!" else "الهدف 🎯",
                                    color = if (demoCompleted) PointHighlight else TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Step Navigation Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == currentStepIndex) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (idx == currentStepIndex) GoldPrimary else TextMuted.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons (Next / Previous / Complete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = {
                            currentStepIndex--
                            demoPieceSelected = false
                            demoCompleted = false
                            soundManager?.playCheckerSelect()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق", fontSize = 13.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Next or Finish Button
                Button(
                    onClick = {
                        if (currentStepIndex < totalSteps - 1) {
                            currentStepIndex++
                            demoPieceSelected = false
                            demoCompleted = false
                            soundManager?.playCheckerSelect()
                        } else {
                            soundManager?.playVictory()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("tutorial_next_button")
                ) {
                    Text(
                        text = if (currentStepIndex < totalSteps - 1) "التالي" else "إنهاء والبدء باللعب 🚀",
                        color = BoardWoodDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (currentStepIndex < totalSteps - 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = BoardWoodDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
