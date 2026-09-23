package com.example.domino.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domino.engine.DominoEngine
import com.example.domino.model.DominoEnd
import com.example.domino.model.DominoGameState
import com.example.domino.model.PlacedTile
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointHighlight
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun DominoCanvas(
    state: DominoGameState,
    onEndClicked: (DominoEnd) -> Unit,
    modifier: Modifier = Modifier
) {
    var userZoom by remember { mutableFloatStateOf(1.0f) }
    var userPan by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "domino_beacon")
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("domino_board_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        userZoom = (userZoom * zoom).coerceIn(0.6f, 3.5f)
                        userPan += pan
                    }
                }
                .pointerInput(state, userZoom, userPan) {
                    detectTapGestures { tapOffset ->
                        val played = state.playedTiles
                        if (played.isEmpty() && state.selectedTile != null) {
                            onEndClicked(DominoEnd.LEFT)
                            return@detectTapGestures
                        }

                        if (state.validEndsForSelected.isNotEmpty()) {
                            // Convert screen tap coordinates to logical board coordinates
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            val bounds = calculateChainBounds(played)
                            val autoScale = calculateAutoScale(bounds, w, h)
                            val finalScale = autoScale * userZoom
                            val chainCenter = Offset((bounds.minX + bounds.maxX) / 2f, (bounds.minY + bounds.maxY) / 2f)
                            val screenCenter = Offset(w / 2f + userPan.x, h / 2f + userPan.y)

                            val first = played.firstOrNull()
                            val last = played.lastOrNull()

                            if (first != null && state.validEndsForSelected.contains(DominoEnd.LEFT)) {
                                val firstScreenPos = logicalToScreen(first.x, first.y, chainCenter, screenCenter, finalScale)
                                val dist = hypot(tapOffset.x - firstScreenPos.x, tapOffset.y - firstScreenPos.y)
                                if (dist < 85f * userZoom.coerceAtLeast(0.8f)) {
                                    onEndClicked(DominoEnd.LEFT)
                                    return@detectTapGestures
                                }
                            }

                            if (last != null && state.validEndsForSelected.contains(DominoEnd.RIGHT)) {
                                val lastScreenPos = logicalToScreen(last.x, last.y, chainCenter, screenCenter, finalScale)
                                val dist = hypot(tapOffset.x - lastScreenPos.x, tapOffset.y - lastScreenPos.y)
                                if (dist < 85f * userZoom.coerceAtLeast(0.8f)) {
                                    onEndClicked(DominoEnd.RIGHT)
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw 3D Luxury Tabletop Background with Royal Felt & Carvings
            draw3DTableBackground(w, h)

            val played = state.playedTiles
            val bounds = calculateChainBounds(played)
            val autoScale = calculateAutoScale(bounds, w, h)
            val finalScale = autoScale * userZoom
            val chainCenter = Offset((bounds.minX + bounds.maxX) / 2f, (bounds.minY + bounds.maxY) / 2f)
            val screenCenter = Offset(w / 2f + userPan.x, h / 2f + userPan.y)

            // 2. If board is empty, draw prominent central placement target
            if (played.isEmpty()) {
                drawEmptyBoardTarget(screenCenter, state.selectedTile != null, beaconPulse, finalScale)
            } else {
                // Draw all placed 3D domino tiles with dynamic camera transform
                played.forEach { placed ->
                    val screenPos = logicalToScreen(placed.x, placed.y, chainCenter, screenCenter, finalScale)
                    draw3DDominoTile(
                        screenPos = screenPos,
                        isVertical = placed.isVertical,
                        leftOrTopVal = placed.tile.left,
                        rightOrBottomVal = placed.tile.right,
                        scale = finalScale
                    )
                }

                // Draw open end glowing interactive beacons if a tile is selected
                if (state.validEndsForSelected.isNotEmpty()) {
                    val first = played.firstOrNull()
                    val last = played.lastOrNull()

                    if (first != null && state.validEndsForSelected.contains(DominoEnd.LEFT)) {
                        val firstScreenPos = logicalToScreen(first.x, first.y, chainCenter, screenCenter, finalScale)
                        val beaconOffset = if (first.isVertical) Offset(0f, -DominoEngine.TILE_LENGTH * 0.45f * finalScale)
                        else Offset(-DominoEngine.TILE_LENGTH * 0.45f * finalScale, 0f)
                        drawOpenEndBeacon(
                            firstScreenPos + beaconOffset,
                            state.leftOpenEnd ?: 0,
                            beaconPulse,
                            finalScale
                        )
                    }

                    if (last != null && state.validEndsForSelected.contains(DominoEnd.RIGHT)) {
                        val lastScreenPos = logicalToScreen(last.x, last.y, chainCenter, screenCenter, finalScale)
                        val beaconOffset = if (last.isVertical) Offset(0f, DominoEngine.TILE_LENGTH * 0.45f * finalScale)
                        else Offset(DominoEngine.TILE_LENGTH * 0.45f * finalScale, 0f)
                        drawOpenEndBeacon(
                            lastScreenPos + beaconOffset,
                            state.rightOpenEnd ?: 0,
                            beaconPulse,
                            finalScale
                        )
                    }
                }
            }
        }

        // 3. Top Floating Status Ribbon (Open Ends & Bank Count)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Open End Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark.copy(alpha = 0.92f))
                    .border(1.5.dp, if (state.validEndsForSelected.contains(DominoEnd.LEFT)) PointHighlight else GoldDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⬅️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.leftOpenEnd != null) "يسار: ${state.leftOpenEnd}" else "طاولة خالية",
                        color = if (state.validEndsForSelected.contains(DominoEnd.LEFT)) PointHighlight else GoldSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Central Royal Watermark / Boneyard
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark.copy(alpha = 0.92f))
                    .border(1.2.dp, GoldPrimary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Boneyard",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "البنك: ${state.boneyard.size}",
                        color = GoldSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Open End Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark.copy(alpha = 0.92f))
                    .border(1.5.dp, if (state.validEndsForSelected.contains(DominoEnd.RIGHT)) PointHighlight else GoldDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.rightOpenEnd != null) "يمين: ${state.rightOpenEnd}" else "طاولة خالية",
                        color = if (state.validEndsForSelected.contains(DominoEnd.RIGHT)) PointHighlight else GoldSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "➡️", fontSize = 12.sp)
                }
            }
        }

        // 4. Floating Zoom & Center Camera Controls (Top-Right under status bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BoardWoodDark.copy(alpha = 0.88f))
                .border(1.2.dp, GoldDark, RoundedCornerShape(16.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { userZoom = (userZoom * 1.25f).coerceAtMost(3.5f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = GoldSecondary, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = {
                    userZoom = 1.0f
                    userPan = Offset.Zero
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = "Reset Zoom", tint = GoldPrimary, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = { userZoom = (userZoom / 1.25f).coerceAtLeast(0.6f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = GoldSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Bounding box calculations
private data class ChainBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
)

private fun calculateChainBounds(played: List<PlacedTile>): ChainBounds {
    if (played.isEmpty()) {
        return ChainBounds(-100f, 100f, -60f, 60f)
    }
    var minX = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE
    var maxY = Float.MIN_VALUE

    played.forEach { t ->
        val halfW = (if (t.isVertical) DominoEngine.TILE_WIDTH else DominoEngine.TILE_LENGTH) / 2f
        val halfH = (if (t.isVertical) DominoEngine.TILE_LENGTH else DominoEngine.TILE_WIDTH) / 2f
        minX = min(minX, t.x - halfW)
        maxX = max(maxX, t.x + halfW)
        minY = min(minY, t.y - halfH)
        maxY = max(maxY, t.y + halfH)
    }

    return ChainBounds(minX, maxX, minY, maxY)
}

private fun calculateAutoScale(bounds: ChainBounds, w: Float, h: Float): Float {
    val spanX = (bounds.maxX - bounds.minX + 160f).coerceAtLeast(220f)
    val spanY = (bounds.maxY - bounds.minY + 160f).coerceAtLeast(180f)
    val scaleX = w / spanX
    val scaleY = h / spanY
    return min(scaleX, scaleY).coerceIn(1.1f, 2.8f)
}

private fun logicalToScreen(
    logicalX: Float,
    logicalY: Float,
    chainCenter: Offset,
    screenCenter: Offset,
    scale: Float
): Offset {
    val sx = screenCenter.x + (logicalX - chainCenter.x) * scale
    val sy = screenCenter.y + (logicalY - chainCenter.y) * scale
    return Offset(sx, sy)
}

private fun DrawScope.draw3DTableBackground(w: Float, h: Float) {
    // Rich outer mahogany wood frame
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3E1F11), Color(0xFF241007), Color(0xFF130804)),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = w * 0.85f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Inner Velvet Green Domino Felt surface
    val margin = 8f
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF184433), Color(0xFF103326), Color(0xFF092017)),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.75f
        ),
        topLeft = Offset(margin, margin),
        size = Size(w - margin * 2, h - margin * 2),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Gold inlaid perimeter border with double filigree line
    drawRoundRect(
        brush = Brush.linearGradient(listOf(GoldDark, GoldSecondary, GoldPrimary, GoldDark)),
        topLeft = Offset(margin + 2.5f, margin + 2.5f),
        size = Size(w - (margin + 2.5f) * 2, h - (margin + 2.5f) * 2),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 2.0f)
    )

    // Subtle center watermark crown emblem
    val cx = w / 2f
    val cy = h / 2f
    drawCircle(
        color = GoldPrimary.copy(alpha = 0.04f),
        radius = min(w, h) * 0.28f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawEmptyBoardTarget(
    screenCenter: Offset,
    hasSelectedTile: Boolean,
    pulse: Float,
    scale: Float
) {
    val targetW = DominoEngine.TILE_LENGTH * scale
    val targetH = DominoEngine.TILE_WIDTH * scale
    val color = if (hasSelectedTile) PointHighlight.copy(alpha = pulse) else GoldPrimary.copy(alpha = 0.5f)

    drawRoundRect(
        color = color,
        topLeft = Offset(screenCenter.x - targetW / 2f, screenCenter.y - targetH / 2f),
        size = Size(targetW, targetH),
        cornerRadius = CornerRadius(10f * scale, 10f * scale),
        style = Stroke(width = 3f * scale)
    )

    drawCircle(
        color = color,
        radius = 12f * scale,
        center = screenCenter
    )
}

private fun DrawScope.drawOpenEndBeacon(
    center: Offset,
    openValue: Int,
    pulse: Float,
    scale: Float
) {
    val beaconRadius = 32f * scale.coerceIn(0.9f, 1.8f)

    // Radiant pulsing halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PointHighlight.copy(alpha = pulse * 0.85f), Color.Transparent),
            center = center,
            radius = beaconRadius * 1.5f
        ),
        radius = beaconRadius * 1.5f,
        center = center
    )

    // Outer bright beacon circle
    drawCircle(
        color = PointHighlight.copy(alpha = pulse),
        radius = beaconRadius,
        center = center
    )

    // Inner ivory core with number
    drawCircle(
        color = Color(0xFFFFFDE7),
        radius = beaconRadius * 0.72f,
        center = center
    )

    // Draw open value number inside beacon
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f * scale.coerceIn(0.9f, 1.6f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawText(
            openValue.toString(),
            center.x,
            center.y + (paint.textSize * 0.35f),
            paint
        )
    }
}

