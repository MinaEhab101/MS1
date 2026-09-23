package com.example.domino.engine

import com.example.domino.model.DominoEnd
import com.example.domino.model.DominoGameMode
import com.example.domino.model.DominoGameState
import com.example.domino.model.DominoTile
import com.example.domino.model.PlacedTile
import com.example.game.AIDifficulty
import com.example.game.Player
import kotlin.random.Random

object DominoEngine {

    const val TILE_LENGTH = 96f
    const val TILE_WIDTH = 48f
    const val TILE_GAP = 6f
    const val MAX_HORIZONTAL_EXTENT = 260f

    fun generateFullSet(): List<DominoTile> {
        val tiles = mutableListOf<DominoTile>()
        var id = 1
        for (i in 0..6) {
            for (j in i..6) {
                tiles.add(DominoTile(id = id++, left = i, right = j))
            }
        }
        return tiles
    }

    fun startNewGame(
        mode: DominoGameMode = DominoGameMode.VS_AI,
        aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
        opponentName: String = if (mode == DominoGameMode.PASS_AND_PLAY) "Player 2" else "Grand AI King",
        opponentAvatar: String = if (mode == DominoGameMode.PASS_AND_PLAY) "👤" else "🤖",
        opponentCountry: String = if (mode == DominoGameMode.PASS_AND_PLAY) "🇪🇬" else "👑",
        opponentCoins: Int = 1200
    ): DominoGameState {
        val deck = generateFullSet().shuffled(Random(System.currentTimeMillis()))
        val p1Hand = deck.subList(0, 7).toMutableList()
        val p2Hand = deck.subList(7, 14).toMutableList()
        val boneyard = deck.subList(14, 28).toMutableList()

        // Find starting player: player with highest double
        var startingPlayer = Player.WHITE
        var highestDouble = -1

        p1Hand.filter { it.isDouble }.forEach {
            if (it.left > highestDouble) {
                highestDouble = it.left
                startingPlayer = Player.WHITE
            }
        }

        p2Hand.filter { it.isDouble }.forEach {
            if (it.left > highestDouble) {
                highestDouble = it.left
                startingPlayer = Player.BLACK
            }
        }

        // If neither has doubles, compare highest pip tiles
        if (highestDouble == -1) {
            val p1Max = p1Hand.maxOfOrNull { it.totalPips } ?: 0
            val p2Max = p2Hand.maxOfOrNull { it.totalPips } ?: 0
            startingPlayer = if (p1Max >= p2Max) Player.WHITE else Player.BLACK
        }

        return DominoGameState(
            playerHand = p1Hand,
            opponentHandCount = p2Hand.size,
            opponentHand = p2Hand,
            boneyard = boneyard,
            playedTiles = emptyList(),
            leftOpenEnd = null,
            rightOpenEnd = null,
            turn = startingPlayer,
            mode = mode,
            aiDifficulty = aiDifficulty,
            opponentName = opponentName,
            opponentAvatar = opponentAvatar,
            opponentCountry = opponentCountry,
            opponentCoins = opponentCoins,
            isPassPlayHidden = (mode == DominoGameMode.PASS_AND_PLAY && startingPlayer == Player.BLACK)
        )
    }

    fun getValidEndsForTile(tile: DominoTile, state: DominoGameState): Set<DominoEnd> {
        if (state.playedTiles.isEmpty()) {
            return setOf(DominoEnd.LEFT, DominoEnd.RIGHT)
        }
        val result = mutableSetOf<DominoEnd>()
        val l = state.leftOpenEnd
        val r = state.rightOpenEnd

        if (l != null && tile.matches(l)) {
            result.add(DominoEnd.LEFT)
        }
        if (r != null && tile.matches(r)) {
            result.add(DominoEnd.RIGHT)
        }
        return result
    }

    fun hasAnyValidMove(hand: List<DominoTile>, state: DominoGameState): Boolean {
        if (state.playedTiles.isEmpty()) return hand.isNotEmpty()
        val l = state.leftOpenEnd ?: return false
        val r = state.rightOpenEnd ?: return false
        return hand.any { it.matches(l) || it.matches(r) }
    }

