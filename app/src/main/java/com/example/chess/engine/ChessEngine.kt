package com.example.chess.engine

import com.example.chess.model.ChessGameState
import com.example.chess.model.ChessMove
import com.example.chess.model.ChessPiece
import com.example.chess.model.MoveType
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import java.util.UUID

object ChessEngine {

    fun createInitialBoard(): Map<Square, ChessPiece> {
        val board = mutableMapOf<Square, ChessPiece>()

        // White pieces (ranks 0 and 1)
        board[Square(0, 0)] = ChessPiece("w_r1", PieceType.ROOK, PieceColor.WHITE)
        board[Square(1, 0)] = ChessPiece("w_n1", PieceType.KNIGHT, PieceColor.WHITE)
        board[Square(2, 0)] = ChessPiece("w_b1", PieceType.BISHOP, PieceColor.WHITE)
        board[Square(3, 0)] = ChessPiece("w_q", PieceType.QUEEN, PieceColor.WHITE)
        board[Square(4, 0)] = ChessPiece("w_k", PieceType.KING, PieceColor.WHITE)
        board[Square(5, 0)] = ChessPiece("w_b2", PieceType.BISHOP, PieceColor.WHITE)
        board[Square(6, 0)] = ChessPiece("w_n2", PieceType.KNIGHT, PieceColor.WHITE)
        board[Square(7, 0)] = ChessPiece("w_r2", PieceType.ROOK, PieceColor.WHITE)
        for (f in 0..7) {
            board[Square(f, 1)] = ChessPiece("w_p$f", PieceType.PAWN, PieceColor.WHITE)
        }

        // Black pieces (ranks 7 and 6)
        board[Square(0, 7)] = ChessPiece("b_r1", PieceType.ROOK, PieceColor.BLACK)
        board[Square(1, 7)] = ChessPiece("b_n1", PieceType.KNIGHT, PieceColor.BLACK)
        board[Square(2, 7)] = ChessPiece("b_b1", PieceType.BISHOP, PieceColor.BLACK)
        board[Square(3, 7)] = ChessPiece("b_q", PieceType.QUEEN, PieceColor.BLACK)
        board[Square(4, 7)] = ChessPiece("b_k", PieceType.KING, PieceColor.BLACK)
        board[Square(5, 7)] = ChessPiece("b_b2", PieceType.BISHOP, PieceColor.BLACK)
        board[Square(6, 7)] = ChessPiece("b_n2", PieceType.KNIGHT, PieceColor.BLACK)
        board[Square(7, 7)] = ChessPiece("b_r2", PieceType.ROOK, PieceColor.BLACK)
        for (f in 0..7) {
            board[Square(f, 6)] = ChessPiece("b_p$f", PieceType.PAWN, PieceColor.BLACK)
        }

        return board
    }

    fun startNewGame(): ChessGameState {
        val board = createInitialBoard()
        return ChessGameState(
            board = board,
            turn = PieceColor.WHITE
        )
    }

    fun findKingSquare(board: Map<Square, ChessPiece>, color: PieceColor): Square? {
        return board.entries.firstOrNull { it.value.type == PieceType.KING && it.value.color == color }?.key
    }

    fun isSquareAttacked(board: Map<Square, ChessPiece>, target: Square, byColor: PieceColor): Boolean {
        for ((sq, piece) in board) {
            if (piece.color == byColor) {
                if (canPieceAttackSquare(board, piece, sq, target)) {
                    return true
                }
            }
        }
        return false
    }

    private fun canPieceAttackSquare(
        board: Map<Square, ChessPiece>,
        piece: ChessPiece,
        from: Square,
        to: Square
    ): Boolean {
        val df = to.file - from.file
        val dr = to.rank - from.rank

        return when (piece.type) {
            PieceType.PAWN -> {
                val forward = if (piece.isWhite) 1 else -1
                dr == forward && (df == 1 || df == -1)
            }
            PieceType.KNIGHT -> {
                (kotlin.math.abs(df) == 1 && kotlin.math.abs(dr) == 2) ||
                        (kotlin.math.abs(df) == 2 && kotlin.math.abs(dr) == 1)
            }
            PieceType.BISHOP -> {
                if (kotlin.math.abs(df) == kotlin.math.abs(dr) && df != 0) {
                    isPathClear(board, from, to)
                } else false
            }
            PieceType.ROOK -> {
                if ((df == 0 && dr != 0) || (df != 0 && dr == 0)) {
                    isPathClear(board, from, to)
                } else false
            }
            PieceType.QUEEN -> {
                if ((kotlin.math.abs(df) == kotlin.math.abs(dr) && df != 0) ||
                    (df == 0 && dr != 0) || (df != 0 && dr == 0)
                ) {
                    isPathClear(board, from, to)
                } else false
            }
            PieceType.KING -> {
                kotlin.math.abs(df) <= 1 && kotlin.math.abs(dr) <= 1 && (df != 0 || dr != 0)
            }
        }
    }

