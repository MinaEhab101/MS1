package com.example.domino.model

enum class DominoEnd {
    LEFT,
    RIGHT
}

data class DominoTile(
    val id: Int,
    val left: Int,
    val right: Int
) {
    val isDouble: Boolean get() = left == right
    val totalPips: Int get() = left + right

    fun matches(value: Int): Boolean = left == value || right == value

    fun flipped(): DominoTile = DominoTile(id, right, left)
}

data class PlacedTile(
    val tile: DominoTile,
    val x: Float,
    val y: Float,
    val isVertical: Boolean,
    val isFlipped: Boolean,
    val rotationDegrees: Float = 0f
)
