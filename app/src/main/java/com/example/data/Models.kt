package com.example.data

import com.example.game.BoardState
import com.example.game.GamePhase
import com.example.game.Move
import com.example.game.Player as GamePlayerColor
import com.example.game.WinType

/**
 * Entity representing an individual Backgammon piece (checker).
 * Compatible with Firebase Firestore and the game engine.
 */
data class Piece(
    val id: String = "",
    val player: String = "WHITE", // "WHITE" or "BLACK"
    val point: Int = 0,           // 0: Bar, 1..24: Point, -1: Off
    val isBar: Boolean = false,
    val isOff: Boolean = false,
    val stackIndex: Int = 0       // 0-indexed position within the point/bar/off stack
) {
    val playerColor: GamePlayerColor
        get() = if (player.equals("BLACK", ignoreCase = true)) GamePlayerColor.BLACK else GamePlayerColor.WHITE

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "player" to player,
        "point" to point,
        "isBar" to isBar,
        "isOff" to isOff,
        "stackIndex" to stackIndex
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Piece = Piece(
            id = (map["id"] as? String) ?: "",
            player = (map["player"] as? String) ?: "WHITE",
            point = ((map["point"] as? Number)?.toInt()) ?: 0,
            isBar = (map["isBar"] as? Boolean) ?: false,
            isOff = (map["isOff"] as? Boolean) ?: false,
            stackIndex = ((map["stackIndex"] as? Number)?.toInt()) ?: 0
        )
    }
}

/**
 * Entity representing a player in a Backgammon match.
 * Compatible with Firebase Firestore and the game engine.
 */
data class Player(
    val id: String = "",
    val name: String = "Player",
    val color: String = "WHITE", // "WHITE" or "BLACK"
    val rating: Int = 1200,
    val avatarIndex: Int = 0,
    val isAi: Boolean = false,
    val isOnline: Boolean = true,
    val pipCount: Int = 167
) {
    val playerColor: GamePlayerColor
        get() = if (color.equals("BLACK", ignoreCase = true)) GamePlayerColor.BLACK else GamePlayerColor.WHITE

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "color" to color,
        "rating" to rating,
        "avatarIndex" to avatarIndex,
        "isAi" to isAi,
        "isOnline" to isOnline,
        "pipCount" to pipCount
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Player = Player(
            id = (map["id"] as? String) ?: "",
            name = (map["name"] as? String) ?: "Player",
            color = (map["color"] as? String) ?: "WHITE",
            rating = ((map["rating"] as? Number)?.toInt()) ?: 1200,
            avatarIndex = ((map["avatarIndex"] as? Number)?.toInt()) ?: 0,
            isAi = (map["isAi"] as? Boolean) ?: false,
            isOnline = (map["isOnline"] as? Boolean) ?: true,
            pipCount = ((map["pipCount"] as? Number)?.toInt()) ?: 167
        )

        fun fromUserProfile(user: UserProfile, color: String = "WHITE", pipCount: Int = 167): Player = Player(
            id = user.userId,
            name = user.username,
            color = color,
            rating = user.rating,
            avatarIndex = user.avatarIndex,
            isAi = false,
            isOnline = true,
            pipCount = pipCount
        )
    }
}

/**
 * Entity representing the full Backgammon board state.
 * Compatible with Firebase Firestore serialization and converts to/from [BoardState].
 */
