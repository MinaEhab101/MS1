package com.example.ui.screens.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.game.BoardState
import com.example.game.GraphicsQuality
import com.example.game.GraphicsSettings
import com.example.game.Move
import com.example.game.Player
import com.example.ui.theme.BoardFelt
import com.example.ui.theme.BoardWoodDark
import com.example.ui.theme.BoardWoodMedium
import com.example.ui.theme.CheckerBlack
import com.example.ui.theme.CheckerBlackBorder
import com.example.ui.theme.CheckerWhite
import com.example.ui.theme.CheckerWhiteBorder
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.PointDark
import com.example.ui.theme.PointHighlight
import com.example.ui.theme.PointLight
import kotlin.math.abs
import kotlin.math.min

@Composable
fun BoardCanvas(
    state: BoardState,
    onPointClicked: (Int) -> Unit,
    onBarClicked: (Player) -> Unit,
    onBearOffClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quality by GraphicsSettings.currentQuality.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val crownShine by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "crownShine"
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    remember { com.example.game.AppearanceManager.initialize(context) }
    val equippedBoard by com.example.game.AppearanceManager.equippedBoard.collectAsState()
    val equippedSkin by com.example.game.AppearanceManager.equippedSkin.collectAsState()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("backgammon_board_canvas")
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()

                        val point = detectClickedPoint(offset.x, offset.y, width, height)
                        when {
                            point in 1..24 -> onPointClicked(point)
                            point == 0 -> onBarClicked(state.turn)
                            point == -1 -> onBearOffClicked()
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw 3D Wooden Cabinet Frame & Felt Surface
            draw3DBoardFrameAndFelt(w, h, quality, equippedBoard)

            // Layout dimensions
            val trayWidth = w * 0.082f
            val playAreaWidth = w - trayWidth
            val barWidth = playAreaWidth * 0.082f
            val quadWidth = (playAreaWidth - barWidth) / 2f
            val pointWidth = quadWidth / 6f
            val triangleHeight = h * 0.385f

            val legalTargets = state.legalMovesForSelected.map { it.toPoint }.toSet()

            // 2. Draw 3D Inlaid Triangles & Highlights
            draw3DPoints(
                w = playAreaWidth,
                h = h,
                pointWidth = pointWidth,
                barWidth = barWidth,
                triangleHeight = triangleHeight,
                selectedPoint = state.selectedPoint,
                legalTargets = legalTargets,
                pulseAlpha = pulseAlpha,
                quality = quality
            )

            // 3. Draw 3D Center Wooden Divider & Brass Hinges
            draw3DBarStructure(quadWidth, barWidth, h, quality)

            // 4. Draw 3D Checkers on Points
            drawAll3DCheckers(
                state = state,
                playAreaWidth = playAreaWidth,
                h = h,
                pointWidth = pointWidth,
                barWidth = barWidth,
                selectedPoint = state.selectedPoint,
                pulseAlpha = pulseAlpha,
                quality = quality,
                crownShine = crownShine,
                skin = equippedSkin
            )

            // 5. Draw 3D Center Bar Checkers
            draw3DBarCheckers(
                state = state,
                playAreaWidth = playAreaWidth,
                h = h,
                barWidth = barWidth,
                selectedPoint = state.selectedPoint,
                pulseAlpha = pulseAlpha,
                quality = quality,
                skin = equippedSkin
            )

            // 6. Draw 3D Bear-Off Trays
            draw3DBearOffTray(
                state = state,
                w = w,
                h = h,
                trayWidth = trayWidth,
                canBearOffSelected = state.legalMovesForSelected.any { it.toPoint == Move.OFF_POINT },
                pulseAlpha = pulseAlpha,
                quality = quality
            )

            // 7. Ambient Spotlight Lighting Overlay
            if (quality.enableReflections) {
                drawOverheadSpotlight(w, h)
            }
        }
    }
}

