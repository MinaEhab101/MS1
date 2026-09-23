package com.example.game

enum class Player {
    WHITE,
    BLACK;

    fun opponent(): Player = if (this == WHITE) BLACK else WHITE

    /** Movement direction on the 1..24 point scale: WHITE moves 24 -> 1 (-1), BLACK moves 1 -> 24 (+1) */
    val direction: Int get() = if (this == WHITE) -1 else 1

    /** Home board points range: White is 1..6, Black is 19..24 */
    val homeRange: IntRange get() = if (this == WHITE) 1..6 else 19..24

    /** Outer board points range: White is 7..12, Black is 13..18 */
    val outerRange: IntRange get() = if (this == WHITE) 7..12 else 13..18

    /** Opponent outer board points range */
    val opponentOuterRange: IntRange get() = if (this == WHITE) 13..18 else 7..12

    /** Opponent home board points range where checkers from bar re-enter */
    val opponentHomeRange: IntRange get() = if (this == WHITE) 19..24 else 1..6

    /** Calculate point where a checker from the bar enters for a given die roll (1..6) */
    fun barEntryTarget(die: Int): Int = if (this == WHITE) 25 - die else die

    /** Point number for bearing off with exact die roll */
    fun bearOffSource(die: Int): Int = if (this == WHITE) die else 25 - die

    /** Distance from point to bearing off tray */
    fun distanceToOff(point: Int): Int = if (this == WHITE) point else 25 - point

    /** True if given point is in player's home board */
    fun isHomePoint(point: Int): Boolean = point in homeRange
}

enum class GamePhase {
    ROLL_DICE,
    MOVE_CHECKERS,
    DOUBLING_OFFERED,
    GAME_OVER
}

enum class WinType(val pointsMultiplier: Int, val displayName: String) {
    SINGLE(1, "Single"),
    GAMMON(2, "Gammon"),
    BACKGAMMON(3, "Backgammon");

    companion object {
        fun fromPointsMultiplier(multiplier: Int): WinType = when (multiplier) {
            3 -> BACKGAMMON
            2 -> GAMMON
            else -> SINGLE
        }
    }
}