fun DrawScope.draw3DDominoTile(
    screenPos: Offset,
    isVertical: Boolean,
    leftOrTopVal: Int,
    rightOrBottomVal: Int,
    scale: Float
) {
    val tileW = (if (isVertical) DominoEngine.TILE_WIDTH else DominoEngine.TILE_LENGTH) * scale
    val tileH = (if (isVertical) DominoEngine.TILE_LENGTH else DominoEngine.TILE_WIDTH) * scale
    val topLeft = Offset(screenPos.x - tileW / 2f, screenPos.y - tileH / 2f)

    // 1. Double Layered 3D Drop Shadow for deep table elevation
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.40f),
        topLeft = Offset(topLeft.x + 5f * scale, topLeft.y + 7f * scale),
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(topLeft.x + 2f * scale, topLeft.y + 3f * scale),
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(7f * scale, 7f * scale)
    )

    // 2. 3D Beveled Side Wall Rim (Warm Antique Bone Ivory with ambient shadow)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFD6C8AE), Color(0xFFB5A486), Color(0xFF8A795E)),
            startY = topLeft.y,
            endY = topLeft.y + tileH + 3.5f * scale
        ),
        topLeft = Offset(topLeft.x, topLeft.y + 2.8f * scale),
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(7f * scale, 7f * scale)
    )

    // 3. Luxurious Ivory Bone Face Surface with realistic pillow gradient and specular sheen
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFE),
                Color(0xFFFCF9F1),
                Color(0xFFF4ECD8),
                Color(0xFFE5D7BD)
            ),
            center = Offset(topLeft.x + tileW * 0.25f, topLeft.y + tileH * 0.25f),
            radius = tileW * 1.5f
        ),
        topLeft = topLeft,
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(6.5f * scale, 6.5f * scale)
    )

    // 4. Subtle Cushion Bevel Highlight on Top/Left Edges
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.2f))),
        start = Offset(topLeft.x + 6f * scale, topLeft.y + 1f * scale),
        end = Offset(topLeft.x + tileW - 6f * scale, topLeft.y + 1f * scale),
        strokeWidth = 1.6f * scale
    )
    drawLine(
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.2f))),
        start = Offset(topLeft.x + 1f * scale, topLeft.y + 6f * scale),
        end = Offset(topLeft.x + 1f * scale, topLeft.y + tileH - 6f * scale),
        strokeWidth = 1.6f * scale
    )

    // 5. Crisp Perimeter Border (Fine Antique Edge)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFC7BBA2), Color(0xFFAFA085), Color(0xFF8C7D64))
        ),
        topLeft = topLeft,
        size = Size(tileW, tileH),
        cornerRadius = CornerRadius(6.5f * scale, 6.5f * scale),
        style = Stroke(width = 1.2f * scale)
    )

    // 6. Deep Center Carved Dividing Groove & Polished Brass Spinner Rivet Pin
    val pinRadius = 3.6f * scale
    if (isVertical) {
        val midY = topLeft.y + tileH / 2f
        // Inset carved trench shadow
        drawLine(
            color = Color(0xFF5E4F39),
            start = Offset(topLeft.x + 4f * scale, midY - 0.7f * scale),
            end = Offset(topLeft.x + tileW - 4f * scale, midY - 0.7f * scale),
            strokeWidth = 1.8f * scale
        )
        // Trench highlight rim
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(topLeft.x + 4f * scale, midY + 1.0f * scale),
            end = Offset(topLeft.x + tileW - 4f * scale, midY + 1.0f * scale),
            strokeWidth = 1.2f * scale
        )

        // Brass Spinner Outer Recessed Ring Socket
        drawCircle(
            color = Color(0xFF423726),
            radius = pinRadius + 1.2f * scale,
            center = Offset(screenPos.x, midY)
        )
        // Polished Brass Dome Spinner with 3D Spherical Light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9C4), // Bright specular highlight
                    Color(0xFFFFD54F), // Pure polished gold brass
                    Color(0xFFD4AF37), // Classic metallic brass
                    Color(0xFF8B6B1B)  // Dark shadow rim
                ),
                center = Offset(screenPos.x - 1.2f * scale, midY - 1.2f * scale),
                radius = pinRadius * 1.4f
            ),
            radius = pinRadius,
            center = Offset(screenPos.x, midY)
        )
        // Micro specular reflection on brass
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = pinRadius * 0.28f,
            center = Offset(screenPos.x - pinRadius * 0.35f, midY - pinRadius * 0.35f)
        )
    } else {
        val midX = topLeft.x + tileW / 2f
        // Inset carved trench shadow
        drawLine(
            color = Color(0xFF5E4F39),
            start = Offset(midX - 0.7f * scale, topLeft.y + 4f * scale),
            end = Offset(midX - 0.7f * scale, topLeft.y + tileH - 4f * scale),
            strokeWidth = 1.8f * scale
        )
        // Trench highlight rim
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(midX + 1.0f * scale, topLeft.y + 4f * scale),
            end = Offset(midX + 1.0f * scale, topLeft.y + tileH - 4f * scale),
            strokeWidth = 1.2f * scale
        )

        // Brass Spinner Outer Recessed Ring Socket
        drawCircle(
            color = Color(0xFF423726),
            radius = pinRadius + 1.2f * scale,
            center = Offset(midX, screenPos.y)
        )
        // Polished Brass Dome Spinner with 3D Spherical Light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9C4),
                    Color(0xFFFFD54F),
                    Color(0xFFD4AF37),
                    Color(0xFF8B6B1B)
                ),
                center = Offset(midX - 1.2f * scale, screenPos.y - 1.2f * scale),
                radius = pinRadius * 1.4f
            ),
            radius = pinRadius,
            center = Offset(midX, screenPos.y)
        )
        // Micro specular reflection on brass
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = pinRadius * 0.28f,
            center = Offset(midX - pinRadius * 0.35f, screenPos.y - pinRadius * 0.35f)
        )
    }

    // 7. Draw Deeply Engraved 3D Dots (Pips) on Both Halves
    val dotRadius = 4.0f * scale
    val dotColor = Color(0xFF141210) // Rich Obsidian Enamel

    if (isVertical) {
        val halfH = tileH / 2f
        // Top half
        drawPipsPattern(
            count = leftOrTopVal,
            center = Offset(screenPos.x, topLeft.y + halfH / 2f),
            halfSize = halfH * 0.36f,
            radius = dotRadius,
            color = dotColor
        )
        // Bottom half
        drawPipsPattern(
            count = rightOrBottomVal,
            center = Offset(screenPos.x, topLeft.y + halfH + halfH / 2f),
            halfSize = halfH * 0.36f,
            radius = dotRadius,
            color = dotColor
        )
    } else {
        val halfW = tileW / 2f
        // Left half
        drawPipsPattern(
            count = leftOrTopVal,
            center = Offset(topLeft.x + halfW / 2f, screenPos.y),
            halfSize = halfW * 0.36f,
            radius = dotRadius,
            color = dotColor
        )
        // Right half
        drawPipsPattern(
            count = rightOrBottomVal,
            center = Offset(topLeft.x + halfW + halfW / 2f, screenPos.y),
            halfSize = halfW * 0.36f,
            radius = dotRadius,
            color = dotColor
        )
    }

    // 8. Small Corner Numeral Badge for instant clarity
    if (scale >= 1.0f) {
        drawCornerNumerals(
            topLeft = topLeft,
            tileW = tileW,
            tileH = tileH,
            isVertical = isVertical,
            val1 = leftOrTopVal,
            val2 = rightOrBottomVal,
            scale = scale
        )
    }
}

