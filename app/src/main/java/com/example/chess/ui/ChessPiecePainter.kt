package com.example.chess.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object ChessPiecePainter {

    // White piece luxury palette
    private val WhiteLight = Color(0xFFFFFDF5)
    private val WhiteMid = Color(0xFFEADBBE)
    private val WhiteDark = Color(0xFFC0A678)
    private val WhiteShadow = Color(0xFF755E38)
    private val WhiteGoldRim = Color(0xFFFFD54F)

    // Black piece luxury palette
    private val BlackLight = Color(0xFF4A444D)
    private val BlackMid = Color(0xFF26202A)
    private val BlackDark = Color(0xFF131016)
    private val BlackShadow = Color(0xFF070509)
    private val BlackSilverRim = Color(0xFF90A4AE)

    fun drawPiece(
        drawScope: DrawScope,
        piece: ChessPiece,
        center: Offset,
        size: Float,
        elevation: Float = 0f
    ) {
        val isWhite = piece.color == PieceColor.WHITE
        val primaryBrush = if (isWhite) {
            Brush.linearGradient(
                colors = listOf(WhiteLight, WhiteMid, WhiteDark),
                start = Offset(center.x - size * 0.35f, center.y - size * 0.45f - elevation),
                end = Offset(center.x + size * 0.35f, center.y + size * 0.45f - elevation)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(BlackLight, BlackMid, BlackDark),
                start = Offset(center.x - size * 0.35f, center.y - size * 0.45f - elevation),
                end = Offset(center.x + size * 0.35f, center.y + size * 0.45f - elevation)
            )
        }

        val rimColor = if (isWhite) WhiteGoldRim.copy(alpha = 0.85f) else BlackSilverRim.copy(alpha = 0.7f)
        val shadowY = center.y + size * 0.36f + (elevation * 0.3f)
        val shadowAlpha = (0.45f - (elevation / size) * 0.2f).coerceIn(0.1f, 0.45f)

        // 1. Draw 3D Base Drop Shadow
        drawScope.drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(center.x - size * 0.32f, shadowY - size * 0.1f),
            size = Size(size * 0.64f, size * 0.2f)
        )

        val drawCenter = Offset(center.x, center.y - elevation)

        // 2. Draw 3D Piece Geometry
        when (piece.type) {
            PieceType.PAWN -> drawPawn(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
            PieceType.KNIGHT -> drawKnight(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
            PieceType.BISHOP -> drawBishop(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
            PieceType.ROOK -> drawRook(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
            PieceType.QUEEN -> drawQueen(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
            PieceType.KING -> drawKing(drawScope, drawCenter, size, primaryBrush, rimColor, isWhite)
        }
    }

    private fun drawBasePedestal(
        scope: DrawScope,
        center: Offset,
        size: Float,
        brush: Brush,
        rimColor: Color
    ) {
        val baseY = center.y + size * 0.26f
        val baseWidth = size * 0.58f
        val baseHeight = size * 0.16f

        // Bottom plinth ring
        scope.drawOval(
            brush = brush,
            topLeft = Offset(center.x - baseWidth * 0.5f, baseY),
            size = Size(baseWidth, baseHeight)
        )
        scope.drawOval(
            color = rimColor,
            topLeft = Offset(center.x - baseWidth * 0.5f, baseY),
            size = Size(baseWidth, baseHeight),
            style = Stroke(width = size * 0.03f)
        )

        // Stepped middle plinth
        val midWidth = baseWidth * 0.78f
        val midHeight = baseHeight * 0.85f
        scope.drawOval(
            brush = brush,
            topLeft = Offset(center.x - midWidth * 0.5f, baseY - size * 0.05f),
            size = Size(midWidth, midHeight)
        )
    }

    private fun drawPawn(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Stem
        val stemPath = Path().apply {
            moveTo(c.x - s * 0.18f, c.y + s * 0.24f)
            cubicTo(
                c.x - s * 0.14f, c.y + s * 0.05f,
                c.x - s * 0.08f, c.y - s * 0.05f,
                c.x - s * 0.12f, c.y - s * 0.12f
            )
            lineTo(c.x + s * 0.12f, c.y - s * 0.12f)
            cubicTo(
                c.x + s * 0.08f, c.y - s * 0.05f,
                c.x + s * 0.14f, c.y + s * 0.05f,
                c.x + s * 0.18f, c.y + s * 0.24f
            )
            close()
        }
        scope.drawPath(stemPath, brush)
        scope.drawPath(stemPath, rimColor, style = Stroke(width = s * 0.025f))

        // Collar ring
        scope.drawOval(
            brush = brush,
            topLeft = Offset(c.x - s * 0.15f, c.y - s * 0.14f),
            size = Size(s * 0.3f, s * 0.08f)
        )
        scope.drawOval(
            color = rimColor,
            topLeft = Offset(c.x - s * 0.15f, c.y - s * 0.14f),
            size = Size(s * 0.3f, s * 0.08f),
            style = Stroke(width = s * 0.025f)
        )

        // Head orb
        val headRadius = s * 0.15f
        val headCenter = Offset(c.x, c.y - s * 0.23f)
        scope.drawCircle(brush = brush, radius = headRadius, center = headCenter)
        scope.drawCircle(color = rimColor, radius = headRadius, center = headCenter, style = Stroke(width = s * 0.025f))

        // Specular glint
        val glintCenter = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f)
        scope.drawCircle(
            color = if (isWhite) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
            radius = headRadius * 0.32f,
            center = glintCenter
        )
    }

    private fun drawRook(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Tower body
        val towerPath = Path().apply {
            moveTo(c.x - s * 0.20f, c.y + s * 0.24f)
            lineTo(c.x - s * 0.15f, c.y - s * 0.14f)
            lineTo(c.x + s * 0.15f, c.y - s * 0.14f)
            lineTo(c.x + s * 0.20f, c.y + s * 0.24f)
            close()
        }
        scope.drawPath(towerPath, brush)
        scope.drawPath(towerPath, rimColor, style = Stroke(width = s * 0.025f))

        // Capital corniche
        scope.drawOval(
            brush = brush,
            topLeft = Offset(c.x - s * 0.22f, c.y - s * 0.18f),
            size = Size(s * 0.44f, s * 0.09f)
        )
        scope.drawOval(
            color = rimColor,
            topLeft = Offset(c.x - s * 0.22f, c.y - s * 0.18f),
            size = Size(s * 0.44f, s * 0.09f),
            style = Stroke(width = s * 0.025f)
        )

        // Battlements (Crenels)
        val crenelY = c.y - s * 0.32f
        val battlementHeight = s * 0.14f
        val battlementPath = Path().apply {
            moveTo(c.x - s * 0.22f, c.y - s * 0.17f)
            lineTo(c.x - s * 0.22f, crenelY)
            lineTo(c.x - s * 0.12f, crenelY)
            lineTo(c.x - s * 0.12f, crenelY + battlementHeight * 0.5f)
            lineTo(c.x - s * 0.04f, crenelY + battlementHeight * 0.5f)
            lineTo(c.x - s * 0.04f, crenelY)
            lineTo(c.x + s * 0.04f, crenelY)
            lineTo(c.x + s * 0.04f, crenelY + battlementHeight * 0.5f)
            lineTo(c.x + s * 0.12f, crenelY + battlementHeight * 0.5f)
            lineTo(c.x + s * 0.12f, crenelY)
            lineTo(c.x + s * 0.22f, crenelY)
            lineTo(c.x + s * 0.22f, c.y - s * 0.17f)
            close()
        }
        scope.drawPath(battlementPath, brush)
        scope.drawPath(battlementPath, rimColor, style = Stroke(width = s * 0.025f, join = StrokeJoin.Round))

        // Center embrasure highlight
        scope.drawCircle(
            color = if (isWhite) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f),
            radius = s * 0.04f,
            center = Offset(c.x, c.y - s * 0.02f)
        )
    }

    private fun drawKnight(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Horse profile path
        val horsePath = Path().apply {
            moveTo(c.x - s * 0.20f, c.y + s * 0.24f)
            // Chest curve
            cubicTo(
                c.x - s * 0.26f, c.y + s * 0.10f,
                c.x - s * 0.26f, c.y - s * 0.05f,
                c.x - s * 0.18f, c.y - s * 0.16f
            )
            // Muzzle
            lineTo(c.x - s * 0.24f, c.y - s * 0.15f)
            cubicTo(
                c.x - s * 0.26f, c.y - s * 0.24f,
                c.x - s * 0.18f, c.y - s * 0.28f,
                c.x - s * 0.10f, c.y - s * 0.26f
            )
            // Forehead to ear
            lineTo(c.x - s * 0.04f, c.y - s * 0.35f)
            lineTo(c.x + s * 0.02f, c.y - s * 0.27f)
            // Mane & arch back
            cubicTo(
                c.x + s * 0.12f, c.y - s * 0.22f,
                c.x + s * 0.22f, c.y - s * 0.08f,
                c.x + s * 0.18f, c.y + s * 0.24f
            )
            close()
        }
        scope.drawPath(horsePath, brush)
        scope.drawPath(horsePath, rimColor, style = Stroke(width = s * 0.025f, join = StrokeJoin.Round))

        // Eye
        scope.drawCircle(
            color = if (isWhite) Color(0xFF3E2723) else Color.White,
            radius = s * 0.028f,
            center = Offset(c.x - s * 0.10f, c.y - s * 0.19f)
        )
        // Mane details
        val manePath = Path().apply {
            moveTo(c.x + s * 0.02f, c.y - s * 0.18f)
            lineTo(c.x + s * 0.12f, c.y - s * 0.14f)
            moveTo(c.x + s * 0.05f, c.y - s * 0.08f)
            lineTo(c.x + s * 0.15f, c.y - s * 0.05f)
            moveTo(c.x + s * 0.07f, c.y + s * 0.04f)
            lineTo(c.x + s * 0.16f, c.y + s * 0.08f)
        }
        scope.drawPath(manePath, rimColor, style = Stroke(width = s * 0.025f, cap = StrokeCap.Round))
    }

    private fun drawBishop(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Bishop body
        val bodyPath = Path().apply {
            moveTo(c.x - s * 0.18f, c.y + s * 0.24f)
            cubicTo(
                c.x - s * 0.14f, c.y + s * 0.08f,
                c.x - s * 0.10f, c.y - s * 0.06f,
                c.x - s * 0.14f, c.y - s * 0.12f
            )
            lineTo(c.x + s * 0.14f, c.y - s * 0.12f)
            cubicTo(
                c.x + s * 0.10f, c.y - s * 0.06f,
                c.x + s * 0.14f, c.y + s * 0.08f,
                c.x + s * 0.18f, c.y + s * 0.24f
            )
            close()
        }
        scope.drawPath(bodyPath, brush)
        scope.drawPath(bodyPath, rimColor, style = Stroke(width = s * 0.025f))

        // Collar ring
        scope.drawOval(
            brush = brush,
            topLeft = Offset(c.x - s * 0.16f, c.y - s * 0.14f),
            size = Size(s * 0.32f, s * 0.07f)
        )

        // Mitre head (oval pointed at top)
        val mitrePath = Path().apply {
            moveTo(c.x, c.y - s * 0.35f) // point
            cubicTo(
                c.x - s * 0.20f, c.y - s * 0.28f,
                c.x - s * 0.20f, c.y - s * 0.14f,
                c.x, c.y - s * 0.12f
            )
            cubicTo(
                c.x + s * 0.20f, c.y - s * 0.14f,
                c.x + s * 0.20f, c.y - s * 0.28f,
                c.x, c.y - s * 0.35f
            )
            close()
        }
        scope.drawPath(mitrePath, brush)
        scope.drawPath(mitrePath, rimColor, style = Stroke(width = s * 0.025f))

        // Diagonal miter cleft (slit)
        scope.drawLine(
            color = if (isWhite) WhiteShadow else Color.Black,
            start = Offset(c.x - s * 0.03f, c.y - s * 0.29f),
            end = Offset(c.x + s * 0.08f, c.y - s * 0.18f),
            strokeWidth = s * 0.035f,
            cap = StrokeCap.Round
        )

        // Top finial sphere
        scope.drawCircle(brush = brush, radius = s * 0.038f, center = Offset(c.x, c.y - s * 0.37f))
        scope.drawCircle(color = rimColor, radius = s * 0.038f, center = Offset(c.x, c.y - s * 0.37f), style = Stroke(width = s * 0.02f))
    }

    private fun drawQueen(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Flared body
        val bodyPath = Path().apply {
            moveTo(c.x - s * 0.20f, c.y + s * 0.24f)
            cubicTo(
                c.x - s * 0.14f, c.y + s * 0.06f,
                c.x - s * 0.10f, c.y - s * 0.06f,
                c.x - s * 0.15f, c.y - s * 0.14f
            )
            lineTo(c.x + s * 0.15f, c.y - s * 0.14f)
            cubicTo(
                c.x + s * 0.10f, c.y - s * 0.06f,
                c.x + s * 0.14f, c.y + s * 0.06f,
                c.x + s * 0.20f, c.y + s * 0.24f
            )
            close()
        }
        scope.drawPath(bodyPath, brush)
        scope.drawPath(bodyPath, rimColor, style = Stroke(width = s * 0.025f))

        // Waist ring
        scope.drawOval(
            brush = brush,
            topLeft = Offset(c.x - s * 0.17f, c.y - s * 0.15f),
            size = Size(s * 0.34f, s * 0.07f)
        )

        // Coronet (5 peaks)
        val crownPath = Path().apply {
            moveTo(c.x - s * 0.16f, c.y - s * 0.14f)
            lineTo(c.x - s * 0.22f, c.y - s * 0.32f) // left peak
            lineTo(c.x - s * 0.11f, c.y - s * 0.22f)
            lineTo(c.x - s * 0.06f, c.y - s * 0.36f) // mid-left peak
            lineTo(c.x, c.y - s * 0.23f)
            lineTo(c.x + s * 0.06f, c.y - s * 0.36f) // mid-right peak
            lineTo(c.x + s * 0.11f, c.y - s * 0.22f)
            lineTo(c.x + s * 0.22f, c.y - s * 0.32f) // right peak
            lineTo(c.x + s * 0.16f, c.y - s * 0.14f)
            close()
        }
        scope.drawPath(crownPath, brush)
        scope.drawPath(crownPath, rimColor, style = Stroke(width = s * 0.025f, join = StrokeJoin.Round))

        // Pearls on peaks
        val peaks = listOf(
            Offset(c.x - s * 0.22f, c.y - s * 0.32f),
            Offset(c.x - s * 0.06f, c.y - s * 0.36f),
            Offset(c.x + s * 0.06f, c.y - s * 0.36f),
            Offset(c.x + s * 0.22f, c.y - s * 0.32f)
        )
        for (pt in peaks) {
            scope.drawCircle(brush = brush, radius = s * 0.032f, center = pt)
            scope.drawCircle(color = rimColor, radius = s * 0.032f, center = pt, style = Stroke(width = s * 0.015f))
        }

        // Center jewel sphere
        scope.drawCircle(color = if (isWhite) Color(0xFFD32F2F) else Color(0xFFFFD54F), radius = s * 0.035f, center = Offset(c.x, c.y - s * 0.27f))
    }

    private fun drawKing(
        scope: DrawScope,
        c: Offset,
        s: Float,
        brush: Brush,
        rimColor: Color,
        isWhite: Boolean
    ) {
        drawBasePedestal(scope, c, s, brush, rimColor)

        // Regal King Body
        val bodyPath = Path().apply {
            moveTo(c.x - s * 0.21f, c.y + s * 0.24f)
            cubicTo(
                c.x - s * 0.16f, c.y + s * 0.06f,
                c.x - s * 0.11f, c.y - s * 0.06f,
                c.x - s * 0.16f, c.y - s * 0.14f
            )
            lineTo(c.x + s * 0.16f, c.y - s * 0.14f)
            cubicTo(
                c.x + s * 0.11f, c.y - s * 0.06f,
                c.x + s * 0.16f, c.y + s * 0.06f,
                c.x + s * 0.21f, c.y + s * 0.24f
            )
            close()
        }
        scope.drawPath(bodyPath, brush)
        scope.drawPath(bodyPath, rimColor, style = Stroke(width = s * 0.025f))

        // Ring
        scope.drawOval(
            brush = brush,
            topLeft = Offset(c.x - s * 0.18f, c.y - s * 0.15f),
            size = Size(s * 0.36f, s * 0.07f)
        )

        // Royal Dome Crown
        val crownPath = Path().apply {
            moveTo(c.x - s * 0.17f, c.y - s * 0.14f)
            cubicTo(
                c.x - s * 0.24f, c.y - s * 0.26f,
                c.x - s * 0.12f, c.y - s * 0.33f,
                c.x, c.y - s * 0.32f
            )
            cubicTo(
                c.x + s * 0.12f, c.y - s * 0.33f,
                c.x + s * 0.24f, c.y - s * 0.26f,
                c.x + s * 0.17f, c.y - s * 0.14f
            )
            close()
        }
        scope.drawPath(crownPath, brush)
        scope.drawPath(crownPath, rimColor, style = Stroke(width = s * 0.025f))

        // Cross Finial (+)
        val crossCenterY = c.y - s * 0.38f
        val crossH = s * 0.13f
        val crossW = s * 0.09f
        val strokeW = s * 0.035f

        // Vertical bar
        scope.drawLine(
            color = rimColor,
            start = Offset(c.x, crossCenterY - crossH * 0.5f),
            end = Offset(c.x, crossCenterY + crossH * 0.5f),
            strokeWidth = strokeW,
            cap = StrokeCap.Square
        )
        // Horizontal bar
        scope.drawLine(
            color = rimColor,
            start = Offset(c.x - crossW * 0.5f, crossCenterY - crossH * 0.1f),
            end = Offset(c.x + crossW * 0.5f, crossCenterY - crossH * 0.1f),
            strokeWidth = strokeW,
            cap = StrokeCap.Square
        )
    }
}