private fun DrawScope.draw3DBoardFrameAndFelt(
    w: Float,
    h: Float,
    quality: GraphicsQuality,
    material: com.example.game.BoardMaterial = com.example.game.AppearanceManager.WALNUT_WOOD
) {
    // Outer shadow behind board
    if (quality != GraphicsQuality.LOW) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.6f * quality.shadowAlpha),
            topLeft = Offset(4f, 6f),
            size = Size(w, h),
            cornerRadius = CornerRadius(16f, 16f)
        )
    }

    // Outer rich cabinet frame based on equipped material
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = material.frameGradient,
            center = Offset(w * 0.45f, h * 0.35f),
            radius = w * 0.85f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 3D Bevel highlight on top & left rail
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF8D532B), Color(0xFF5A3018))
        ),
        start = Offset(4f, 2f),
        end = Offset(w - 4f, 2f),
        strokeWidth = 3f
    )
    drawLine(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF8D532B), Color(0xFF381B0D))
        ),
        start = Offset(2f, 4f),
        end = Offset(2f, h - 4f),
        strokeWidth = 3f
    )

    // 3D Bevel shadow on bottom & right rail
    drawLine(
        color = Color(0xFF0D0603),
        start = Offset(4f, h - 2f),
        end = Offset(w - 4f, h - 2f),
        strokeWidth = 3.5f
    )
    drawLine(
        color = Color(0xFF0D0603),
        start = Offset(w - 2f, 4f),
        end = Offset(w - 2f, h - 4f),
        strokeWidth = 3.5f
    )

    // Inner playing felt margin
    val margin = 9f
    val feltW = w - margin * 2
    val feltH = h - margin * 2

    // Recessed Ambient Occlusion Shadow inside the frame onto felt
    if (quality != GraphicsQuality.LOW) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.55f * quality.shadowAlpha),
            topLeft = Offset(margin - 2f, margin - 2f),
            size = Size(feltW + 4f, feltH + 4f),
            cornerRadius = CornerRadius(10f, 10f)
        )
    }

    // Material Playing Field Surface (Walnut, Marble, Obsidian, or Emerald)
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = material.surfaceColors,
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.75f
        ),
        topLeft = Offset(margin, margin),
        size = Size(feltW, feltH),
        cornerRadius = CornerRadius(9f, 9f)
    )

    // Twin Engraved Golden Field Crowns (Left and Right board centers as in photo)
    val halfFeltW = feltW / 2f
    drawGoldenFieldCrown(Offset(margin + halfFeltW * 0.5f, h * 0.5f), size = 28f)
    drawGoldenFieldCrown(Offset(margin + halfFeltW * 1.5f, h * 0.5f), size = 28f)

    // Inlaid Decorative Border
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(material.accentBorder, GoldPrimary, GoldDark, material.accentBorder)
        ),
        topLeft = Offset(margin + 2.5f, margin + 2.5f),
        size = Size(feltW - 5f, feltH - 5f),
        cornerRadius = CornerRadius(7f, 7f),
        style = Stroke(width = 2f)
    )

    // 4 Antique Brass Corner Brackets
    drawBrassCornerBrackets(margin, feltW, feltH)
}

private fun DrawScope.drawGoldenFieldCrown(center: Offset, size: Float) {
    val crownColor = GoldPrimary.copy(alpha = 0.52f)
    val cx = center.x
    val cy = center.y
    val halfW = size * 1.15f
    val h = size * 0.85f

    val crownPath = Path().apply {
        moveTo(cx - halfW, cy + h * 0.5f)
        lineTo(cx - halfW, cy - h * 0.2f)
        lineTo(cx - halfW * 0.5f, cy + h * 0.1f)
        lineTo(cx, cy - h * 0.65f)
        lineTo(cx + halfW * 0.5f, cy + h * 0.1f)
        lineTo(cx + halfW, cy - h * 0.2f)
        lineTo(cx + halfW, cy + h * 0.5f)
        close()
    }

    drawPath(path = crownPath, color = crownColor, style = Fill)
    drawPath(path = crownPath, color = GoldSecondary.copy(alpha = 0.85f), style = Stroke(width = 1.5f))

    // Crown jewels
    drawCircle(GoldSecondary, 2.2f, Offset(cx - halfW, cy - h * 0.25f))
    drawCircle(GoldSecondary, 2.6f, Offset(cx, cy - h * 0.7f))
    drawCircle(GoldSecondary, 2.2f, Offset(cx + halfW, cy - h * 0.25f))
}

