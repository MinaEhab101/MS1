package com.example.chess.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.chess.engine.ChessEngine
import com.example.chess.model.ChessGameState
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import kotlin.math.min

@Composable
fun ChessCanvas3D(
    state: ChessGameState,
    onSquareClicked: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for check indicator and selected square
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Wood & Board Themes
    val lightSquareColor = remember { Color(0xFFE8D5B7) }
    val darkSquareColor = remember { Color(0xFF6B492B) }
    val woodBorderDark = remember { Color(0xFF331F10) }
    val woodBorderMid = remember { Color(0xFF4A2E19) }
    val brassGold = remember { Color(0xFFDAA520) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()

        // Maintain square board aspect ratio within container
        val boardDiameter = min(w * 0.96f, h * 0.92f)
        val boardOriginX = (w - boardDiameter) / 2f
        val boardOriginY = (h - boardDiameter) / 2f

        val borderThickness = boardDiameter * 0.055f
        val playableSize = boardDiameter - (borderThickness * 2)
        val squareSize = playableSize / 8f

        // 3D Perspective settings
        val is3D = state.is3DView
        val perspectiveRatio = if (is3D) 0.88f else 1.0f // Foreshortening factor at top of board
        val depthExtrusion = if (is3D) boardDiameter * 0.045f else 0f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.turn, state.isGameOver, state.isBoardFlipped, is3D) {
                    detectTapGestures { tapOffset ->
                        val clickedSq = mapTapToSquare(
                            tapOffset = tapOffset,
                            boardOriginX = boardOriginX,
                            boardOriginY = boardOriginY,
                            borderThickness = borderThickness,
                            squareSize = squareSize,
                            isFlipped = state.isBoardFlipped
                        )
                        if (clickedSq != null && clickedSq.isValid) {
                            onSquareClicked(clickedSq)
                        }
                    }
                }
        ) {
            // 1. Draw 3D Slab Shadow (Under board)
            if (is3D) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(boardOriginX + borderThickness * 0.3f, boardOriginY + depthExtrusion * 1.5f),
                    size = Size(boardDiameter, boardDiameter)
                )

                // Draw 3D Wooden Table Slab side edge
                val slabPath = Path().apply {
                    moveTo(boardOriginX, boardOriginY + boardDiameter)
                    lineTo(boardOriginX + boardDiameter, boardOriginY + boardDiameter)
                    lineTo(boardOriginX + boardDiameter, boardOriginY + boardDiameter + depthExtrusion)
                    lineTo(boardOriginX, boardOriginY + boardDiameter + depthExtrusion)
                    close()
                }
                drawPath(
                    slabPath,
                    Brush.verticalGradient(listOf(woodBorderDark, Color(0xFF1E120A)))
                )
            }

            // 2. Outer Mahogany Wooden Border with Bevel & Brass Inlay
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(woodBorderMid, woodBorderDark),
                    center = Offset(w / 2f, h / 2f),
                    radius = boardDiameter * 0.8f
                ),
                topLeft = Offset(boardOriginX, boardOriginY),
                size = Size(boardDiameter, boardDiameter)
            )

            // Brass inlay line around grid
            drawRect(
                color = brassGold.copy(alpha = 0.8f),
                topLeft = Offset(boardOriginX + borderThickness * 0.82f, boardOriginY + borderThickness * 0.82f),
                size = Size(boardDiameter - borderThickness * 1.64f, boardDiameter - borderThickness * 1.64f),
                style = Stroke(width = 2.5f)
            )

            // Inner dark inset border
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(boardOriginX + borderThickness - 2f, boardOriginY + borderThickness - 2f),
                size = Size(playableSize + 4f, playableSize + 4f),
                style = Stroke(width = 3f)
            )

            // 3. Draw Rank (1-8) and File (a-h) Coordinate Labels in Border
            drawCoordinates(
                drawScope = this,
                boardOriginX = boardOriginX,
                boardOriginY = boardOriginY,
                borderThickness = borderThickness,
                squareSize = squareSize,
                isFlipped = state.isBoardFlipped,
                textColor = brassGold
            )

            // 4. Draw 64 Chess Squares
            for (f in 0..7) {
                for (r in 0..7) {
                    val displayF = if (state.isBoardFlipped) 7 - f else f
                    val displayR = if (state.isBoardFlipped) r else 7 - r

                    val sqX = boardOriginX + borderThickness + (displayF * squareSize)
                    val sqY = boardOriginY + borderThickness + (displayR * squareSize)
                    val square = Square(f, r)

                    val isLight = square.isLightSquare
                    val baseColor = if (isLight) lightSquareColor else darkSquareColor

                    // Base Square with subtle wood grain gradient
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = if (isLight) listOf(baseColor, Color(0xFFDCC49E))
                            else listOf(baseColor, Color(0xFF55351B)),
                            start = Offset(sqX, sqY),
                            end = Offset(sqX + squareSize, sqY + squareSize)
                        ),
                        topLeft = Offset(sqX, sqY),
                        size = Size(squareSize, squareSize)
                    )

                    // Last move highlight (amber glow)
                    val isLastMoveFrom = state.lastMove?.from == square
                    val isLastMoveTo = state.lastMove?.to == square
                    if (isLastMoveFrom || isLastMoveTo) {
                        drawRect(
                            color = Color(0xFFFFD54F).copy(alpha = 0.35f),
                            topLeft = Offset(sqX, sqY),
                            size = Size(squareSize, squareSize)
                        )
                    }

                    // Selected square highlight (bright gold glow)
                    if (state.selectedSquare == square) {
                        drawRect(
                            color = Color(0xFFFFEB3B).copy(alpha = 0.45f * pulseAlpha),
                            topLeft = Offset(sqX, sqY),
                            size = Size(squareSize, squareSize)
                        )
                        drawRect(
                            color = Color(0xFFFFD700),
                            topLeft = Offset(sqX + 2f, sqY + 2f),
                            size = Size(squareSize - 4f, squareSize - 4f),
                            style = Stroke(width = 3.5f)
                        )
                    }

                    // Check highlight on King
                    if (state.isInCheck) {
                        val piece = state.board[square]
                        if (piece != null && piece.type == com.example.chess.model.PieceType.KING && piece.color == state.turn) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFE53935).copy(alpha = 0.85f * pulseAlpha), Color.Transparent),
                                    center = Offset(sqX + squareSize / 2f, sqY + squareSize / 2f),
                                    radius = squareSize * 0.75f
                                ),
                                center = Offset(sqX + squareSize / 2f, sqY + squareSize / 2f),
                                radius = squareSize * 0.65f
                            )
                        }
                    }
                }
            }

            // 5. Draw Legal Move Indicator Rings & Dots
            val validTargets = state.validMovesForSelected
            for (move in validTargets) {
                val displayF = if (state.isBoardFlipped) 7 - move.to.file else move.to.file
                val displayR = if (state.isBoardFlipped) move.to.rank else 7 - move.to.rank

                val sqCenterX = boardOriginX + borderThickness + (displayF * squareSize) + (squareSize / 2f)
                val sqCenterY = boardOriginY + borderThickness + (displayR * squareSize) + (squareSize / 2f)

                if (move.capturedPiece != null || move.moveType == com.example.chess.model.MoveType.EN_PASSANT) {
                    // Capture target: red/coral combat ring with corner brackets
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = 0.8f),
                        radius = squareSize * 0.42f,
                        center = Offset(sqCenterX, sqCenterY),
                        style = Stroke(width = 4f)
                    )
                } else {
                    // Normal move: glowing teal/gold dot
                    drawCircle(
                        color = Color(0xFF26A69A).copy(alpha = 0.75f),
                        radius = squareSize * 0.16f,
                        center = Offset(sqCenterX, sqCenterY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = squareSize * 0.08f,
                        center = Offset(sqCenterX, sqCenterY)
                    )
                }
            }

            // 6. Draw 3D Chess Pieces
            for ((square, piece) in state.board) {
                val isSelected = (state.selectedSquare == square)
                val displayF = if (state.isBoardFlipped) 7 - square.file else square.file
                val displayR = if (state.isBoardFlipped) square.rank else 7 - square.rank

                val sqCenterX = boardOriginX + borderThickness + (displayF * squareSize) + (squareSize / 2f)
                val sqCenterY = boardOriginY + borderThickness + (displayR * squareSize) + (squareSize / 2f)

                val elevation = if (isSelected) squareSize * 0.18f else 0f

                ChessPiecePainter.drawPiece(
                    drawScope = this,
                    piece = piece,
                    center = Offset(sqCenterX, sqCenterY),
                    size = squareSize * 0.88f,
                    elevation = elevation
                )
            }
        }
    }
}