    private fun isPathClear(board: Map<Square, ChessPiece>, from: Square, to: Square): Boolean {
        val stepF = (to.file - from.file).coerceIn(-1, 1)
        val stepR = (to.rank - from.rank).coerceIn(-1, 1)

        var curF = from.file + stepF
        var curR = from.rank + stepR

        while (curF != to.file || curR != to.rank) {
            if (board.containsKey(Square(curF, curR))) {
                return false
            }
            curF += stepF
            curR += stepR
        }
        return true
    }

    fun isKingInCheck(board: Map<Square, ChessPiece>, color: PieceColor): Boolean {
        val kingSq = findKingSquare(board, color) ?: return false
        return isSquareAttacked(board, kingSq, color.opposite)
    }

    fun getLegalMoves(state: ChessGameState): List<ChessMove> {
        val pseudoMoves = mutableListOf<ChessMove>()

        for ((sq, piece) in state.board) {
            if (piece.color == state.turn) {
                pseudoMoves.addAll(getPseudoLegalMovesForPiece(state, sq, piece))
            }
        }

        // Filter moves that leave king in check
        return pseudoMoves.filter { move ->
            val simulatedBoard = simulateMoveBoard(state.board, move)
            !isKingInCheck(simulatedBoard, state.turn)
        }
    }

    fun getLegalMovesForSquare(state: ChessGameState, from: Square): List<ChessMove> {
        val piece = state.board[from] ?: return emptyList()
        if (piece.color != state.turn) return emptyList()

        val pseudo = getPseudoLegalMovesForPiece(state, from, piece)
        return pseudo.filter { move ->
            val simulatedBoard = simulateMoveBoard(state.board, move)
            !isKingInCheck(simulatedBoard, state.turn)
        }
    }

    private fun getPseudoLegalMovesForPiece(
        state: ChessGameState,
        from: Square,
        piece: ChessPiece
    ): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val board = state.board

        when (piece.type) {
            PieceType.PAWN -> {
                val forward = if (piece.isWhite) 1 else -1
                val startRank = if (piece.isWhite) 1 else 6
                val promoRank = if (piece.isWhite) 7 else 0

                // 1 step forward
                val oneStep = Square(from.file, from.rank + forward)
                if (oneStep.isValid && !board.containsKey(oneStep)) {
                    if (oneStep.rank == promoRank) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pType ->
                            moves.add(ChessMove(from, oneStep, piece, moveType = MoveType.PROMOTION, promotionType = pType))
                        }
                    } else {
                        moves.add(ChessMove(from, oneStep, piece))
                    }

                    // 2 steps forward
                    if (from.rank == startRank) {
                        val twoStep = Square(from.file, from.rank + 2 * forward)
                        if (twoStep.isValid && !board.containsKey(twoStep)) {
                            moves.add(ChessMove(from, twoStep, piece))
                        }
                    }
                }