private fun DrawScope.drawBrassCornerBrackets(margin: Float, feltW: Float, feltH: Float) {
    val bSize = 18f
    val corners = listOf(
        Offset(margin, margin),
        Offset(margin + feltW - bSize, margin),
        Offset(margin, margin + feltH - bSize),
        Offset(margin + feltW - bSize, margin + feltH - bSize)
    )

    corners.forEach { c ->
        // Brass bracket plate
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFB8860B), Color(0xFF795548)),
                start = c,
                end = Offset(c.x + bSize, c.y + bSize)
            ),
            topLeft = c,
            size = Size(bSize, bSize),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Rivet screw in center
        drawCircle(
            color = Color(0xFF3E2723),
            radius = 2.2f,
            center = Offset(c.x + bSize / 2f, c.y + bSize / 2f)
        )
        drawCircle(
            color = Color(0xFFFFF9C4),
            radius = 1f,
            center = Offset(c.x + bSize / 2f - 0.7f, c.y + bSize / 2f - 0.7f)
        )
    }
}

private fun DrawScope.draw3DBarStructure(
    quadWidth: Float,
    barWidth: Float,
    h: Float,
    quality: GraphicsQuality
) {
    val barX = quadWidth + 9f
    val barW = barWidth

    // Wooden Bar Ridge
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF28140A), Color(0xFF4A2511), Color(0xFF5E3016), Color(0xFF28140A)),
            startX = barX,
            endX = barX + barW
        ),
        topLeft = Offset(barX, 9f),
        size = Size(barW, h - 18f)
    )

    // Vertical gold inlays along bar sides
    drawLine(
        color = GoldDark,
        start = Offset(barX, 9f),
        end = Offset(barX, h - 9f),
        strokeWidth = 1.5f
    )
    drawLine(
        color = GoldDark,
        start = Offset(barX + barW, 9f),
        end = Offset(barX + barW, h - 9f),
        strokeWidth = 1.5f
    )

    // Center Gold Clasp & Hinges
    val centerY = h / 2f
    val hingeH = 26f
    val hingeW = barW + 4f
    val hingeX = barX - 2f

    // 3D Brass Center Hinge
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(GoldSecondary, GoldPrimary, Color(0xFF6B4810), GoldPrimary),
            start = Offset(hingeX, centerY - hingeH / 2),
            end = Offset(hingeX + hingeW, centerY + hingeH / 2)
        ),
        topLeft = Offset(hingeX, centerY - hingeH / 2),
        size = Size(hingeW, hingeH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Hinge Screws
    drawCircle(Color(0xFF3E2723), 2f, Offset(hingeX + 4f, centerY))
    drawCircle(Color(0xFF3E2723), 2f, Offset(hingeX + hingeW - 4f, centerY))
}