private fun DrawScope.drawCornerNumerals(
    topLeft: Offset,
    tileW: Float,
    tileH: Float,
    isVertical: Boolean,
    val1: Int,
    val2: Int,
    scale: Float
) {
    val textSize = 11f * scale.coerceIn(0.9f, 1.4f)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 100, 80, 50)
            this.textSize = textSize
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.LEFT
        }

        if (isVertical) {
            drawText(val1.toString(), topLeft.x + 5f * scale, topLeft.y + textSize + 2f * scale, paint)
            drawText(val2.toString(), topLeft.x + 5f * scale, topLeft.y + tileH / 2f + textSize + 2f * scale, paint)
        } else {
            drawText(val1.toString(), topLeft.x + 5f * scale, topLeft.y + textSize + 2f * scale, paint)
            drawText(val2.toString(), topLeft.x + tileW / 2f + 5f * scale, topLeft.y + textSize + 2f * scale, paint)
        }
    }
}

fun DrawScope.drawPipsPattern(
    count: Int,
    center: Offset,
    halfSize: Float,
    radius: Float,
    color: Color
) {
    val cx = center.x
    val cy = center.y
    val s = halfSize

    fun drawIndentedDot(dotX: Float, dotY: Float) {
        // 1. Inset Carved Trench Drop Shadow (Top-Left depression into the bone)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(dotX - radius * 0.4f, dotY - radius * 0.4f),
                radius = radius * 1.3f
            ),
            radius = radius + 0.8f,
            center = Offset(dotX, dotY)
        )
        // 2. Deep Obsidian Enamel Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2C241E), color, Color(0xFF0A0908)),
                center = Offset(dotX - radius * 0.2f, dotY - radius * 0.2f),
                radius = radius
            ),
            radius = radius,
            center = Offset(dotX, dotY)
        )
        // 3. Ambient bounce reflection highlight on bottom-right carved rim
        drawCircle(
            color = Color.White.copy(alpha = 0.40f),
            radius = radius * 0.65f,
            center = Offset(dotX + radius * 0.25f, dotY + radius * 0.28f)
        )
        // 4. Specular gloss highlight pin-dot
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = radius * 0.25f,
            center = Offset(dotX - radius * 0.35f, dotY - radius * 0.35f)
        )
    }

    when (count) {
        1 -> {
            drawIndentedDot(cx, cy)
        }
        2 -> {
            drawIndentedDot(cx - s, cy - s)
            drawIndentedDot(cx + s, cy + s)
        }
        3 -> {
            drawIndentedDot(cx - s, cy - s)
            drawIndentedDot(cx, cy)
            drawIndentedDot(cx + s, cy + s)
        }
        4 -> {
            drawIndentedDot(cx - s, cy - s)
            drawIndentedDot(cx + s, cy - s)
            drawIndentedDot(cx - s, cy + s)
            drawIndentedDot(cx + s, cy + s)
        }
        5 -> {
            drawIndentedDot(cx - s, cy - s)
            drawIndentedDot(cx + s, cy - s)
            drawIndentedDot(cx, cy)
            drawIndentedDot(cx - s, cy + s)
            drawIndentedDot(cx + s, cy + s)
        }
        6 -> {
            drawIndentedDot(cx - s, cy - s)
            drawIndentedDot(cx - s, cy)
            drawIndentedDot(cx - s, cy + s)
            drawIndentedDot(cx + s, cy - s)
            drawIndentedDot(cx + s, cy)
            drawIndentedDot(cx + s, cy + s)
        }
    }
}