                // Diagonal captures
                for (df in listOf(-1, 1)) {
                    val capSq = Square(from.file + df, from.rank + forward)
                    if (capSq.isValid) {
                        val targetPiece = board[capSq]
                        if (targetPiece != null && targetPiece.color != piece.color) {
                            if (capSq.rank == promoRank) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pType ->
                                    moves.add(ChessMove(from, capSq, piece, capturedPiece = targetPiece, moveType = MoveType.PROMOTION, promotionType = pType))
                                }
                            } else {
                                moves.add(ChessMove(from, capSq, piece, capturedPiece = targetPiece, moveType = MoveType.CAPTURE))
                            }
                        } else if (state.enPassantTarget == capSq) {
                            // En passant
                            val passedPawnSq = Square(capSq.file, from.rank)
                            val passedPawn = board[passedPawnSq]
                            if (passedPawn != null && passedPawn.color != piece.color) {
                                moves.add(ChessMove(from, capSq, piece, capturedPiece = passedPawn, moveType = MoveType.EN_PASSANT))
                            }
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val jumps = listOf(
                    Pair(1, 2), Pair(2, 1), Pair(-1, 2), Pair(-2, 1),
                    Pair(1, -2), Pair(2, -1), Pair(-1, -2), Pair(-2, -1)
                )
                for ((df, dr) in jumps) {
                    val to = Square(from.file + df, from.rank + dr)
                    if (to.isValid) {
                        val target = board[to]
                        if (target == null) {
                            moves.add(ChessMove(from, to, piece))
                        } else if (target.color != piece.color) {
                            moves.add(ChessMove(from, to, piece, capturedPiece = target, moveType = MoveType.CAPTURE))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                val dirs = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
                addRayMoves(moves, board, from, piece, dirs)
            }

            PieceType.ROOK -> {
                val dirs = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
                addRayMoves(moves, board, from, piece, dirs)
            }

            PieceType.QUEEN -> {
                val dirs = listOf(
                    Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                )
                addRayMoves(moves, board, from, piece, dirs)
            }

            PieceType.KING -> {
                for (df in -1..1) {
                    for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val to = Square(from.file + df, from.rank + dr)
                        if (to.isValid) {
                            val target = board[to]
                            if (target == null) {
                                moves.add(ChessMove(from, to, piece))
                            } else if (target.color != piece.color) {
                                moves.add(ChessMove(from, to, piece, capturedPiece = target, moveType = MoveType.CAPTURE))
                            }
                        }
                    }
                }

                // Castling
                if (!piece.hasMoved && !isKingInCheck(board, piece.color)) {
                    val rank = if (piece.isWhite) 0 else 7

                    // Kingside castling (O-O)
                    val rK = board[Square(7, rank)]
                    if (rK != null && rK.type == PieceType.ROOK && !rK.hasMoved && rK.color == piece.color) {
                        val fSq = Square(5, rank)
                        val gSq = Square(6, rank)
                        if (!board.containsKey(fSq) && !board.containsKey(gSq)) {
                            if (!isSquareAttacked(board, fSq, piece.color.opposite) &&
                                !isSquareAttacked(board, gSq, piece.color.opposite)
                            ) {
                                moves.add(ChessMove(from, gSq, piece, moveType = MoveType.CASTLE_KINGSIDE))
                            }
                        }
                    }

                    // Queenside castling (O-O-O)
                    val rQ = board[Square(0, rank)]
                    if (rQ != null && rQ.type == PieceType.ROOK && !rQ.hasMoved && rQ.color == piece.color) {
                        val bSq = Square(1, rank)
                        val cSq = Square(2, rank)
                        val dSq = Square(3, rank)
                        if (!board.containsKey(bSq) && !board.containsKey(cSq) && !board.containsKey(dSq)) {
                            if (!isSquareAttacked(board, cSq, piece.color.opposite) &&
                                !isSquareAttacked(board, dSq, piece.color.opposite)
                            ) {
                                moves.add(ChessMove(from, cSq, piece, moveType = MoveType.CASTLE_QUEENSIDE))
                            }
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun addRayMoves(
        moves: MutableList<ChessMove>,
        board: Map<Square, ChessPiece>,
        from: Square,
        piece: ChessPiece,
        directions: List<Pair<Int, Int>>
    ) {
        for ((df, dr) in directions) {
            var curF = from.file + df
            var curR = from.rank + dr
            while (curF in 0..7 && curR in 0..7) {
                val sq = Square(curF, curR)
                val target = board[sq]
                if (target == null) {
                    moves.add(ChessMove(from, sq, piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(ChessMove(from, sq, piece, capturedPiece = target, moveType = MoveType.CAPTURE))
                    }
                    break
                }
                curF += df
                curR += dr
            }
        }
    }

    private fun simulateMoveBoard(board: Map<Square, ChessPiece>, move: ChessMove): Map<Square, ChessPiece> {
        val next = board.toMutableMap()
        next.remove(move.from)

        when (move.moveType) {
            MoveType.EN_PASSANT -> {
                val passedPawnSq = Square(move.to.file, move.from.rank)
                next.remove(passedPawnSq)
                next[move.to] = move.piece.copy(hasMoved = true)
            }
            MoveType.CASTLE_KINGSIDE -> {
                val rank = move.from.rank
                next[move.to] = move.piece.copy(hasMoved = true)
                val rook = next.remove(Square(7, rank))
                if (rook != null) {
                    next[Square(5, rank)] = rook.copy(hasMoved = true)
                }
            }
            MoveType.CASTLE_QUEENSIDE -> {
                val rank = move.from.rank
                next[move.to] = move.piece.copy(hasMoved = true)
                val rook = next.remove(Square(0, rank))
                if (rook != null) {
                    next[Square(3, rank)] = rook.copy(hasMoved = true)
                }
            }
            MoveType.PROMOTION -> {
                val promoType = move.promotionType ?: PieceType.QUEEN
                next[move.to] = ChessPiece(
                    id = "promo_${UUID.randomUUID().toString().take(6)}",
                    type = promoType,
                    color = move.piece.color,
                    hasMoved = true
                )
            }
            else -> {
                next[move.to] = move.piece.copy(hasMoved = true)
            }
        }

        return next
    }

    fun makeMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val newBoard = simulateMoveBoard(state.board, move)
        val nextTurn = state.turn.opposite

        val captured = move.capturedPiece
        val newCapturedWhite = if (captured != null && state.turn == PieceColor.WHITE) {
            state.capturedByWhite + captured
        } else state.capturedByWhite

        val newCapturedBlack = if (captured != null && state.turn == PieceColor.BLACK) {
            state.capturedByBlack + captured
        } else state.capturedByBlack

        // En passant target for next move
        val nextEnPassant = if (move.piece.type == PieceType.PAWN && kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            val midRank = (move.from.rank + move.to.rank) / 2
            Square(move.from.file, midRank)
        } else null

        // Half-move clock (resets on pawn advance or capture)
        val newHalfMoveClock = if (move.piece.type == PieceType.PAWN || move.capturedPiece != null) {
            0
        } else {
            state.halfMoveClock + 1
        }

        val newFullMove = if (state.turn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        val inCheck = isKingInCheck(newBoard, nextTurn)

        // Check legal moves of opponent to see if checkmate or stalemate
        val tempState = state.copy(
            board = newBoard,
            turn = nextTurn,
            enPassantTarget = nextEnPassant
        )
        val opponentLegalMoves = getLegalMoves(tempState)

        val checkmate = inCheck && opponentLegalMoves.isEmpty()
        val stalemate = !inCheck && opponentLegalMoves.isEmpty()
        val is50Moves = newHalfMoveClock >= 100
        val isInsufficient = hasInsufficientMaterial(newBoard)

        val draw = stalemate || is50Moves || isInsufficient
        val drawReason = when {
            stalemate -> "Stalemate (تعادل بالحصار)"
            is50Moves -> "50-Move Rule Draw (قاعدة الخمسين نقلة)"
            isInsufficient -> "Insufficient Material (قطع غير كافية للفوز)"
            else -> null
        }

        val san = generateSan(move, inCheck, checkmate)
        val moveWithSan = move.copy(isCheck = inCheck, isCheckmate = checkmate, sanNotation = san)

        return state.copy(
            board = newBoard,
            turn = nextTurn,
            selectedSquare = null,
            validMovesForSelected = emptyList(),
            lastMove = moveWithSan,
            enPassantTarget = nextEnPassant,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMove,
            isInCheck = inCheck,
            isCheckmate = checkmate,
            isStalemate = stalemate,
            isDraw = draw,
            drawReason = drawReason,
            winner = if (checkmate) state.turn else null,
            capturedByWhite = newCapturedWhite,
            capturedByBlack = newCapturedBlack,
            moveHistory = state.moveHistory + moveWithSan,
            pendingPromotionMove = null
        )
    }

    private fun generateSan(move: ChessMove, isCheck: Boolean, isCheckmate: Boolean): String {
        val checkSuffix = when {
            isCheckmate -> "#"
            isCheck -> "+"
            else -> ""
        }

        if (move.moveType == MoveType.CASTLE_KINGSIDE) return "O-O$checkSuffix"
        if (move.moveType == MoveType.CASTLE_QUEENSIDE) return "O-O-O$checkSuffix"

        val sb = StringBuilder()
        if (move.piece.type != PieceType.PAWN) {
            sb.append(move.piece.type.symbol)
        } else if (move.capturedPiece != null || move.moveType == MoveType.EN_PASSANT) {
            sb.append(('a' + move.from.file))
        }

        if (move.capturedPiece != null || move.moveType == MoveType.EN_PASSANT) {
            sb.append("x")
        }

        sb.append(move.to.algebraic)

        if (move.moveType == MoveType.PROMOTION) {
            sb.append("=").append(move.promotionType?.symbol ?: "Q")
        }

        sb.append(checkSuffix)
        return sb.toString()
    }

    private fun hasInsufficientMaterial(board: Map<Square, ChessPiece>): Boolean {
        val pieces = board.values
        if (pieces.size == 2) return true // Only K vs K
        if (pieces.size == 3) {
            val nonKings = pieces.filter { it.type != PieceType.KING }
            if (nonKings.all { it.type == PieceType.BISHOP || it.type == PieceType.KNIGHT }) {
                return true // K+B vs K or K+N vs K
            }
        }
        return false
    }
}