private fun DrawScope.draw3DPoints(
    w: Float,
    h: Float,
    pointWidth: Float,
    barWidth: Float,
    triangleHeight: Float,
    selectedPoint: Int?,
    legalTargets: Set<Int>,
    pulseAlpha: Float,
    quality: GraphicsQuality
) {
    val quadWidth = pointWidth * 6f

    for (p in 1..24) {
        val (startX, isTop) = getPointCoordinates(p, pointWidth, barWidth, quadWidth)
        val endX = startX + pointWidth
        val apexY = if (isTop) triangleHeight else (h - triangleHeight)
        val baseY = if (isTop) 9f else (h - 9f)

        val isPointDark = (p % 2 == 0)
        // Luxury Wood Inlays: Red Mahogany Point vs Champagne Maple Point
        val colors = if (isPointDark) {
            listOf(Color(0xFF6D1B1B), Color(0xFF8B2525), Color(0xFF4A1010))
        } else {
            listOf(Color(0xFFD7C4A5), Color(0xFFF3E7D3), Color(0xFFB5A17F))
        }

        val trianglePath = Path().apply {
            moveTo(startX + 1f, baseY)
            lineTo(endX - 1f, baseY)
            lineTo(startX + pointWidth / 2f, apexY)
            close()
        }

        // Draw Inlaid Point Body with Depth Gradient
        drawPath(
            path = trianglePath,
            brush = Brush.verticalGradient(
                colors = if (isTop) colors else colors.reversed(),
                startY = if (isTop) baseY else apexY,
                endY = if (isTop) apexY else baseY
            ),
            style = Fill
        )

        // Subtle Gold Hairline Inlay Border
        drawPath(
            path = trianglePath,
            color = if (isPointDark) Color(0x33FFD700) else Color(0x228B5A2B),
            style = Stroke(width = 1f)
        )

        // Highlight if this point is a legal move target
        if (legalTargets.contains(p)) {
            // Radiant Glowing Beacon Outline
            drawPath(
                path = trianglePath,
                color = PointHighlight.copy(alpha = pulseAlpha),
                style = Stroke(width = 3.5f)
            )

            // Pulsing target beacon dot at apex
            drawCircle(
                color = PointHighlight.copy(alpha = pulseAlpha),
                radius = 6.5f,
                center = Offset(startX + pointWidth / 2f, apexY)
            )
            drawCircle(
                color = Color.White.copy(alpha = pulseAlpha),
                radius = 3f,
                center = Offset(startX + pointWidth / 2f, apexY)
            )
        }

        // Selected source point golden halo
        if (selectedPoint == p) {
            drawPath(
                path = trianglePath,
                color = GoldPrimary.copy(alpha = 0.5f * pulseAlpha),
                style = Fill
            )
        }
    }
}

private fun DrawScope.drawAll3DCheckers(
    state: BoardState,
    playAreaWidth: Float,
    h: Float,
    pointWidth: Float,
    barWidth: Float,
    selectedPoint: Int?,
    pulseAlpha: Float,
    quality: GraphicsQuality,
    crownShine: Float,
    skin: com.example.game.PieceSkin = com.example.game.AppearanceManager.ROYAL_PORCELAIN
) {
    val quadWidth = pointWidth * 6f
    val checkerDiameter = pointWidth * 0.98f
    val checkerRadius = checkerDiameter / 2f

    for (p in 1..24) {
        val count = state.points[p]
        if (count == 0) continue

        val isWhite = count > 0
        val numCheckers = abs(count)
        val (startX, isTop) = getPointCoordinates(p, pointWidth, barWidth, quadWidth)
        val centerX = startX + pointWidth / 2f

        val maxVisible = 5
        val spacing = if (numCheckers > maxVisible) {
            (checkerDiameter * 3.8f) / numCheckers
        } else {
            checkerDiameter * 0.98f
        }

        for (i in 0 until min(numCheckers, maxVisible)) {
            val centerY = if (isTop) {
                14f + checkerRadius + (i * spacing)
            } else {
                h - 14f - checkerRadius - (i * spacing)
            }

            val isTopChecker = (i == min(numCheckers, maxVisible) - 1)
            val isSelected = isTopChecker && (selectedPoint == p)

            draw3DChecker(
                center = Offset(centerX, centerY),
                radius = checkerRadius,
                isWhite = isWhite,
                isSelected = isSelected,
                pulseAlpha = pulseAlpha,
                quality = quality,
                crownShine = crownShine,
                skin = skin
            )

            // Draw stack count on top checker if > 5
            if (isTopChecker && numCheckers > 5) {
                drawTextNative(
                    text = numCheckers.toString(),
                    x = centerX,
                    y = centerY,
                    isWhiteChecker = isWhite
                )
            }
        }
    }
}

