package com.example.chess.model

import com.example.game.AIDifficulty

enum class PieceColor {
    WHITE, BLACK;

    val opposite: PieceColor
        get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000)
}

data class Square(val file: Int, val rank: Int) {
    val isValid: Boolean get() = file in 0..7 && rank in 0..7

    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    val isLightSquare: Boolean
        get() = (file + rank) % 2 != 0

    companion object {
        fun fromAlgebraic(alg: String): Square? {
            if (alg.length != 2) return null
            val f = alg[0] - 'a'
            val r = alg[1].digitToIntOrNull()?.minus(1) ?: return null
            return if (f in 0..7 && r in 0..7) Square(f, r) else null
        }
    }
}

data class ChessPiece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    val isWhite: Boolean get() = color == PieceColor.WHITE
}

enum class MoveType {
    NORMAL,
    CAPTURE,
    CASTLE_KINGSIDE,
    CASTLE_QUEENSIDE,
    EN_PASSANT,
    PROMOTION
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val moveType: MoveType = MoveType.NORMAL,
    val promotionType: PieceType? = null,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val sanNotation: String = ""
)

enum class ChessGameMode {
    VS_AI,
    PASS_AND_PLAY,
    ONLINE,
    PRIVATE_ROOM
}

data class ChessGameState(
    val board: Map<Square, ChessPiece> = emptyMap(),
    val turn: PieceColor = PieceColor.WHITE,
    val selectedSquare: Square? = null,
    val validMovesForSelected: List<ChessMove> = emptyList(),
    val lastMove: ChessMove? = null,
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val isInCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val drawReason: String? = null,
    val winner: PieceColor? = null,
    val whiteTimeRemainingSec: Int = 600,
    val blackTimeRemainingSec: Int = 600,
    val isTimerRunning: Boolean = false,
    val capturedByWhite: List<ChessPiece> = emptyList(),
    val capturedByBlack: List<ChessPiece> = emptyList(),
    val moveHistory: List<ChessMove> = emptyList(),
    val pendingPromotionMove: ChessMove? = null,
    val mode: ChessGameMode = ChessGameMode.VS_AI,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val opponentName: String = "Grand AI",
    val opponentRating: Int = 1200,
    val roomCode: String? = null,
    val isBoardFlipped: Boolean = false,
    val is3DView: Boolean = true,
    val cameraTiltAngle: Float = 35f
) {
    val isGameOver: Boolean
        get() = isCheckmate || isStalemate || isDraw || winner != null

    val materialAdvantageWhite: Int
        get() {
            val wScore = capturedByWhite.sumOf { it.type.value }
            val bScore = capturedByBlack.sumOf { it.type.value }
            return (wScore - bScore) / 100
        }
}