    fun playTile(
        state: DominoGameState,
        tile: DominoTile,
        end: DominoEnd
    ): DominoGameState {
        val isWhite = state.turn == Player.WHITE
        val currentHand = if (isWhite) state.playerHand.toMutableList() else state.opponentHand.toMutableList()
        val foundIndex = currentHand.indexOfFirst { it.id == tile.id }
        if (foundIndex != -1) {
            currentHand.removeAt(foundIndex)
        }

        val playedList = state.playedTiles.toMutableList()
        var newLeftEnd = state.leftOpenEnd
        var newRightEnd = state.rightOpenEnd

        if (playedList.isEmpty()) {
            // First tile at logical origin (0, 0)
            val isVert = tile.isDouble
            playedList.add(
                PlacedTile(
                    tile = tile,
                    x = 0f,
                    y = 0f,
                    isVertical = isVert,
                    isFlipped = false,
                    rotationDegrees = if (isVert) 90f else 0f
                )
            )
            newLeftEnd = tile.left
            newRightEnd = tile.right
        } else {
            val isLeftEnd = (end == DominoEnd.LEFT)
            val openVal = if (isLeftEnd) state.leftOpenEnd!! else state.rightOpenEnd!!

            val orientedTile = if (isLeftEnd) {
                if (tile.right == openVal) tile else tile.flipped()
            } else {
                if (tile.left == openVal) tile else tile.flipped()
            }

            val nextOpen = if (isLeftEnd) orientedTile.left else orientedTile.right
            if (isLeftEnd) {
                newLeftEnd = nextOpen
            } else {
                newRightEnd = nextOpen
            }

            // Calculate serpentine coordinates
            val placed = calculateNextPlacement(playedList, orientedTile, isLeftEnd)
            if (isLeftEnd) {
                playedList.add(0, placed)
            } else {
                playedList.add(placed)
            }
        }

        val nextPlayer = if (state.turn == Player.WHITE) Player.BLACK else Player.WHITE
        val p1Hand = if (isWhite) currentHand else state.playerHand
        val p2Hand = if (!isWhite) currentHand else state.opponentHand

        // Check victory
        if (currentHand.isEmpty()) {
            val opponentPips = (if (isWhite) p2Hand else p1Hand).sumOf { it.totalPips }
            return state.copy(
                playerHand = p1Hand,
                opponentHand = p2Hand,
                opponentHandCount = p2Hand.size,
                playedTiles = playedList,
                leftOpenEnd = newLeftEnd,
                rightOpenEnd = newRightEnd,
                selectedTile = null,
                validEndsForSelected = emptySet(),
                isGameOver = true,
                winner = state.turn,
                winReason = if (isWhite) "DOMINO! (دومينو) - You played all your tiles!" else "DOMINO! (دومينو) - Opponent played all tiles!",
                playerScore = if (isWhite) state.playerScore + opponentPips else state.playerScore,
                opponentScore = if (!isWhite) state.opponentScore + opponentPips else state.opponentScore
            )
        }

        // Check blocked game
        val newState = state.copy(
            playerHand = p1Hand,
            opponentHand = p2Hand,
            opponentHandCount = p2Hand.size,
            playedTiles = playedList,
            leftOpenEnd = newLeftEnd,
            rightOpenEnd = newRightEnd,
            turn = nextPlayer,
            selectedTile = null,
            validEndsForSelected = emptySet(),
            isPassPlayHidden = (state.mode == DominoGameMode.PASS_AND_PLAY)
        )

        return checkBlockedOrContinue(newState)
    }

    private fun calculateNextPlacement(
        existingTiles: List<PlacedTile>,
        tile: DominoTile,
        isLeftEnd: Boolean
    ): PlacedTile {
        val stepH = TILE_LENGTH + TILE_GAP
        val stepV = TILE_LENGTH + TILE_GAP

        if (isLeftEnd) {
            // Heading towards left (negative X, then snake up into negative Y)
            val ref = existingTiles.first()
            val isRefAtLeftBound = ref.x <= -MAX_HORIZONTAL_EXTENT
            val isRefAtRightBound = ref.x >= MAX_HORIZONTAL_EXTENT

            // Check previous direction
            val prevTile = existingTiles.getOrNull(1)
            val isMovingRight = prevTile != null && ref.x > prevTile.x

            return when {
                !isMovingRight && isRefAtLeftBound -> {
                    // Turn upwards!
                    PlacedTile(
                        tile = tile,
                        x = ref.x,
                        y = ref.y - stepV,
                        isVertical = !tile.isDouble,
                        isFlipped = false
                    )
                }
                isMovingRight && isRefAtRightBound -> {
                    // Turn upwards again!
                    PlacedTile(
                        tile = tile,
                        x = ref.x,
                        y = ref.y - stepV,
                        isVertical = !tile.isDouble,
                        isFlipped = false
                    )
                }
                isMovingRight -> {
                    // Moving rightwards in upper loop
                    PlacedTile(
                        tile = tile,
                        x = ref.x + stepH,
                        y = ref.y,
                        isVertical = tile.isDouble,
                        isFlipped = false
                    )
                }
                else -> {
                    // Default: moving leftwards
                    PlacedTile(
                        tile = tile,
                        x = ref.x - stepH,
                        y = ref.y,
                        isVertical = tile.isDouble,
                        isFlipped = false
                    )
                }
            }
        } else {
            // Heading towards right (positive X, then snake down into positive Y)
            val ref = existingTiles.last()
            val isRefAtRightBound = ref.x >= MAX_HORIZONTAL_EXTENT
            val isRefAtLeftBound = ref.x <= -MAX_HORIZONTAL_EXTENT

            val prevTile = existingTiles.getOrNull(existingTiles.size - 2)
            val isMovingLeft = prevTile != null && ref.x < prevTile.x

            return when {
                !isMovingLeft && isRefAtRightBound -> {
                    // Turn downwards!
                    PlacedTile(
                        tile = tile,
                        x = ref.x,
                        y = ref.y + stepV,
                        isVertical = !tile.isDouble,
                        isFlipped = false
                    )
                }
                isMovingLeft && isRefAtLeftBound -> {
                    // Turn downwards again!
                    PlacedTile(
                        tile = tile,
                        x = ref.x,
                        y = ref.y + stepV,
                        isVertical = !tile.isDouble,
                        isFlipped = false
                    )
                }
                isMovingLeft -> {
                    // Moving leftwards in lower loop
                    PlacedTile(
                        tile = tile,
                        x = ref.x - stepH,
                        y = ref.y,
                        isVertical = tile.isDouble,
                        isFlipped = false
                    )
                }
                else -> {
                    // Default: moving rightwards
                    PlacedTile(
                        tile = tile,
                        x = ref.x + stepH,
                        y = ref.y,
                        isVertical = tile.isDouble,
                        isFlipped = false
                    )
                }
            }
        }
    }