private fun DrawScope.draw3DBarCheckers(
    state: BoardState,
    playAreaWidth: Float,
    h: Float,
    barWidth: Float,
    selectedPoint: Int?,
    pulseAlpha: Float,
    quality: GraphicsQuality,
    skin: com.example.game.PieceSkin = com.example.game.AppearanceManager.ROYAL_PORCELAIN
) {
    val quadWidth = (playAreaWidth - barWidth) / 2f
    val barCenterX = quadWidth + barWidth / 2f + 9f
    val radius = min(barWidth * 0.44f, 25f)

    if (state.barWhite > 0) {
        val count = state.barWhite
        val isSelected = (selectedPoint == Move.BAR_POINT && state.turn == Player.WHITE)
        val centerY = h * 0.28f
        draw3DChecker(
            center = Offset(barCenterX, centerY),
            radius = radius,
            isWhite = true,
            isSelected = isSelected,
            pulseAlpha = pulseAlpha,
            quality = quality,
            crownShine = 0f,
            skin = skin
        )
        if (count > 1) {
            drawTextNative(count.toString(), barCenterX, centerY, true)
        }
    }

    if (state.barBlack > 0) {
        val count = state.barBlack
        val isSelected = (selectedPoint == Move.BAR_POINT && state.turn == Player.BLACK)
        val centerY = h * 0.72f
        draw3DChecker(
            center = Offset(barCenterX, centerY),
            radius = radius,
            isWhite = false,
            isSelected = isSelected,
            pulseAlpha = pulseAlpha,
            quality = quality,
            crownShine = 0f,
            skin = skin
        )
        if (count > 1) {
            drawTextNative(count.toString(), barCenterX, centerY, false)
        }
    }
}

private fun DrawScope.draw3DChecker(
    center: Offset,
    radius: Float,
    isWhite: Boolean,
    isSelected: Boolean,
    pulseAlpha: Float,
    quality: GraphicsQuality,
    crownShine: Float,
    skin: com.example.game.PieceSkin = com.example.game.AppearanceManager.ROYAL_PORCELAIN
) {
    // Elevation lift offset when selected
    val liftY = if (isSelected) -8f else 0f
    val effCenter = center.copy(y = center.y + liftY)
    val shadowOffset = if (isSelected) 11f else 4f

    // 1. Directional 3D Cast Drop Shadow onto Felt
    if (quality.enableDynamicShadows) {
        drawCircle(
            color = Color.Black.copy(alpha = (if (isSelected) 0.55f else 0.42f) * quality.shadowAlpha),
            radius = radius + (if (isSelected) 4.5f else 2f),
            center = center.copy(y = center.y + shadowOffset, x = center.x + 2f)
        )
    }

    // 2. 3D Cylindrical Thickness Rim (Darker bottom crescent for actual height)
    if (quality.checker3DDepthLayers >= 2) {
        val rimColor = if (isWhite) skin.whiteRimColor else skin.blackRimColor
        drawCircle(
            color = rimColor,
            radius = radius,
            center = effCenter.copy(y = effCenter.y + 2.5f)
        )
    }

    // 3. Top Face - Spherical Dome Lighting (from equipped skin)
    val baseColors = if (isWhite) skin.whiteBaseColors else skin.blackBaseColors

    drawCircle(
        brush = Brush.radialGradient(
            colors = baseColors,
            center = Offset(effCenter.x - radius * 0.35f, effCenter.y - radius * 0.35f),
            radius = radius * 1.35f
        ),
        radius = radius,
        center = effCenter
    )

    // 4. Outer Beveled Metallic Rim Border
    if (quality.checker3DDepthLayers >= 2) {
        val rimBorderColor = if (isWhite) skin.whiteRimColor else skin.blackRimColor
        drawCircle(
            color = rimBorderColor,
            radius = radius,
            center = effCenter,
            style = Stroke(width = 2.2f)
        )
    }

    // 5. Concentric Inner Royal Ridge (Dome Indentation as seen in photo)
    if (quality.checker3DDepthLayers >= 3) {
        drawCircle(
            color = if (isWhite) skin.whiteRimColor.copy(alpha = 0.55f) else skin.blackRimColor,
            radius = radius * 0.65f,
            center = effCenter,
            style = Stroke(width = 1.6f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isWhite) {
                    listOf(skin.whiteBaseColors.first(), skin.whiteBaseColors.last())
                } else {
                    listOf(skin.blackBaseColors.first(), skin.blackBaseColors.last())
                },
                center = Offset(effCenter.x - radius * 0.2f, effCenter.y - radius * 0.2f),
                radius = radius * 0.7f
            ),
            radius = radius * 0.60f,
            center = effCenter
        )
    }

    // 6. Ultra Tier Specular Highlight
    if (quality.checker3DDepthLayers >= 4) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(effCenter.x - radius * 0.3f, effCenter.y - radius * 0.3f),
                radius = radius * 0.4f
            ),
            radius = radius * 0.35f,
            center = Offset(effCenter.x - radius * 0.25f, effCenter.y - radius * 0.25f)
        )
    }

    // 6. Engraved Royal Crown Symbol in Center
    drawRoyalCrownSymbol(effCenter, radius * 0.30f, isWhite, skin)

    // 7. Selected Glowing Royal Aura Ring
    if (isSelected) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldSecondary, GoldPrimary, Color.Transparent),
                center = effCenter,
                radius = radius + 10f
            ),
            radius = radius + 6f,
            center = effCenter,
            style = Stroke(width = 3.5f)
        )
    }
}

