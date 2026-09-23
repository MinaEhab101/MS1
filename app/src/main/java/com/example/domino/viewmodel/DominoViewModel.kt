package com.example.domino.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.domino.engine.DominoEngine
import com.example.domino.model.DominoEnd
import com.example.domino.model.DominoGameMode
import com.example.domino.model.DominoGameState
import com.example.domino.model.DominoTile
import com.example.firebase.AuthRepository
import com.example.firebase.FirestoreRepository
import com.example.game.AIDifficulty
import com.example.game.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DominoViewModel(
    val soundManager: SoundManager? = null,
    val authRepository: AuthRepository? = null,
    val firestoreRepository: FirestoreRepository? = null
) : ViewModel() {

    private val _gameState = MutableStateFlow(DominoEngine.startNewGame())
    val gameState: StateFlow<DominoGameState> = _gameState.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    init {
        checkAiTurn()
    }

    fun startNewGame(
        mode: DominoGameMode = _gameState.value.mode,
        difficulty: AIDifficulty = _gameState.value.aiDifficulty,
        opponentName: String? = null,
        opponentAvatar: String? = null,
        opponentCountry: String? = null
    ) {
        val resolvedName = opponentName ?: when (mode) {
            DominoGameMode.VS_AI -> if (difficulty == AIDifficulty.HARD) "Master AI King" else "Grand AI King"
            DominoGameMode.ONLINE -> "Ahmed_123"
            DominoGameMode.PASS_AND_PLAY -> "Player 2"
            DominoGameMode.PRIVATE_ROOM -> "Room Guest"
        }

        val resolvedAvatar = opponentAvatar ?: when (mode) {
            DominoGameMode.VS_AI -> "🤖"
            DominoGameMode.ONLINE -> "👑"
            DominoGameMode.PASS_AND_PLAY -> "👤"
            DominoGameMode.PRIVATE_ROOM -> "🔑"
        }

        val resolvedCountry = opponentCountry ?: when (mode) {
            DominoGameMode.ONLINE -> "🇰🇼"
            DominoGameMode.PASS_AND_PLAY -> "🇪🇬"
            else -> "👑"
        }

        _gameState.value = DominoEngine.startNewGame(
            mode = mode,
            aiDifficulty = difficulty,
            opponentName = resolvedName,
            opponentAvatar = resolvedAvatar,
            opponentCountry = resolvedCountry,
            opponentCoins = if (mode == DominoGameMode.ONLINE) 980 else 1250
        )
        checkAiTurn()
    }

    fun onTileSelected(tile: DominoTile) {
        val state = _gameState.value
        if (state.isGameOver || _isAiThinking.value) return

        if (state.selectedTile?.id == tile.id) {
            // Deselect
            _gameState.value = state.copy(selectedTile = null, validEndsForSelected = emptySet())
            return
        }

        val ends = DominoEngine.getValidEndsForTile(tile, state)
        if (ends.isNotEmpty()) {
            soundManager?.playMovePiece()
            _gameState.value = state.copy(selectedTile = tile, validEndsForSelected = ends)

            // If only one valid end or board is empty, auto-play for effortless UX
            if (ends.size == 1) {
                onPlayOnEnd(ends.first())
            }
        }
    }

    fun onPlayOnEnd(end: DominoEnd) {
        val state = _gameState.value
        val tile = state.selectedTile ?: return
        if (state.isGameOver || _isAiThinking.value) return

        soundManager?.playHitPiece() // Authentic heavy domino clack sound
        val updated = DominoEngine.playTile(state, tile, end)
        _gameState.value = updated

        if (updated.isGameOver) {
            if (updated.winner == Player.WHITE) {
                soundManager?.playWin()
                rewardCoins(100)
            } else {
                soundManager?.playLose()
            }
        } else {
            checkAiTurn()
        }
    }

    fun onDrawClicked() {
        val state = _gameState.value
        if (state.isGameOver || _isAiThinking.value) return
        if (state.boneyard.isEmpty()) return

        soundManager?.playMovePiece()
        val updated = DominoEngine.drawFromBoneyard(state)
        _gameState.value = updated

        if (updated.isGameOver) {
            soundManager?.playWin()
        }
    }

    fun onPassClicked() {
        val state = _gameState.value
        if (state.isGameOver || _isAiThinking.value) return

        soundManager?.playButtonClick()
        val updated = DominoEngine.passTurn(state)
        _gameState.value = updated

        if (updated.isGameOver) {
            if (updated.winner == Player.WHITE) {
                soundManager?.playWin()
                rewardCoins(75)
            } else {
                soundManager?.playLose()
            }
        } else {
            checkAiTurn()
        }
    }

    fun revealPassPlayHand() {
        val state = _gameState.value
        _gameState.value = state.copy(isPassPlayHidden = false)
    }

    private fun rewardCoins(amount: Int) {
        viewModelScope.launch {
            authRepository?.addCoins(amount)
            val profile = authRepository?.currentUser?.value
            if (profile != null) {
                firestoreRepository?.updateUserCoins(profile.userId, profile.coins + amount)
            }
        }
    }

    private fun checkAiTurn() {
        val state = _gameState.value
        // Only run AI if mode is VS_AI or ONLINE (simulated live opponent)
        val isOpponentAi = state.mode == DominoGameMode.VS_AI || state.mode == DominoGameMode.ONLINE

        if (!state.isGameOver && state.turn == Player.BLACK && isOpponentAi) {
            _isAiThinking.value = true
            viewModelScope.launch {
                val delayTime = if (state.mode == DominoGameMode.ONLINE) 1300L else 900L
                delay(delayTime)

                var currentState = _gameState.value
                var aiMove = DominoEngine.computeAiMove(currentState)

                // If no move, AI draws from boneyard until it finds one or bank is empty
                while (aiMove == null && currentState.boneyard.isNotEmpty()) {
                    delay(500)
                    soundManager?.playMovePiece()
                    currentState = DominoEngine.drawFromBoneyard(currentState)
                    _gameState.value = currentState
                    if (currentState.isGameOver) {
                        _isAiThinking.value = false
                        return@launch
                    }
                    aiMove = DominoEngine.computeAiMove(currentState)
                }

                if (aiMove != null) {
                    soundManager?.playHitPiece()
                    val (tile, end) = aiMove
                    val afterPlay = DominoEngine.playTile(currentState, tile, end)
                    _gameState.value = afterPlay
                    if (afterPlay.isGameOver) {
                        if (afterPlay.winner == Player.BLACK) {
                            soundManager?.playLose()
                        } else {
                            soundManager?.playWin()
                            rewardCoins(100)
                        }
                    }
                } else {
                    // Pass turn
                    val afterPass = DominoEngine.passTurn(currentState)
                    _gameState.value = afterPass
                    if (afterPass.isGameOver) {
                        if (afterPass.winner == Player.BLACK) soundManager?.playLose() else soundManager?.playWin()
                    }
                }
                _isAiThinking.value = false
            }
        }
    }
}
