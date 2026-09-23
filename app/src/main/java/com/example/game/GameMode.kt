package com.example.game

enum class AIDifficulty(val label: String, val thinkingTimeMs: Long) {
    EASY("Easy", 700L),
    MEDIUM("Medium", 900L),
    HARD("Hard", 1100L),
    EXPERT("Expert", 1300L)
}

sealed class GameMode {
    data object Local2Player : GameMode()
    data class VsAI(val difficulty: AIDifficulty = AIDifficulty.MEDIUM) : GameMode()
    data class OnlineMatch(val matchId: String, val isHost: Boolean) : GameMode()
    data class PrivateRoom(val roomCode: String, val isHost: Boolean) : GameMode()
}
