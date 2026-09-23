package com.example.game

/**
 * Represents a single checker movement in Backgammon.
 * @param fromPoint 1..24, or [BAR_POINT] (0) if entering from the bar.
 * @param toPoint 1..24, or [OFF_POINT] (-1) if bearing off.
 * @param dieUsed The die face value used for this move (1..6).
 * @param isHit Whether an opponent single blot on destination was hit to the bar.
 */
data class Move(
    val fromPoint: Int,
    val toPoint: Int,
    val dieUsed: Int,
    val isHit: Boolean = false
) {
    val isFromBar: Boolean get() = fromPoint == BAR_POINT
    val isBearingOff: Boolean get() = toPoint == OFF_POINT

    /**
     * Standard Backgammon move notation: e.g. "24/18", "bar/20*", "6/off".
     */
    fun toNotation(): String {
        val fromStr = if (isFromBar) "bar" else fromPoint.toString()
        val toStr = if (isBearingOff) "off" else toPoint.toString()
        val hitSuffix = if (isHit) "*" else ""
        return "$fromStr/$toStr$hitSuffix"
    }

    companion object {
        const val BAR_POINT = 0
        const val OFF_POINT = -1
    }
}