    fun drawFromBoneyard(state: DominoGameState): DominoGameState {
        if (state.boneyard.isEmpty()) return state
        val boneyard = state.boneyard.toMutableList()
        val drawn = boneyard.removeAt(0)

        val isWhite = state.turn == Player.WHITE
        val newPlayerHand = if (isWhite) state.playerHand + drawn else state.playerHand
        val newOpponentHand = if (!isWhite) state.opponentHand + drawn else state.opponentHand

        val updatedState = state.copy(
            boneyard = boneyard,
            playerHand = newPlayerHand,
            opponentHand = newOpponentHand,
            opponentHandCount = newOpponentHand.size
        )

        return checkBlockedOrContinue(updatedState)
    }

    fun passTurn(state: DominoGameState): DominoGameState {
        val nextPlayer = if (state.turn == Player.WHITE) Player.BLACK else Player.WHITE
        val updated = state.copy(
            turn = nextPlayer,
            selectedTile = null,
            validEndsForSelected = emptySet(),
            isPassPlayHidden = (state.mode == DominoGameMode.PASS_AND_PLAY)
        )
        return checkBlockedOrContinue(updated)
    }

    private fun checkBlockedOrContinue(state: DominoGameState): DominoGameState {
        val p1CanMove = hasAnyValidMove(state.playerHand, state)
        val p2CanMove = hasAnyValidMove(state.opponentHand, state)

        if (!p1CanMove && !p2CanMove && state.boneyard.isEmpty()) {
            // Blocked Game (قفلة)
            val p1Pips = state.playerHand.sumOf { it.totalPips }
            val p2Pips = state.opponentHand.sumOf { it.totalPips }

            val winner = when {
                p1Pips < p2Pips -> Player.WHITE
                p2Pips < p1Pips -> Player.BLACK
                else -> null // Draw
            }

            val diff = kotlin.math.abs(p1Pips - p2Pips)

            return state.copy(
                isGameOver = true,
                isBlockedGame = true,
                winner = winner,
                winReason = if (winner != null) {
                    val winnerName = if (winner == Player.WHITE) "You Won" else state.opponentName
                    "BLOCKED GAME (قفلة)! $winnerName with lowest pip count ($p1Pips vs $p2Pips)"
                } else "BLOCKED GAME (قفلة) - Tied pip count!",
                playerScore = if (winner == Player.WHITE) state.playerScore + diff else state.playerScore,
                opponentScore = if (winner == Player.BLACK) state.opponentScore + diff else state.opponentScore
            )
        }

        return state
    }

    fun computeAiMove(state: DominoGameState): Pair<DominoTile, DominoEnd>? {
        val validMoves = mutableListOf<Pair<DominoTile, DominoEnd>>()

        state.opponentHand.forEach { tile ->
            val ends = getValidEndsForTile(tile, state)
            ends.forEach { end ->
                validMoves.add(Pair(tile, end))
            }
        }

        if (validMoves.isEmpty()) return null

        // Strategy based on difficulty:
        return when (state.aiDifficulty) {
            AIDifficulty.EASY -> validMoves.random()
            AIDifficulty.MEDIUM -> {
                // Prioritize doubles, else highest pips
                validMoves.maxWithOrNull(
                    compareBy<Pair<DominoTile, DominoEnd>> { it.first.isDouble }
                        .thenBy { it.first.totalPips }
                ) ?: validMoves.first()
            }
            AIDifficulty.HARD, AIDifficulty.EXPERT -> {
                // Strategic block & pip reduction: play heaviest tiles first
                validMoves.maxByOrNull { it.first.totalPips + (if (it.first.isDouble) 5 else 0) } ?: validMoves.first()
            }
        }
    }
}