private fun DrawScope.drawRoyalCrownSymbol(
    center: Offset,
    size: Float,
    isWhite: Boolean,
    skin: com.example.game.PieceSkin = com.example.game.AppearanceManager.ROYAL_PORCELAIN
) {
    val crownColor = if (isWhite) skin.crownColorWhite else skin.crownColorBlack
    val cx = center.x
    val cy = center.y
    val halfW = size * 1.1f
    val h = size * 0.85f

    // 3-Pointed Royal Crown Path
    val crownPath = Path().apply {
        moveTo(cx - halfW, cy + h * 0.5f)
        lineTo(cx - halfW, cy - h * 0.2f)
        lineTo(cx - halfW * 0.5f, cy + h * 0.1f)
        lineTo(cx, cy - h * 0.6f) // Center tallest peak
        lineTo(cx + halfW * 0.5f, cy + h * 0.1f)
        lineTo(cx + halfW, cy - h * 0.2f)
        lineTo(cx + halfW, cy + h * 0.5f)
        close()
    }

    drawPath(path = crownPath, color = crownColor, style = Fill)

    // Crown Peak Jewels (3 tiny dots on peaks)
    val dotRadius = 1.5f
    drawCircle(crownColor, dotRadius, Offset(cx - halfW, cy - h * 0.25f))
    drawCircle(crownColor, dotRadius * 1.2f, Offset(cx, cy - h * 0.65f))
    drawCircle(crownColor, dotRadius, Offset(cx + halfW, cy - h * 0.25f))
}

