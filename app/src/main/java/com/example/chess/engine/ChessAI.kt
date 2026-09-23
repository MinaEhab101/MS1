package com.example.chess.engine

import com.example.chess.model.ChessGameState
import com.example.chess.model.ChessMove
import com.example.chess.model.ChessPiece
import com.example.chess.model.MoveType
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.game.AIDifficulty
import kotlin.random.Random

object ChessAI {

    // Piece-Square Tables (PST) for White (invert rank for Black)
    private val PAWN_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val QUEEN_TABLE = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MIDDLE_TABLE = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    fun computeBestMove(state: ChessGameState): ChessMove? {
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) return null

        return when (state.aiDifficulty) {
            AIDifficulty.EASY -> {
                // 30% chance to pick a capture if available, otherwise random
                val captures = legalMoves.filter { it.capturedPiece != null }
                if (captures.isNotEmpty() && Random.nextFloat() < 0.35f) {
                    captures.random()
                } else {
                    legalMoves.random()
                }
            }

            AIDifficulty.MEDIUM -> {
                // 1-ply evaluation: maximize piece value gains and center control
                legalMoves.maxByOrNull { move ->
                    evaluateSingleMove(state, move) + Random.nextInt(-15, 15)
                } ?: legalMoves.random()
            }

            AIDifficulty.HARD -> {
                // Minimax with Alpha-Beta pruning at depth 2
                findMinimaxMove(state, legalMoves, depth = 2)
            }

            AIDifficulty.EXPERT -> {
                // Minimax with Alpha-Beta pruning at depth 3
                findMinimaxMove(state, legalMoves, depth = 3)
            }
        }
    }

    private fun evaluateSingleMove(state: ChessGameState, move: ChessMove): Int {
        var score = 0
        if (move.capturedPiece != null) {
            score += move.capturedPiece.type.value - (move.piece.type.value / 10)
        }
        if (move.moveType == MoveType.CASTLE_KINGSIDE || move.moveType == MoveType.CASTLE_QUEENSIDE) {
            score += 60
        }
        if (move.moveType == MoveType.PROMOTION) {
            score += 800
        }
        // Center control
        if (move.to.file in 3..4 && move.to.rank in 3..4) {
            score += 20
        }
        return score
    }

    private fun findMinimaxMove(
        state: ChessGameState,
        legalMoves: List<ChessMove>,
        depth: Int
    ): ChessMove {
        val isMaximizing = (state.turn == PieceColor.WHITE)
        var bestScore = if (isMaximizing) -1000000 else 1000000
        var bestMove = legalMoves.first()

        // Move ordering: sort captures first
        val sortedMoves = legalMoves.sortedByDescending { move ->
            (move.capturedPiece?.type?.value ?: 0) - (move.piece.type.value / 10)
        }

        var alpha = -1000000
        var beta = 1000000

        for (move in sortedMoves) {
            val nextState = ChessEngine.makeMove(state, move)
            val score = minimax(nextState, depth - 1, alpha, beta, !isMaximizing)

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                alpha = maxOf(alpha, bestScore)
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
                beta = minOf(beta, bestScore)
            }

            if (beta <= alpha) break
        }

        return bestMove
    }

    private fun minimax(
        state: ChessGameState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        isMaximizing: Boolean
    ): Int {
        if (state.isCheckmate) {
            return if (state.winner == PieceColor.WHITE) 500000 + depth else -500000 - depth
        }
        if (state.isDraw || state.isStalemate) return 0
        if (depth == 0) return evaluateBoard(state.board)

        var alpha = alphaInit
        var beta = betaInit
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) return 0

        val sortedMoves = legalMoves.sortedByDescending { move ->
            (move.capturedPiece?.type?.value ?: 0)
        }

        if (isMaximizing) {
            var maxEval = -1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.makeMove(state, move)
                val evaluation = minimax(nextState, depth - 1, alpha, beta, false)
                maxEval = maxOf(maxEval, evaluation)
                alpha = maxOf(alpha, evaluation)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.makeMove(state, move)
                val evaluation = minimax(nextState, depth - 1, alpha, beta, true)
                minEval = minOf(minEval, evaluation)
                beta = minOf(beta, evaluation)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    fun evaluateBoard(board: Map<Square, ChessPiece>): Int {
        var whiteScore = 0
        var blackScore = 0

        for ((sq, piece) in board) {
            val baseVal = piece.type.value
            val pstVal = getPstValue(piece.type, piece.color, sq)
            val totalPieceVal = baseVal + pstVal

            if (piece.isWhite) {
                whiteScore += totalPieceVal
            } else {
                blackScore += totalPieceVal
            }
        }

        return whiteScore - blackScore
    }

    private fun getPstValue(type: PieceType, color: PieceColor, square: Square): Int {
        val file = square.file
        val rank = if (color == PieceColor.WHITE) 7 - square.rank else square.rank
        val index = rank * 8 + file
        if (index !in 0..63) return 0

        return when (type) {
            PieceType.PAWN -> PAWN_TABLE[index]
            PieceType.KNIGHT -> KNIGHT_TABLE[index]
            PieceType.BISHOP -> BISHOP_TABLE[index]
            PieceType.ROOK -> ROOK_TABLE[index]
            PieceType.QUEEN -> QUEEN_TABLE[index]
            PieceType.KING -> KING_MIDDLE_TABLE[index]
        }
    }
}
