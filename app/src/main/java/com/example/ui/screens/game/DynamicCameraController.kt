package com.example.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GraphicsQuality
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch

enum class CameraPreset(
    val labelAr: String,
    val labelEn: String,
    val basePitch: Float,
    val baseZoom: Float,
    val offsetYRatio: Float
) {
    CINEMATIC_CLOSE(
        labelAr = "سينمائي قريب",
        labelEn = "Cinematic Close",
        basePitch = 14f,
        baseZoom = 1.22f,
        offsetYRatio = -0.04f
    ),
    ACTION_ZOOM(
        labelAr = "تركيز حركي",
        labelEn = "Action Focus",
        basePitch = 18f,
        baseZoom = 1.35f,
        offsetYRatio = -0.06f
    ),
    PERSPECTIVE_3D(
        labelAr = "ثلاثي الأبعاد",
        labelEn = "Perspective 3D",
        basePitch = 16f,
        baseZoom = 1.10f,
        offsetYRatio = 0f
    ),
    TOP_DOWN(
        labelAr = "من الأعلى",
        labelEn = "Top-Down",
        basePitch = 0f,
        baseZoom = 1.04f,
        offsetYRatio = 0f
    )
}

@Stable
class DynamicCameraController(
    initialPreset: CameraPreset = CameraPreset.CINEMATIC_CLOSE
) {
    var activePreset by mutableStateOf(initialPreset)
        private set

    var userZoomOffset by mutableFloatStateOf(0f)
        private set

    val animPitch = Animatable(initialPreset.basePitch)
    val animZoom = Animatable(initialPreset.baseZoom)
    val animOffsetY = Animatable(initialPreset.offsetYRatio)

    fun cyclePreset() {
        val next = when (activePreset) {
            CameraPreset.CINEMATIC_CLOSE -> CameraPreset.ACTION_ZOOM
            CameraPreset.ACTION_ZOOM -> CameraPreset.PERSPECTIVE_3D
            CameraPreset.PERSPECTIVE_3D -> CameraPreset.TOP_DOWN
            CameraPreset.TOP_DOWN -> CameraPreset.CINEMATIC_CLOSE
        }
        setPreset(next)
    }

    fun setPreset(preset: CameraPreset) {
        activePreset = preset
    }

    fun zoomIn() {
        if (userZoomOffset < 0.35f) {
            userZoomOffset = (userZoomOffset + 0.08f).coerceAtMost(0.35f)
        }
    }

    fun zoomOut() {
        if (userZoomOffset > -0.15f) {
            userZoomOffset = (userZoomOffset - 0.08f).coerceAtLeast(-0.15f)
        }
    }

    fun resetZoom() {
        userZoomOffset = 0f
        setPreset(CameraPreset.CINEMATIC_CLOSE)
    }

    fun triggerActionZoom(active: Boolean) {
        if (activePreset == CameraPreset.CINEMATIC_CLOSE || activePreset == CameraPreset.ACTION_ZOOM) {
            activePreset = if (active) CameraPreset.ACTION_ZOOM else CameraPreset.CINEMATIC_CLOSE
        }
    }
}

@Composable
fun rememberDynamicCameraController(
    initialPreset: CameraPreset = CameraPreset.CINEMATIC_CLOSE
): DynamicCameraController {
    return remember { DynamicCameraController(initialPreset) }
}

@Composable
fun FloatingCameraControls(
    controller: DynamicCameraController,
    modifier: Modifier = Modifier,
    onSoundClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark.copy(alpha = 0.88f))
            .border(1.dp, GoldDark, RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Preset Switcher Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(BoardWoodDark, Color(0xFF221108))))
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .clickable {
                    onSoundClick()
                    controller.cyclePreset()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Camera Angle",
                tint = GoldSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = controller.activePreset.labelAr,
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Zoom Out (-)
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(BoardWoodDark)
                .clickable {
                    onSoundClick()
                    controller.zoomOut()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = TextPrimary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Current Zoom Level Indicator
        val displayZoomPct = ((controller.activePreset.baseZoom + controller.userZoomOffset) * 100).toInt()
        Text(
            text = "${displayZoomPct}%",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Zoom In (+)
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(BoardWoodDark)
                .clickable {
                    onSoundClick()
                    controller.zoomIn()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Reset (Center Target)
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(BoardWoodDark)
                .clickable {
                    onSoundClick()
                    controller.resetZoom()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterCenterFocus,
                contentDescription = "Reset Camera",
                tint = GoldSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
