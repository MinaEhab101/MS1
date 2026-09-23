package com.example.chess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.chess.engine.ChessAI
import com.example.chess.engine.ChessEngine
import com.example.chess.model.ChessGameMode
import com.example.chess.model.ChessGameState
import com.example.chess.model.ChessMove
import com.example.chess.model.MoveType
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.game.AIDifficulty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChessViewModel(
    private val soundManager: SoundManager? = null,
    private val authRepository: AuthRepository? = null,
    private val firestoreRepository: FirestoreRepository? = null
) : ViewModel() {

    private val _gameState = MutableStateFlow(ChessGameState())
    val gameState: StateFlow<ChessGameState> = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private var aiJob: Job? = null

    init {
        startNewGame(ChessGameMode.VS_AI, AIDifficulty.MEDIUM)
    }

    fun startNewGame(
        mode: ChessGameMode = ChessGameMode.VS_AI,
        difficulty: AIDifficulty = AIDifficulty.MEDIUM,
        opponentName: String = if (mode == ChessGameMode.PASS_AND_PLAY) "Player 2" else "Grandmaster AI",
        opponentRating: Int = when (difficulty) {
            AIDifficulty.EASY -> 1100
            AIDifficulty.MEDIUM -> 1450
            AIDifficulty.HARD -> 1800
            AIDifficulty.EXPERT -> 2200
        },
        timeLimitSec: Int = 600
    ) {
        timerJob?.cancel()
        aiJob?.cancel()

        val initial = ChessEngine.startNewGame()
        _gameState.value = initial.copy(
            mode = mode,
            aiDifficulty = difficulty,
            opponentName = opponentName,
            opponentRating = opponentRating,
            whiteTimeRemainingSec = timeLimitSec,
            blackTimeRemainingSec = timeLimitSec,
            isTimerRunning = (mode == ChessGameMode.ONLINE || timeLimitSec > 0),
            isBoardFlipped = false
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _gameState.value
                if (current.isGameOver || !current.isTimerRunning) continue

                if (current.turn == PieceColor.WHITE) {
                    val newTime = (current.whiteTimeRemainingSec - 1).coerceAtLeast(0)
                    if (newTime == 0) {
                        handleTimeOut(PieceColor.BLACK)
                        break
                    } else {
                        _gameState.value = current.copy(whiteTimeRemainingSec = newTime)
                    }
                } else {
                    val newTime = (current.blackTimeRemainingSec - 1).coerceAtLeast(0)
                    if (newTime == 0) {
                        handleTimeOut(PieceColor.WHITE)
                        break
                    } else {
                        _gameState.value = current.copy(blackTimeRemainingSec = newTime)
                    }
                }
            }
        }
    }

    private fun handleTimeOut(winner: PieceColor) {
        val current = _gameState.value
        val winnerName = if (winner == PieceColor.WHITE) "White (الأبيض)" else "Black (الأسود)"
        _gameState.value = current.copy(
            winner = winner,
            isDraw = false,
            drawReason = "Time Out (نفاد الوقت)! $winnerName wins on time."
        )
        if (winner == PieceColor.WHITE) {
            soundManager?.playWin()
            onMatchFinished(won = true)
        } else {
            soundManager?.playLose()
            onMatchFinished(won = false)
        }
    }

    fun onSquareClicked(square: Square) {
        val current = _gameState.value
        if (current.isGameOver) return

        // In VS_AI mode, ignore user click when it's AI's turn (Black)
        if (current.mode == ChessGameMode.VS_AI && current.turn == PieceColor.BLACK) {
            return
        }

        val clickedPiece = current.board[square]

        // 1. If clicking own piece, select it and compute legal moves
        if (clickedPiece != null && clickedPiece.color == current.turn) {
            if (current.selectedSquare == square) {
                // Deselect
                _gameState.value = current.copy(selectedSquare = null, validMovesForSelected = emptyList())
            } else {
                val legalMoves = ChessEngine.getLegalMovesForSquare(current, square)
                soundManager?.playButtonClick()
                _gameState.value = current.copy(
                    selectedSquare = square,
                    validMovesForSelected = legalMoves
                )
            }
            return
        }

        // 2. If a piece is already selected, check if clicked square is a valid target
        val selected = current.selectedSquare
        if (selected != null) {
            val matchingMove = current.validMovesForSelected.firstOrNull { it.to == square }
            if (matchingMove != null) {
                // Check if this move requires pawn promotion selection
                if (matchingMove.moveType == MoveType.PROMOTION) {
                    _gameState.value = current.copy(pendingPromotionMove = matchingMove)
                } else {
                    executeMove(matchingMove)
                }
                return
            }
        }

        // 3. Otherwise deselect
        if (current.selectedSquare != null) {
            _gameState.value = current.copy(selectedSquare = null, validMovesForSelected = emptyList())
        }
    }

    fun selectPromotionPiece(promotionType: PieceType) {
        val current = _gameState.value
        val pending = current.pendingPromotionMove ?: return
        val finalMove = pending.copy(promotionType = promotionType)
        executeMove(finalMove)
    }

    fun dismissPromotion() {
        val current = _gameState.value
        _gameState.value = current.copy(pendingPromotionMove = null)
    }

    private fun executeMove(move: ChessMove) {
        val current = _gameState.value
        val nextState = ChessEngine.makeMove(current, move)
        _gameState.value = nextState

        // Audio and haptics
        if (move.capturedPiece != null || move.moveType == MoveType.EN_PASSANT) {
            soundManager?.playChessCapture()
        } else {
            soundManager?.playChessMove()
        }

        if (nextState.isInCheck) {
            soundManager?.playChessCheck()
        }

        // Handle game outcome
        if (nextState.isCheckmate) {
            if (nextState.winner == PieceColor.WHITE) {
                soundManager?.playWin()
                onMatchFinished(won = true)
            } else {
                soundManager?.playLose()
                onMatchFinished(won = false)
            }
        } else if (nextState.isDraw) {
            soundManager?.playNotification()
            onMatchFinished(won = null)
        } else {
            // Check if next turn is AI's turn
            if (nextState.mode == ChessGameMode.VS_AI && nextState.turn == PieceColor.BLACK) {
                triggerAiMove()
            }
        }
    }

    private fun triggerAiMove() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            // Realistic AI contemplation delay
            val delayTime = when (_gameState.value.aiDifficulty) {
                AIDifficulty.EASY -> 400L
                AIDifficulty.MEDIUM -> 600L
                AIDifficulty.HARD -> 850L
                AIDifficulty.EXPERT -> 1100L
            }
            delay(delayTime)

            val current = _gameState.value
            if (current.isGameOver || current.turn != PieceColor.BLACK) return@launch

            val bestMove = ChessAI.computeBestMove(current)
            if (bestMove != null) {
                val nextState = ChessEngine.makeMove(current, bestMove)
                _gameState.value = nextState

                if (bestMove.capturedPiece != null || bestMove.moveType == MoveType.EN_PASSANT) {
                    soundManager?.playChessCapture()
                } else {
                    soundManager?.playChessMove()
                }

                if (nextState.isInCheck) {
                    soundManager?.playChessCheck()
                }

                if (nextState.isCheckmate) {
                    soundManager?.playLose()
                    onMatchFinished(won = false)
                } else if (nextState.isDraw) {
                    soundManager?.playNotification()
                    onMatchFinished(won = null)
                }
            }
        }
    }

    fun undoMove() {
        val current = _gameState.value
        if (current.moveHistory.isEmpty() || current.isGameOver) return

        // In AI mode, undo 2 moves (AI move + player move); in local 2-player undo 1 move
        val movesToPop = if (current.mode == ChessGameMode.VS_AI && current.moveHistory.size >= 2) 2 else 1
        val newHistory = current.moveHistory.dropLast(movesToPop)

        // Replay from initial board
        var replayedState = ChessEngine.startNewGame().copy(
            mode = current.mode,
            aiDifficulty = current.aiDifficulty,
            opponentName = current.opponentName,
            opponentRating = current.opponentRating,
            whiteTimeRemainingSec = current.whiteTimeRemainingSec,
            blackTimeRemainingSec = current.blackTimeRemainingSec,
            isBoardFlipped = current.isBoardFlipped,
            is3DView = current.is3DView
        )

        for (m in newHistory) {
            replayedState = ChessEngine.makeMove(replayedState, m)
        }

        _gameState.value = replayedState
        soundManager?.playButtonClick()
    }

    fun resignGame() {
        val current = _gameState.value
        if (current.isGameOver) return
        val winner = current.turn.opposite
        _gameState.value = current.copy(
            winner = winner,
            isDraw = false,
            drawReason = "Resignation (استسلام)"
        )
        soundManager?.playLose()
        onMatchFinished(won = false)
    }

    fun offerDraw() {
        val current = _gameState.value
        if (current.isGameOver) return
        _gameState.value = current.copy(
            isDraw = true,
            drawReason = "Mutual Agreement (اتفاق الطرفين على التعادل)"
        )
        soundManager?.playNotification()
        onMatchFinished(won = null)
    }

    fun toggleBoardFlipped() {
        _gameState.value = _gameState.value.copy(
            isBoardFlipped = !_gameState.value.isBoardFlipped
        )
        soundManager?.playButtonClick()
    }

    fun toggle3DView() {
        _gameState.value = _gameState.value.copy(
            is3DView = !_gameState.value.is3DView
        )
        soundManager?.playButtonClick()
    }

    private fun onMatchFinished(won: Boolean?) {
        val user = authRepository?.currentUser?.value ?: return
        viewModelScope.launch {
            val deltaRating = when (won) {
                true -> 18
                false -> -14
                null -> 2
            }
            val newChessRating = (user.chessRating + deltaRating).coerceAtLeast(400)
            val newCoins = if (won == true) user.coins + 150 else user.coins + 30
            val newWins = if (won == true) user.wins + 1 else user.wins
            val newLosses = if (won == false) user.losses + 1 else user.losses
            val newDraws = if (won == null) user.draws + 1 else user.draws
            val newXp = user.xp + 100

            val updatedUser = user.copy(
                chessRating = newChessRating,
                coins = newCoins,
                wins = newWins,
                losses = newLosses,
                draws = newDraws,
                xp = newXp,
                totalGames = user.totalGames + 1
            )
            authRepository.saveUser(updatedUser)
            firestoreRepository?.syncUserProfile(updatedUser)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        aiJob?.cancel()
    }
}