data class Board(
    // 25 integers: index 0 unused, index 1..24 represents points (+ for White, - for Black)
    val points: List<Int> = List(25) { 0 },
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val offWhite: Int = 0,
    val offBlack: Int = 0,
    val dice: List<Int> = emptyList(),
    val initialDice: List<Int> = emptyList(),
    val turn: String = "WHITE",      // "WHITE" or "BLACK"
    val phase: String = "ROLL_DICE", // "ROLL_DICE", "MOVE_CHECKERS", "DOUBLING_OFFERED", "GAME_OVER"
    val cubeValue: Int = 1,
    val cubeOwner: String? = null,
    val winner: String? = null,
    val winType: String? = null,
    val lastRollD1: Int = 0,
    val lastRollD2: Int = 0,
    val pieces: List<Piece> = emptyList()
) {
    fun toBoardState(): BoardState {
        val pts = IntArray(25)
        for (i in 0 until minOf(25, points.size)) {
            pts[i] = points[i]
        }
        val turnColor = if (turn.equals("BLACK", ignoreCase = true)) GamePlayerColor.BLACK else GamePlayerColor.WHITE
        val phaseEnum = try {
            GamePhase.valueOf(phase)
        } catch (_: Exception) {
            GamePhase.ROLL_DICE
        }
        val winnerColor = when (winner?.uppercase()) {
            "WHITE" -> GamePlayerColor.WHITE
            "BLACK" -> GamePlayerColor.BLACK
            else -> null
        }
        val winTypeEnum = when (winType?.uppercase()) {
            "GAMMON" -> WinType.GAMMON
            "BACKGAMMON" -> WinType.BACKGAMMON
            "SINGLE" -> WinType.SINGLE
            else -> null
        }
        val cubeOwnerColor = when (cubeOwner?.uppercase()) {
            "WHITE" -> GamePlayerColor.WHITE
            "BLACK" -> GamePlayerColor.BLACK
            else -> null
        }
        val lastRoll = if (lastRollD1 > 0 && lastRollD2 > 0) Pair(lastRollD1, lastRollD2) else null

        return BoardState(
            points = pts,
            barWhite = barWhite,
            barBlack = barBlack,
            offWhite = offWhite,
            offBlack = offBlack,
            turn = turnColor,
            dice = dice,
            initialDice = initialDice.ifEmpty { dice },
            lastRoll = lastRoll,
            phase = phaseEnum,
            winner = winnerColor,
            winType = winTypeEnum,
            cubeValue = cubeValue,
            cubeOwner = cubeOwnerColor
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "points" to points,
        "barWhite" to barWhite,
        "barBlack" to barBlack,
        "offWhite" to offWhite,
        "offBlack" to offBlack,
        "dice" to dice,
        "initialDice" to initialDice,
        "turn" to turn,
        "phase" to phase,
        "cubeValue" to cubeValue,
        "cubeOwner" to cubeOwner,
        "winner" to winner,
        "winType" to winType,
        "lastRollD1" to lastRollD1,
        "lastRollD2" to lastRollD2,
        "pieces" to pieces.map { it.toMap() }
    )

    companion object {
        fun fromBoardState(state: BoardState): Board {
            val piecesList = mutableListOf<Piece>()

            // White on bar
            repeat(state.barWhite) { idx ->
                piecesList.add(Piece(id = "w_bar_$idx", player = "WHITE", point = 0, isBar = true, stackIndex = idx))
            }
            // Black on bar
            repeat(state.barBlack) { idx ->
                piecesList.add(Piece(id = "b_bar_$idx", player = "BLACK", point = 0, isBar = true, stackIndex = idx))
            }
            // Points 1..24
            for (pt in 1..24) {
                val count = state.points[pt]
                if (count > 0) {
                    repeat(count) { idx ->
                        piecesList.add(Piece(id = "w_${pt}_$idx", player = "WHITE", point = pt, stackIndex = idx))
                    }
                } else if (count < 0) {
                    repeat(-count) { idx ->
                        piecesList.add(Piece(id = "b_${pt}_$idx", player = "BLACK", point = pt, stackIndex = idx))
                    }
                }
            }
            // White off
            repeat(state.offWhite) { idx ->
                piecesList.add(Piece(id = "w_off_$idx", player = "WHITE", point = -1, isOff = true, stackIndex = idx))
            }
            // Black off
            repeat(state.offBlack) { idx ->
                piecesList.add(Piece(id = "b_off_$idx", player = "BLACK", point = -1, isOff = true, stackIndex = idx))
            }

            return Board(
                points = state.points.toList(),
                barWhite = state.barWhite,
                barBlack = state.barBlack,
                offWhite = state.offWhite,
                offBlack = state.offBlack,
                dice = state.dice,
                initialDice = state.initialDice,
                turn = state.turn.name,
                phase = state.phase.name,
                cubeValue = state.cubeValue,
                cubeOwner = state.cubeOwner?.name,
                winner = state.winner?.name,
                winType = state.winType?.name,
                lastRollD1 = state.lastRoll?.first ?: 0,
                lastRollD2 = state.lastRoll?.second ?: 0,
                pieces = piecesList
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Board = Board(
            points = (map["points"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: List(25) { 0 },
            barWhite = ((map["barWhite"] as? Number)?.toInt()) ?: 0,
            barBlack = ((map["barBlack"] as? Number)?.toInt()) ?: 0,
            offWhite = ((map["offWhite"] as? Number)?.toInt()) ?: 0,
            offBlack = ((map["offBlack"] as? Number)?.toInt()) ?: 0,
            dice = (map["dice"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            initialDice = (map["initialDice"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            turn = (map["turn"] as? String) ?: "WHITE",
            phase = (map["phase"] as? String) ?: "ROLL_DICE",
            cubeValue = ((map["cubeValue"] as? Number)?.toInt()) ?: 1,
            cubeOwner = map["cubeOwner"] as? String,
            winner = map["winner"] as? String,
            winType = map["winType"] as? String,
            lastRollD1 = ((map["lastRollD1"] as? Number)?.toInt()) ?: 0,
            lastRollD2 = ((map["lastRollD2"] as? Number)?.toInt()) ?: 0,
            pieces = (map["pieces"] as? List<Map<String, Any?>>)?.map { Piece.fromMap(it) } ?: emptyList()
        )
    }
}

/**
 * Entity representing an entire Backgammon match session.
 * Fully compatible with Firebase Firestore and the game engine.
 */
data class Match(
    val matchId: String = "",
    val hostPlayer: Player = Player(id = "host", name = "Host", color = "WHITE"),
    val guestPlayer: Player = Player(id = "guest", name = "Guest", color = "BLACK"),
    val board: Board = Board(),
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, ABANDONED
    val mode: String = "ONLINE",     // ONLINE, VS_AI, LOCAL, PRIVATE_ROOM
    val winnerId: String? = null,
    val winnerColor: String? = null,
    val winType: String? = null,
    val pointsAwarded: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMoveAt: Long = System.currentTimeMillis()
) {
    val isFinished: Boolean get() = status == "FINISHED" || board.winner != null

    fun toMap(): Map<String, Any?> = mapOf(
        "matchId" to matchId,
        "hostPlayer" to hostPlayer.toMap(),
        "guestPlayer" to guestPlayer.toMap(),
        "board" to board.toMap(),
        "status" to status,
        "mode" to mode,
        "winnerId" to winnerId,
        "winnerColor" to winnerColor,
        "winType" to winType,
        "pointsAwarded" to pointsAwarded,
        "createdAt" to createdAt,
        "lastMoveAt" to lastMoveAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Match = Match(
            matchId = (map["matchId"] as? String) ?: "",
            hostPlayer = (map["hostPlayer"] as? Map<String, Any?>)?.let { Player.fromMap(it) } ?: Player(),
            guestPlayer = (map["guestPlayer"] as? Map<String, Any?>)?.let { Player.fromMap(it) } ?: Player(),
            board = (map["board"] as? Map<String, Any?>)?.let { Board.fromMap(it) } ?: Board(),
            status = (map["status"] as? String) ?: "WAITING",
            mode = (map["mode"] as? String) ?: "ONLINE",
            winnerId = map["winnerId"] as? String,
            winnerColor = map["winnerColor"] as? String,
            winType = map["winType"] as? String,
            pointsAwarded = ((map["pointsAwarded"] as? Number)?.toInt()) ?: 1,
            createdAt = ((map["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            lastMoveAt = ((map["lastMoveAt"] as? Number)?.toLong()) ?: System.currentTimeMillis()
        )
    }
}

data class Friend(
    val id: String = "",
    val name: String = "",
    val rating: Int = 1200,
    val isOnline: Boolean = true,
    val avatarIndex: Int = 0
)

data class OnlineMatch(
    val matchId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED, ABANDONED
    val currentTurn: String = "WHITE",
    val boardPoints: List<Int> = emptyList(),
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val offWhite: Int = 0,
    val offBlack: Int = 0,
    val dice: List<Int> = emptyList(),
    val lastRollD1: Int = 0,
    val lastRollD2: Int = 0,
    val phase: String = "ROLL_DICE",
    val winnerId: String? = null,
    val lastMoveTimestamp: Long = System.currentTimeMillis()
)

data class PrivateRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val guestId: String? = null,
    val guestName: String? = null,
    val status: String = "WAITING" // WAITING, READY, PLAYING
)