private fun DrawScope.draw3DBearOffTray(
    state: BoardState,
    w: Float,
    h: Float,
    trayWidth: Float,
    canBearOffSelected: Boolean,
    pulseAlpha: Float,
    quality: GraphicsQuality
) {
    val trayX = w - trayWidth

    // Tray Wood Background
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF281308), Color(0xFF45220E), Color(0xFF200E05))
        ),
        topLeft = Offset(trayX, 0f),
        size = Size(trayWidth, h)
    )

    // Inner Velvet Tray Well
    drawRoundRect(
        color = Color(0xFF0F0B08),
        topLeft = Offset(trayX + 4f, 12f),
        size = Size(trayWidth - 8f, h - 24f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Left Border separator
    drawLine(
        color = GoldDark,
        start = Offset(trayX, 0f),
        end = Offset(trayX, h),
        strokeWidth = 2.5f
    )

    // White tray (bottom half)
    val whiteTrayY = h * 0.53f
    val trayH = h * 0.44f

    if (canBearOffSelected && state.turn == Player.WHITE) {
        drawRoundRect(
            color = PointHighlight.copy(alpha = pulseAlpha * 0.65f),
            topLeft = Offset(trayX + 3f, whiteTrayY),
            size = Size(trayWidth - 6f, trayH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = GoldPrimary,
            topLeft = Offset(trayX + 3f, whiteTrayY),
            size = Size(trayWidth - 6f, trayH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(2f)
        )
    }

    // Black tray (top half)
    val blackTrayY = 12f
    if (canBearOffSelected && state.turn == Player.BLACK) {
        drawRoundRect(
            color = PointHighlight.copy(alpha = pulseAlpha * 0.65f),
            topLeft = Offset(trayX + 3f, blackTrayY),
            size = Size(trayWidth - 6f, trayH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = GoldPrimary,
            topLeft = Offset(trayX + 3f, blackTrayY),
            size = Size(trayWidth - 6f, trayH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(2f)
        )
    }

    // Borne-Off Pieces Count Texts
    drawTextNative(
        text = "W: ${state.offWhite}/15",
        x = trayX + trayWidth / 2f,
        y = whiteTrayY + trayH * 0.5f,
        isWhiteChecker = true
    )
    drawTextNative(
        text = "B: ${state.offBlack}/15",
        x = trayX + trayWidth / 2f,
        y = blackTrayY + trayH * 0.5f,
        isWhiteChecker = false
    )
}

private fun DrawScope.drawOverheadSpotlight(w: Float, h: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.07f),
                Color.Transparent
            ),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = w * 0.7f
        ),
        size = Size(w, h)
    )
}

private fun DrawScope.drawTextNative(
    text: String,
    x: Float,
    y: Float,
    isWhiteChecker: Boolean
) {
    val paint = android.graphics.Paint().apply {
        color = if (isWhiteChecker) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y + 10f,
        paint
    )
}

private fun getPointCoordinates(
    point: Int,
    pointWidth: Float,
    barWidth: Float,
    quadWidth: Float
): Pair<Float, Boolean> {
    val isTop = point in 13..24
    val startX = when (point) {
        in 13..18 -> (point - 13) * pointWidth
        in 19..24 -> quadWidth + barWidth + (point - 19) * pointWidth
        in 7..12 -> (12 - point) * pointWidth
        in 1..6 -> quadWidth + barWidth + (6 - point) * pointWidth
        else -> 0f
    }
    return Pair(startX, isTop)
}

private fun detectClickedPoint(x: Float, y: Float, w: Float, h: Float): Int {
    val trayWidth = w * 0.082f
    if (x >= w - trayWidth) {
        return -1 // Bear-off tray
    }

    val playAreaWidth = w - trayWidth
    val barWidth = playAreaWidth * 0.082f
    val quadWidth = (playAreaWidth - barWidth) / 2f
    val pointWidth = quadWidth / 6f

    // Check Bar
    if (x >= quadWidth && x <= quadWidth + barWidth) {
        return 0 // Bar
    }

    val isTop = y < h / 2f

    return if (isTop) {
        if (x < quadWidth) {
            val idx = (x / pointWidth).toInt().coerceIn(0, 5)
            13 + idx
        } else {
            val adjustedX = x - (quadWidth + barWidth)
            val idx = (adjustedX / pointWidth).toInt().coerceIn(0, 5)
            19 + idx
        }
    } else {
        if (x < quadWidth) {
            val idx = (x / pointWidth).toInt().coerceIn(0, 5)
            12 - idx
        } else {
            val adjustedX = x - (quadWidth + barWidth)
            val idx = (adjustedX / pointWidth).toInt().coerceIn(0, 5)
            6 - idx
        }
    }
}