private fun drawCoordinates(
    drawScope: DrawScope,
    boardOriginX: Float,
    boardOriginY: Float,
    borderThickness: Float,
    squareSize: Float,
    isFlipped: Boolean,
    textColor: Color
) {
    drawScope.drawIntoCanvas { canvas ->
        val textPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = borderThickness * 0.42f
            color = android.graphics.Color.argb(
                (textColor.alpha * 255).toInt(),
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Files (a - h) on bottom and top borders
        for (f in 0..7) {
            val fileChar = if (isFlipped) ('h' - f) else ('a' + f)
            val centerX = boardOriginX + borderThickness + (f * squareSize) + (squareSize / 2f)

            // Bottom border
            val bottomY = boardOriginY + borderThickness + (8 * squareSize) + (borderThickness * 0.65f)
            canvas.nativeCanvas.drawText(fileChar.toString(), centerX, bottomY, textPaint)

            // Top border
            val topY = boardOriginY + (borderThickness * 0.58f)
            canvas.nativeCanvas.drawText(fileChar.toString(), centerX, topY, textPaint)
        }

        // Ranks (1 - 8) on left and right borders
        for (r in 0..7) {
            val rankNum = if (isFlipped) (r + 1) else (8 - r)
            val centerY = boardOriginY + borderThickness + (r * squareSize) + (squareSize * 0.62f)

            // Left border
            val leftX = boardOriginX + (borderThickness * 0.45f)
            canvas.nativeCanvas.drawText(rankNum.toString(), leftX, centerY, textPaint)

            // Right border
            val rightX = boardOriginX + borderThickness + (8 * squareSize) + (borderThickness * 0.55f)
            canvas.nativeCanvas.drawText(rankNum.toString(), rightX, centerY, textPaint)
        }
    }
}

private fun mapTapToSquare(
    tapOffset: Offset,
    boardOriginX: Float,
    boardOriginY: Float,
    borderThickness: Float,
    squareSize: Float,
    isFlipped: Boolean
): Square? {
    val relativeX = tapOffset.x - (boardOriginX + borderThickness)
    val relativeY = tapOffset.y - (boardOriginY + borderThickness)

    val gridF = (relativeX / squareSize).toInt()
    val gridR = (relativeY / squareSize).toInt()

    if (gridF !in 0..7 || gridR !in 0..7) return null

    val file = if (isFlipped) 7 - gridF else gridF
    val rank = if (isFlipped) gridR else 7 - gridR

    return Square(file, rank)
}
