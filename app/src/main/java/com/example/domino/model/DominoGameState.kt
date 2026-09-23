package com.example.domino.model

import com.example.game.AIDifficulty
import com.example.game.Player

enum class DominoGameMode {
    VS_AI,
    ONLINE,
    PRIVATE_ROOM,
    PASS_AND_PLAY
}

data class DominoGameState(
    val playerHand: List<DominoTile> = emptyList(),
    val opponentHandCount: Int = 7,
    val opponentHand: List<DominoTile> = emptyList(), // Only visible in 2 Players Pass & Play mode or debug
    val boneyard: List<DominoTile> = emptyList(),
    val playedTiles: List<PlacedTile> = emptyList(),
    val leftOpenEnd: Int? = null,
    val rightOpenEnd: Int? = null,
    val turn: Player = Player.WHITE, // WHITE = Player 1, BLACK = AI / Opponent / Player 2
    val selectedTile: DominoTile? = null,
    val validEndsForSelected: Set<DominoEnd> = emptySet(),
    val isGameOver: Boolean = false,
    val winner: Player? = null,
    val winReason: String = "",
    val playerScore: Int = 0,
    val opponentScore: Int = 0,
    val mode: DominoGameMode = DominoGameMode.VS_AI,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val opponentName: String = "Grand AI King",
    val opponentAvatar: String = "🤖",
    val opponentCountry: String = "👑",
    val opponentCoins: Int = 1200,
    val isBlockedGame: Boolean = false,
    val isPassPlayHidden: Boolean = false,
    val onlineStatusText: String? = null
) {
    val isPassPlay: Boolean get() = mode == DominoGameMode.PASS_AND_PLAY
}
