package com.example.game

import kotlin.math.abs

/**
 * Immutable snapshot of the Backgammon board.
 * points[1..24]: positive for White checkers count, negative for Black checkers count, 0 if empty.
 * White moves towards 1 (bears off 1..6).
 * Black moves towards 24 (bears off 19..24).
 */
data class BoardState(
    val points: IntArray = IntArray(25),
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val offWhite: Int = 0,
    val offBlack: Int = 0,
    val turn: Player = Player.WHITE,
    val dice: List<Int> = emptyList(),
    val initialDice: List<Int> = emptyList(),
    val lastRoll: Pair<Int, Int>? = null,
    val phase: GamePhase = GamePhase.ROLL_DICE,
    val winner: Player? = null,
    val winType: WinType? = null,
    val cubeValue: Int = 1,
    val cubeOwner: Player? = null,
    val doubler: Player? = null,
    val selectedPoint: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val moveHistory: List<Move> = emptyList(),
    val turnMoves: List<Move> = emptyList(),
    val turnStartState: BoardState? = null
) {
    fun deepCopy(): BoardState = copy(
        points = points.copyOf(),
        dice = ArrayList(dice),
        initialDice = ArrayList(initialDice),
        legalMovesForSelected = ArrayList(legalMovesForSelected),
        moveHistory = ArrayList(moveHistory),
        turnMoves = ArrayList(turnMoves)
    )

    fun bar(player: Player): Int = if (player == Player.WHITE) barWhite else barBlack
    fun off(player: Player): Int = if (player == Player.WHITE) offWhite else offBlack
    fun hasCheckersOnBar(player: Player): Boolean = bar(player) > 0

    fun totalCheckers(player: Player): Int {
        var count = bar(player) + off(player)
        for (p in 1..24) {
            val v = points[p]
            if (player == Player.WHITE && v > 0) count += v
            if (player == Player.BLACK && v < 0) count += (-v)
        }
        return count
    }

    fun getCheckerCount(point: Int): Int {
        if (point !in 1..24) return 0
        return abs(points[point])
    }

    fun calculateWhitePipCount(): Int {
        var pips = barWhite * 25
        for (i in 1..24) {
            if (points[i] > 0) {
                pips += points[i] * i
            }
        }
        return pips
    }

    fun calculateBlackPipCount(): Int {
        var pips = barBlack * 25
        for (i in 1..24) {
            if (points[i] < 0) {
                pips += (-points[i]) * (25 - i)
            }
        }
        return pips
    }

    fun getPlayerAt(point: Int): Player? {
        if (point !in 1..24) return null
        val v = points[point]
        return when {
            v > 0 -> Player.WHITE
            v < 0 -> Player.BLACK
            else -> null
        }
    }

    /**
     * A point is blocked if the opponent has 2 or more checkers on it.
     */
    fun isPointBlocked(point: Int, forPlayer: Player): Boolean {
        if (point !in 1..24) return true
        val v = points[point]
        return if (forPlayer == Player.WHITE) v <= -2 else v >= 2
    }

    /**
     * A point is open if it has 0 checkers, own checkers, or at most 1 opponent checker (a blot).
     */
    fun isPointOpen(point: Int, forPlayer: Player): Boolean = !isPointBlocked(point, forPlayer)

    /**
     * A point has an opponent blot if it contains exactly 1 opponent checker.
     */
    fun isBlot(point: Int, forPlayer: Player): Boolean {
        if (point !in 1..24) return false
        val v = points[point]
        return if (forPlayer == Player.WHITE) v == -1 else v == 1
    }

    /**
     * Bearing off is legal only when all 15 active checkers of the player are in their home board,
     * and no checkers are on the bar.
     */
    fun canBearOff(player: Player): Boolean {
        if (hasCheckersOnBar(player)) return false
        return if (player == Player.WHITE) {
            // White home board is 1..6. Any checkers in 7..24 blocks bearing off.
            (7..24).none { points[it] > 0 }
        } else {
            // Black home board is 19..24. Any checkers in 1..18 blocks bearing off.
            (1..18).none { points[it] < 0 }
        }
    }

    /**
     * Returns true if an intermediate move within the current turn can be undone.
     */
    fun canUndo(): Boolean = turnMoves.isNotEmpty() && turnStartState != null && phase == GamePhase.MOVE_CHECKERS

    /**
     * Returns true if [player] is allowed to offer a double at the current state.
     */
    fun canOfferDouble(player: Player): Boolean {
        if (phase != GamePhase.ROLL_DICE || winner != null) return false
        if (turn != player) return false
        if (cubeValue >= 64) return false
        // Can double if cube is centered (unowned) or currently owned by player
        return cubeOwner == null || cubeOwner == player
    }

    /**
     * Pip count calculation: total distance all remaining checkers must travel to bear off.
     */
    fun pipCount(player: Player): Int {
        var count = 0
        if (player == Player.WHITE) {
            count += barWhite * 25
            for (p in 1..24) {
                if (points[p] > 0) {
                    count += points[p] * p
                }
            }
        } else {
            count += barBlack * 25
            for (p in 1..24) {
                if (points[p] < 0) {
                    count += (-points[p]) * (25 - p)
                }
            }
        }
        return count
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardState) return false
        if (!points.contentEquals(other.points)) return false
        if (barWhite != other.barWhite) return false
        if (barBlack != other.barBlack) return false
        if (offWhite != other.offWhite) return false
        if (offBlack != other.offBlack) return false
        if (turn != other.turn) return false
        if (dice != other.dice) return false
        if (initialDice != other.initialDice) return false
        if (lastRoll != other.lastRoll) return false
        if (phase != other.phase) return false
        if (winner != other.winner) return false
        if (winType != other.winType) return false
        if (cubeValue != other.cubeValue) return false
        if (cubeOwner != other.cubeOwner) return false
        if (doubler != other.doubler) return false
        if (selectedPoint != other.selectedPoint) return false
        return true
    }

    override fun hashCode(): Int {
        var result = points.contentHashCode()
        result = 31 * result + barWhite
        result = 31 * result + barBlack
        result = 31 * result + offWhite
        result = 31 * result + offBlack
        result = 31 * result + turn.hashCode()
        result = 31 * result + dice.hashCode()
        result = 31 * result + initialDice.hashCode()
        result = 31 * result + (lastRoll?.hashCode() ?: 0)
        result = 31 * result + phase.hashCode()
        result = 31 * result + (winner?.hashCode() ?: 0)
        result = 31 * result + (winType?.hashCode() ?: 0)
        result = 31 * result + cubeValue
        result = 31 * result + (cubeOwner?.hashCode() ?: 0)
        result = 31 * result + (doubler?.hashCode() ?: 0)
        return result
    }
}
