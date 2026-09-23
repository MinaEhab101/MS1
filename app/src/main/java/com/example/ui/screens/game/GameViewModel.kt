package com.example.ui.screens.game

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.ai.BackgammonAI
import com.example.audio.SoundManager
import com.example.firebase.AuthRepository
import com.example.game.AIDifficulty
import com.example.game.BackgammonEngine
import com.example.game.BoardState
import com.example.game.GameHistoryManager
import com.example.game.GameMode
import com.example.game.GamePhase
import com.example.game.GameType
import com.example.game.Move
import com.example.game.MoveValidationResult
import com.example.game.MoveValidationService
import com.example.game.Player
import com.example.game.ReplayStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val context: Context,
    val gameMode: GameMode,
    val soundManager: SoundManager,
    val authRepository: AuthRepository
) : ViewModel() {

    private val _boardState = MutableStateFlow(BackgammonEngine.createInitialBoard())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _ratingChange = MutableStateFlow(0)
    val ratingChange: StateFlow<Int> = _ratingChange.asStateFlow()

    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned: StateFlow<Int> = _coinsEarned.asStateFlow()

    private val _ruleViolationMessage = MutableStateFlow<String?>(null)
    val ruleViolationMessage: StateFlow<String?> = _ruleViolationMessage.asStateFlow()

    private val recordedSteps = mutableListOf<ReplayStep>()

    init {
        GameHistoryManager.initialize(context)
    }

    fun clearRuleViolationMessage() {
        _ruleViolationMessage.value = null
    }

    fun onRollDiceClicked() {
        val current = _boardState.value
        if (current.phase != GamePhase.ROLL_DICE || current.winner != null) return
        if (gameMode is GameMode.VsAI && current.turn == Player.BLACK) return

        soundManager.playDiceRoll()
        val rolled = BackgammonEngine.rollDice(current)
        _boardState.value = rolled

        checkTurnAndTriggerAI(rolled)
    }

    fun onPointClicked(point: Int) {
        val current = _boardState.value
        if (current.phase != GamePhase.MOVE_CHECKERS || current.winner != null) return
        if (gameMode is GameMode.VsAI && current.turn == Player.BLACK) return

        val selected = current.selectedPoint
        val player = current.turn

        if (selected != null) {
            // Check if tapped point is a valid move destination
            val move = current.legalMovesForSelected.firstOrNull { it.toPoint == point }
            if (move != null) {
                _ruleViolationMessage.value = null
                executeMove(move)
                return
            } else {
                // Diagnose reason using MoveValidationService
                val validation = MoveValidationService.validateMove(current, selected, point)
                if (validation is MoveValidationResult.Invalid) {
                    _ruleViolationMessage.value = validation.messageAr
                }
            }
        }

        // Otherwise, try to select checkers at point
        val count = current.points[point]
        val isCurrentPlayerPiece = (player == Player.WHITE && count > 0) || (player == Player.BLACK && count < 0)

        // Cannot select regular point if player has checkers on the bar!
        val hasCheckersOnBar = (player == Player.WHITE && current.barWhite > 0) || (player == Player.BLACK && current.barBlack > 0)
        if (hasCheckersOnBar) {
            _ruleViolationMessage.value = "يجب إلزامياً إدخال القطع من الحاجز (Bar) أولاً!"
            return
        }

        if (isCurrentPlayerPiece) {
            val legal = BackgammonEngine.getLegalMovesForPoint(current, point)
            _boardState.value = current.copy(
                selectedPoint = point,
                legalMovesForSelected = legal
            )
            _ruleViolationMessage.value = null
            soundManager.playButtonClick()
        } else {
            // Deselect
            _boardState.value = current.copy(
                selectedPoint = null,
                legalMovesForSelected = emptyList()
            )
        }
    }

    fun onBarClicked(player: Player) {
        val current = _boardState.value
        if (current.phase != GamePhase.MOVE_CHECKERS || current.winner != null) return
        if (gameMode is GameMode.VsAI && current.turn == Player.BLACK) return
        if (current.turn != player) return

        val barCount = if (player == Player.WHITE) current.barWhite else current.barBlack
        if (barCount <= 0) return

        val legal = BackgammonEngine.getLegalMovesForPoint(current, Move.BAR_POINT)
        _boardState.value = current.copy(
            selectedPoint = Move.BAR_POINT,
            legalMovesForSelected = legal
        )
        _ruleViolationMessage.value = null
        soundManager.playButtonClick()
    }

    fun onBearOffTrayClicked() {
        val current = _boardState.value
        if (current.phase != GamePhase.MOVE_CHECKERS || current.winner != null) return
        val selected = current.selectedPoint ?: return

        val move = current.legalMovesForSelected.firstOrNull { it.toPoint == Move.OFF_POINT }
        if (move != null) {
            _ruleViolationMessage.value = null
            executeMove(move)
        } else {
            val validation = MoveValidationService.validateMove(current, selected, Move.OFF_POINT)
            if (validation is MoveValidationResult.Invalid) {
                _ruleViolationMessage.value = validation.messageAr
            }
        }
    }

    private fun executeMove(move: Move) {
        val prev = _boardState.value
        if (move.isHit) {
            soundManager.playHitPiece()
        } else {
            soundManager.playMovePiece()
        }

        // Record for 3D Game Replay
        val stepNum = recordedSteps.size + 1
        val playerName = if (prev.turn == Player.WHITE) "أنت" else "الذكاء الاصطناعي"
        val comment = when {
            move.isBearingOff -> "إخراج قطعة نحو الخارج (Bearing Off) بالنرد [${move.dieUsed}]."
            move.isHit -> "صيد بلطة الخصم على النقطة ${move.toPoint} بالنرد [${move.dieUsed}]!"
            move.fromPoint == Move.BAR_POINT -> "إعادة الدخول من الحاجز إلى النقطة ${move.toPoint} بالنرد [${move.dieUsed}]."
            else -> "تحريك قطعة من النقطة ${move.fromPoint} إلى ${move.toPoint} بالنرد [${move.dieUsed}]."
        }
        recordedSteps.add(
            ReplayStep(
                stepIndex = stepNum,
                player = playerName,
                commentary = comment,
                dice = listOf(move.dieUsed),
                fromPoint = move.fromPoint,
                toPoint = move.toPoint,
                isHit = move.isHit,
                isBearOff = move.isBearingOff
            )
        )

        val nextState = BackgammonEngine.applyMove(prev, move)
        _boardState.value = nextState

        if (nextState.winner != null) {
            handleGameOver(nextState)
        } else {
            checkTurnAndTriggerAI(nextState)
        }
    }

    private fun checkTurnAndTriggerAI(state: BoardState) {
        if (gameMode is GameMode.VsAI && state.turn == Player.BLACK && state.winner == null) {
            triggerAITurn(gameMode.difficulty)
        }
    }

    private fun triggerAITurn(difficulty: AIDifficulty) {
        viewModelScope.launch {
            _isAiThinking.value = true
            delay(difficulty.thinkingTimeMs)

            var state = _boardState.value
            if (state.turn != Player.BLACK || state.winner != null) {
                _isAiThinking.value = false
                return@launch
            }

            // 1. AI rolls dice if in ROLL_DICE phase
            if (state.phase == GamePhase.ROLL_DICE) {
                soundManager.playDiceRoll()
                state = BackgammonEngine.rollDice(state)
                _boardState.value = state
                delay(900)
            }

            // 2. AI executes moves sequentially
            while (state.turn == Player.BLACK && state.phase == GamePhase.MOVE_CHECKERS && state.winner == null) {
                val bestMove = BackgammonAI.getBestMove(state, difficulty)
                if (bestMove == null) {
                    // No legal moves left, turn passes
                    break
                }

                delay(650)
                if (bestMove.isHit) {
                    soundManager.playHitPiece()
                } else {
                    soundManager.playMovePiece()
                }

                state = BackgammonEngine.applyMove(state, bestMove)
                _boardState.value = state

                if (state.winner != null) {
                    handleGameOver(state)
                    break
                }
            }

            _isAiThinking.value = false
        }
    }

    private fun handleGameOver(state: BoardState) {
        val userWon = state.winner == Player.WHITE
        if (userWon) {
            soundManager.playWin()
            _ratingChange.value = 24
            _coinsEarned.value = 150
            authRepository.updateStats(won = true, ratingChange = 24)
        } else {
            soundManager.playLose()
            _ratingChange.value = -18
            _coinsEarned.value = 30
            authRepository.updateStats(won = false, ratingChange = -18)
        }

        // Save into GameHistoryManager
        val opponent = when (gameMode) {
            is GameMode.VsAI -> "الذكاء الاصطناعي (${gameMode.difficulty.label})"
            is GameMode.Local2Player -> "لاعب محلي 2"
            else -> "منافس أونلاين"
        }
        GameHistoryManager.saveCompletedGame(
            gameType = GameType.BACKGAMMON,
            title = if (userWon) "فوز ملحمي في طاولة الزهر 🏆" else "مباراة طاولة قوية",
            opponentName = opponent,
            isWin = userWon,
            finalScore = "${state.offWhite} - ${state.offBlack}",
            steps = recordedSteps.toList()
        )
    }

    fun restartGame() {
        _boardState.value = BackgammonEngine.createInitialBoard()
        _isAiThinking.value = false
        _ratingChange.value = 0
        _coinsEarned.value = 0
    }

    fun undoTurnMove() {
        val current = _boardState.value
        val turnStart = current.turnStartState
        if (turnStart != null && current.turn == Player.WHITE && !_isAiThinking.value) {
            _boardState.value = turnStart
            soundManager.playMovePiece()
        }
    }

    fun resign(activity: Activity? = null) {
        val current = _boardState.value
        if (current.winner != null) return

        val opponent = current.turn.opponent()
        val ended = current.copy(
            winner = opponent,
            phase = GamePhase.GAME_OVER
        )
        _boardState.value = ended
        handleGameOver(ended)

        activity?.let {
            AdManager.showInterstitial(it) {}
        }
    }
}
